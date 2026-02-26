package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class TakeshidoBombBayEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float FIRE_RADIUS_SCALE = 0.1f;
    private static final float FIRE_RADIUS_MIN = 50f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null) return;
        ShipAPI ship = weapon.getShip();
        if (ship == null || ship.isHulk()) return;
        if (ship.getShipAI() == null) return;
        if (weapon.usesAmmo() && weapon.getAmmo() <= 0) return;

        ShipwideAIFlags flags = ship.getAIFlags();
        Object maneuverTarget = flags != null ? flags.getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) : null;
        if (!(maneuverTarget instanceof CombatEntityAPI)) return;

        CombatEntityAPI target = (CombatEntityAPI) maneuverTarget;
        Vector2f shipLoc = ship.getLocation();
        Vector2f targetLoc = target.getLocation();
        if (shipLoc == null || targetLoc == null) return;

        float dx = targetLoc.x - shipLoc.x;
        float dy = targetLoc.y - shipLoc.y;
        float distSq = dx * dx + dy * dy;
        float dist = (float) Math.sqrt(distSq);

        if (!(target instanceof ShipAPI)) return;
        float rangeGate = Math.max(FIRE_RADIUS_MIN, target.getCollisionRadius() * FIRE_RADIUS_SCALE);
        boolean allowFire = dist <= rangeGate;

        if (allowFire) {
            weapon.setForceFireOneFrame(true);
        } else {
            weapon.setForceNoFireOneFrame(true);
        }
    }

    
}
