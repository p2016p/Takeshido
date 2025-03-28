package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.hullmods.seven_667mod;
import data.scripts.world.systems.epta.seven_spawnstorefront;
import org.lazywizard.lazylib.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lazywizard.lazylib.CollectionUtils;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class seven_shipSystem_idash extends BaseShipSystemScript {

	ShipAPI[] targets = new ShipAPI[10];
	int targetcount = 0;
	int i = 0;
	Vector2f recent;
	float timeflow = 0.1f;
	float velocity = 200;
	public static float dashdistance = 500;

	public static Color hit = new Color(242, 85, 85, 162);
	public static Color hitfringe = new Color(200, 100, 100, 0);

	public static float AAFULL = 0.25f;
	public static float AAFADE = 0.25f;

	IntervalUtil systerval = new IntervalUtil(0.1f,0.1f);

	private static org.apache.log4j.Logger log = Global.getLogger(seven_shipSystem_idash.class);

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();

		WeaponAPI sword = (WeaponAPI) ship.getCustomData().get("mursword");

		if(player) {
			if (effectLevel == 1f) {
				engine.getTimeMult().modifyMult(id, 0.1f);
			} else {
				engine.getTimeMult().unmodifyMult(id);
			}
		}

		if(effectLevel>0 && effectLevel<1 && ship.getCustomData().containsKey("killship")){
			SpriteAPI sprite = Global.getSettings().getSprite("spatk", "smolwhitepixel");
			ShipAPI e = (ShipAPI) ship.getCustomData().get("killship");
			Vector2f p1 = e.getExactBounds().getSegments().get(MathUtils.getRandomNumberInRange(0,e.getExactBounds().getSegments().size()-1)).getP1();
			Vector2f p2 = e.getExactBounds().getSegments().get(MathUtils.getRandomNumberInRange(0,e.getExactBounds().getSegments().size()-1)).getP1();
			MagicRender.battlespace(sprite,MathUtils.getMidpoint(p1,p2),e.getVelocity(), new Vector2f(MathUtils.getDistance(p1,p2),3),new Vector2f(0,0),VectorUtils.getAngle(p1,p2),0f,Color.white,true,0,ship.getSystem().getChargeDownDur()*effectLevel,0.5f);
			ship.getLocation().set(p2);
			ship.setFacing(VectorUtils.getAngle(p1,p2));
		}

		String spatk = "";
		if(effectLevel == 1){
			if(!ship.getCustomData().containsKey("aisystemhelper")){
				if(Mouse.getEventButton()==0) {
					if (ship.getEngineController().isDecelerating() || ship.getEngineController().isAcceleratingBackwards()) spatk = "counter";
					else if (ship.getEngineController().isStrafingLeft()) spatk = "leftslash";
					else if (ship.getEngineController().isStrafingRight()) spatk = "rightslash";
					else if (ship.getEngineController().isAccelerating()) spatk = "fwdslash";
					else if (ship.getAngularVelocity() < -15 && ship.getEngineController().isTurningRight()) spatk = "spinright";
					else if (ship.getAngularVelocity() > 15 && ship.getEngineController().isTurningLeft()) spatk = "spinleft";
					else spatk = "strongatk";
				}
				else if(Keyboard.isKeyDown(Keyboard.KEY_V)) spatk = "beam";

				ship.setCustomData("spatk",spatk);
			}else if(ship.getCustomData().containsKey("aisystemhelper")) {
				spatk = (String) ship.getCustomData().get("aisystemhelper");
				ship.setCustomData("spatk",spatk);
				ship.removeCustomData("aisystemhelper");
			}

			if(spatk.equals("strongatk")){
				if (AIUtils.getNearestEnemy(ship) != null) {
					if (CollisionUtils.isPointWithinBounds(ship.getLocation(),AIUtils.getNearestEnemy(ship))){
						//AIUtils.getNearestEnemy(ship).getSpriteAPI().setColor(Color.BLACK);
						ship.getCustomData().put("killship",AIUtils.getNearestEnemy(ship));
					}
				}
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0f);
			}
			if(spatk.equals("counter")){
				ShipAPI biggestboy = null;
				float biggestdamage = 0f;
				float totaldamage = 0f;
				if(!engine.getProjectiles().isEmpty()) {
					for (DamagingProjectileAPI p : engine.getProjectiles()) {
						if (MathUtils.getDistance(ship.getLocation(), p.getLocation()) < sword.getRange()+10f) {
							if(p.getDamage().getDamage()>biggestdamage && !p.getSource().isAlly()){
								biggestdamage = p.getDamage().getDamage();
								biggestboy = p.getSource();
							}
							totaldamage = totaldamage+p.getDamage().getDamage();
						}
					}
				}
				if(!engine.getBeams().isEmpty()) {
					for (BeamAPI p : engine.getBeams()) {
						if (MathUtils.getDistance(ship.getLocation(), p.getTo()) < sword.getRange()+10f) {
							if(p.getDamage().getDamage()>biggestdamage && !p.getSource().isAlly()){
								biggestdamage = p.getDamage().getDamage();
								biggestboy = p.getSource();
							}
							totaldamage = totaldamage+p.getDamage().getDamage();
						}
					}
				}
				if(biggestboy==null || biggestboy.isAlly()){
					if(AIUtils.getNearestEnemy(ship)!=null && MathUtils.getDistance(ship,AIUtils.getNearestEnemy(ship))<1200f){
						biggestboy = AIUtils.getNearestEnemy(ship);
					}
				}
				if(biggestboy!=null){
					ship.setFacing(VectorUtils.getAngle(ship.getLocation(),biggestboy.getLocation()));
					spawnaa(ship,4, MathUtils.getDistance(ship.getLocation(),biggestboy.getLocation()));
					ship.getLocation().set(biggestboy.getLocation());
					ship.getVelocity().set(0,0);

					SpriteAPI sprite = Global.getSettings().getSprite("spatk", "fwdslash");
					MagicRender.battlespace(sprite,sword.getLocation(),new Vector2f(0f,0f),new Vector2f(sprite.getWidth(),sprite.getHeight()),new Vector2f(0f,0f),ship.getFacing()-90,0f, Color.white,true,0f,AAFULL,AAFADE);
					Vector2f wpos = sword.getLocation();
					engine.applyDamage(biggestboy, biggestboy.getLocation(), (sword.getDamage().getDamage() + totaldamage) / 2f, sword.getDamageType(), 0, true, false, ship);
				}
				ship.setCustomData("murforceanim",3);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
			if(spatk.equals("leftslash")){
				spawnaa(ship,1, dashdistance);
				Vector2f aaloc = VectorUtils.rotate(new Vector2f(0,dashdistance),ship.getFacing());
				Vector2f wpos = sword.getLocation();
				ship.getLocation().set(new Vector2f(aaloc.getX()+ship.getLocation().getX(),aaloc.getY()+ship.getLocation().getY()));
				ship.getVelocity().set(VectorUtils.rotate(new Vector2f(0,velocity),ship.getFacing()));
				if(AIUtils.getNearbyEnemies(ship,10000)!=null) {
					for (ShipAPI s : AIUtils.getNearbyEnemies(ship, 10000)) {
						if (CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s) != null){
							engine.applyDamage(s,CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s),sword.getDamage().getDamage(),sword.getDamageType(),0,true,false,ship);
						}
					}
				}
				SpriteAPI sprite = Global.getSettings().getSprite("spatk", "leftslash");
				MagicRender.battlespace(sprite,sword.getLocation(),new Vector2f(0f,0f),new Vector2f(sprite.getWidth(),sprite.getHeight()),new Vector2f(0f,0f),ship.getFacing()-90,0f, Color.white,true,0f,AAFULL,AAFADE);
				ship.setCustomData("murforceanim",0);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
			if(spatk.equals("rightslash")){
				spawnaa(ship,2, dashdistance);
				Vector2f aaloc = VectorUtils.rotate(new Vector2f(0,-dashdistance),ship.getFacing());
				Vector2f wpos = sword.getLocation();
				ship.getLocation().set(new Vector2f(aaloc.getX()+ship.getLocation().getX(),aaloc.getY()+ship.getLocation().getY()));
				ship.getVelocity().set(VectorUtils.rotate(new Vector2f(0,-velocity),ship.getFacing()));
				if(AIUtils.getNearbyEnemies(ship,10000)!=null) {
					for (ShipAPI s : AIUtils.getNearbyEnemies(ship, 10000)) {
						if (CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s) != null){
							engine.applyDamage(s,CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s),sword.getDamage().getDamage(),sword.getDamageType(),0,true,false,ship);
						}
					}
				}
				SpriteAPI sprite = Global.getSettings().getSprite("spatk", "rightslash");
				MagicRender.battlespace(sprite,sword.getLocation(),new Vector2f(0f,0f),new Vector2f(sprite.getWidth(),sprite.getHeight()),new Vector2f(0f,0f),ship.getFacing()-90,0f, Color.white,true,0f,AAFULL,AAFADE);
				ship.setCustomData("murforceanim",2);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
			if(spatk.equals("fwdslash")){
				spawnaa(ship,3, dashdistance);
				Vector2f aaloc = VectorUtils.rotate(new Vector2f(dashdistance,0),ship.getFacing());
				Vector2f wpos = sword.getLocation();
				ship.getLocation().set(new Vector2f(aaloc.getX()+ship.getLocation().getX(),aaloc.getY()+ship.getLocation().getY()));
				ship.getVelocity().set(VectorUtils.rotate(new Vector2f(velocity,0),ship.getFacing()));
				if(AIUtils.getNearbyEnemies(ship,10000)!=null) {
					for (ShipAPI s : AIUtils.getNearbyEnemies(ship, 10000)) {
						if (CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s) != null){
							engine.applyDamage(s,CollisionUtils.getCollisionPoint(wpos, sword.getLocation(), s),sword.getDamage().getDamage(),sword.getDamageType(),0,true,false,ship);
						}
					}
				}
				SpriteAPI sprite = Global.getSettings().getSprite("spatk", "fwdslash");
				MagicRender.battlespace(sprite,sword.getLocation(),new Vector2f(0f,0f),new Vector2f(sprite.getWidth(),sprite.getHeight()),new Vector2f(0f,0f),ship.getFacing()-90,0f, Color.white,true,0f,AAFULL,AAFADE);
				ship.setCustomData("murforceanim",3);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
			if(spatk.equals("beam")){
				DamagingProjectileAPI p = (DamagingProjectileAPI) engine.spawnProjectile(ship,null,"seven_murasama_bladebeam",sword.getLocation(),ship.getFacing(),ship.getVelocity());
				p.getDamage().getModifier().modifyMult(id,sword.getDamage().getModifier().getMult() + ship.getFluxLevel());
				ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getCurrFlux()*0.5f);
				ship.setCustomData("murforceanim",0);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
			if(spatk.equals("spinleft") || spatk.equals("spinright")){
				if(!engine.getProjectiles().isEmpty()) {
					for (DamagingProjectileAPI p : engine.getProjectiles()) {
						if (MathUtils.getDistance(ship.getLocation(), p.getLocation()) < sword.getRange()+10f) {
							p.setSource(ship);
							p.setOwner(ship.getOwner());
							int dir = MathUtils.getRandomNumberInRange(0,360);
							p.setFacing(dir);
							p.getVelocity().set(VectorUtils.rotate(p.getVelocity(),dir));
						}
					}
				}
				DamagingExplosionSpec spin = new DamagingExplosionSpec(0.1f,
						sword.getRange()+10f,
						sword.getRange()+10f,
						sword.getDamage().getDamage(),
						sword.getDamage().getDamage(),
						CollisionClass.PROJECTILE_FF,
						CollisionClass.PROJECTILE_FIGHTER,
						0f,
						0f,
						0f,
						0,
						new Color(0,0,0,0),
						new Color(0,0,0,0));
				spin.setDamageType(DamageType.ENERGY);
				engine.spawnDamagingExplosion(spin,ship,ship.getLocation());

				SpriteAPI sprite = Global.getSettings().getSprite("spatk", "leftspin");
				float rotate = -45f;
				if (spatk.equals("spinleft")){
					ship.setAngularVelocity(1000);
					ship.setCustomData("murforceanim",0);
				}
				else {
					rotate = 45f;
					ship.setAngularVelocity(-1000);
					sprite = Global.getSettings().getSprite("spatk", "rightspin");
					ship.setCustomData("murforceanim",2);
				}
				MagicRender.battlespace(sprite,sword.getLocation(),new Vector2f(0f,0f),new Vector2f(sprite.getWidth(),sprite.getHeight()),new Vector2f(0f,0f),ship.getFacing()-90+rotate,0f, Color.white,true,0f,AAFULL,AAFADE);
				ship.getSystem().forceState(ShipSystemAPI.SystemState.OUT, 0.9f);
			}
		}
	}


	public void unapply(MutableShipStatsAPI stats, String id) {

		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		engine.getTimeMult().unmodifyMult(id);
		WeaponAPI sword = (WeaponAPI) ship.getCustomData().get("mursword");

		if(ship.getCustomData().containsKey("killship")){
			ShipAPI e = (ShipAPI) ship.getCustomData().get("killship");
			for (int i = 0; i < 10; i++) {
				Vector2f p1 = e.getExactBounds().getSegments().get(MathUtils.getRandomNumberInRange(0,e.getExactBounds().getSegments().size()-1)).getP1();
				engine.applyDamage(e, p1, sword.getDamage().getDamage()*0.2f, DamageType.FRAGMENTATION, 0, true, false, ship);
			}
			if(e.getHullLevel()<=0.15f) {
				while (e.getHullLevel()>0){
					engine.applyDamage(e, e.getLocation(), sword.getDamage().getDamage()*0.2f, DamageType.FRAGMENTATION, 0, true, false, ship);
				}
				for (int i = 0; i < 10; i++) {
					e.splitShip();
				}
			}
			ship.removeCustomData("killship");
		}
	}

	private void spawnaa (ShipAPI ship, int dir, float dist){
		for (int i = 0; i < 4; i++) {
			Vector2f aaloc = null;
			if(dir == 1){
				aaloc = VectorUtils.rotate(new Vector2f(0, dashdistance/4 * i), ship.getFacing());
			}else if(dir == 2){
				aaloc = VectorUtils.rotate(new Vector2f(0, dashdistance/4 * i), ship.getFacing());
			}else if(dir == 3){
				aaloc = VectorUtils.rotate(new Vector2f(dashdistance/4 * i, 0), ship.getFacing());
			} else if(dir == 4){
				aaloc = VectorUtils.rotate(new Vector2f(dist/4 * i, 0), ship.getFacing());
			}
			Vector2f aapos = new Vector2f(aaloc.getX()+ship.getLocation().getX(),aaloc.getY()+ship.getLocation().getY());
			MagicRender.battlespace(
					Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
					aapos,
					new Vector2f(0, 0),
					new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
					new Vector2f(0, 0),
					ship.getFacing() - 90f,
					0,
					new Color(255, 255, 255, 120),
					true,
					0f,
					0f,
					0f,
					0f,
					0f,
					0f,
					AAFULL,AAFADE,
					CombatEngineLayers.ABOVE_SHIPS_LAYER
			);
		}
	}
}
