package data.scripts.campaign.rulecmd;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class TakeshidoRaceRuleCMD extends BaseCommandPlugin {
    private static org.apache.log4j.Logger log = Logger.getLogger(TakeshidoRaceRuleCMD.class);
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        log.info("racism test.");
        //takeshidoRace race1 = new takeshidoRace();
        //nuke: This is pretty weird but it's how it's done in vanilla (BarCMD.java:105)
        //nuke: you need to do it that way because if you try to call a bar event script directly through rules.csv, it'll CTD and complain that you can't cast to bar event or something.
//        BarCMD cmd = (BarCMD) getEntityMemory(memoryMap).get("$BarCMD");
//        BarEventDialogPlugin plugin = new BarEventDialogPlugin(cmd, dialog.getPlugin(), race1, memoryMap);
//        dialog.setPlugin(plugin);
//        plugin.init(dialog);
        return true;
    }
}