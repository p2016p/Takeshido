package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public class takeshido_powertrain extends BaseHullMod {

    private static final float MAX_SPEED_BONUS = 0.75f;     // +75% top speed
    private static final float MIN_ACCEL_MULT = 0.15f;      // 15% accel near max speed

    private static final String DYN_ID = "takeshido_powertrain_dyn";
    private static final String SPEED_ID = "takeshido_powertrain_speed";
    private static final String DECEL_DYN_ID = "takeshido_powertrain_decel_dyn";
    private static final String STRIKECRAFT_MOD = "strikeCraft";
    private static final String STRIKECRAFT_FRIG_MOD = "armaa_strikeCraftFrig";
    private static final String STRIKECRAFT_FRIG_MOD_ALT = "amraa_strikeCraftFrig";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getZeroFluxSpeedBoost().modifyMult(id, 0f);

        // Apply speed bonus only to non-strikecraft.
        if (stats.getVariant() != null) {
            boolean isStrikecraft = stats.getVariant().hasHullMod(STRIKECRAFT_MOD)
                    && (stats.getVariant().hasHullMod(STRIKECRAFT_FRIG_MOD)
                    || stats.getVariant().hasHullMod(STRIKECRAFT_FRIG_MOD_ALT));
            if (!isStrikecraft) {
                stats.getMaxSpeed().modifyMult(SPEED_ID, 1f + MAX_SPEED_BONUS);
            }
        } else {
            // Fallback: apply speed bonus if we can't determine tags.
            stats.getMaxSpeed().modifyMult(SPEED_ID, 1f + MAX_SPEED_BONUS);
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) return;

        MutableShipStatsAPI stats = ship.getMutableStats();
        float max = stats.getMaxSpeed().getModifiedValue();
        if (max <= 0f || ship.getVelocity() == null) return;

        Vector2f vel = ship.getVelocity();
        float speed = vel.length();
        float startSpeed = Math.min(60f, max * 0.30f);
        float midSpeed = Math.min(120f, max * 0.60f);
        float endSpeed = Math.min(180f, max * 0.90f);

        float baseScaled;
        if (speed <= startSpeed) {
            baseScaled = 1f;
        } else if (speed >= endSpeed) {
            baseScaled = MIN_ACCEL_MULT;
        } else {
            float denom = (2.5f * endSpeed) - (1.5f * midSpeed) - startSpeed;
            float k = denom > 0.0001f ? (1f - MIN_ACCEL_MULT) / denom : 1f - MIN_ACCEL_MULT;
            float atMid = 1f - k * (midSpeed - startSpeed);

            if (speed <= midSpeed) {
                baseScaled = 1f - k * (speed - startSpeed);
            } else {
                baseScaled = atMid - (2.5f * k) * (speed - midSpeed);
            }
        }

        if (baseScaled < MIN_ACCEL_MULT) baseScaled = MIN_ACCEL_MULT;
        if (baseScaled > 1f) baseScaled = 1f;

        float finalScaled = baseScaled;
        if (speed > 0.1f) {
            float desiredAccelAngle = getDesiredAccelWorldAngle(ship);
            Vector2f accelDir = new Vector2f(
                    (float) Math.cos(Math.toRadians(desiredAccelAngle)),
                    (float) Math.sin(Math.toRadians(desiredAccelAngle))
            );
            Vector2f velDir = new Vector2f(vel);
            velDir.normalise();
            float dot = Vector2f.dot(velDir, accelDir);
            float align = Math.max(0f, Math.min(1f, dot));
            // Full diminishing when aligned with velocity; none when perpendicular/opposed.
            finalScaled = 1f - (1f - baseScaled) * align;
        }
        stats.getAcceleration().modifyMult(DYN_ID, finalScaled);
        stats.getDeceleration().modifyMult(DECEL_DYN_ID, finalScaled);
    }

    private float getDesiredAccelWorldAngle(ShipAPI ship) {
        if (ship.getEngineController() == null) return ship.getFacing();

        float forward = 0f;
        float right = 0f;

        if (ship.getEngineController().isAccelerating()) {
            forward += 1f;
        }
        if (ship.getEngineController().isAcceleratingBackwards()
                || ship.getEngineController().isDecelerating()) {
            forward -= 1f;
        }
        // Note: Starsector angles are clockwise; flip right/left to match world angles.
        if (ship.getEngineController().isStrafingRight()) {
            right -= 1f;
        }
        if (ship.getEngineController().isStrafingLeft()) {
            right += 1f;
        }

        // If no inputs, default to facing direction.
        if (Math.abs(forward) < 0.001f && Math.abs(right) < 0.001f) {
            return ship.getFacing();
        }

        float localAngle = (float) Math.toDegrees(Math.atan2(right, forward));
        if (localAngle < 0f) localAngle += 360f;

        float worldAngle = ship.getFacing() + localAngle;
        while (worldAngle < 0f) worldAngle += 360f;
        while (worldAngle >= 360f) worldAngle -= 360f;

        return worldAngle;
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        switch (index) {
            case 0:
                return Math.round(MAX_SPEED_BONUS * 100f) + "%";
            case 1:
                return Math.round(MIN_ACCEL_MULT * 100f) + "%";
            default:
                return null;
        }
    }
}
