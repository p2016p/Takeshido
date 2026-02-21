package data.scripts.racing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import data.scripts.combat.TakeshidoRaceCombatPlugin;

import java.util.HashMap;
import java.util.Map;

public class TakeshidoRaceBattleCreationPlugin extends BattleCreationPluginImpl {
    private final String raceId;

    public TakeshidoRaceBattleCreationPlugin(String raceId) {
        this.raceId = raceId;
    }

    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        TakeshidoRacingManager.RaceContext race = TakeshidoRacingManager.getActiveRace(raceId);
        if (race == null) {
            super.initBattle(context, api);
            return;
        }

        context.aiRetreatAllowed = false;
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.fightToTheLast = true;

        api.initFleet(FleetSide.PLAYER, "TKS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "CPU", FleetGoal.ATTACK, true);

        Map<String, Float> skillOverrides = new HashMap<>();

        FleetMemberAPI playerMember = race.playerFleetMember;
        if (playerMember == null) {
            super.initBattle(context, api);
            return;
        }

        playerMember.setFlagship(true);
        playerMember.setCaptain(Global.getSector().getPlayerPerson());
        api.addFleetMember(FleetSide.PLAYER, playerMember);

        race.playerMemberId = playerMember.getId();
        race.memberIdToRacerId.put(playerMember.getId(), race.playerRacerId);
        skillOverrides.put(playerMember.getId(), 1.0f);

        int index = 0;
        int aiAdded = 0;
        for (TakeshidoRacingManager.RacerDefinition def : race.aiRacers) {
            FleetMemberAPI ai = def.member;
            if (ai == null) {
                if (def.variantId != null && !def.variantId.trim().isEmpty()) {
                    ai = TakeshidoRacingManager.createFleetMemberForVariant(def.variantId, def.name);
                } else {
                    ai = TakeshidoRacingManager.createFleetMemberForHull(def.hullId, def.name);
                }
            }
            if (ai == null) continue;
            if (def.member == null) {
                ai.setId(race.raceId + "_ai_" + index);
            }
            if (def.captain != null && ai.getCaptain() == null) {
                ai.setCaptain(def.captain);
            }
            ensureCrewAndCR(ai);
            api.addFleetMember(FleetSide.ENEMY, ai);

            race.memberIdToRacerId.put(ai.getId(), def.racerId);
            skillOverrides.put(ai.getId(), def.skill);
            index++;
            aiAdded++;
        }

        int expectedRacers = Math.max(1, 1 + aiAdded);
        race.expectedRacers = expectedRacers;

        api.initMap(-40000f, 40000f, -40000f, 40000f);
        api.getContext().enemyDeployAll = true;
        api.getContext().fightToTheLast = true;

        api.addPlugin(new TakeshidoRaceCombatPlugin(
                TakeshidoRacingManager.RACE_HULLMOD_ID,
                race.lapsToWin,
                expectedRacers,
                false,
                race.trackId,
                skillOverrides,
                race.raceId
        ));
    }

    private void ensureCrewAndCR(FleetMemberAPI member) {
        if (member == null || member.getRepairTracker() == null) return;
        if (member.getStatus() != null) {
            member.getStatus().repairFully();
            member.getStatus().setHullFraction(1f);
            member.getStatus().repairArmorAllCells(1f);
            member.getStatus().resetDamageTaken();
        }
        if (member.getCrewComposition() != null) {
            float crew = member.getNeededCrew();
            if (crew <= 0f) crew = member.getMinCrew();
            if (crew <= 0f) crew = 1f;
            member.getCrewComposition().setCrew(crew);
        }
        member.getRepairTracker().setMothballed(false);
        member.getRepairTracker().setCrashMothballed(false);
        float max = member.getRepairTracker().getMaxCR();
        if (max <= 0f) max = 1f;
        float target = TakeshidoRacingManager.ROSTER_TARGET_CR;
        if (target > max) target = max;
        member.getRepairTracker().setCR(target);
        member.getRepairTracker().setCRPriorToMothballing(target);
        member.setStatUpdateNeeded(true);
    }


    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
    }
}
