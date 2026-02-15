package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.HashSet;
import java.util.Set;

public class TakeshidoAddStrikecraft implements BaseCommand {
    private static final String TAG = "takeshidostrikecraft";

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP && context != CommandContext.CAMPAIGN_MARKET) {
            Console.showMessage("Command can only be used in campaign.");
            return CommandResult.WRONG_CONTEXT;
        }

        CampaignFleetAPI fleet = Global.getSector() != null ? Global.getSector().getPlayerFleet() : null;
        if (fleet == null) {
            Console.showMessage("Player fleet not found.");
            return CommandResult.ERROR;
        }

        Set<String> ownedHullIds = new HashSet<>();
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            ownedHullIds.add(member.getHullId());
        }

        int added = 0;
        int skipped = 0;
        int failed = 0;

        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (!spec.hasTag(TAG)) {
                continue;
            }

            String hullId = spec.getHullId();
            if (ownedHullIds.contains(hullId)) {
                skipped++;
                continue;
            }

            FleetMemberAPI member = createMember(spec);
            if (member == null) {
                Console.showMessage("Failed to add hull: " + hullId);
                failed++;
                continue;
            }

            fleet.getFleetData().addFleetMember(member);
            ownedHullIds.add(hullId);
            added++;
        }

        fleet.getFleetData().sort();
        Console.showMessage("Added " + added + " ships, skipped " + skipped + ", failed " + failed + ".");
        return CommandResult.SUCCESS;
    }

    private FleetMemberAPI createMember(ShipHullSpecAPI spec) {
        String hullId = spec.getHullId();
        String[] variantIds = new String[] {
                hullId + "_standard",
                hullId + "_Hull",
                hullId
        };

        for (String variantId : variantIds) {
            if (variantId == null || variantId.isEmpty()) {
                continue;
            }
            ShipVariantAPI variant = tryGetVariant(variantId);
            if (variant != null
                    && variant.getHullSpec() != null
                    && hullId.equals(variant.getHullSpec().getHullId())) {
                return Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            }
        }

        ShipVariantAPI fallback = Global.getSettings().createEmptyVariant(hullId + "_autogen", spec);
        return Global.getFactory().createFleetMember(FleetMemberType.SHIP, fallback);
    }

    private ShipVariantAPI tryGetVariant(String variantId) {
        try {
            return Global.getSettings().getVariant(variantId);
        } catch (Exception ex) {
            return null;
        }
    }
}
