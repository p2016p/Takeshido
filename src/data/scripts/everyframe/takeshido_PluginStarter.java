package data.scripts.everyframe;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.loading.Description;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import exerelin.utilities.NexFactionConfig.StartFleetSet;
import exerelin.utilities.NexFactionConfig.StartFleetType;
import java.util.ArrayList;
import java.util.List;

public class takeshido_PluginStarter extends BaseEveryFrameCombatPlugin {

    private boolean addedOnce = false;
    private boolean addedOnce1 = false;

    public static String ANANKEDESC = "A step towards complete transcendence. Your strength is vast and your potential is limitless. Honor the trust which has been placed upon you by the origin of your cause and those that revere him.";

    @Override
    public void init(CombatEngineAPI engine) {
        /*
        if (!addedOnce && Global.getSettings().getMissionScore("TheGift") > 0 && Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            NexFactionConfig faction = NexConfig.getFactionConfig("sevencorp");
            StartFleetSet fleetSet = faction.getStartFleetSet(StartFleetType.SUPER.name());
            List<String> anankeFleet = new ArrayList<>(1);
            anankeFleet.add("seven_ananke_gift");
            fleetSet.addFleet(anankeFleet);
            Global.getSettings().getDescription("seven_ananke", Description.Type.SHIP).setText1(ANANKEDESC);
            addedOnce = true;
        }

        if (!addedOnce1 && Global.getSettings().getModManager().isModEnabled("armaa") && !addedOnce1 && Global.getSettings().getModManager().isModEnabled("nexerelin")) {
            NexFactionConfig faction = NexConfig.getFactionConfig("pirates");
            StartFleetSet fleetSet = faction.getStartFleetSet(StartFleetType.CARRIER_LARGE.name());
            List<String> garageFleet = new ArrayList<>(9);
            garageFleet.add("seven_garage_sponsored");
            garageFleet.add("seven_hobi_custom_standard");
            garageFleet.add("seven_350x_custom_standard");
            garageFleet.add("seven_camirillo_custom_standard");
            garageFleet.add("seven_BR97_custom_standard");
            garageFleet.add("seven_NMW_G3_custom_standard");
            garageFleet.add("seven_vroomicorn_custom_standard");
            garageFleet.add("seven_bonta_custom_standard");
            garageFleet.add("seven_bionda_custom_standard");
            fleetSet.addFleet(garageFleet);
            addedOnce1 = true;
        }
        */
    }
}
