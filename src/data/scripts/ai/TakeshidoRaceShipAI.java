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

    private float doNotFire = 0f;

    public TakeshidoRaceShipAI(ShipAPI ship, RaceState race, RacerState state) {
        this.ship = ship;
        this.race = race;
        this.state = state;
    }

    @Override
    public void advance(float amount) {
        if (ship == null || ship.isHulk()) return;
        if (race == null || race.checkpoints == null || race.checkpoints.isEmpty()) return;

        if (doNotFire > 0f) doNotFire -= amount;

        Vector2f target = race.checkpoints.get(clampIndex(state.nextCheckpoint, race.checkpoints.size()));
        Vector2f loc = ship.getLocation();

        // Always face the next checkpoint (kills the turn-oscillation issue)
        float desired = angleTo(loc, target);
        ship.setFacing(desired);
        ship.setAngularVelocity(0f);

        float dist = distance(loc, target);

        // Simple throttle: mostly just go
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);

        // If we’re very close, bleed speed a bit so we don’t orbit forever
        if (dist < race.checkpointRadius * 0.85f) {
            ship.giveCommand(ShipCommand.DECELERATE, null, 0);
        }

        // Optional: small strafe “nudge” toward target if you want tighter cornering
        // (comment out if your cars should be strictly nose-forward)
        Vector2f vel = ship.getVelocity();
        if (vel != null && vel.length() > 50f && dist > race.checkpointRadius * 0.75f) {
            // Strafe toward target based on whether target is left/right of forward direction
            float forward = ship.getFacing();
            float toTgt = desired;
            float side = shortestRotation(forward, toTgt);
            if (side > 8f) ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
            else if (side < -8f) ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
        }
    }

    @Override
    public void setDoNotFireDelay(float amount) {
        doNotFire = Math.max(doNotFire, amount);
    }

    @Override public void forceCircumstanceEvaluation() {}
    @Override public boolean needsRefit() { return false; }
    @Override public ShipwideAIFlags getAIFlags() { return flags; }
    @Override public void cancelCurrentManeuver() {}
    @Override public ShipAIConfig getConfig() { return config; }
    @Override public void setTargetOverride(ShipAPI target) {}

    private int clampIndex(int idx, int size) {
        if (size <= 0) return 0;
        while (idx < 0) idx += size;
        while (idx >= size) idx -= size;
        return idx;
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
}

