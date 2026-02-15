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
    private static final float TURN_RATE_MARGIN = 0.65f;
    private static final float OVERTAKE_DIST = 220f;
    private static final float OVERTAKE_LANE_WIDTH = 120f;
    private static final float OVERTAKE_OFFSET = 40f;
    private static final float BRAKE_LOOKAHEAD_TIME = 0.65f;
    private static final float BRAKE_LOOKAHEAD_MIN_UNITS = 40f;
    private static final float BRAKE_LOOKAHEAD_MAX_UNITS = 420f;

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

//        if (closestCenter >= 0) {
//            Vector2f offset = computeOvertakeOffset(closestCenter);
//            if (offset != null) {
//                target = new Vector2f(target.x + offset.x, target.y + offset.y);
//            }
//        }

        float desired = angleTo(loc, target);
        float delta = shortestRotation(ship.getFacing(), desired);
        float maxTurnRate = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
        if (amount > 0f && maxTurnRate > 0f) {
            float desiredAngVel = clamp(delta / amount, -maxTurnRate, maxTurnRate);
            ship.setAngularVelocity(desiredAngVel);
        }

        boolean tooTight = false;
//        if (race.racelineKappa.size() == race.raceline.size() && race.unitsPerMeter > 0f) {
//            float speedUnits = ship.getVelocity() != null ? ship.getVelocity().length() : 0f;
//            float speedMps = speedUnits / race.unitsPerMeter;
//            float kappa = Math.abs(race.racelineKappa.get(closestRace));
//            float requiredTurnRateDeg = (speedMps * kappa) * 57.2958f;
//            tooTight = requiredTurnRateDeg > (maxTurnRate * TURN_RATE_MARGIN);
//        }

        // Braking based on curvature of the actual line being targeted (centerline/raceline blend)
        if (race.unitsPerMeter > 0f) {
            float speedUnits = ship.getVelocity() != null ? ship.getVelocity().length() : 0f;
            float brakeLookaheadUnits = clamp(speedUnits * BRAKE_LOOKAHEAD_TIME,
                    BRAKE_LOOKAHEAD_MIN_UNITS, BRAKE_LOOKAHEAD_MAX_UNITS);
            int brakeSteps = Math.max(1, Math.round(brakeLookaheadUnits / 40f));

            float maxDecel = ship.getMutableStats().getDeceleration().getModifiedValue();
            if (race.centerline != null && !race.centerline.isEmpty()
                    && race.raceline != null
                    && race.raceline.size() == race.centerline.size()
                    && closestCenter >= 0) {
                float skill = clamp(state != null ? state.skill : 0f, 0f, 1f);
                int n = race.centerline.size();
                float distAlong = 0f;
                int prevIdx = closestCenter;
                Vector2f prevPt = lerp(race.centerline.get(prevIdx), race.raceline.get(prevIdx), skill);
                for (int i = 0; i <= brakeSteps; i++) {
                    int idx = closestCenter + i;
                    while (idx >= n) idx -= n;
                    if (i > 0) {
                        Vector2f curPt = lerp(race.centerline.get(idx), race.raceline.get(idx), skill);
                        distAlong += distance(prevPt, curPt);
                        prevPt = curPt;
                    }

                    float kappa = Math.abs(curvatureAtBlend(idx, skill));
                    if (kappa <= 0f) continue;

                    float maxAllowedSpeed = (maxTurnRate * TURN_RATE_MARGIN) / (kappa * 57.2958f);
                    if (speedUnits <= maxAllowedSpeed) continue;

                    float timeTo = distAlong / Math.max(speedUnits, 1f);
                    if (timeTo <= 0.01f) {
                        tooTight = true;
                        break;
                    }

                    float requiredDecel = (speedUnits - maxAllowedSpeed) / timeTo;
                    if (requiredDecel >= maxDecel) {
                        tooTight = true;
                        break;
                    }
                }
            } else if (race.raceline != null && race.raceline.size() >= 3) {
                int n = race.raceline.size();
                float distAlong = 0f;
                int prevIdx = closestRace;
                Vector2f prevPt = race.raceline.get(prevIdx);
                for (int i = 0; i <= brakeSteps; i++) {
                    int idx = closestRace + i;
                    while (idx >= n) idx -= n;
                    if (i > 0) {
                        Vector2f curPt = race.raceline.get(idx);
                        distAlong += distance(prevPt, curPt);
                        prevPt = curPt;
                    }

                    float kappa = Math.abs(curvatureAt(race.raceline, idx));
                    if (kappa <= 0f) continue;

                    float maxAllowedSpeed = (maxTurnRate * TURN_RATE_MARGIN) / (kappa * 57.2958f);
                    if (speedUnits <= maxAllowedSpeed) continue;

                    float timeTo = distAlong / Math.max(speedUnits, 1f);
                    if (timeTo <= 0.01f) {
                        tooTight = true;
                        break;
                    }

                    float requiredDecel = (speedUnits - maxAllowedSpeed) / timeTo;
                    if (requiredDecel >= maxDecel) {
                        tooTight = true;
                        break;
                    }
                }
            }
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

    private float curvatureAt(java.util.List<Vector2f> line, int idx) {
        int n = line.size();
        if (n < 3) return 0f;
        int i0 = (idx - 1 + n) % n;
        int i1 = idx % n;
        int i2 = (idx + 1) % n;
        return curvatureFromThree(line.get(i0), line.get(i1), line.get(i2));
    }

    private float curvatureAtBlend(int idx, float skill) {
        int n = race.centerline.size();
        if (n < 3 || race.raceline == null || race.raceline.size() != n) return 0f;
        int i0 = (idx - 1 + n) % n;
        int i1 = idx % n;
        int i2 = (idx + 1) % n;

        Vector2f a = lerp(race.centerline.get(i0), race.raceline.get(i0), skill);
        Vector2f b = lerp(race.centerline.get(i1), race.raceline.get(i1), skill);
        Vector2f c = lerp(race.centerline.get(i2), race.raceline.get(i2), skill);
        return curvatureFromThree(a, b, c);
    }

    private float distanceAlongLine(java.util.List<Vector2f> line, int startIdx, int steps) {
        if (line == null || line.size() < 2 || steps <= 0) return 0f;
        int n = line.size();
        float dist = 0f;
        for (int i = 0; i < steps; i++) {
            int aIdx = startIdx + i;
            int bIdx = startIdx + i + 1;
            while (aIdx >= n) aIdx -= n;
            while (bIdx >= n) bIdx -= n;
            dist += distance(line.get(aIdx), line.get(bIdx));
        }
        return dist;
    }

    private float distanceAlongBlend(int startIdx, int steps, float skill) {
        int n = race.centerline.size();
        if (n < 2 || race.raceline == null || race.raceline.size() != n || steps <= 0) return 0f;
        float dist = 0f;
        for (int i = 0; i < steps; i++) {
            int aIdx = startIdx + i;
            int bIdx = startIdx + i + 1;
            while (aIdx >= n) aIdx -= n;
            while (bIdx >= n) bIdx -= n;
            Vector2f a = lerp(race.centerline.get(aIdx), race.raceline.get(aIdx), skill);
            Vector2f b = lerp(race.centerline.get(bIdx), race.raceline.get(bIdx), skill);
            dist += distance(a, b);
        }
        return dist;
    }

    private float curvatureFromThree(Vector2f a, Vector2f b, Vector2f c) {
        float ab = distance(a, b);
        float bc = distance(b, c);
        float ca = distance(c, a);
        if (ab < 0.0001f || bc < 0.0001f || ca < 0.0001f) return 0f;

        float cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
        float area2 = Math.abs(cross); // 2 * triangle area
        return (2f * area2) / (ab * bc * ca); // curvature = 1/R
    }
}
