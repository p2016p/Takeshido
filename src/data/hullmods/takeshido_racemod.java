package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

public class takeshido_racemod extends BaseHullMod {

    private static final String MOD_ID = "takeshido_race_timedilation";
    private static final float SPEED_TIME_MULT = 5f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || ship.isHulk()) return;

        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getTimeMult().modifyMult(MOD_ID, SPEED_TIME_MULT);

        float inv = 1f / SPEED_TIME_MULT;
        stats.getAcceleration().modifyMult(MOD_ID, inv);
        stats.getDeceleration().modifyMult(MOD_ID, inv);
        stats.getTurnAcceleration().modifyMult(MOD_ID, inv);
        stats.getMaxTurnRate().modifyMult(MOD_ID, inv);
        stats.getFluxDissipation().modifyMult(MOD_ID, inv);
        stats.getCRLossPerSecondPercent().modifyMult(MOD_ID, inv);

        if (ship.getVelocity() == null) return;

        float facingRad = (float) Math.toRadians(ship.getFacing());
        Vector2f forward = new Vector2f((float) Math.cos(facingRad), (float) Math.sin(facingRad));
        Vector2f vel = ship.getVelocity();
        float forwardSpeed = Vector2f.dot(vel, forward);
        Vector2f forwardVel = new Vector2f(forward);
        forwardVel.scale(forwardSpeed);
        vel.set(forwardVel);
    }
}
