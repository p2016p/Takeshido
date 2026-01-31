package data.scripts.combat;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;

import data.scripts.ai.TakeshidoRaceShipAI;
import org.magiclib.util.MagicRender;

public class TakeshidoRaceCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(TakeshidoRaceCombatPlugin.class);

    private final String raceHullmodId;
    private final int lapsToWin;
    private final int expectedRacers;
    private final boolean isDeathRace;
    private final String trackId;

    // to figure out what checkpoint we start at
    public int startIndex = 0;

    // Track wall tuning
    private float wallOffsetExtra = 0f;   // set to e.g. 50f later if you want wider lane than checkpointRadius

    // Visual edge markers (non-physics walls)
    private float edgeMarkerSpacing = 200f;     // along the segment
    private float edgeMarkerSize = 90f;         // particle size
    private float edgeMarkerBrightness = 0.85f; // 0..1
    private float edgeMarkerDuration = 0.35f;   // seconds


    // Starting grid tuning
    private int gridColumns = 4;              // keep 4
    private float desiredLaneSpacing = 260f;  // what you want, but will be clamped
    private float rowSpacing = 320f;          // constant is fine
    private float gridEdgeMargin = 80f;       // safety gap from the walls
    private float gridWidthMult = 1.0f;       // adjustable width knob (0.5 = tighter, 1.0 = normal)
    private float aheadOffset = 900f;


    private static final String OFFTRACK_MOD_ID = "takeshido_offtrack";

    // How harsh the penalty is
    private float offTrackMaxSpeedMult = 0.20f;  // 20% top speed when off track
    private float offTrackAccelMult    = 0.35f;  // optional, helps them actually slow down



    private CombatEngineAPI engine;
    private boolean setupComplete = false;

    private RaceState race;

    // track marker timing
    private float markerTimer = 0f;

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.setDoNotEndCombat(true);
        this.race = new RaceState(lapsToWin);
        engine.getCustomData().put("takeshido_race_state", race);
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null || engine.isPaused()) return;

        // Keep attempting setup until all ships are actually present
        if (!setupComplete) {
            setupComplete = trySetup();
            if (!setupComplete) return; // don’t run race logic until setup is real
        }

        if (!isDeathRace) {
            // USE_SYSTEM is a ShipCommand; blocking it stops both AI and player from activating the ship system. :contentReference[oaicite:4]{index=4}
            for (ShipAPI s : race.racers.keySet()) {
                if (s == null || s.isHulk()) continue;
                s.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                s.blockCommandForOneFrame(ShipCommand.FIRE);
            }
        }


        // draw/refresh track markers
        markerTimer += amount;
        if (markerTimer >= 0.25f) {
            markerTimer = 0f;
            spawnTrackMarkers();
        }

        // spawn the edge markers
        spawnTrackEdgeMarkers();

        updateRaceProgress();

        applyOffTrackPenalty();

        ShipAPI player = engine.getPlayerShip();
        if (player != null) {
            RacerState ps = race.racers.get(player);
            if (ps != null) {
                String msg = "Lap " + (ps.lap + 1) + "/" + race.lapsToWin +
                        "   CP " + (ps.nextCheckpoint + 1) + "/" + race.checkpoints.size();
                engine.maintainStatusForPlayerShip(this, null, "Race", msg, false);
            }
        }

        if (race.finished) {
            engine.endCombat(1f, race.winnerSide);
        }
    }

    private boolean trySetup() {
        ShipAPI player = engine.getPlayerShip();
        if (player == null) return false;

        List<ShipAPI> cars = new ArrayList<>();
        for (ShipAPI s : engine.getShips()) {
            if (isRacer(s)) cars.add(s);
        }

        // Wait until all racers exist before doing one-time placement/AI swap
        if (cars.size() < expectedRacers) return false;

        TrackSpec track = data.scripts.combat.TrackLoader.get(trackId);

// If you want per-mission laps to override JSON, keep lapsToWin as-is.
// If you want the track to define laps when mission passes 0, do:
// if (this.lapsToWin <= 0 && track.lapsToWin != null) race.lapsToWin = track.lapsToWin;

        race.checkpoints = track.checkpoints;
        race.checkpointRadius = track.checkpointRadius;
        race.startIndex = ((track.startIndex % race.checkpoints.size()) + race.checkpoints.size()) % race.checkpoints.size();


// Visual corridor width + marker spacing from track
        this.wallOffsetExtra = track.wallOffsetExtra;
        this.edgeMarkerSpacing = track.edgeMarkerSpacing;

// Grid tuning from track
        this.gridColumns = track.grid.columns;
        this.desiredLaneSpacing = track.grid.desiredLaneSpacing;
        this.rowSpacing = track.grid.rowSpacing;
        this.gridEdgeMargin = track.grid.edgeMargin;
        this.gridWidthMult = track.grid.widthMult;
        this.aheadOffset = track.grid.aheadOffset; // you’ll add this as a field and use it in placeStartingGrid



        // Place everyone on the same starting grid near the track edge
        placeStartingGrid(cars);



        // Stop them from shooting if its not a deathrace
        if (!isDeathRace) {
            applyNonDeathRaceLockout(cars);
        }

        // Initialize racer state + AI for non-player racers
        race.racers.clear();
        for (ShipAPI s : cars) {
            RacerState rs = new RacerState();
            race.racers.put(s, rs);

            int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
            rs.nextCheckpoint = firstTarget;

            if (s != player) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            }
        }


        engine.getCombatUI().addMessage(1, "Race start! Complete " + lapsToWin + " laps.");
        return true;
    }

    private void spawnTrackEdgeMarkers() {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return;

        float offset = race.checkpointRadius + wallOffsetExtra;
        int n = race.checkpoints.size();

        for (int i = 0; i < n; i++) {
            Vector2f a = race.checkpoints.get(i);
            Vector2f b = race.checkpoints.get((i + 1) % n);

            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1f) continue;

            // Unit direction along segment
            float ux = dx / len;
            float uy = dy / len;

            // Unit perpendicular (left/right)
            float px = -uy;
            float py = ux;

            // Dots down the segment on both sides
            for (float t = edgeMarkerSpacing; t < len; t += edgeMarkerSpacing) {
                float x = a.x + ux * t;
                float y = a.y + uy * t;

                float lx = x + px * offset;
                float ly = y + py * offset;

                float rx = x - px * offset;
                float ry = y - py * offset;

                // Two edge colors: slightly different alpha so you can “read” the corridor
                engine.addSmoothParticle(
                        new Vector2f(lx, ly),
                        new Vector2f(0f, 0f),
                        edgeMarkerSize,
                        edgeMarkerBrightness,
                        edgeMarkerDuration,
                        new java.awt.Color(160, 170, 180, 170)
                );

                engine.addSmoothParticle(
                        new Vector2f(rx, ry),
                        new Vector2f(0f, 0f),
                        edgeMarkerSize,
                        edgeMarkerBrightness,
                        edgeMarkerDuration,
                        new java.awt.Color(160, 170, 180, 170)
                );
            }
        }
    }


    private void spawnTrackMarkers() {
        if (race.checkpoints == null || race.checkpoints.isEmpty()) return;

        // Mark every other checkpoint to keep it light
        for (int i = 0; i < race.checkpoints.size(); i += 2) {
            Vector2f p = race.checkpoints.get(i);
            engine.addSmoothParticle(
                    new Vector2f(p.x, p.y),
                    new Vector2f(0f, 0f),
                    120f,        // size
                    0.9f,        // brightness
                    0.35f,       // duration
                    new Color(255, 255, 255, 200)
            );
        }

        // Also mark the next checkpoint for the player
        ShipAPI player = engine.getPlayerShip();
        if (player != null) {
            RacerState ps = race.racers.get(player);
            if (ps != null && !race.checkpoints.isEmpty()) {
                Vector2f next = race.checkpoints.get(ps.nextCheckpoint % race.checkpoints.size());
                engine.addSmoothParticle(
                        new Vector2f(next.x, next.y),
                        new Vector2f(0f, 0f),
                        220f,
                        1.0f,
                        0.25f,
                        new Color(255, 240, 120, 220)
                );
            }
        }
    }


    private void applyOffTrackPenalty() {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return;

        float halfWidth = race.checkpointRadius + wallOffsetExtra;

        for (ShipAPI ship : race.racers.keySet()) {
            if (ship == null || ship.isHulk()) continue;

            RacerState rs = race.racers.get(ship);
            if (rs == null || rs.finished) continue;

            // Always clear first just to make sure cuz i had some issue with it before
            ship.getMutableStats().getMaxSpeed().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getAcceleration().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getDeceleration().unmodify(OFFTRACK_MOD_ID);

            float dist = distanceToTrackCenterline(ship.getLocation());
            boolean offTrack = dist > halfWidth;

            if (offTrack) {
                ship.getMutableStats().getMaxSpeed().modifyMult(OFFTRACK_MOD_ID, offTrackMaxSpeedMult);
                ship.getMutableStats().getAcceleration().modifyMult(OFFTRACK_MOD_ID, offTrackAccelMult);
                ship.getMutableStats().getDeceleration().modifyMult(OFFTRACK_MOD_ID, offTrackAccelMult);

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            "Takeshido Offroad",
                            null,
                            "Off track",
                            "Top speed heavily reduced",
                            true
                    );
                }
            }
        }
    }

    private float distanceToTrackCenterline(Vector2f p) {
        int n = race.checkpoints.size();
        float best = Float.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            Vector2f a = race.checkpoints.get(i);
            Vector2f b = race.checkpoints.get((i + 1) % n);
            float d = distancePointToSegment(p, a, b);
            if (d < best) best = d;
        }
        return best;
    }

    private float distancePointToSegment(Vector2f p, Vector2f a, Vector2f b) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;

        float apx = p.x - a.x;
        float apy = p.y - a.y;

        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) {
            // a and b are the same point
            float dx = p.x - a.x;
            float dy = p.y - a.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        float cx = a.x + abx * t;
        float cy = a.y + aby * t;

        float dx = p.x - cx;
        float dy = p.y - cy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }



    private boolean isRacer(ShipAPI s) {
        if (s == null || s.isHulk()) return false;
        try {
            return s.getVariant() != null && s.getVariant().hasHullMod(raceHullmodId);
        } catch (Throwable t) {
            return false;
        }
    }

    private void updateRaceProgress() {
        if (race.checkpoints == null || race.checkpoints.isEmpty()) return;

        ShipAPI player = engine.getPlayerShip();
        if (player != null && player.isHulk()) {
            race.finished = true;
            race.winnerSide = FleetSide.ENEMY;
            return;
        }

        for (Map.Entry<ShipAPI, RacerState> e : race.racers.entrySet()) {
            ShipAPI ship = e.getKey();
            RacerState rs = e.getValue();

            if (rs.finished) continue;
            if (ship == null || ship.isHulk()) continue;

            Vector2f cp = race.checkpoints.get(rs.nextCheckpoint);
            if (distance(ship.getLocation(), cp) <= race.checkpointRadius) {
                rs.nextCheckpoint++;
                if (rs.nextCheckpoint >= race.checkpoints.size()) rs.nextCheckpoint = 0;

                // Lap increments when you pass CP0 (start/finish), i.e. next becomes 1
                int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
                if (rs.nextCheckpoint == firstTarget) {
                    rs.lap++;
                    if (rs.lap >= race.lapsToWin) {
                        rs.finished = true;
                        race.finished = true;
                        race.winnerSide = (ship == engine.getPlayerShip()) ? FleetSide.PLAYER : FleetSide.ENEMY;

                        engine.addFloatingText(
                                new Vector2f(ship.getLocation()),
                                (ship == engine.getPlayerShip() ? "You win!" : "You lost!"),
                                30f, Color.WHITE, ship, 1f, 1f
                        );
                        return;
                    }
                }
            }
        }
    }

    private void applyNonDeathRaceLockout(List<ShipAPI> cars) {
        for (ShipAPI s : cars) {
            if (s == null) continue;
            disableAllWeapons(s);
            forceHoldFire(s);
        }
    }

    private void disableAllWeapons(ShipAPI s) {
        // WeaponAPI.disable() exists and permanently disables that weapon for the battle. :contentReference[oaicite:2]{index=2}
        for (com.fs.starfarer.api.combat.WeaponAPI w : s.getAllWeapons()) {
            if (w != null){
                w.disable(true);
            }
        }
    }

    private void forceHoldFire(ShipAPI s) {
        // In case any weapon slips through (decorative/system weapons, etc.)
        // HOLD_FIRE is a ShipCommand. :contentReference[oaicite:3]{index=3}
        s.blockCommandForOneFrame(com.fs.starfarer.api.combat.ShipCommand.HOLD_FIRE);
    }

    private Vector2f startPoint() {
        if (race.checkpoints == null || race.checkpoints.isEmpty()) return new Vector2f(0f, 0f);
        return race.checkpoints.get(race.startIndex);
    }


    private void placeStartingGrid(List<ShipAPI> cars) {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return;

        int n = race.checkpoints.size();
        int start = ((race.startIndex % n) + n) % n;
        int next = (start + 1) % n;

        Vector2f cpStart = race.checkpoints.get(start);
        Vector2f cpNext  = race.checkpoints.get(next);


        // Travel direction at the start line: start -> next
        float dx = cpNext.x - cpStart.x;
        float dy = cpNext.y - cpStart.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) { dx = 1f; dy = 0f; len = 1f; }
        float dirX = dx / len;
        float dirY = dy / len;

        // Perpendicular for lane offsets
        float perpX = -dirY;
        float perpY = dirX;

        // Anchor: place the front row aheadOffset BEFORE the first target (cpNext),
        // on the centerline of the start segment.
        Vector2f anchor = new Vector2f(
                cpNext.x - dirX * aheadOffset,
                cpNext.y - dirY * aheadOffset
        );

        // Corridor half-width (visual edges and offtrack math use the same concept)
        float halfWidth = race.checkpointRadius + wallOffsetExtra;
        float safeHalfWidth = Math.max(0f, halfWidth - gridEdgeMargin);

        // Clamp lane spacing so outer columns remain within the corridor
        float cols = Math.max(2, gridColumns);
        float maxLaneSpacing = (2f * safeHalfWidth) / (cols - 1f);

        float laneSpacing = desiredLaneSpacing * gridWidthMult;
        laneSpacing = Math.min(laneSpacing, maxLaneSpacing);
        laneSpacing = Math.max(80f, laneSpacing); // prevent collapsing into a stack

        float facing = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        for (int i = 0; i < cars.size(); i++) {
            ShipAPI s = cars.get(i);
            if (s == null) continue;

            int row = i / gridColumns;
            int col = i % gridColumns;

            float centerCol = (gridColumns - 1) / 2f;
            float colOffset = (col - centerCol) * laneSpacing;

            // Rows extend backward along -dir, columns spread along perp
            Vector2f pos = new Vector2f(
                    anchor.x - dirX * (row * rowSpacing) + perpX * colOffset,
                    anchor.y - dirY * (row * rowSpacing) + perpY * colOffset
            );

            s.getLocation().set(pos);
            s.getVelocity().set(0f, 0f);
            s.setFacing(facing);
            s.setAngularVelocity(0f);
        }
    }





    private float distance(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static class RaceState {
        public int lapsToWin;
        public float checkpointRadius = 350f;
        public List<Vector2f> checkpoints = new ArrayList<>();
        public Map<ShipAPI, RacerState> racers = new LinkedHashMap<>();
        public boolean finished = false;
        public FleetSide winnerSide = FleetSide.PLAYER;
        public int startIndex;

        public RaceState(int lapsToWin) {
            this.lapsToWin = lapsToWin;
        }
    }

    public static class RacerState {
        public int lap = 0;
        public int nextCheckpoint = 1;
        public boolean finished = false;
    }
}

