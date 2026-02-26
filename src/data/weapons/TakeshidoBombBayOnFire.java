package data.weapons;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class TakeshidoBombBayOnFire implements OnFireEffectPlugin {
    private static final float TIME_WARP_VEL_MULT = 6f;

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        if (projectile == null || weapon == null || engine == null) return;
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;
        Vector2f shipVel = ship.getVelocity();
        if (shipVel == null) return;
        Vector2f projVel = projectile.getVelocity();
        if (projVel == null) {
            engine.addPlugin(new BombVelocityFix(projectile, ship));
            return;
        }

        projVel.set(shipVel);
        projVel.scale(TIME_WARP_VEL_MULT);
    }

    private static class BombVelocityFix extends BaseEveryFrameCombatPlugin {
        private final DamagingProjectileAPI projectile;
        private final ShipAPI ship;
        private CombatEngineAPI engine;
        private boolean done = false;

        private BombVelocityFix(DamagingProjectileAPI projectile, ShipAPI ship) {
            this.projectile = projectile;
            this.ship = ship;
        }

        @Override
        public void init(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (done || engine == null || projectile == null || ship == null) {
                cleanup();
                return;
            }
            if (!engine.isEntityInPlay(projectile)) {
                cleanup();
                return;
            }
            Vector2f shipVel = ship.getVelocity();
            Vector2f projVel = projectile.getVelocity();
            if (shipVel == null || projVel == null) return;

            projVel.set(shipVel);
            projVel.scale(TIME_WARP_VEL_MULT);
            cleanup();
        }

        private void cleanup() {
            if (done) return;
            done = true;
            if (engine != null) {
                engine.removePlugin(this);
            }
        }
    }
}
