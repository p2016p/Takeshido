package data.missions.TheGift;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class MissionDefinition implements MissionDefinitionPlugin {

    public void defineMission(MissionDefinitionAPI api) {

        api.initFleet(FleetSide.PLAYER, "ECS", FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, "DVFS", FleetGoal.ATTACK, true);

//        api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 3);
//		api.getDefaultCommander(FleetSide.PLAYER).getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 3);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, "An Accursed Warrior.");
        api.setFleetTagline(FleetSide.ENEMY, "Father Albright's Divine Spear.");

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem("Ananke's Gift must survive.");
        api.addBriefingItem("Father Albright has brought a massive force to bear; Do not get surrounded.");

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters
        FleetMemberAPI member = api.addToFleet(FleetSide.PLAYER, "seven_ananke_gift", FleetMemberType.SHIP, "Ananke's Gift", true);

        FactionAPI sevencorp = Global.getSector().getFaction("sevencorp");
        PersonAPI officer = sevencorp.createRandomPerson(FullName.Gender.MALE);
        officer.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
        officer.getStats().setLevel(1);
        officer.setFaction(sevencorp.getId());
        officer.setPersonality(Personalities.STEADY);
        officer.getName().setFirst("Accursed");
        officer.getName().setLast("Warrior");
        officer.setPortraitSprite("graphics/portraits/portrait43.png");
        officer.getName().setGender(FullName.Gender.MALE);
        member.setCaptain(officer);

        // Set up the enemy fleet.
        api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, "The Biggun", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "atlas2_Standard", FleetMemberType.SHIP, "Saigor", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, "Shieldcrash", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "dominator_Outdated", FleetMemberType.SHIP, "Allowance", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "Wham", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "Bam", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "Laughing", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "Crying", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Ludd's Light", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Servant", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Servitude", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Ludd's Hope", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Dogmatic", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Pragmatic", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Deathbringer", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Mythical", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "brawler_pather_Raider", FleetMemberType.SHIP, "Slayer", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "colossus2_Pather", FleetMemberType.SHIP, "Last Lash", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "colossus2_Pather", FleetMemberType.SHIP, "Penance", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "lasher_luddic_path_Raider", FleetMemberType.SHIP, "Judgement", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "lasher_luddic_path_Raider", FleetMemberType.SHIP, "Excommunicate", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "lasher_luddic_path_Raider", FleetMemberType.SHIP, "Crime", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "lasher_luddic_path_Raider", FleetMemberType.SHIP, "Punishment", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "hound_luddic_path_Attack", FleetMemberType.SHIP, "Divine Light", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "hound_luddic_path_Attack", FleetMemberType.SHIP, "Flames of Rebirth", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "hound_luddic_path_Attack", FleetMemberType.SHIP, "Tormentor", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "hound_luddic_path_Attack", FleetMemberType.SHIP, "Punisher", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Raider", FleetMemberType.SHIP, "Lighthammer", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Strike", FleetMemberType.SHIP, "Devotions", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Raider", FleetMemberType.SHIP, "Crusader", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Strike", FleetMemberType.SHIP, "God is Good", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Raider", FleetMemberType.SHIP, "Wrath of Ludd", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Strike", FleetMemberType.SHIP, "God's Light", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Raider", FleetMemberType.SHIP, "Flamer", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "kite_luddic_path_Strike", FleetMemberType.SHIP, "Good Tidings", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "God's Will", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "Postmortem Reward", false).getCaptain().setPersonality("reckless");
        api.addToFleet(FleetSide.ENEMY, "manticore_pirates_Assault", FleetMemberType.SHIP, "2.1k Virgins", false).getCaptain().setPersonality("reckless");

        api.defeatOnShipLoss("Ananke's Gift");

        float width = 24000f;
        float height = 24000f;
        api.initMap((float)-width/2f, (float)width/2f, (float)-height/2f, (float)height/2f);

        float minX = -width/2;
        float minY = -height/2;

        for (int i = 0; i < 5; i++) { //15
            float x = (float) Math.random() * width - width/2;
            float y = (float) Math.random() * height - height/2;
            float radius = 100f + (float) Math.random() * 900f;
            api.addNebula(x, y, radius);
        }

        api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.4f, 2000);
        api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.5f, 2000);
        api.addNebula(minX + width * 0.8f - 1000, minY + height * 0.6f, 2000);
        api.addPlugin(new Plugin(width,height));
    }

    private final class Plugin extends BaseEveryFrameCombatPlugin {

        private boolean done = false;
        private final float mapX;
        private final float mapY;
        private float timer = 5f;

        private Plugin(float mapX, float mapY) {
            this.mapX = mapX;
            this.mapY = mapY;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (done || Global.getCombatEngine() == null || Global.getCombatEngine().isPaused()) {
                return;
            }

            timer -= amount;
            if (timer <= 0f) {
                for (FleetMemberAPI member : Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getReservesCopy()) {
                    if (!Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedCopy().contains(member)) {
                        Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).spawnFleetMember(member, getSafeSpawn(FleetSide.ENEMY, mapX, mapY), 270f, 1f);
                    }
                }
                done = true;
            }

            for (FleetMemberAPI member : Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedCopy()){
                if(!member.getVariant().hasHullMod("seven_finalrites")){
                    member.getVariant().addMod("seven_finalrites");
                }
            }
        }

        @Override
        public void init(CombatEngineAPI engine) {
        }

        private Vector2f getSafeSpawn(FleetSide side, float mapX, float mapY) {
            Vector2f spawnLocation = new Vector2f();

            spawnLocation.x = MathUtils.getRandomNumberInRange(-mapX / 2, mapX / 2);
            if (side == FleetSide.PLAYER) {
                spawnLocation.y = (-mapY / 2f);

            } else {
                spawnLocation.y = mapY / 2;
            }

            return spawnLocation;
        }
    }
}