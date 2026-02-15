package data.missions.TestRace;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import data.scripts.combat.TakeshidoRaceCombatPlugin;

import java.util.HashMap;
import java.util.Map;

public class MissionDefinition implements MissionDefinitionPlugin {


    @Override
    public void defineMission(MissionDefinitionAPI api) {
        // Set up fleets
        api.initFleet(FleetSide.PLAYER, "TKS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY,  "CPU", FleetGoal.ATTACK, true);

        api.setFleetTagline(FleetSide.PLAYER, "Ares");
        api.setFleetTagline(FleetSide.ENEMY,  "The Latest and Greatest");

        api.addBriefingItem("Complete 3 laps. First finisher wins.");
        api.addBriefingItem("Your car is extremely modified and be more than a match for the other racers.");

        FleetMemberAPI player = api.addToFleet(FleetSide.PLAYER, "takeshido_ares_special_standard", FleetMemberType.SHIP, "Ares Special", true);

        FleetMemberAPI cpu2 = api.addToFleet(FleetSide.ENEMY, "takeshido_sweetjps_transam20000wr_standard", FleetMemberType.SHIP, "Sweet JPs Trans Am 20000WR", false);
        FleetMemberAPI cpu3 = api.addToFleet(FleetSide.ENEMY, "takeshido_escudo_standard", FleetMemberType.SHIP, "Suzuki Escudo", false);
        FleetMemberAPI cpu4 = api.addToFleet(FleetSide.ENEMY, "takeshido_917_30_standard", FleetMemberType.SHIP, "Porsche 917/30", false);
        FleetMemberAPI cpu5 = api.addToFleet(FleetSide.ENEMY, "takeshido_919_standard", FleetMemberType.SHIP, "Porsche 919", false);
        FleetMemberAPI cpu6 = api.addToFleet(FleetSide.ENEMY, "takeshido_veyron_standard", FleetMemberType.SHIP, "Bugatti Veyron", false);
        FleetMemberAPI cpu7 = api.addToFleet(FleetSide.ENEMY, "takeshido_f1_standard", FleetMemberType.SHIP, "McLaren F1", false);
        FleetMemberAPI cpu8 = api.addToFleet(FleetSide.ENEMY, "takeshido_zonda_cinque_standard", FleetMemberType.SHIP, "Pagani Zonda Cinque", false);

        Map<String, Float> skillOverrides = new HashMap<>();
        if (player != null) skillOverrides.put(player.getId(), 1.0f);
        if (cpu2 != null) skillOverrides.put(cpu2.getId(), 0.8f);
        if (cpu3 != null) skillOverrides.put(cpu3.getId(), 0.8f);
        if (cpu4 != null) skillOverrides.put(cpu4.getId(), 0.8f);
        if (cpu5 != null) skillOverrides.put(cpu5.getId(), 0.8f);
        if (cpu6 != null) skillOverrides.put(cpu6.getId(), 0.8f);
        if (cpu7 != null) skillOverrides.put(cpu7.getId(), 0.8f);
        if (cpu8 != null) skillOverrides.put(cpu8.getId(), 0.8f);

        api.defeatOnShipLoss("Ares Special");

        // Map
        api.initMap(-40000f, 40000f, -40000f, 40000f);

        // make sure we deploy everyone or else it wont start properly
        api.getContext().enemyDeployAll = true;
        api.getContext().fightToTheLast = true;

        // Race controller plugin
        api.addPlugin(new TakeshidoRaceCombatPlugin("takeshido_racemod", 1, 8, false, "sochi", skillOverrides));

    }
}


