package data.scripts.campaign.rulecmd.isCommssionedChecks_rulecmd;

//nuke: basically the SFC "sfcemployee" script. thanks to WMGreyWind for telling me how it's done.

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class isTTCommissioned_ruleCMD extends BaseCommandPlugin {
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null) return false;
        return (Factions.TRITACHYON.equals(Misc.getCommissionFactionId()));
    }
}
