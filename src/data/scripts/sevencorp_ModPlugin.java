package data.scripts;

// Testing 123

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import data.scripts.characters.eptaCharacterFactory;
import data.scripts.utils.takeshido_util_sysgen;
import data.scripts.world.sevencorpGenTakeshido;
import org.magiclib.util.MagicSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//nuke: Sometimes you can't safely import kotlin libraries into .java files. Importing extension methods won't work, as java don't have them. But importing regular classes should work.

public class sevencorp_ModPlugin extends BaseModPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(sevencorp_ModPlugin.class);

    // note: These variables MUST be initialized as false or else the faction enabling config stops working. While generation won't occur, the disabled factions will be added to intel, and without the correct relationships.

    // note: Part of the issue was caused by the use of OnEnabled. Breakpoints show that onNewGameAfterEconomyLoad() runs *before* onEnabled and have the correct config-derived variables there, but then OnEnabled runs and has the default variables there. Why? No fucking clue.
    // note: Even without OnEnabled, if the variables are initialized as true, then some other process adds the factions to intel anyway.
    // note: Testing with Nex enabled/not enabled shows that Nex has nothing to do with these bugs.

    // note: onNewGame is also dogfucking me - the check for whether Meiyo is null ain't working

    // note: I tried a complete reversion to RESTART. Got surveylevel bugs. I'll just try to patch that out and then call it there.

    // note: saatana perkele vittu, it's the config reading that's causing the crashes. going to switch from one OnApplicationLoad read to just reading it anew in each function in case that's causing memleaks somehow.

    @Override
    public void onNewGame() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        boolean isTakeshidoEnabled=MagicSettings.getBoolean("Takeshido","enableTakeshidoSubfaction");
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean haveSOTF = Global.getSettings().getModManager().isModEnabled("secretsofthefrontieralt");

        //nuke: I'm doing this in modplugin because I think it'll be faster than doing the isModEnabled call elsewhere. I'm not sure if it really is.
        if(haveSOTF){
            data.put("have_SOTF", "-");
        }

        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {

            if (isTakeshidoEnabled) {
                new sevencorpGenTakeshido().generate(Global.getSector());
                data.put("takeshido_generated", "1.6.4");
            }
        }
    }

    //nuke: no idea why onEnabled is only spawning the faction in intel but not the systems

    //nuke: I'm pretty sure checking by getEntityByID doesn't work, I've left it in for legacy reasons.


    @Override
    public void onGameLoad(boolean wasEnabledBefore) {

        boolean loadIntoExistingSave=MagicSettings.getBoolean("Takeshido","loadIntoExistingSave");

        if(loadIntoExistingSave) {

            ModManagerAPI modManager=Global.getSettings().getModManager();
            SectorAPI sector= Global.getSector();
            Map<String, Object> data = sector.getPersistentData();

            boolean isTakeshidoEnabled = MagicSettings.getBoolean("Takeshido", "enableTakeshidoSubfaction");
            boolean haveNexerelin = modManager.isModEnabled("nexerelin");
            boolean haveArmaa = modManager.isModEnabled("armaa");
            boolean haveSOTF = modManager.isModEnabled("secretsofthefrontieralt");




//            if (!data.containsKey("epta_campaign_listener_hullmod_added")) {
//                seven_util_misc.addGlobalSuppressedHullMod("Devtool Sevencorp Campaign Manager");
//                data.put("epta_campaign_listener_hullmod_added", "-");
//            }

            if (haveSOTF) {
                data.put("have_SOTF", "-");
            }
            if (!haveSOTF && (null != data.get("have_SOTF"))) {
                data.remove("have_SOTF");
            }

            StarSystemAPI meiyo_sys = sector.getStarSystem("Meiyo");

            if(ali_sys!=null&& data.get("$decivved_Alicerce")!=null){
                List<SectorEntityToken> entitiesInAlicerce=ali_sys.getAllEntities();
                for (SectorEntityToken entity: entitiesInAlicerce) {
                    MarketAPI dedEpta=entity.getMarket();
                    if(dedEpta!=null){
                        DecivTracker decivtracker=DecivTracker.getInstance(); //note: wait, this is a static object I think? I shouldn't need to do this???
                        decivtracker.decivilize(dedEpta, true, true);
                    }
                }
                data.put("$decivved_Alicerce","-");
            }

            //nuke: put in a ~~second~~ third layer of protection to prevent future double-spawning
            //nuke: I'm pretty sure those duplicate layers of protection don't even work and only the memkey matters.
            if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
                if (isEptaEnabled && !data.containsKey("epta_generated")){
                    MarketAPI proelefsi = new seven_proelefsi_mainscript().generate(Global.getSector());
                    seven_util_sysgen.exploreAll(proelefsi.getStarSystem());
                    new seven_mobile_storefront().generate(Global.getSector());

                    eptaCharacterFactory.createDecimus(proelefsi);
                    eptaCharacterFactory.createAshley(proelefsi);
//                    eptaCharacterFactory.createAnodyne(proelefsi);
//                    eptaCharacterFactory.createTriela(proelefsi);
//                    eptaCharacterFactory.createRoseKanta(proelefsi);
//                    eptaCharacterFactory.createAdProSecSecretary(proelefsi);
                    data.put("epta_generated", "version 1.6.4");
                }

                //            if(isSemiCivilizedRemnantsEnabled&&isEptaEnabled&&!data.containsKey("semiCivilizedRemnants_generated")){
                //                new sevencorpGenSemiCivilizedRemnant().generate(Global.getSector());
                //                data.put("semiCivilizedRemnants_generated", "-");
                //            }

                if (isTakeshidoEnabled && !data.containsKey("takeshido_generated") && Global.getSector().getEntityById("meiyo_star") == null && meiyo_sys == null) {
                    new sevencorpGenTakeshido().generate(Global.getSector());
                    data.put("takeshido_generated", "1.6.4");
                }
            }

            //        if (!data.containsKey("epta_generated") && Global.getSector().getEntityById("sevencorp_alicerce") == null) {
            //            new sevencorpGenEpta().generate(Global.getSector());
            //        }
            //        if (!data.containsKey("takeshido_generated") && Global.getSector().getEntityById("meiyo_star") == null) {
            //            new sevencorpGenTakeshido().generate(Global.getSector());
            //        }

            if (meiyo_sys != null) {
                seven_util_sysgen.exploreAll(meiyo_sys);
            }

            //nuke: adds a sort of fleet/campaign manager in hullmods, designed to do calculations during onFleetSync which can't be handled by efficient listeners.

            //nuke: data.hullmods.seven_campaignManager_hullMod

            //seven_AIMod_Utils.applyHullmodToAllPlayerShips("seven_campaignManager");


            if (haveArmaa) {
                //// adding the Mibitsu Hobi Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_Hobi_Custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_Hobi_Custom");
                //                // adding the Evade Camirillo GT Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_camirillo_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_camirillo_custom");
                //                // adding the 350x Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_350x_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_350x_custom");
                //                // adding the BR97 Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_BR97_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_BR97_custom");
                //                // adding the Bonta Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_bonta_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_bonta_custom");
                //                // adding the Bionda Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_bionda_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_bionda_custom");
                //                // adding the NMW_G3 Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_NMW_G3_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_NMW_G3_custom");
                //                // adding the vroomicorn Custom
                //                Global.getSector().getFaction("pirates").getKnownShips().add("seven_vroomicorn_custom");
                //                Global.getSector().getFaction("pirates").addUseWhenImportingShip("seven_vroomicorn_custom");



                // adding the Galevis
                sector.getFaction("sevencorp").getKnownShips().add("seven_galevis");
                sector.getFaction("sevencorp").addUseWhenImportingShip("seven_galevis");
                // adding the Skopefetis
                sector.getFaction("sevencorp").getKnownShips().add("seven_skopefetis");
                sector.getFaction("sevencorp").addUseWhenImportingShip("seven_skopefetis");
                // adding both Zakus
                sector.getFaction("sevencorp").getKnownShips().add("seven_zaku");
                sector.getFaction("sevencorp").addUseWhenImportingShip("seven_zaku");
                sector.getFaction("sevencorp").getKnownShips().add("seven_zaku2");
                sector.getFaction("sevencorp").addUseWhenImportingShip("seven_zaku2");


                for(ShipHullSpecAPI s : Global.getSettings().getAllShipHullSpecs()){
                    if(s.hasTag("bushidostrikecraft")){
                        if(!s.hasTag("bushido")){
                            s.addTag("bushido");
                        }
                        sector.getFaction("bushido").getKnownShips().add(s.getHullId());
                        sector.getFaction("bushido").addUseWhenImportingShip(s.getHullId());
                    }
                }

                if (!seven_frigateautoforge.ships.contains("seven_galevis_integrated"))
                    seven_frigateautoforge.ships.add("seven_galevis_integrated");
                if (!seven_frigateautoforge.ships.contains("seven_skopefetis_integrated"))
                    seven_frigateautoforge.ships.add("seven_skopefetis_integrated");
            }

//            FactionAPI pirateFaction = Global.getSector().getFaction(Factions.PIRATES);

            //if (isTakeshidoEnabled) {
            //                List<ShipHullSpecAPI> allShips = Global.getSettings().getAllShipHullSpecs();
            //                for (ShipHullSpecAPI ship : allShips) {
            //                    if (ship.hasTag("bushido") || (ship.hasTag("bushidostrikecraft") && haveArmaa)) {
            //                        String hullID = ship.getBaseHullId();
            //                        pirateFaction.addKnownShip(hullID, false);
            //                        pirateFaction.useWhenImportingShip(hullID);
            //                    }
            //                }
            //                List<WeaponSpecAPI> allWeapons = Global.getSettings().getAllWeaponSpecs();
            //                for (WeaponSpecAPI weapon : allWeapons) {
            //                    if (weapon.hasTag("bushido")) {
            //                        String weaponID = weapon.getWeaponId();
            //                        pirateFaction.addKnownWeapon(weaponID, false);
            //                    }
            //                }
            //                List<FighterWingSpecAPI> allWings = Global.getSettings().getAllFighterWingSpecs();
            //                for (FighterWingSpecAPI wing : allWings) {
            //                    if (wing.hasTag("bushido")) {
            //                        String wingID = wing.getId();
            //                        pirateFaction.addKnownFighter(wingID, false);
            //                    }
            //                }
            //            }


            Global.getSector().getFaction("pirates").clearShipRoleCache();
            Global.getSector().getFaction("sevencorp").clearShipRoleCache();



                    /* if(!Global.getSector().getIntelManager().hasIntelOfClass(seven_CharacterInteractIntel.class)) {
                        Global.getSector().getIntelManager().addIntel(new seven_CharacterInteractIntel(), true);
                    }
                    */

            //final int midSavePatchVer;
            //
            //                    if( data.get("epta_midSavePatchCount")!=null){
            //                        midSavePatchVer = (int) data.get("epta_midSavePatchCount");
            //
            //                        //nuke: It is inevitable that we will need to overwrite previous changes, as we will not get the character interaction work perfect in one go. Changes will be made through a series of patches can be applied mid-game, and we will check for all patches not yet applied and run them all in order.
            //
            //
            //                    }
            //                    else{
            //                        midSavePatchVer=0;
            //                    }
            //
            //            //        note: this will go from smallest number to biggest number: ie. 0 to the number of the final midSavePatchVer. Do not break, just continue down and run all the needed patches in order until the final one.
            //
            //                    switch (midSavePatchVer) {
            //                        case 0:
            //                            data.put("epta_midSavePatchCount", 1);
            //                            EdSullivan.createCharacter();
            //                            SectorAPI sector = Global.getSector();
            //                            FactionAPI faction = sector.getFaction("sevencorp");
            //                            StarSystemAPI system = sector.getStarSystem("Alicerce");
            //                            SectorEntityToken station = system.getEntityById("zetaComputers_alicerce");
            //                            station.setCustomDescriptionId("sevencorp_zeta_computers_station");
            //                    }

            //if(haveNexerelin){
            //                MercDataManager.MercCompanyDef animeprotags = new MercDataManager.MercCompanyDef();
            //                animeprotags.averageSMods=5;
            //                animeprotags.desc="Anime Protags";
            //                animeprotags.id="animeprotags";
            //                List<List<String>> shiplist = new ArrayList<>();
            //                shiplist.add(
            //                animeprotags.ships=shiplist;
            //
            //
            //                MercDataManager.getAllDefs().add(animeprotags);
            //            }
        }
    }

    @Override
    public void onNewGameAfterEconomyLoad() {


    }


    @Override
    public void onApplicationLoad() {
        boolean haveArmaa = Global.getSettings().getModManager().isModEnabled("armaa");

        Global.getSettings().resetCached();

        //if (haveArmaa) {
        //            // adding the Mibitsu Hobi Custom
        //            //Global.getSettings().getHullSpec("seven_Hobi_Custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_Hobi_Custom").addTag("bushido");
        //            // adding the Evade Camirillo GT Custom
        //            //Global.getSettings().getHullSpec("seven_camirillo_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_camirillo_custom").addTag("bushido");
        //            // adding the 350x Custom
        //            //Global.getSettings().getHullSpec("seven_350x_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_350x_custom").addTag("bushido");
        //            // adding the BR97 Custom
        //            //Global.getSettings().getHullSpec("seven_BR97_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_BR97_custom").addTag("bushido");
        //            // adding the Bonta Custom
        //           // Global.getSettings().getHullSpec("seven_bonta_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_bonta_custom").addTag("bushido");
        //            // adding the Bionda Custom
        //            //Global.getSettings().getHullSpec("seven_bionda_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_bionda_custom").addTag("bushido");
        //            // adding the NMW_G3 Custom
        //            //Global.getSettings().getHullSpec("seven_NMW_G3_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_NMW_G3_custom").addTag("bushido");
        //            // adding the vroomicorn Custom
        //            //Global.getSettings().getHullSpec("seven_vroomicorn_custom").addTag("rare_bp");
        //            Global.getSettings().getHullSpec("seven_vroomicorn_custom").addTag("bushido");
        //            // adding the Galevis
        //            Global.getSettings().getHullSpec("seven_galevis").addTag("sevencorp");
        //            Global.getSettings().getHullSpec("seven_galevis").addTag("epta_phaseship_bp");
        //            // adding the Skopefetis
        //            Global.getSettings().getHullSpec("seven_skopefetis").addTag("sevencorp");
        //            Global.getSettings().getHullSpec("seven_skopefetis").addTag("epta_phaseship_bp");
        //        }
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case "seven_sminos_lighttest":
                return new PluginPick<MissileAIPlugin>(new seven_sminos_rocket_AI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        switch (weapon.getId()) {
            case "seven_sminos_leftlight":
            case "seven_sminos_rightlight":
                return new PluginPick<AutofireAIPlugin>(new seven_sminos_autofireAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }

//    public static void addEptaTransients() {
//
//    }
}