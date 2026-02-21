package data.scripts;


import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import data.scripts.campaign.TakeshidoMeiyoMusicScript;
import data.scripts.racing.TakeshidoRacingManager;
import data.scripts.world.takeshido_takeshidoGen;
import exerelin.campaign.SectorManager;
import org.magiclib.util.MagicSettings;

import java.util.Map;

public class takeshido_ModPlugin extends BaseModPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(takeshido_ModPlugin.class);

    @Override
    public void onNewGame() {
        Map<String, Object> data = Global.getSector().getPersistentData();
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");

        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            new takeshido_takeshidoGen().generate(Global.getSector());
            data.put("takeshido_generated", "1.0.0");
        }

        TakeshidoRacingManager.ensureCampaignPlugin();
        TakeshidoMeiyoMusicScript.ensure();
    }


    @Override
    public void onGameLoad(boolean wasEnabledBefore) {

        Map<String, Object> data = Global.getSector().getPersistentData();
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");

        if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
            if (data.get("takeshido_generated") == null) {
                new takeshido_takeshidoGen().generate(Global.getSector());
                data.put("takeshido_generated", "1.0.0");
            }
        }

        TakeshidoRacingManager.ensureCampaignPlugin();
        TakeshidoRacingManager.ensureRaceCoordinators();
        TakeshidoMeiyoMusicScript.ensure();
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        TakeshidoRacingManager.ensureRaceCoordinators();
        TakeshidoMeiyoMusicScript.ensure();

    }


    @Override
    public void onApplicationLoad() {
    }

}
