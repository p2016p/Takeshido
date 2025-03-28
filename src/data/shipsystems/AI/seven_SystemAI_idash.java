package data.shipsystems.AI;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import data.shipsystems.scripts.seven_shipSystem_idash;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class seven_SystemAI_idash implements ShipSystemAIScript{

	private IntervalUtil timer;
	private float averageRangeSystem = 0;
	private float averageRangeNormal;
	private FluxTrackerAPI fluxTracker;
	private ShipSystemAPI system;
	private ShipAPI ship;
	//private CombatEngineAPI engine;

	// used for threat weighting
	private HashMap<ShipAPI.HullSize, Float> mults = new HashMap<>();

	private static org.apache.log4j.Logger log = Global.getLogger(seven_SystemAI_idash.class);

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		// load things from settings
		timer = new IntervalUtil(0.5f, 0.5f);

		// initialize variables
		this.ship = ship;
		fluxTracker = ship.getFluxTracker();
		this.system = system;

		mults.put(ShipAPI.HullSize.CAPITAL_SHIP, 1.5f);
		mults.put(ShipAPI.HullSize.CRUISER, 1.25f);
		mults.put(ShipAPI.HullSize.DESTROYER, 1f);
		mults.put(ShipAPI.HullSize.FRIGATE, 0.75f);
		mults.put(ShipAPI.HullSize.FIGHTER, 0f); // don't turn on the system to shoot fighters
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {

		WeaponAPI sword = (WeaponAPI) ship.getCustomData().get("mursword");

		CombatEngineAPI engine = Global.getCombatEngine();

		float spin = 0;
		float beam = 0;
		float fwd = 0;
		float left = 0;
		float right = 0;
		float counter = 0;
		float strong = 0;

		if(sword!=null) {

			if (target != null) {
				//!ship.getShipAI().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF) && !ship.getShipAI().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) && !ship.getShipAI().getAIFlags().hasFlag(ShipwideAIFlags.AIFlags.BACK_OFF_MIN_RANGE) &&
				if (ship.getHullLevel() > 0.5f && ship.getCurrentCR() > 0.4f && sword.distanceFromArc(target.getLocation()) == 0 && MathUtils.getDistance(ship.getLocation(), target.getLocation()) > sword.getRange() - 20f) {
					ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
					ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
					ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
				}
			}

			// if system isn't active and system is off cooldown
			if (!system.isStateActive() && system.getCooldownRemaining() == 0) {
				if (!AIUtils.getNearbyEnemies(ship, 10000f).isEmpty()) {
					for (ShipAPI s : AIUtils.getNearbyEnemies(ship, 10000f)) {
						if (s.isAlive() && !s.isHulk()) {
							if (s.getExactBounds() != null) {
								if ((MathUtils.getDistance(CollisionUtils.getNearestPointOnBounds(ship.getLocation(), s), ship.getLocation()) < sword.getRange() || CollisionUtils.isPointWithinBounds(ship.getLocation(), s))) {
									spin = spin + sword.getDamage().getDamage();
								}
							} else {
								if (MathUtils.getDistance(ship, s.getLocation()) < 100) {
									spin = spin + sword.getDamage().getDamage();
								}
							}
							Vector2f v1;
							Vector2f v2;
							v1 = VectorUtils.rotate(new Vector2f(0, seven_shipSystem_idash.dashdistance), ship.getFacing());
							v2 = new Vector2f(v1.getX() + sword.getLocation().getX(), v1.getY() + sword.getLocation().getY());
							if (CollisionUtils.getCollisionPoint(sword.getLocation(), v2, s) != null) {
								left = left + sword.getDamage().getDamage();
							}

							v1 = VectorUtils.rotate(new Vector2f(0, -seven_shipSystem_idash.dashdistance), ship.getFacing());
							v2 = new Vector2f(v1.getX() + sword.getLocation().getX(), v1.getY() + sword.getLocation().getY());
							if (CollisionUtils.getCollisionPoint(sword.getLocation(), v2, s) != null) {
								right = right + sword.getDamage().getDamage();
							}

							v1 = VectorUtils.rotate(new Vector2f(seven_shipSystem_idash.dashdistance, 0), ship.getFacing());
							v2 = new Vector2f(v1.getX() + sword.getLocation().getX(), v1.getY() + sword.getLocation().getY());
							if (CollisionUtils.getCollisionPoint(sword.getLocation(), v2, s) != null) {
								fwd = fwd + sword.getDamage().getDamage();
								beam = beam + (350f * (sword.getDamage().getModifier().getMult() + ship.getFluxLevel())) * 0.75f;
							}
						}
					}


					ShipAPI biggestboy = null;
					float biggestdamage = 0f;
					float totaldamage = 0f;
					if (!engine.getProjectiles().isEmpty()) {
						for (DamagingProjectileAPI p : engine.getProjectiles()) {
							if (MathUtils.getDistance(ship.getLocation(), p.getLocation()) < sword.getRange() + 10f) {
								if (p.getDamage().getDamage() > biggestdamage) {
									biggestdamage = p.getDamage().getDamage();
									biggestboy = p.getSource();
								}
								totaldamage = totaldamage + p.getDamage().getDamage();

								spin = spin + p.getDamage().getDamage() * 0.3f;
							}
						}
					}
					if (!engine.getBeams().isEmpty()) {
						for (BeamAPI p : engine.getBeams()) {
							if (MathUtils.getDistance(ship.getLocation(), p.getTo()) < sword.getRange() + 10f) {
								if (p.getDamage().getDamage() > biggestdamage) {
									biggestdamage = p.getDamage().getDamage();
									biggestboy = p.getSource();
								}
								totaldamage = totaldamage + p.getDamage().getDamage();
							}
						}
					}
					if (biggestboy != null) {
						counter = counter + (sword.getDamage().getDamage() + totaldamage) / 2f;
					}


					if (CollisionUtils.isPointWithinBounds(ship.getLocation(), AIUtils.getNearestEnemy(ship)) && AIUtils.getNearestEnemy(ship).getHullLevel() <= 0.15f) {
						strong = 10000f;
					}
				}

				float thresh = 450f - system.getAmmo() * 90f;
				if (spin > thresh || beam > thresh || fwd > thresh || left > thresh || right > thresh || counter > thresh || strong > thresh) {
					float var = spin;
					if (var >= beam && var >= fwd && var >= left && var >= right && var >= counter && var >= strong) {
						ship.setCustomData("aisystemhelper", "spinleft");
						ship.useSystem();
					}
					var = beam;
					if (var >= spin && var >= fwd && var >= left && var >= right && var >= counter && var >= strong) {
						ship.setCustomData("aisystemhelper", "beam");
						ship.useSystem();
					}
					var = fwd;
					if (var >= spin && var >= beam && var >= left && var >= right && var >= counter && var >= strong) {
						ship.setCustomData("aisystemhelper", "fwdslash");
						ship.useSystem();
					}
					var = left;
					if (var >= spin && var >= beam && var >= fwd && var >= right && var >= counter && var >= strong) {
						ship.setCustomData("aisystemhelper", "leftslash");
						ship.useSystem();
					}
					var = right;
					if (var >= spin && var >= beam && var >= fwd && var >= left && var >= counter && var >= strong) {
						ship.setCustomData("aisystemhelper", "rightslash");
						ship.useSystem();
					}
					var = counter;
					if (var >= spin && var >= beam && var >= fwd && var >= left && var >= right && var >= strong) {
						ship.setCustomData("aisystemhelper", "counter");
						ship.useSystem();
					}
					var = strong;
					if (var >= spin && var >= beam && var >= fwd && var >= left && var >= right && var >= counter) {
						ship.setCustomData("aisystemhelper", "strongatk");
						ship.useSystem();
					}
				}
			}
		}
	}

	private float getThreatWeight(float range, ShipAPI ship)
	{
		float threatWeightTotal = 0f;
		for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
			if (enemy == null || enemy.getFleetMember() == null) {
				continue;
			}

			float weight = enemy.getFleetMember().getDeploymentCostSupplies();
			weight *= mults.get(enemy.getHullSize());
			if (enemy.getFluxTracker().isOverloadedOrVenting()) {
				weight *= 0.75f;
			}
			if (enemy.getHullLevel() < 0.4f) {
				weight *= 0.5f;
			}
			if (enemy.getFluxLevel() > 0.5f) {
				weight *= 0.5f;
			}
			if (enemy.getEngineController().isFlamedOut()) {
				weight *= 0.5f;
			}

			threatWeightTotal += weight;
		}
		return threatWeightTotal;
	}
}
