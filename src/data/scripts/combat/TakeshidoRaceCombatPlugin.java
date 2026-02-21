package data.scripts.combat;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;

import data.scripts.ai.TakeshidoRaceShipAI;
import data.scripts.racing.TakeshidoRacingManager;

public class TakeshidoRaceCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(TakeshidoRaceCombatPlugin.class);

    private final String raceHullmodId;
    private final int lapsToWin;
    private final int expectedRacers;
    private final boolean isDeathRace;
    private final String trackId;
    private final Map<String, Float> skillOverrides;
    private final String raceId;

    // to figure out what checkpoint we start at
    public int startIndex = 0;

    // Wall asteroid tuning
    private float wallAsteroidSpacing = 180f;
    private int wallAsteroidType = 0;
    private float wallAsteroidTrackBuffer = 40f;


    // Starting grid tuning
    private int gridColumns = 4;              // keep 4
    private float rowSpacing = 320f;          // constant is fine
    private float gridEdgeMargin = 80f;       // safety gap from the walls
    private float aheadOffset = 900f;


    private static final String OFFTRACK_MOD_ID = "takeshido_offtrack";
    private static final String START_LOCK_ID = "takeshido_start_lock";
    private static final float WALL_PUSH_PER_SEC = 260f;
    private static final float WALL_PUSH_MULT = 1.0f;
    private static final float WALL_LATERAL_DAMP = 0.7f;
    private static final float CAR_REPEL_BUFFER = 8f;
    private static final float CAR_REPEL_PER_SEC = 120f;
    private static final float MAX_RACE_SECONDS = 600f;
    private static final String RACE_MUSIC_FORCE_KEY = "takeshido_race_music_id";
    private static final List<String> RACE_MUSIC_IDS = Arrays.asList(
            "takeshido_race_kissyougoodbye",
            "takeshido_race_driveyoucrazy",
            "takeshido_race_freedomtowin",
            "takeshido_race_getcloser",
            "takeshido_race_greenmonster"
    );

    // How harsh the penalty is
    private float offTrackMaxSpeedMult = 0.20f;  // 20% top speed when off track
    private float offTrackAccelMult    = 0.35f;  // optional, helps them actually slow down



    private CombatEngineAPI engine;
    private boolean setupComplete = false;

    private RaceState race;

    // track marker timing
    private float markerTimer = 0f;
    private boolean countdownActive = false;
    private float countdownTimer = 0f;
    private float countdownDelayTimer = 0f;
    private int lastCountdownSecond = -1;
    private boolean countdownSoundPlayed = false;
    private boolean enginesSuppressed = false;
    private boolean raceMusicStarted = false;
    private boolean raceFinishPlayed = false;
    private boolean postCountdownAIEnsured = false;
    private boolean resultsStored = false;
    private float raceTimer = 0f;
    private String currentRaceMusicId = null;
    private final Random raceMusicRandom = new Random();

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = null;
        this.raceId = null;
    }

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId,
                                     Map<String, Float> skillOverrides) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = skillOverrides;
        this.raceId = null;
    }

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId,
                                     Map<String, Float> skillOverrides, String raceId) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = skillOverrides;
        this.raceId = raceId;
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
            if (!setupComplete) return; // don't run race logic until setup is real
        }

        if (!isDeathRace) {
            // USE_SYSTEM is a ShipCommand; blocking it stops both AI and player from activating the ship system.
            for (ShipAPI s : race.racers.keySet()) {
                if (s == null || s.isHulk()) continue;
                s.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                s.blockCommandForOneFrame(ShipCommand.FIRE);
            }
        }

        if (countdownActive) {
            // Track markers disabled
//            markerTimer += amount;
//            if (markerTimer >= 0.25f) {
//                markerTimer = 0f;
//                // spawnTrackMarkers(); // disabled: track markers no longer needed
//            }

            // keep wall asteroids in place
            maintainWallAsteroids();

            updateCountdown(amount);
            return;
        }


        // Track markers disabled
//        markerTimer += amount;
//        if (markerTimer >= 0.25f) {
//            markerTimer = 0f;
//            // spawnTrackMarkers(); // disabled: track markers no longer needed
//        }

        // keep wall asteroids in place
        maintainWallAsteroids();

        updateRaceProgress();

        raceTimer += amount;
        if (!race.finished && raceTimer >= MAX_RACE_SECONDS) {
            endRaceByTimeout();
        }

        applyOffTrackPenalty();
        applySoftWall(amount);
        // applySoftCarRepulsion(amount); // disabled: allow normal ship-ship collisions

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
            finalizeFinishOrder();
            storeResultsIfNeeded();
            playRaceFinish();
            engine.endCombat(1f, race.winnerSide);
        } else if (raceMusicStarted && !raceFinishPlayed) {
            ensureRaceMusicLoop();
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

        this.wallAsteroidSpacing = track.wallAsteroidSpacing;
        this.wallAsteroidType = track.wallAsteroidType;
        this.wallAsteroidTrackBuffer = track.wallAsteroidTrackBuffer;

        race.checkpoints = track.checkpoints;
        race.checkpointRadius = track.checkpointRadius;

        if (track.centerline != null && !track.centerline.isEmpty()) {
            race.centerline = track.centerline;
            race.wLeft = track.wLeft;
            race.wRight = track.wRight;
            race.useWidths = track.useCsvWidths && track.wLeft.size() == track.centerline.size() && track.wRight.size() == track.centerline.size();
        } else {
            // fallback: treat checkpoints as centerline
            race.centerline = race.checkpoints;
            race.useWidths = false;
        }
        if (track.raceline != null) {
            race.raceline = new ArrayList<Vector2f>(track.raceline);
            race.racelineS = new ArrayList<Float>(track.racelineS);
            race.racelinePsi = new ArrayList<Float>(track.racelinePsi);
            race.racelineKappa = new ArrayList<Float>(track.racelineKappa);
            race.racelineVx = new ArrayList<Float>(track.racelineVx);
        } else {
            race.raceline = new ArrayList<Vector2f>();
            race.racelineS = new ArrayList<Float>();
            race.racelinePsi = new ArrayList<Float>();
            race.racelineKappa = new ArrayList<Float>();
            race.racelineVx = new ArrayList<Float>();
        }
        race.unitsPerMeter = track.unitsPerMeter;

// If you're not using CSV widths, keep your existing corridor definition:
        race.corridorHalfWidth = race.checkpointRadius;


// If you want per-mission laps to override JSON, keep lapsToWin as-is.
// If you want the track to define laps when mission passes 0, do:
// if (this.lapsToWin <= 0 && track.lapsToWin != null) race.lapsToWin = track.lapsToWin;

        race.startIndex = ((track.startIndex % race.checkpoints.size()) + race.checkpoints.size()) % race.checkpoints.size();


        // Grid tuning from track
        this.gridColumns = track.grid.columns;
        this.rowSpacing = track.grid.rowSpacing;
        this.gridEdgeMargin = track.grid.edgeMargin;
        this.aheadOffset = track.grid.aheadOffset;

        race.gates = buildGates();
        race.wallAsteroids = buildWallAsteroids();



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
            rs.skill = getSkillForShip(s);
            race.racers.put(s, rs);

            int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
            rs.nextCheckpoint = firstTarget;
            rs.lastPos = new Vector2f(s.getLocation());

            if (s != player) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            }
        }


        startCountdown();
        return true;
    }


    private List<GateSpec> buildGates() {
        List<GateSpec> gates = new ArrayList<>();
        if (race.centerline == null || race.centerline.size() < 2 || race.checkpoints == null) return gates;

        for (Vector2f cp : race.checkpoints) {
            ClosestPoint closest = findClosestPointOnCenterline(cp);
            if (closest == null) continue;

            Vector2f a = race.centerline.get(closest.segIndex);
            Vector2f b = race.centerline.get((closest.segIndex + 1) % race.centerline.size());

            Vector2f tangent = unit(b.x - a.x, b.y - a.y);
            Vector2f normal = new Vector2f(-tangent.y, tangent.x);

            float wl = getWallLeftWidth(closest.segIndex, closest.t);
            float wr = getWallRightWidth(closest.segIndex, closest.t);

            gates.add(new GateSpec(new Vector2f(closest.point), tangent, normal, wl, wr));
        }

        return gates;
    }

    private List<WallAsteroidSpec> buildWallAsteroids() {
        List<WallAsteroidSpec> out = new ArrayList<>();
        if (race.centerline == null || race.centerline.size() < 2) return out;

        float spacing = Math.max(40f, wallAsteroidSpacing);
        float inset = 0f;

        List<Vector2f> leftEdge = buildOffsetPolyline(true, inset);
        List<Vector2f> rightEdge = buildOffsetPolyline(false, inset);

        float effectiveSpacing = spacing;

        for (Vector2f p : samplePolylineAtSpacing(leftEdge, effectiveSpacing)) {
            Vector2f pos = pushOutOfTrackAgainstCenterline(p);
            out.add(spawnWallAsteroid(pos));
        }
        for (Vector2f p : samplePolylineAtSpacing(rightEdge, effectiveSpacing)) {
            Vector2f pos = pushOutOfTrackAgainstCenterline(p);
            out.add(spawnWallAsteroid(pos));
        }

        return out;
    }

    private WallAsteroidSpec spawnWallAsteroid(Vector2f location) {
        WallAsteroidSpec spec = new WallAsteroidSpec(location, wallAsteroidType);
        if (engine == null) return spec;

        CombatEntityAPI entity = engine.spawnAsteroid(spec.type, location.x, location.y, 0f, 0f);
        if (entity != null) {
            entity.setCollisionClass(CollisionClass.NONE);
            entity.setHitpoints(1000000f);
            if (entity.getVelocity() != null) {
                entity.getVelocity().set(0f, 0f);
            }
            entity.setAngularVelocity(0f);
        }
        spec.entity = entity;
        return spec;
    }

    private void maintainWallAsteroids() {
        if (race.wallAsteroids == null || race.wallAsteroids.isEmpty() || engine == null) return;

        for (WallAsteroidSpec spec : race.wallAsteroids) {
            if (spec == null) continue;
            if (spec.entity == null || !engine.isEntityInPlay(spec.entity)) {
                spec.entity = engine.spawnAsteroid(spec.type, spec.location.x, spec.location.y, 0f, 0f);
                if (spec.entity != null) {
                    spec.entity.setCollisionClass(CollisionClass.NONE);
                    spec.entity.setHitpoints(1000000f);
                }
            }
            if (spec.entity != null) {
                spec.entity.getLocation().set(spec.location);
                if (spec.entity.getVelocity() != null) {
                    spec.entity.getVelocity().set(0f, 0f);
                }
                spec.entity.setAngularVelocity(0f);
                spec.entity.setHitpoints(1000000f);
            }
        }
    }

    private float getLeftWidth(int segIndex, float tSeg) {
        if (!race.useWidths || race.wLeft.size() < race.centerline.size()) return race.corridorHalfWidth;
        int j = (segIndex + 1) % race.centerline.size();
        float wlA = race.wLeft.get(segIndex);
        float wlB = race.wLeft.get(j);
        return (wlA + (wlB - wlA) * tSeg);
    }

    private float getRightWidth(int segIndex, float tSeg) {
        if (!race.useWidths || race.wRight.size() < race.centerline.size()) return race.corridorHalfWidth;
        int j = (segIndex + 1) % race.centerline.size();
        float wrA = race.wRight.get(segIndex);
        float wrB = race.wRight.get(j);
        return (wrA + (wrB - wrA) * tSeg);
    }

    private float getWallLeftWidth(int segIndex, float tSeg) {
        return getLeftWidth(segIndex, tSeg) + wallAsteroidTrackBuffer;
    }

    private float getWallRightWidth(int segIndex, float tSeg) {
        return getRightWidth(segIndex, tSeg) + wallAsteroidTrackBuffer;
    }

    private ClosestPoint findClosestPointOnCenterline(Vector2f p) {
        if (race.centerline == null || race.centerline.size() < 2) return null;

        float best = Float.MAX_VALUE;
        ClosestPoint bestPoint = null;

        for (int i = 0; i < race.centerline.size(); i++) {
            Vector2f a = race.centerline.get(i);
            Vector2f b = race.centerline.get((i + 1) % race.centerline.size());
            ClosestPoint cp = closestPointOnSegment(p, a, b, i);
            if (cp != null && cp.distSq < best) {
                best = cp.distSq;
                bestPoint = cp;
            }
        }

        return bestPoint;
    }

    private ClosestPoint closestPointOnSegment(Vector2f p, Vector2f a, Vector2f b, int segIndex) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) return null;

        float apx = p.x - a.x;
        float apy = p.y - a.y;
        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        Vector2f point = new Vector2f(a.x + abx * t, a.y + aby * t);
        float dx = p.x - point.x;
        float dy = p.y - point.y;
        float distSq = dx * dx + dy * dy;

        return new ClosestPoint(segIndex, t, point, distSq);
    }

    private Vector2f pushOutOfTrackAgainstCenterline(Vector2f pos) {
        ClosestPoint cp = findClosestPointOnCenterline(pos);
        if (cp == null) return pos;

        Vector2f a = race.centerline.get(cp.segIndex);
        Vector2f b = race.centerline.get((cp.segIndex + 1) % race.centerline.size());
        Vector2f tangent = unit(b.x - a.x, b.y - a.y);
        Vector2f normal = new Vector2f(-tangent.y, tangent.x);

        float wl = getLeftWidth(cp.segIndex, cp.t);
        float wr = getRightWidth(cp.segIndex, cp.t);

        float dx = pos.x - cp.point.x;
        float dy = pos.y - cp.point.y;
        float side = dx * normal.x + dy * normal.y;

        float desired = (side >= 0f ? wl : wr) + Math.max(0f, wallAsteroidTrackBuffer);
        float deficit = desired - Math.abs(side);
        if (deficit > 0f) {
            float sign = side >= 0f ? 1f : -1f;
            pos.x += normal.x * deficit * sign;
            pos.y += normal.y * deficit * sign;
        }

        return pos;
    }

    private List<Vector2f> buildOffsetPolyline(boolean leftSide, float inset) {
        List<Vector2f> out = new ArrayList<>();
        int n = race.centerline.size();
        if (n < 2) return out;

        for (int i = 0; i < n; i++) {
            Vector2f prev = race.centerline.get((i - 1 + n) % n);
            Vector2f cur = race.centerline.get(i);
            Vector2f next = race.centerline.get((i + 1) % n);

            Vector2f t1 = unit(cur.x - prev.x, cur.y - prev.y);
            Vector2f t2 = unit(next.x - cur.x, next.y - cur.y);
            Vector2f n1 = new Vector2f(-t1.y, t1.x);
            Vector2f n2 = new Vector2f(-t2.y, t2.x);

            Vector2f miter = unit(n1.x + n2.x, n1.y + n2.y);
            float dot = miter.x * n1.x + miter.y * n1.y;
            if (Math.abs(dot) < 0.1f) {
                miter = n1;
                dot = 1f;
            }

            float width = leftSide ? getLeftWidth(i, 0f) : getRightWidth(i, 0f);
            float dist = width + wallAsteroidTrackBuffer + inset;
            float miterScale = dist / dot;
            float maxMiter = dist * 2f;
            if (miterScale > maxMiter) miterScale = maxMiter;
            if (miterScale < -maxMiter) miterScale = -maxMiter;

            Vector2f offset = new Vector2f(
                    cur.x + miter.x * miterScale * (leftSide ? 1f : -1f),
                    cur.y + miter.y * miterScale * (leftSide ? 1f : -1f)
            );

            out.add(offset);
        }

        return out;
    }

    private List<Vector2f> samplePolylineAtSpacing(List<Vector2f> line, float spacing) {
        List<Vector2f> samples = new ArrayList<>();
        if (line == null || line.size() < 2) return samples;

        float totalLen = 0f;
        for (int i = 0; i < line.size(); i++) {
            Vector2f a = line.get(i);
            Vector2f b = line.get((i + 1) % line.size());
            totalLen += distance(a, b);
        }
        if (totalLen <= 0.001f) return samples;

        int steps = Math.max(1, Math.round(totalLen / spacing));
        float stepLen = totalLen / steps;

        float acc = 0f;
        Vector2f prev = new Vector2f(line.get(0));
        samples.add(new Vector2f(prev));

        for (int i = 0; i < line.size(); i++) {
            Vector2f cur = line.get((i + 1) % line.size());
            float seg = distance(prev, cur);
            float remaining = seg;
            Vector2f start = new Vector2f(prev);

            while (acc + remaining >= stepLen) {
                float step = stepLen - acc;
                float f = (seg - remaining + step) / seg;
                Vector2f p = new Vector2f(
                        start.x + (cur.x - start.x) * f,
                        start.y + (cur.y - start.y) * f
                );
                samples.add(p);
                remaining -= step;
                acc = 0f;
            }

            acc += remaining;
            prev = cur;
        }

        return samples;
    }

    private Vector2f unit(float x, float y) {
        float len = (float) Math.sqrt(x * x + y * y);
        if (len < 0.0001f) return new Vector2f(1f, 0f);
        return new Vector2f(x / len, y / len);
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

        for (ShipAPI ship : race.racers.keySet()) {
            if (ship == null || ship.isHulk()) continue;

            RacerState rs = race.racers.get(ship);
            if (rs == null || rs.finished) continue;

            // Always clear first just to make sure cuz i had some issue with it before
            ship.getMutableStats().getMaxSpeed().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getAcceleration().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getDeceleration().unmodify(OFFTRACK_MOD_ID);

            float distSq = distanceToTrackCenterlineSq(ship.getLocation());
            ClosestPoint cp = findClosestPointOnCenterline(ship.getLocation());
            boolean offTrack = true;

            if (cp != null) {
                Vector2f a = race.centerline.get(cp.segIndex);
                Vector2f b = race.centerline.get((cp.segIndex + 1) % race.centerline.size());
                Vector2f tangent = unit(b.x - a.x, b.y - a.y);
                Vector2f normal = new Vector2f(-tangent.y, tangent.x);

                float wl = getWallLeftWidth(cp.segIndex, cp.t);
                float wr = getWallRightWidth(cp.segIndex, cp.t);

                float dx = ship.getLocation().x - cp.point.x;
                float dy = ship.getLocation().y - cp.point.y;
                float side = dx * normal.x + dy * normal.y;
                float allowed = side >= 0f ? wl : wr;

                offTrack = Math.abs(side) > allowed;
            } else {
                float halfWidthSq = race.corridorHalfWidth * race.corridorHalfWidth;
                offTrack = distSq > halfWidthSq;
            }


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

    private float distanceToTrackCenterlineSq(Vector2f p) {
        int n = race.centerline.size();
        float best = Float.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            Vector2f a = race.centerline.get(i);
            Vector2f b = race.centerline.get((i + 1) % n);
            float d2 = distancePointToSegmentSq(p, a, b);
            if (d2 < best) best = d2;
        }
        return best;
    }

    private float distancePointToSegmentSq(Vector2f p, Vector2f a, Vector2f b) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;

        float apx = p.x - a.x;
        float apy = p.y - a.y;

        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) {
            float dx = p.x - a.x;
            float dy = p.y - a.y;
            return dx * dx + dy * dy;
        }

        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        float cx = a.x + abx * t;
        float cy = a.y + aby * t;

        float dx = p.x - cx;
        float dy = p.y - cy;
        return dx * dx + dy * dy;
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
            race.endedByPlayerDNF = true;
            race.winnerSide = FleetSide.ENEMY;
            recordDNF(player);
            race.finished = true;
            return;
        }

        for (Map.Entry<ShipAPI, RacerState> e : race.racers.entrySet()) {
            ShipAPI ship = e.getKey();
            RacerState rs = e.getValue();

            if (rs.finished) continue;
            if (ship == null) continue;
            if (ship.isHulk()) {
                rs.finished = true;
                recordDNF(ship);
                if (race.finishOrderMemberIds.size() >= race.racers.size()) {
                    race.finished = true;
                }
                continue;
            }

            boolean crossed = false;
            GateSpec gate = (race.gates != null && race.gates.size() == race.checkpoints.size())
                    ? race.gates.get(rs.nextCheckpoint)
                    : null;

            if (gate != null && rs.lastPos != null) {
                Vector2f cur = ship.getLocation();
                float prevS = dotDelta(rs.lastPos, gate.center, gate.tangent);
                float curS = dotDelta(cur, gate.center, gate.tangent);

                if (prevS < 0f && curS >= 0f) {
                    float lat = dotDelta(cur, gate.center, gate.normal);
                    float pad = Math.max(20f, ship.getCollisionRadius() * 0.25f);
                    if (lat <= gate.wLeft + pad && lat >= -gate.wRight - pad) {
                        crossed = true;
                    }
                }
            } else {
                Vector2f cp = race.checkpoints.get(rs.nextCheckpoint);
                if (distance(ship.getLocation(), cp) <= race.checkpointRadius) {
                    crossed = true;
                }
            }

            if (crossed) {
                rs.nextCheckpoint++;
                if (rs.nextCheckpoint >= race.checkpoints.size()) rs.nextCheckpoint = 0;

                // Lap increments when you pass CP0 (start/finish), i.e. next becomes 1
                int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
                if (rs.nextCheckpoint == firstTarget) {
                    rs.lap++;
                    if (rs.lap >= race.lapsToWin) {
                        rs.finished = true;
                        recordFinish(ship);

                        if (race.finishOrderMemberIds.size() == 1) {
                            race.winnerSide = (ship == engine.getPlayerShip()) ? FleetSide.PLAYER : FleetSide.ENEMY;
                            engine.addFloatingText(
                                    new Vector2f(ship.getLocation()),
                                    (ship == engine.getPlayerShip() ? "You win!" : "You lost!"),
                                    30f, Color.WHITE, ship, 1f, 1f
                            );
                        }

                        if (ship == engine.getPlayerShip()) {
                            race.finished = true;
                        }

                        if (race.finishOrderMemberIds.size() >= race.racers.size()) {
                            race.finished = true;
                        }
                    }
                }
            }

            if (rs.lastPos == null) {
                rs.lastPos = new Vector2f(ship.getLocation());
            } else {
                rs.lastPos.set(ship.getLocation());
            }
        }
    }

    private void recordFinish(ShipAPI ship) {
        if (ship == null) return;
        if (ship.getFleetMember() == null) return;
        String id = ship.getFleetMember().getId();
        if (id == null) return;
        if (!race.finishOrderMemberIds.contains(id)) {
            race.finishOrderMemberIds.add(id);
        }
    }

    private void recordDNF(ShipAPI ship) {
        if (ship == null) return;
        if (ship.getFleetMember() == null) return;
        String id = ship.getFleetMember().getId();
        if (id == null) return;
        if (!race.finishOrderMemberIds.contains(id)) {
            race.finishOrderMemberIds.add(id);
        }
        if (!race.dnfMemberIds.contains(id)) {
            race.dnfMemberIds.add(id);
        }
    }

    private void endRaceByTimeout() {
        if (race.finished) return;
        race.endedByTimeout = true;
        race.finished = true;
        if (!race.finishOrderMemberIds.isEmpty()) {
            String first = race.finishOrderMemberIds.get(0);
            ShipAPI player = engine.getPlayerShip();
            if (player != null && player.getFleetMember() != null) {
                race.winnerSide = first.equals(player.getFleetMember().getId()) ? FleetSide.PLAYER : FleetSide.ENEMY;
            }
        }
    }

    private void finalizeFinishOrder() {
        if (race.racers == null || race.racers.isEmpty()) return;

        List<ShipAPI> remaining = new ArrayList<>();
        for (Map.Entry<ShipAPI, RacerState> e : race.racers.entrySet()) {
            ShipAPI ship = e.getKey();
            if (ship == null || ship.getFleetMember() == null) continue;
            String id = ship.getFleetMember().getId();
            if (id == null) continue;
            if (!race.finishOrderMemberIds.contains(id)) {
                remaining.add(ship);
            }
        }

        if (!remaining.isEmpty()) {
            Collections.sort(remaining, new Comparator<ShipAPI>() {
                @Override
                public int compare(ShipAPI a, ShipAPI b) {
                    RacerState ra = race.racers.get(a);
                    RacerState rb = race.racers.get(b);
                    float pa = getProgressScore(a, ra);
                    float pb = getProgressScore(b, rb);
                    return Float.compare(pb, pa);
                }
            });

            for (ShipAPI ship : remaining) {
                recordFinish(ship);
            }
        }

        if (!race.finishOrderMemberIds.isEmpty()) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null && player.getFleetMember() != null) {
                String first = race.finishOrderMemberIds.get(0);
                race.winnerSide = first.equals(player.getFleetMember().getId()) ? FleetSide.PLAYER : FleetSide.ENEMY;
            }
        }
    }

    private float getProgressScore(ShipAPI ship, RacerState rs) {
        if (ship == null || rs == null || race.checkpoints == null) return 0f;
        int cpCount = race.checkpoints.size();
        int lastCheckpoint = rs.nextCheckpoint - 1;
        if (lastCheckpoint < 0) lastCheckpoint = cpCount - 1;
        float progress = rs.lap * cpCount + lastCheckpoint;
        return progress;
    }

    private void storeResultsIfNeeded() {
        if (resultsStored) return;
        if (raceId == null || raceId.trim().isEmpty()) return;
        resultsStored = true;
        TakeshidoRacingManager.storeRaceResult(raceId, race.finishOrderMemberIds, race.dnfMemberIds, race.endedByTimeout, race.endedByPlayerDNF);
        TakeshidoRacingManager.processRaceResult(raceId);
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

    private void startCountdown() {
        countdownActive = true;
        countdownDelayTimer = 3f;
        countdownTimer = 3f;
        lastCountdownSecond = -1;
        countdownSoundPlayed = false;
        enginesSuppressed = false;
        raceMusicStarted = false;
        raceTimer = 0f;
        resultsStored = false;
        applyStartLockout();
    }

    private void updateCountdown(float amount) {
        if (!countdownActive) return;

        applyStartLockout();
        if (countdownDelayTimer > 0f) {
            countdownDelayTimer -= amount;
            if (!enginesSuppressed) {
                setEngineFlameLevel(0f);
                enginesSuppressed = true;
            }
            return;
        }

        if (!countdownSoundPlayed && Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playCustomMusic(0, 0, "takeshido_countdown", false);
            countdownSoundPlayed = true;
        }

        if (enginesSuppressed) {
            setEngineFlameLevel(1f);
            enginesSuppressed = false;
        }

        countdownTimer -= amount;

        int currentSecond = (int) Math.ceil(Math.max(0f, countdownTimer));
        if (currentSecond != lastCountdownSecond) {
            lastCountdownSecond = currentSecond;
            String text = currentSecond > 0 ? String.valueOf(currentSecond) : "GO!";
            Vector2f center = engine.getViewport().getCenter();
            engine.addFloatingText(new Vector2f(center.x, center.y), text, 160f, Color.WHITE, null, 0f, 0f);
        }

        if (countdownTimer <= 0f) {
            countdownActive = false;
            releaseStartLockout();
            ensureRaceAI();
            startRaceMusic();
            engine.getCombatUI().addMessage(1, "Race start! Complete " + lapsToWin + " laps.");
        }
    }

    private void ensureRaceAI() {
        if (postCountdownAIEnsured || engine == null) return;
        postCountdownAIEnsured = true;

        ShipAPI player = engine.getPlayerShip();
        int firstTarget = (race.startIndex + 1) % race.checkpoints.size();

        for (ShipAPI s : engine.getShips()) {
            if (!isRacer(s)) continue;

            RacerState rs = race.racers.get(s);
            if (rs == null) {
                rs = new RacerState();
                rs.skill = getSkillForShip(s);
                rs.nextCheckpoint = firstTarget;
                rs.lastPos = new Vector2f(s.getLocation());
                race.racers.put(s, rs);
            }

            if (s != player) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            }
        }
    }

    private void startRaceMusic() {
        if (raceMusicStarted) return;
        raceMusicStarted = true;
        Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
        currentRaceMusicId = pickRaceMusicId(null);
        Global.getSoundPlayer().playCustomMusic(0, 0, currentRaceMusicId, false);
    }

    private void playRaceFinish() {
        if (raceFinishPlayed || Global.getSoundPlayer() == null) return;
        raceFinishPlayed = true;
        Global.getSoundPlayer().pauseCustomMusic();
        Global.getSoundPlayer().playCustomMusic(0, 0, "takeshido_race_finish", false);
    }

    private void ensureRaceMusicLoop() {
        if (engine == null || Global.getSoundPlayer() == null) return;
        String playing = getCurrentMusicIdSafe();
        if (playing != null) return;

        String next = pickRaceMusicId(currentRaceMusicId);
        currentRaceMusicId = next;
        Global.getSoundPlayer().playCustomMusic(0, 0, currentRaceMusicId, false);
    }

    private String pickRaceMusicId(String exclude) {
        String forced = getForcedRaceMusicId();
        if (forced != null) return forced;
        if (RACE_MUSIC_IDS.isEmpty()) return "takeshido_race";

        if (exclude == null || RACE_MUSIC_IDS.size() == 1) {
            return RACE_MUSIC_IDS.get(raceMusicRandom.nextInt(RACE_MUSIC_IDS.size()));
        }

        String pick = exclude;
        int guard = 0;
        while (pick.equals(exclude) && guard < 10) {
            pick = RACE_MUSIC_IDS.get(raceMusicRandom.nextInt(RACE_MUSIC_IDS.size()));
            guard++;
        }
        return pick;
    }

    private String getForcedRaceMusicId() {
        if (engine == null || engine.getCustomData() == null) return null;
        Object val = engine.getCustomData().get(RACE_MUSIC_FORCE_KEY);
        if (!(val instanceof String)) return null;
        String id = ((String) val).trim();
        return id.isEmpty() ? null : id;
    }

    private String getCurrentMusicIdSafe() {
        if (Global.getSoundPlayer() == null) return null;
        String id = Global.getSoundPlayer().getCurrentMusicId();
        if (id == null) return null;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return null;
        if ("nothing".equalsIgnoreCase(trimmed)) return null;
        return id;
    }

    private void applySoftWall(float amount) {
        if (race.centerline == null || race.centerline.size() < 2) return;

        for (ShipAPI ship : race.racers.keySet()) {
            if (ship == null || ship.isHulk()) continue;

            ClosestPoint cp = findClosestPointOnCenterline(ship.getLocation());
            if (cp == null) continue;

            Vector2f a = race.centerline.get(cp.segIndex);
            Vector2f b = race.centerline.get((cp.segIndex + 1) % race.centerline.size());
            Vector2f tangent = unit(b.x - a.x, b.y - a.y);
            Vector2f normal = new Vector2f(-tangent.y, tangent.x);

            float wl = getWallLeftWidth(cp.segIndex, cp.t);
            float wr = getWallRightWidth(cp.segIndex, cp.t);

            float dx = ship.getLocation().x - cp.point.x;
            float dy = ship.getLocation().y - cp.point.y;
            float side = dx * normal.x + dy * normal.y;
            float allowed = side >= 0f ? wl : wr;
            float penetration = Math.abs(side) - allowed;

            if (penetration <= 0f) continue;

            float sign = side >= 0f ? 1f : -1f;
            float push = Math.min(penetration * WALL_PUSH_MULT, WALL_PUSH_PER_SEC * amount);

            ship.getLocation().x -= normal.x * sign * push;
            ship.getLocation().y -= normal.y * sign * push;

            Vector2f vel = ship.getVelocity();
            if (vel != null) {
                float lateral = vel.x * normal.x + vel.y * normal.y;
                if (lateral * sign > 0f) {
                    vel.x -= normal.x * lateral * WALL_LATERAL_DAMP;
                    vel.y -= normal.y * lateral * WALL_LATERAL_DAMP;
                }
            }

            float face = ship.getFacing();
            float desired = (float) Math.toDegrees(Math.atan2(tangent.y, tangent.x));
            float delta = shortestRotation(face, desired);
            ship.setFacing(face + delta * 0.15f);
            ship.setAngularVelocity(ship.getAngularVelocity() * 0.5f);
        }
    }

    private void applySoftCarRepulsion(float amount) {
        if (amount <= 0f) return;
        if (race.racers == null || race.racers.isEmpty()) return;

        List<ShipAPI> cars = new ArrayList<>(race.racers.keySet());
        int n = cars.size();
        for (int i = 0; i < n; i++) {
            ShipAPI a = cars.get(i);
            if (a == null || a.isHulk()) continue;
            a.setCollisionClass(CollisionClass.NONE);
            for (int j = i + 1; j < n; j++) {
                ShipAPI b = cars.get(j);
                if (b == null || b.isHulk()) continue;
                b.setCollisionClass(CollisionClass.NONE);

                float dx = b.getLocation().x - a.getLocation().x;
                float dy = b.getLocation().y - a.getLocation().y;
                float r = a.getCollisionRadius() + b.getCollisionRadius() + CAR_REPEL_BUFFER;
                float distSq = dx * dx + dy * dy;
                if (distSq <= 0.0001f || distSq > r * r) continue;

                float dist = (float) Math.sqrt(distSq);
                float nx = dx / dist;
                float ny = dy / dist;
                float penetration = r - dist;
                float push = Math.min(penetration * 0.5f, CAR_REPEL_PER_SEC * amount);

                if (a.getVelocity() != null) {
                    a.getVelocity().x -= nx * push;
                    a.getVelocity().y -= ny * push;
                }
                if (b.getVelocity() != null) {
                    b.getVelocity().x += nx * push;
                    b.getVelocity().y += ny * push;
                }
            }
        }
    }

    private float shortestRotation(float fromDeg, float toDeg) {
        float a = normalizeAngle(toDeg) - normalizeAngle(fromDeg);
        if (a > 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    private float normalizeAngle(float a) {
        while (a < 0f) a += 360f;
        while (a >= 360f) a -= 360f;
        return a;
    }

    private void applyStartLockout() {
        float face = startFacing();
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            s.getVelocity().set(0f, 0f);
            s.setAngularVelocity(0f);
            s.setFacing(face);
            s.getMutableStats().getMaxSpeed().modifyMult(START_LOCK_ID, 0f);
            s.getMutableStats().getAcceleration().modifyMult(START_LOCK_ID, 0f);
            s.getMutableStats().getDeceleration().modifyMult(START_LOCK_ID, 0f);
        }
    }

    private void releaseStartLockout() {
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            s.getMutableStats().getMaxSpeed().unmodify(START_LOCK_ID);
            s.getMutableStats().getAcceleration().unmodify(START_LOCK_ID);
            s.getMutableStats().getDeceleration().unmodify(START_LOCK_ID);
        }
    }


    private void setEngineFlameLevel(float level) {
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            if (s.getEngineController() == null) continue;
            for (com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI e : s.getEngineController().getShipEngines()) {
                if (e == null) continue;
                s.getEngineController().setFlameLevel(e.getEngineSlot(), level);
            }
        }
    }

    private float startFacing() {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return 0f;
        int n = race.checkpoints.size();
        int start = ((race.startIndex % n) + n) % n;
        int next = (start + 1) % n;
        Vector2f cpStart = race.checkpoints.get(start);
        Vector2f cpNext = race.checkpoints.get(next);
        float dx = cpNext.x - cpStart.x;
        float dy = cpNext.y - cpStart.y;
        if (Math.abs(dx) < 0.0001f && Math.abs(dy) < 0.0001f) return 0f;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
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

        // Corridor half-width based on the actual track walls at the start segment
        float leftWidth = getWallLeftWidth(start, 0f);
        float rightWidth = getWallRightWidth(start, 0f);
        float safeHalfWidth = Math.max(0f, Math.min(leftWidth, rightWidth) - gridEdgeMargin);

        float cols = Math.max(2, gridColumns);
        float laneSpacing = (2f * safeHalfWidth) / (cols - 1f);
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

    private float dotDelta(Vector2f p, Vector2f center, Vector2f axis) {
        float dx = p.x - center.x;
        float dy = p.y - center.y;
        return dx * axis.x + dy * axis.y;
    }

    private static class GateSpec {
        public final Vector2f center;
        public final Vector2f tangent;
        public final Vector2f normal;
        public final float wLeft;
        public final float wRight;

        public GateSpec(Vector2f center, Vector2f tangent, Vector2f normal, float wLeft, float wRight) {
            this.center = center;
            this.tangent = tangent;
            this.normal = normal;
            this.wLeft = wLeft;
            this.wRight = wRight;
        }
    }

    private static class WallAsteroidSpec {
        public final Vector2f location;
        public final int type;
        public CombatEntityAPI entity;

        public WallAsteroidSpec(Vector2f location, int type) {
            this.location = location;
            this.type = type;
        }
    }

    private static class ClosestPoint {
        public final int segIndex;
        public final float t;
        public final Vector2f point;
        public final float distSq;

        public ClosestPoint(int segIndex, float t, Vector2f point, float distSq) {
            this.segIndex = segIndex;
            this.t = t;
            this.point = point;
            this.distSq = distSq;
        }
    }

    public static class RaceState {
        public int lapsToWin;
        public float checkpointRadius = 350f;
        public List<Vector2f> checkpoints = new ArrayList<>();
        public Map<ShipAPI, RacerState> racers = new LinkedHashMap<>();
        public boolean finished = false;
        public FleetSide winnerSide = FleetSide.PLAYER;
        public int startIndex;

        public List<Vector2f> centerline = new ArrayList<>();
        public List<Vector2f> raceline = new ArrayList<>();
        public List<Float> racelineS = new ArrayList<>();
        public List<Float> racelinePsi = new ArrayList<>();
        public List<Float> racelineKappa = new ArrayList<>();
        public List<Float> racelineVx = new ArrayList<>();
        public float unitsPerMeter = 100f;
        public List<Float> wLeft = new ArrayList<>();
        public List<Float> wRight = new ArrayList<>();
        public boolean useWidths = false;
        public float corridorHalfWidth = 350f; // fallback
        public List<GateSpec> gates = new ArrayList<>();
        public List<WallAsteroidSpec> wallAsteroids = new ArrayList<>();
        public List<String> finishOrderMemberIds = new ArrayList<>();
        public List<String> dnfMemberIds = new ArrayList<>();
        public boolean endedByTimeout = false;
        public boolean endedByPlayerDNF = false;


        public RaceState(int lapsToWin) {
            this.lapsToWin = lapsToWin;
        }
    }

    public static class RacerState {
        public int lap = 0;
        public int nextCheckpoint = 1;
        public boolean finished = false;
        public Vector2f lastPos;
        public float skill = 0f;
    }

    private float getSkillForShip(ShipAPI ship) {
        if (ship == null) return 0f;
        if (skillOverrides != null && ship.getFleetMember() != null) {
            String id = ship.getFleetMember().getId();
            Float override = skillOverrides.get(id);
            if (override != null) return clamp(override, 0f, 1f);
        }

        if (Global.getSector() == null) return 0f;
        if (ship.getCaptain() == null || ship.getCaptain().isDefault()) return 0f;
        String key = ship.getCaptain().getId();
        if (key == null || key.trim().isEmpty()) return 0f;

        Object data = Global.getSector().getPersistentData().get("takeshido_race_skills");
        if (!(data instanceof Map)) {
            Map<String, Float> map = new HashMap<>();
            map.put(key, 0f);
            Global.getSector().getPersistentData().put("takeshido_race_skills", map);
            return 0f;
        }

        @SuppressWarnings("unchecked")
        Map<String, Float> map = (Map<String, Float>) data;
        Float skill = map.get(key);
        if (skill == null) {
            map.put(key, 0f);
            return 0f;
        }
        return clamp(skill, 0f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}


