package data.scripts;


import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import data.scripts.utils.takeshido_util_sysgen;
import data.scripts.world.takeshido_takeshidoGen;
import exerelin.campaign.SectorManager;
import org.magiclib.util.MagicSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//nuke: Sometimes you can't safely import kotlin libraries into .java files. Importing extension methods won't work, as java don't have them. But importing regular classes should work.

public class sevencorp_ModPlugin extends BaseModPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(sevencorp_ModPlugin.class);

    @Override
    public void onNewGame() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        boolean isTakeshidoEnabled = MagicSettings.getBoolean("Takeshido", "enableTakeshidoSubfaction");
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        boolean haveSOTF = Global.getSettings().getModManager().isModEnabled("secretsofthefrontieralt");

        if (haveSOTF) {
            data.put("have_SOTF", "-");
        }

        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {

            if (isTakeshidoEnabled) {
                new takeshido_takeshidoGen().generate(Global.getSector());
                data.put("takeshido_generated", "1.0.0");
            }
        }
    }


    @Override
    public void onGameLoad(boolean wasEnabledBefore) {

        boolean loadIntoExistingSave = MagicSettings.getBoolean("Takeshido", "loadIntoExistingSave");

        if (loadIntoExistingSave) {

            Map<String, Object> data = Global.getSector().getPersistentData();
            boolean isTakeshidoEnabled = MagicSettings.getBoolean("Takeshido", "enableTakeshidoSubfaction");
            boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
            boolean haveSOTF = Global.getSettings().getModManager().isModEnabled("secretsofthefrontieralt");

            if (haveSOTF) {
                data.put("have_SOTF", "-");
            }

            if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {

                if (isTakeshidoEnabled) {
                    new takeshido_takeshidoGen().generate(Global.getSector());
                    data.put("takeshido_generated", "1.0.0");
                }
            }

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

    /*
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

     */

}