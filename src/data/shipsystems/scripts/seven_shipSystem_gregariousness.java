package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class seven_shipSystem_gregariousness extends BaseShipSystemScript {
	public static final float MAX_TIME_MULT = 1.75f;
	public static final float MIN_TIME_MULT = 0.1f;
	public static final float DAM_MULT = 0.1f;
	
	public static final Color JITTER_COLOR = new Color(163,185,216,25);
	public static final Color JITTER_UNDER_COLOR = new Color(163,185,216,45);

	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
	
		if(effectLevel>=1){
			if(!AIUtils.getNearbyEnemies(ship,10000).isEmpty()){
				for(ShipAPI enemy : AIUtils.getNearbyEnemies(ship,10000)){
					if(AIUtils.getNearestAlly(enemy)==null || MathUtils.getDistance(AIUtils.getNearestAlly(enemy).getLocation(),enemy.getLocation())>enemy.getCollisionRadius()+ship.getCollisionRadius()*2){
						engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(true);
						Vector2f pos = MathUtils.getRandomPointOnCircumference(enemy.getLocation(),enemy.getCollisionRadius()+ship.getCollisionRadius());
						ShipAPI copy = engine.getFleetManager(ship.getOwner()).spawnShipOrWing("seven_artifact667_copy_variant", pos, VectorUtils.getAngle(pos,enemy.getLocation()));
						if(enemy.getFleetMember()!=null) {
							copy.getCustomData().put("targetfp", enemy.getFleetMember().getFleetPointCost() / 50f);
						}
						engine.getFleetManager(ship.getOwner()).setSuppressDeploymentMessages(false);
					}
				}
			}
		}
	}
	
	
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		boolean player = false;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
			id = id + "_" + ship.getId();
		} else {
			return;
		}

	}

	public StatusData getStatusData(int index, State state, float effectLevel) {

		return null;
                
        }
        
}