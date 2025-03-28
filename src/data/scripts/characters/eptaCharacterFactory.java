package data.scripts.characters;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import data.scripts.utils.charLib.seven_charLib_components.seven_PersonAPI_extended;

import java.util.ArrayList;
import java.util.List;

public class eptaCharacterFactory {

    public static void createDecimus(MarketAPI market) {

        PersonAPI decimusmaximus = Global.getFactory().createPerson();
        decimusmaximus.setFaction("sevencorp");
        decimusmaximus.setGender(FullName.Gender.MALE);
        decimusmaximus.setPostId(Ranks.POST_FACTION_LEADER);
        decimusmaximus.setRankId(Ranks.FACTION_LEADER);
        decimusmaximus.getName().setFirst("Decimus");
        decimusmaximus.getName().setLast("Maximus");
        decimusmaximus.setPortraitSprite("graphics/portraits/decimusmaximus.png");
        decimusmaximus.setAICoreId(Commodities.ALPHA_CORE);
        decimusmaximus.setId("epta_decimus");
        decimusmaximus.getStats().setLevel(20);
        OfficerManagerEvent.addEliteSkills(decimusmaximus, 100, null);
        if (decimusmaximus.getStats().getSkillLevel(Skills.GUNNERY_IMPLANTS) == 0) {
            decimusmaximus.getStats().increaseSkill(Skills.GUNNERY_IMPLANTS);}
        decimusmaximus.setImportance(PersonImportance.VERY_HIGH);
        market.getCommDirectory().addPerson(decimusmaximus);

        if(!Global.getSector().getImportantPeople().containsPerson(decimusmaximus)) Global.getSector().getImportantPeople().addPerson(decimusmaximus);
    }

    public static void createAnodyne(MarketAPI market){
        PersonAPI anodyne = Global.getFactory().createPerson();
        anodyne.setFaction("sevencorp");
        anodyne.setGender(FullName.Gender.MALE);
        anodyne.setPostId(Ranks.POST_FACTION_LEADER);
        anodyne.setRankId(Ranks.FACTION_LEADER);
        anodyne.getName().setFirst("Anodyne");
        anodyne.getName().setLast("Indefatigable");
        anodyne.setPortraitSprite("graphics/portraits/anodyne.png");
        anodyne.setAICoreId(Commodities.ALPHA_CORE);
        anodyne.setId("epta_anodyne");
        anodyne.getStats().increaseSkill("hypercognition");
        market.setAdmin(anodyne);
        market.getCommDirectory().addPerson(anodyne);

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
        memory.set("$AnodyneRemembersInsult", false);
        memory.set("$AnodyneRefusingToTalk", false);
//        memory.set("$AnodyneIsAnnoyed", false);

        if(!Global.getSector().getImportantPeople().containsPerson(anodyne)) Global.getSector().getImportantPeople().addPerson(anodyne);


        String seven_person_ExtendedID="$anodyne_character_extended";

        seven_PersonAPI_extended person_extended= new seven_PersonAPI_extended(anodyne, seven_person_ExtendedID);
        person_extended.setFormalTitleOutsider("Chairman");
        person_extended.setFormalTitleInsider("Chairman");

        if(memory.get(seven_person_ExtendedID)==null) {
            memory.set(seven_person_ExtendedID, person_extended);
        }
    }

    public static void createAshley(MarketAPI market){
        PersonAPI ashley = Global.getFactory().createPerson();
        ashley.setFaction("sevencorp");
        ashley.setGender(FullName.Gender.FEMALE);
        ashley.setPostId(Ranks.POST_FACTION_LEADER);
        ashley.setRankId(Ranks.FACTION_LEADER);
        ashley.getName().setFirst("Ashley");
        ashley.getName().setLast("St. Tesserae");
        ashley.setPortraitSprite("graphics/portraits/ashleysttesserae.png");
        ashley.setId("epta_ashley");
        market.getCommDirectory().addPerson(ashley);

        if(!Global.getSector().getImportantPeople().containsPerson(ashley)) Global.getSector().getImportantPeople().addPerson(ashley);
    }

    public static void createNathan(MarketAPI market){
        PersonAPI nathan = Global.getFactory().createPerson();
        nathan.setFaction("sevencorp");
        nathan.setGender(FullName.Gender.MALE);
        nathan.setPostId(Ranks.POST_FACTION_LEADER);
        nathan.setRankId(Ranks.FACTION_LEADER);
        nathan.getName().setFirst("Nathan");
        nathan.getName().setLast("Burke");
        nathan.setPortraitSprite("graphics/portraits/nathan.png");
        nathan.setId("epta_nathan");
        market.getCommDirectory().addPerson(nathan);

        if(!Global.getSector().getImportantPeople().containsPerson(nathan)) Global.getSector().getImportantPeople().addPerson(nathan);
    }

    public static void createTriela(MarketAPI market){
        PersonAPI triela = Global.getFactory().createPerson();
        triela.setFaction("sevencorp");
        triela.setGender(FullName.Gender.FEMALE);
        triela.setPostId(Ranks.POST_SENIOR_EXECUTIVE);
        //nuke: I don't know what rank is appropriate
        triela.setRankId(Ranks.CITIZEN);
        triela.getName().setFirst("Triela");
        triela.getName().setLast("Aldens");
        triela.setPortraitSprite("graphics/portraits/triela.png");
        triela.setId("epta_triela");
        market.getCommDirectory().addPerson(triela);

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        String seven_person_ExtendedID="$triela_character_extended";

        seven_PersonAPI_extended triela_extended= new seven_PersonAPI_extended(triela, seven_person_ExtendedID);
        triela_extended.setFormalTitleOutsider("Chairwoman");
        triela_extended.setFormalTitleInsider("Matron");

        if(memory.get(seven_person_ExtendedID)==null) {
            memory.set(seven_person_ExtendedID, triela_extended);
        }

        if(!Global.getSector().getImportantPeople().containsPerson(triela)) Global.getSector().getImportantPeople().addPerson(triela);
    }

    //nuke: The leadership of AdProSec are a small group with unknown numbers, who swap places and use face concealers and vocal scramblers so it is never known exactly who is being interacted with. They are fanatically and *irrationally* paranoid, to the point where they never reveal their individual identities to *anybody* but possibly each other under any circumstance.

    public static void createAdProSec(MarketAPI market){
        PersonAPI adProSec = Global.getFactory().createPerson();
        adProSec.setFaction("sevencorp");
        adProSec.setGender(FullName.Gender.ANY);
        adProSec.setPostId(Ranks.POST_SENIOR_EXECUTIVE);
        //nuke: I don't know what rank is appropriate
        adProSec.setRankId(Ranks.CITIZEN);
        adProSec.getName().setFirst("ANONYMIZED");
        adProSec.getName().setLast("PROSECLEAD");
        //TODO add unique portrait sprite
        adProSec.setPortraitSprite("graphics/portraits/anodyne.png");
        adProSec.setId("epta_AdProSec");
        //market.getCommDirectory().addPerson(adProSec);
    }

    public static void createRoseKanta(MarketAPI market) {
        PersonAPI roseKanta = Global.getFactory().createPerson();
        roseKanta.setFaction("sevencorp");
        roseKanta.setGender(FullName.Gender.FEMALE);
        roseKanta.setPostId(Ranks.POST_SENIOR_EXECUTIVE);
        //nuke: I don't know what rank is appropriate
        roseKanta.setRankId(Ranks.POST_GANGSTER);
        roseKanta.getName().setFirst("Rose");
        roseKanta.getName().setLast("Kanta");
        //nuke: TODO: make kanta portrait
//        roseKanta.setPortraitSprite("graphics/portraits/triela.png");
        roseKanta.setId("epta_triela");
        market.getCommDirectory().addPerson(roseKanta);

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        String seven_person_ExtendedID = "$roseKanta_character_extended";

        seven_PersonAPI_extended roseKanta_extended = new seven_PersonAPI_extended(roseKanta, seven_person_ExtendedID);
        roseKanta_extended.setFormalTitleOutsider("Chairwoman");
        roseKanta_extended.setFormalTitleInsider("Boss");

        if (memory.get(seven_person_ExtendedID) == null) {
            memory.set(seven_person_ExtendedID, roseKanta_extended);
        }

        if(!Global.getSector().getImportantPeople().containsPerson(roseKanta)) Global.getSector().getImportantPeople().addPerson(roseKanta);
    }

    public static void createPianist(MarketAPI market) {
        PersonAPI pianist = Global.getFactory().createPerson();
        pianist.setFaction("sevencorp");
        pianist.setGender(FullName.Gender.FEMALE);
        pianist.setPostId(Ranks.POST_CITIZEN);
        //nuke: yeah I'm really scraping the barrel in terms of emotional manipulation here
        pianist.setRankId(Ranks.MOTHER);
        pianist.getName().setFirst("Sydney");
        pianist.getName().setLast("Taylor");
        //nuke: TODO: make dead pianist portrait. as to extract sympathy, make her young, female, and attractive. we all understand the halo effect, it's nothing personal.
//        roseKanta.setPortraitSprite("graphics/portraits/triela.png");
        pianist.setId("epta_pianist");

//        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
//
//        String seven_person_ExtendedID = "$pianist_character_extended";
//
//        seven_PersonAPI_extended pianist_extended = new seven_PersonAPI_extended(pianist, seven_person_ExtendedID);
//        pianist_extended.setFormalTitleOutsider("Chairwoman");
//        pianist_extended.setFormalTitleInsider("Boss");
//
//        if (memory.get(seven_person_ExtendedID) == null) {
//            memory.set(seven_person_ExtendedID, pianist_extended);
//        }
    }

    public static void createAdProSecSecretary(MarketAPI market) {
        PersonAPI secretary = Global.getFactory().createPerson();
        secretary.setFaction("sevencorp");
        secretary.setGender(FullName.Gender.FEMALE);
        secretary.setPostId(Ranks.POST_CITIZEN);
        //nuke: this is a designated "waifu" and I am determined to make this cursed
        secretary.setRankId(Ranks.SISTER);
        secretary.getName().setFirst("Lovely");
        secretary.getName().setLast("Heart");
        secretary.setId("epta_AdProSecSecretary");
        //nuke: TODO get a conventionally attractive female portrait as the baseline, that'll do it

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        String seven_person_ExtendedID = "$AdProSecSecretary_character_extended";

        seven_PersonAPI_extended character_extended = new seven_PersonAPI_extended(secretary, seven_person_ExtendedID);
        character_extended.setFormalTitleOutsider("Darling");
        character_extended.setFormalTitleInsider("SIGNIT");

        if (memory.get(seven_person_ExtendedID) == null) {
            memory.set(seven_person_ExtendedID, character_extended);
        }
    }
}


