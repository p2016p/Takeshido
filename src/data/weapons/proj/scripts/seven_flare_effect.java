package data.weapons.proj.scripts;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

/**
 * UNUSED
 */
public class seven_flare_effect implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {

        private static float arc = 50;
        private static float pelts = 3;
        private float usedBarrel = 0;
        private float popBarrel = 0;
        
        private boolean init = false;
    
	
	public static int PELLETS = 10;
	
        
	
	public static int DERP_RATE = 25;
	
	
    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon.getShip().getOriginalOwner() == -1 || weapon.getShip().isHulk()) {
            return;
        }
        if (!init) {
            init = true;
        }
    }
    
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        engine.removeEntity(projectile);
        
        String gun_id = "seven_small_flare_shield_dummy";
      
        if(weapon.getId().contains("seven_small_flare_shield")) {
            pelts = PELLETS;
            arc = DERP_RATE;
            gun_id = "seven_small_flare_shield_dummy";
            if (usedBarrel == 0) {
                popBarrel = 0;
                usedBarrel = 0;
            } else {
                popBarrel = 1;
                usedBarrel = 0;
            }
        }
        
        
        for (int f = 1; f <= pelts; f++){
            if (weapon.isFiring()) {
                float offset = ((float)Math.random() * arc) - (arc * 0.5f);
                float angle = weapon.getCurrAngle() + offset;
                DamagingProjectileAPI proj = (DamagingProjectileAPI) engine.spawnProjectile 
                    (weapon.getShip(), weapon, gun_id, weapon.getFirePoint((int) popBarrel), angle, weapon.getShip().getVelocity());
                float shotgunSpeed = (float)Math.random() * 0.25f + 0.85f;
                proj.getVelocity().scale(shotgunSpeed);
            }
        }
    }
}




