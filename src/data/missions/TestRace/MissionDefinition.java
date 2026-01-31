package data.missions.TestRace;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import data.scripts.combat.TakeshidoRaceCombatPlugin;

public class MissionDefinition implements MissionDefinitionPlugin {


    @Override
    public void defineMission(MissionDefinitionAPI api) {
        // Set up fleets
        api.initFleet(FleetSide.PLAYER, "TKS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY,  "CPU", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Takeshido test driver");
        api.setFleetTagline(FleetSide.ENEMY,  "Rival drivers");

        api.addBriefingItem("Complete 3 laps. First finisher wins.");
        api.addBriefingItem("All cars use takeshido_racemod hullmod.");

        // IMPORTANT: These are VARIANT ids (data/variants/*.variant), not hull ids. :contentReference[oaicite:5]{index=5}
        api.addToFleet(FleetSide.PLAYER, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "TKS Player", true);

        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 2", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 3", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 4", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 5", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 6", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 7", false);
        api.addToFleet(FleetSide.ENEMY, "takeshido_vroomicorn_custom_standard", FleetMemberType.SHIP, "CPU 8", false);

        // If the player ship dies, call it a loss
        api.defeatOnShipLoss("TKS Player");

        // Map
        api.initMap(-12000f, 12000f, -12000f, 12000f);

        // Race controller plugin :contentReference[oaicite:6]{index=6}
        api.addPlugin(new TakeshidoRaceCombatPlugin("takeshido_racemod", 3, 8, false, "circle_small"));

    }
}

