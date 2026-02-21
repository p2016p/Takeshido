package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public class takeshido_racemod extends BaseHullMod {

    private static final String MOD_ID = "takeshido_race_timedilation";
    private static final float SPEED_TIME_MULT = 5f;
    private static final float REVERSE_SPEED_CAP = 20f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) return;

        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getTimeMult().modifyMult(MOD_ID, SPEED_TIME_MULT);

        float inv = 1f / SPEED_TIME_MULT;
        stats.getAcceleration().modifyMult(MOD_ID, inv);
        // Increase braking power while keeping overall time-mult balance.
        stats.getDeceleration().modifyMult(MOD_ID, inv * 3f);
        stats.getTurnAcceleration().modifyMult(MOD_ID, inv);
        stats.getMaxTurnRate().modifyMult(MOD_ID, inv);
        stats.getFluxDissipation().modifyMult(MOD_ID, inv);
        stats.getCRLossPerSecondPercent().modifyMult(MOD_ID, inv);

        capReverseSpeed(ship);
    }

    private void capReverseSpeed(ShipAPI ship) {
        Vector2f vel = ship.getVelocity();
        if (vel == null) return;

        float facingRad = (float) Math.toRadians(ship.getFacing());
        float fx = (float) Math.cos(facingRad);
        float fy = (float) Math.sin(facingRad);

        float forwardSpeed = vel.x * fx + vel.y * fy;
        // Cap reverse speed (negative forward component) without killing lateral velocity.
        if (forwardSpeed < -REVERSE_SPEED_CAP) {
            float delta = -REVERSE_SPEED_CAP - forwardSpeed;
            forwardSpeed = -REVERSE_SPEED_CAP;
        }

        // Strip lateral velocity: keep only the component along facing.
        vel.x = fx * forwardSpeed;
        vel.y = fy * forwardSpeed;
    }
}
