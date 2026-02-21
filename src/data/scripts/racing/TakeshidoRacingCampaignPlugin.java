package data.scripts.racing;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.BattleCreationPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

public class TakeshidoRacingCampaignPlugin extends BaseCampaignPlugin {
    public static final String PLUGIN_ID = "takeshido_racing_plugin";

    @Override
    public String getId() {
        return PLUGIN_ID;
    }

    @Override
    public PluginPick<BattleCreationPlugin> pickBattleCreationPlugin(SectorEntityToken opponent) {
        if (opponent instanceof CampaignFleetAPI) {
            MemoryAPI memory = ((CampaignFleetAPI) opponent).getMemoryWithoutUpdate();
            String raceId = memory.getString(TakeshidoRacingManager.RACE_FLEET_MEMORY_KEY);
            if (raceId != null && !raceId.isEmpty()) {
                return new PluginPick<BattleCreationPlugin>(new TakeshidoRaceBattleCreationPlugin(raceId), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            }
        }
        return null;
    }
}
