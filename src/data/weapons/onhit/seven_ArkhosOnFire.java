// Credits: xSevenG7x
//This is a compacted shotgun script, that can be used in a "OnFire" file, my first code made by myself^^
// If you are seeing this, you have my thanks for downloading and playing the mod.
//This one is not a shotgun, but you can understand how to manage the numbers to turn it a shotgun if you desire

package data.weapons.onhit;

import java.awt.Color;
import java.util.Iterator;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.AIHints;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

/**
 */
public class seven_ArkhosOnFire implements OnFireEffectPlugin {

public static Color LIGHTNING_CORE_COLOR = new Color(240, 174, 252, 200);
public static Color LIGHTNING_FRINGE_COLOR = new Color(155, 41, 217, 175);
    
	 public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
      

              //  Vector2f vel = (Vector2f) (projectile.getVelocity());
              //  Vector2f from = projectile.getLocation();
              //  Vector2f to = MathUtils.getPointOnCircumference(projectile.getLocation(), 25, MathUtils.getRandomNumberInRange(10, 70) );
              //  Vector2f to2 = MathUtils.getPointOnCircumference(projectile.getLocation(), 25, MathUtils.getRandomNumberInRange(100, 160) );
              //  float spread1 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-40, 40);
              //  float spread2 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-20, 20);
              //  float spread3 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-20, 20);
              //  float spread4 = projectile.getFacing() + MathUtils.getRandomNumberInRange(-10, 10);
                
             CombatEntityAPI target = projectile.getSource();
             Vector2f point = projectile.getLocation();
            
             
             //   4 projectile shotgun, you can copy dozens of this if you desire for make many pellets as possible.
        //   Also, if your shotgun not have different types of ammo, just copy the weapon id here, and boom, shotgun. 
      //engine.spawnProjectile(weapon.getShip(), weapon, "seven_glaphyra_dummy", projectile.getLocation(), spread1, vel);
      //engine.spawnProjectile(weapon.getShip(), weapon, "seven_glaphyra_dummy1", projectile.getLocation(), spread2, vel);
      //engine.spawnProjectile(weapon.getShip(), weapon, "seven_glaphyra_dummy2", projectile.getLocation(), spread3, vel);
             
            DamagingProjectileAPI e = engine.spawnDamagingExplosion(createExplosionSpec(), projectile.getSource(), point);
	    e.addDamagedAlready(target);
           
                
         }
         
         
         
            public DamagingExplosionSpec createExplosionSpec() {
                // "muzzle flash"
		float damage = 0f;
		DamagingExplosionSpec spec = new DamagingExplosionSpec(
				0.2f, // duration
				50f, // radius
				50f, // coreRadius
				damage, // maxDamage
				damage, // minDamage
				CollisionClass.NONE, // collisionClass
				CollisionClass.NONE, // collisionClassByFighter
				3f, // particleSizeMin
				4f, // particleSizeRange
				0.4f, // particleDuration
				100, // particleCount
				new Color(240,174,252,75), // particleColor
				new Color(155,41,217,20)  // explosionColor
		);

		spec.setDamageType(DamageType.HIGH_EXPLOSIVE);
		spec.setUseDetailedExplosion(false);
                spec.setSoundSetId("");
		return spec;
	}
                
                
        
      
     
    }
  
