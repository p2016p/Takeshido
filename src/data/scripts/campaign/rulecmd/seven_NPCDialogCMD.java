package data.scripts.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.interactionUI.*;

import java.util.List;
import java.util.Map;
seven_
public class takeshido_NPCDialogCMD extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        OptionPanelAPI options = dialog.getOptionPanel();
        options.clearOptions();

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        String cmd = null;

        cmd = params.get(0).getString(memoryMap);

        Global.getSector().getPersistentData().put("takeshido_originaldialog",dialog.getPlugin());

        //nuke: some refactors for neatness and performance, done with Lukas04's help

        InteractionDialogPlugin characterDialog=null;

        switch (cmd) {

            case "RacecoordinatorDialog":
                characterDialog = new seven_RacecoordinatorDialog();
                break;

            case "convertorExecDialog":
                characterDialog = new seven_convertorExecDialog();
                break;

        }

        dialog.setPlugin(characterDialog);
        characterDialog.init(dialog);

        return true;
    }
}
