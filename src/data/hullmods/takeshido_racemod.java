package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import data.scripts.ai.TakeshidoRaceShipAI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;

public class takeshido_racemod extends BaseHullMod {

    private static final String MOD_ID = "takeshido_race_timedilation";
    private static final String HULLMOD_ID = "takeshido_racemod";
    private static final String DRIFT_DATA_KEY = "takeshido_racemod_drift";
    private static final String DRIFT_HOLD_KEY = "takeshido_racemod_drift_hold";
    private static final String DRIFT_PLUGIN_KEY = "takeshido_racemod_drift_plugin";
    private static final String DRIFT_TURN_ID = "takeshido_race_drift_turn";
    private static final float SPEED_TIME_MULT = 6f;
    private static final float REVERSE_SPEED_CAP = 20f;
    private static final float DRIFT_EXIT_ANGLE_DEG = 5f;
    private static final float DRIFT_EXIT_MIN_SPEED_SQ = 25f;
    private static final float DRIFT_LATERAL_DAMP_PER_SEC = 0.2f;
    private static final float DRIFT_SMOKE_RATE_PER_ENGINE = 6f;
    private static final float DRIFT_TURN_RATE_MULT = 1.40f;
    private static final float DRIFT_TURN_RATE_FLAT = 20f;
    private static final String POWERTRAIN_ID = "takeshido_powertrain";
    private static final String TUNER_HULLMOD_ID = "takeshido_tuner";

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats.getVariant();
        if (variant == null) return;

        if (!variant.hasHullMod(TUNER_HULLMOD_ID)) {
            try {
                variant.addPermaMod(TUNER_HULLMOD_ID);
            } catch (Throwable t) {
                variant.addMod(TUNER_HULLMOD_ID);
            }
        }

        Collection<String> mods = new ArrayList<>(variant.getNonBuiltInHullmods());
        for (String modId : mods) {
            if (modId == null) continue;
            if (HULLMOD_ID.equals(modId) || POWERTRAIN_ID.equals(modId) || TUNER_HULLMOD_ID.equals(modId)) continue;
            HullModSpecAPI spec = Global.getSettings().getHullModSpec(modId);
            if (spec != null && isEngineHullmod(spec)) {
                variant.removeMod(modId);
                variant.addSuppressedMod(modId);
            }
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            ensureDriftPlugin(engine);
        }

        boolean driftActive = isDrifting(ship);
        if (driftActive && shouldExitDrift(ship)) {
            clearDrift(ship);
            driftActive = false;
        }

        boolean inRace = engine != null && engine.getCustomData().get("takeshido_race_state") != null;
        if ((!inRace || driftActive) && ship.getShipAI() != null) {
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
        }

        if (driftActive) {
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        }

        ShipwideAIFlags flags = ship.getAIFlags();
        Object maneuverTarget = flags != null ? flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) : null;
        

        if (!inRace && ship.getShipAI() != null && maneuverTarget != null) {
            if (maneuverTarget instanceof com.fs.starfarer.api.combat.CombatEntityAPI) {
                com.fs.starfarer.api.combat.CombatEntityAPI target =
                        (com.fs.starfarer.api.combat.CombatEntityAPI) maneuverTarget;
                Vector2f shipLoc = ship.getLocation();
                Vector2f targetLoc = target.getLocation();
                if (shipLoc != null && targetLoc != null) {
                    float desired = (float) Math.toDegrees(Math.atan2(targetLoc.y - shipLoc.y, targetLoc.x - shipLoc.x));
                    float delta = Math.abs(shortestRotation(ship.getFacing(), desired));
                    float dx = targetLoc.x - shipLoc.x;
                    float dy = targetLoc.y - shipLoc.y;
                    float distSq = dx * dx + dy * dy;
                    float threshold = 100f + target.getCollisionRadius();
                    boolean wantsDrift = delta > 90f && distSq > (threshold * threshold);
                    if (wantsDrift) {
                        ship.setCustomData(DRIFT_DATA_KEY, Boolean.TRUE);
                        ship.setCustomData(DRIFT_HOLD_KEY, Boolean.TRUE);
                    } else {
                        ship.removeCustomData(DRIFT_HOLD_KEY);
                    }
                }
            }
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
        }
//        if (engine != null && engine.getCustomData().get("takeshido_race_state") == null) {
//            if (ship.getShipAI() != null
//                    && !(ship.getShipAI() instanceof TakeshidoDiveBomberAI)
//                    && !(ship.getShipAI() instanceof TakeshidoRaceShipAI)) {
//                ship.setShipAI(new TakeshidoDiveBomberAI(ship));
//            }
//        }

        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getTimeMult().modifyMult(MOD_ID, SPEED_TIME_MULT);

        float inv = 1f / SPEED_TIME_MULT;
        stats.getAcceleration().modifyMult(MOD_ID, inv);
        stats.getDeceleration().modifyMult(MOD_ID, inv);
        stats.getTurnAcceleration().modifyMult(MOD_ID, inv);
        stats.getMaxTurnRate().modifyMult(MOD_ID, inv);
        stats.getFluxDissipation().modifyMult(MOD_ID, inv);
        stats.getShieldUpkeepMult().modifyMult(MOD_ID, inv);
        stats.getBallisticRoFMult().modifyMult(MOD_ID, inv);
        stats.getEnergyRoFMult().modifyMult(MOD_ID, inv);
        stats.getMissileRoFMult().modifyMult(MOD_ID, inv);
        stats.getCRLossPerSecondPercent().modifyMult(MOD_ID, inv);

        capReverseSpeed(ship, !driftActive);
        if (driftActive) {
            stats.getMaxTurnRate().modifyMult(DRIFT_TURN_ID, DRIFT_TURN_RATE_MULT);
            stats.getMaxTurnRate().modifyFlat(DRIFT_TURN_ID, DRIFT_TURN_RATE_FLAT);
            applyDriftLateralDamping(ship, amount);
            spawnDriftSmoke(ship, amount);
        } else {
            stats.getMaxTurnRate().unmodify(DRIFT_TURN_ID);
        }
    }


    private void capReverseSpeed(ShipAPI ship, boolean clampLateral) {
        Vector2f vel = ship.getVelocity();
        if (vel == null) return;

        float facingRad = (float) Math.toRadians(ship.getFacing());
        float fx = (float) Math.cos(facingRad);
        float fy = (float) Math.sin(facingRad);

        float forwardSpeed = vel.x * fx + vel.y * fy;
        float lateralX = vel.x - fx * forwardSpeed;
        float lateralY = vel.y - fy * forwardSpeed;
        // Cap reverse speed (negative forward component) without killing lateral velocity.
        if (forwardSpeed < -REVERSE_SPEED_CAP) {
            forwardSpeed = -REVERSE_SPEED_CAP;
        }
        if (clampLateral) {
            // Strip lateral velocity: keep only the component along facing.
            vel.x = fx * forwardSpeed;
            vel.y = fy * forwardSpeed;
        } else {
            vel.x = fx * forwardSpeed + lateralX;
            vel.y = fy * forwardSpeed + lateralY;
        }
    }

    private void applyDriftLateralDamping(ShipAPI ship, float amount) {
        if (amount <= 0f) return;
        Vector2f vel = ship.getVelocity();
        if (vel == null) return;

        float facingRad = (float) Math.toRadians(ship.getFacing());
        float fx = (float) Math.cos(facingRad);
        float fy = (float) Math.sin(facingRad);

        float forwardSpeed = vel.x * fx + vel.y * fy;
        float lateralX = vel.x - fx * forwardSpeed;
        float lateralY = vel.y - fy * forwardSpeed;

        float damp = (float) Math.exp(-DRIFT_LATERAL_DAMP_PER_SEC * amount);
        lateralX *= damp;
        lateralY *= damp;

        vel.x = fx * forwardSpeed + lateralX;
        vel.y = fy * forwardSpeed + lateralY;
    }

    private void spawnDriftSmoke(ShipAPI ship, float amount) {
        if (amount <= 0f) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || ship == null || ship.getEngineController() == null) return;

        float perEngine = DRIFT_SMOKE_RATE_PER_ENGINE * amount;
        int whole = (int) perEngine;
        float frac = perEngine - whole;

        for (ShipEngineAPI e : ship.getEngineController().getShipEngines()) {
            if (e == null || !e.isActive()) continue;

            int count = whole + ((Math.random() < frac) ? 1 : 0);
            for (int i = 0; i < count; i++) {
                Vector2f origin = new Vector2f(e.getLocation());
                Vector2f jitter = MathUtils.getRandomPointInCircle(null, 6f);
                Vector2f.add(origin, jitter, origin);

                Vector2f vel = new Vector2f(ship.getVelocity());
                vel.scale(0.15f);
                vel.x += MathUtils.getRandomNumberInRange(-10f, 10f);
                vel.y += MathUtils.getRandomNumberInRange(-10f, 10f);

                float size = MathUtils.getRandomNumberInRange(8f, 14f);
                float duration = MathUtils.getRandomNumberInRange(0.5f, 0.9f);
                engine.addSmokeParticle(origin, vel, size, 0.7f, duration, new Color(140, 140, 140, 120));
            }
        }
    }

    private boolean isDrifting(ShipAPI ship) {
        if (ship == null) return false;
        return Boolean.TRUE.equals(ship.getCustomData().get(DRIFT_DATA_KEY));
    }

    private boolean isDriftHeld(ShipAPI ship) {
        if (ship == null) return false;
        return Boolean.TRUE.equals(ship.getCustomData().get(DRIFT_HOLD_KEY));
    }

    private void clearDrift(ShipAPI ship) {
        if (ship == null) return;
        ship.removeCustomData(DRIFT_DATA_KEY);
    }

    private boolean shouldExitDrift(ShipAPI ship) {
        if (isDriftHeld(ship)) return false;

        Vector2f vel = ship.getVelocity();
        if (vel == null) return false;

        float speedSq = vel.x * vel.x + vel.y * vel.y;
        if (speedSq < DRIFT_EXIT_MIN_SPEED_SQ) return false;

        float velAngle = (float) Math.toDegrees(Math.atan2(vel.y, vel.x));
        float delta = Math.abs(shortestRotation(velAngle, ship.getFacing()));
        return delta <= DRIFT_EXIT_ANGLE_DEG;
    }

    private void ensureDriftPlugin(CombatEngineAPI engine) {
        if (engine.getCustomData().get(DRIFT_PLUGIN_KEY) != null) return;
        engine.addPlugin(new DriftInputPlugin());
        engine.getCustomData().put(DRIFT_PLUGIN_KEY, Boolean.TRUE);
    }

    private static class DriftInputPlugin extends BaseEveryFrameCombatPlugin {
        private CombatEngineAPI engine;

        @Override
        public void init(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, java.util.List<InputEventAPI> events) {
            if (engine == null || engine.isPaused() || events == null) return;

            ShipAPI player = engine.getPlayerShip();
            if (player == null || player.isHulk()) return;
            if (player.getVariant() == null || !player.getVariant().hasHullMod(HULLMOD_ID)) return;

            for (InputEventAPI event : events) {
                if (event == null || event.isConsumed()) continue;
                if (event.isKeyDownEvent() &&
                        (event.getEventValue() == Keyboard.KEY_A || event.getEventValue() == Keyboard.KEY_D)) {
                    player.setCustomData(DRIFT_DATA_KEY, Boolean.TRUE);
                    player.setCustomData(DRIFT_HOLD_KEY, Boolean.TRUE);
                    break;
                }
                if (event.isKeyUpEvent() &&
                        (event.getEventValue() == Keyboard.KEY_A || event.getEventValue() == Keyboard.KEY_D)) {
                    player.setCustomData(DRIFT_HOLD_KEY, Boolean.FALSE);
                    break;
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

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isEngineHullmod(HullModSpecAPI spec) {
        if (spec == null) return false;
        for (String tag : spec.getTags()) {
            if (tag != null) {
                String t = tag.toLowerCase();
                if (t.contains("engine") || t.contains("speed")) return true;
            }
        }
        for (String tag : spec.getUITags()) {
            if (tag != null) {
                String t = tag.toLowerCase();
                if (t.contains("engine") || t.contains("speed")) return true;
            }
        }
        return false;
    }
}
