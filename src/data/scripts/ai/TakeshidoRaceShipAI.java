package data.scripts.ai;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipwideAIFlags;

import data.scripts.combat.TakeshidoRaceCombatPlugin.RaceState;
import data.scripts.combat.TakeshidoRaceCombatPlugin.RacerState;

public class TakeshidoRaceShipAI implements ShipAIPlugin {
    private final ShipAPI ship;
    private final RaceState race;
    private final RacerState state;

    private final ShipwideAIFlags flags = new ShipwideAIFlags();
    private final ShipAIConfig config = new ShipAIConfig();

    private static final float LOOKAHEAD_MIN_UNITS = 30f;
    private static final float LOOKAHEAD_MAX_UNITS = 280f;
    private static final float LINE_DIST_FOR_MAX_LOOKAHEAD = 600f;
    private static final float TURN_RATE_MARGIN = 0.75f;
    private static final float OVERTAKE_DIST = 220f;
    private static final float OVERTAKE_LANE_WIDTH = 120f;
    private static final float OVERTAKE_OFFSET = 40f;

    public TakeshidoRaceShipAI(ShipAPI ship, RaceState race, RacerState state) {
        this.ship = ship;
        this.race = race;
        this.state = state;
    }

    @Override
    public void advance(float amount) {
        if (ship == null || ship.isHulk()) return;
        if (race == null || race.raceline == null || race.raceline.isEmpty()) return;

        Vector2f loc = ship.getLocation();
        int closestRace = findClosestIndex(race.raceline, loc);
        Vector2f closestPt = race.raceline.get(closestRace);
        float distToLine = distance(loc, closestPt);

        float t = clamp(distToLine / LINE_DIST_FOR_MAX_LOOKAHEAD, 0f, 1f);
        float lookaheadUnits = LOOKAHEAD_MIN_UNITS + (LOOKAHEAD_MAX_UNITS - LOOKAHEAD_MIN_UNITS) * t;
        int targetRaceIdx = findLookaheadIndex(closestRace, lookaheadUnits);
        Vector2f targetRace = race.raceline.get(targetRaceIdx);

        Vector2f target = targetRace;
        int closestCenter = -1;
        if (race.centerline != null && !race.centerline.isEmpty()) {
            closestCenter = findClosestIndex(race.centerline, loc);
            int targetCenterIdx = findLookaheadIndexOnLine(race.centerline, closestCenter, lookaheadUnits);
            Vector2f targetCenter = race.centerline.get(targetCenterIdx);
            float skill = clamp(state != null ? state.skill : 0f, 0f, 1f);
            target = lerp(targetCenter, targetRace, skill);
        }

        if (closestCenter >= 0) {
            Vector2f offset = computeOvertakeOffset(closestCenter);
            if (offset != null) {
                target = new Vector2f(target.x + offset.x, target.y + offset.y);
            }
        }

        float desired = angleTo(loc, target);
        float delta = shortestRotation(ship.getFacing(), desired);
        float maxTurnRate = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
        if (amount > 0f && maxTurnRate > 0f) {
            float desiredAngVel = clamp(delta / amount, -maxTurnRate, maxTurnRate);
            ship.setAngularVelocity(desiredAngVel);
        }

        boolean tooTight = false;
        if (race.racelineKappa.size() == race.raceline.size() && race.unitsPerMeter > 0f) {
            float speedUnits = ship.getVelocity() != null ? ship.getVelocity().length() : 0f;
            float speedMps = speedUnits / race.unitsPerMeter;
            float kappa = Math.abs(race.racelineKappa.get(closestRace));
            float requiredTurnRateDeg = (speedMps * kappa) * 57.2958f;
            tooTight = requiredTurnRateDeg > (maxTurnRate * TURN_RATE_MARGIN);
        }

        ship.giveCommand(tooTight ? ShipCommand.DECELERATE : ShipCommand.ACCELERATE, null, 0);
    }

    @Override
    public void setDoNotFireDelay(float amount) {
    }

    @Override public void forceCircumstanceEvaluation() {}
    @Override public boolean needsRefit() { return false; }
    @Override public ShipwideAIFlags getAIFlags() { return flags; }
    @Override public void cancelCurrentManeuver() {}
    @Override public ShipAIConfig getConfig() { return config; }
    @Override public void setTargetOverride(ShipAPI target) {}

    private int findLookaheadIndex(int closest, float lookaheadUnits) {
        if (race.racelineS.size() == race.raceline.size() && race.unitsPerMeter > 0f) {
            float lookaheadM = lookaheadUnits / race.unitsPerMeter;
            float sClosest = race.racelineS.get(closest);
            float lastS = race.racelineS.get(race.racelineS.size() - 1);
            float targetS = sClosest + lookaheadM;

            if (targetS <= lastS && targetS >= sClosest) {
                for (int i = closest; i < race.racelineS.size(); i++) {
                    if (race.racelineS.get(i) >= targetS) return i;
                }
            } else {
                if (targetS > lastS) targetS -= lastS;
                for (int i = 0; i < race.racelineS.size(); i++) {
                    if (race.racelineS.get(i) >= targetS) return i;
                }
            }
        }

        int steps = Math.max(1, Math.round(lookaheadUnits / 40f));
        int idx = closest + steps;
        int size = race.raceline.size();
        while (idx >= size) idx -= size;
        return idx;
    }

    private int findClosestIndex(java.util.List<Vector2f> line, Vector2f pos) {
        int bestIdx = 0;
        float best = Float.MAX_VALUE;
        for (int i = 0; i < line.size(); i++) {
            Vector2f p = line.get(i);
            float dx = p.x - pos.x;
            float dy = p.y - pos.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < best) {
                best = d2;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private int findLookaheadIndexOnLine(java.util.List<Vector2f> line, int closest, float lookaheadUnits) {
        int steps = Math.max(1, Math.round(lookaheadUnits / 40f));
        int idx = closest + steps;
        int size = line.size();
        while (idx >= size) idx -= size;
        return idx;
    }

    private Vector2f computeOvertakeOffset(int centerIdx) {
        if (race.centerline == null || race.centerline.size() < 2) return null;
        if (race.racers == null || race.racers.size() < 2) return null;

        int next = centerIdx + 1;
        if (next >= race.centerline.size()) next = 0;
        Vector2f a = race.centerline.get(centerIdx);
        Vector2f b = race.centerline.get(next);
        float tx = b.x - a.x;
        float ty = b.y - a.y;
        float tLen = (float) Math.sqrt(tx * tx + ty * ty);
        if (tLen < 0.0001f) return null;
        tx /= tLen;
        ty /= tLen;
        float nx = -ty;
        float ny = tx;

        ShipAPI closestAhead = null;
        float closestDist = Float.MAX_VALUE;
        for (ShipAPI other : race.racers.keySet()) {
            if (other == null || other == ship || other.isHulk()) continue;
            Vector2f toOther = Vector2f.sub(other.getLocation(), ship.getLocation(), null);
            float dist = toOther.length();
            if (dist > OVERTAKE_DIST) continue;
            float along = toOther.x * tx + toOther.y * ty;
            float lat = toOther.x * nx + toOther.y * ny;
            if (along <= 0f) continue;
            if (Math.abs(lat) > OVERTAKE_LANE_WIDTH) continue;
            if (dist < closestDist) {
                closestDist = dist;
                closestAhead = other;
            }
        }

        if (closestAhead == null) return null;

        float leftSpace = race.corridorHalfWidth;
        float rightSpace = race.corridorHalfWidth;
        if (race.useWidths && centerIdx < race.wLeft.size() && centerIdx < race.wRight.size()) {
            leftSpace = race.wLeft.get(centerIdx);
            rightSpace = race.wRight.get(centerIdx);
        }

        float sign = (leftSpace >= rightSpace) ? 1f : -1f;
        float maxOffset = Math.max(0f, Math.min(leftSpace, rightSpace) - ship.getCollisionRadius() * 0.5f);
        float offset = Math.min(OVERTAKE_OFFSET, maxOffset);
        if (offset <= 0f) return null;

        return new Vector2f(nx * sign * offset, ny * sign * offset);
    }


    private float distance(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float angleTo(Vector2f from, Vector2f to) {
        return (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
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

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private Vector2f lerp(Vector2f a, Vector2f b, float t) {
        return new Vector2f(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t
        );
    }
}
