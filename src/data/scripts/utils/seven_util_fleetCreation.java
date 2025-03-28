package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

//nuke: none of these are used, were written before I was aware of FleetParamsV3. manipulating fleetparams would probably be strictly better than shoving every argument  in individually

public class seven_util_fleetCreation {

    public static CampaignFleetAPI addFleetFromScratch(String faction, String fleetType, String fleetName, Vector2f locVec, String fleetMemberVariantId, PersonAPI captain, SectorEntityToken containingEntity) {
        List<String> fleetMemberVariantIds = new ArrayList<>();
        fleetMemberVariantIds.add(fleetMemberVariantId);
        List<PersonAPI> captainList = new ArrayList<>();
        captainList.add(captain);

        return addFleetFromScratch( faction,  fleetType,  fleetName, locVec, fleetMemberVariantIds, captainList,  containingEntity);

    }
    public static CampaignFleetAPI addFleetFromScratch(String faction, String fleetType, String fleetName, Vector2f locVec, List<String> fleetMemberVariantIds, List<PersonAPI> captainList, SectorEntityToken containingEntity) {
        List<MemFlags> memFlags=null;
        boolean noFactionInFleetName=false;
        boolean fleetTransponderOn=true;
        List<List<String>>shipHints=null;
        return addFleetFromScratch( faction,  fleetType,  fleetName, locVec, fleetMemberVariantIds, memFlags, captainList,  containingEntity, noFactionInFleetName, fleetTransponderOn, shipHints);
    }

    //nuke: the full Big Bastard. probably just going to reference it as a template instead of actually using it, though it should work fine.

    public static CampaignFleetAPI addFleetFromScratch(String faction, String fleetType, String fleetName, Vector2f locVec, List<String> fleetMemberVariantIds, List<MemFlags> memFlags, List<PersonAPI> captainList, SectorEntityToken containingEntity, boolean noFactionInFleetName, boolean fleetTransponderOn, List<List<String>> shipHints){

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(faction, fleetType, null);
        fleet.setName(fleetName);
        fleet.setLocation(locVec.x,locVec.y);

        if (fleetMemberVariantIds.get(0)==null){
            throw new IllegalArgumentException("Failed to provide variant data to seven_AIMod_Utils.addFleetFromScratch!");
        }
        else
        {
            FleetDataAPI fleetData=fleet.getFleetData();
            for (String variantID:fleetMemberVariantIds){
                fleetData.addFleetMember(variantID);
            }
            List<FleetMemberAPI> shipList=fleet.getFleetData().getMembersListCopy();

            boolean isFlagShip=true;
            int shipIndex=0;

            for (FleetMemberAPI ship:shipList){
                if(isFlagShip)
                {
                    fleetData.setFlagship(ship);
                    PersonAPI admiral=captainList.get(0);
                    if(admiral!=null){
                        fleet.setCommander(admiral);
                    }
                    isFlagShip=false;
                }
                ship.updateStats();
                ship.getRepairTracker().setCR(ship.getRepairTracker().getMaxCR());
                for (List<String> shipHintList : shipHints) {
                    for (String shipHint : shipHintList) {
                        ship.getVariant().addTag(shipHint);
                    }
                }


                PersonAPI captain=captainList.get(shipIndex);
                if(captain!=null){
                    ship.setCaptain(captain);
                }
                shipIndex++;
            }


            //nuke: this was in the TTBlackSite code and I have no idea what it means by "perm." I assume it stops it from resetting to whatever the zig experimental variant is, but why would that be a problem?
            // to "perm" the variant so it gets saved and not recreated from the "ziggurat_Experimental" id
//            flagship.setVariant(flagship.getVariant().clone(), false, false);
//            flagship.getVariant().setSource(VariantSource.REFIT);
//            flagship.getVariant().addTag(Tags.SHIP_LIMITED_TOOLTIP);


            //fleet.getFleetData()
            if (memFlags!=null)
            {
                for (MemFlags memflag:memFlags){
                    fleet.getMemoryWithoutUpdate().set(String.valueOf(memflag), true);
                }
            }


            if(containingEntity!=null){
                containingEntity.getContainingLocation().addEntity(fleet);
            }

            if(noFactionInFleetName){
                fleet.setNoFactionInName(true);
            }
            else{
                fleet.setNoFactionInName(false);
            }
            fleet.clearAbilities();
            fleet.setTransponderOn(fleetTransponderOn);
        }

        return fleet;
    }

    //nuke: should probably have this rely upon a more general hullmod applying method
    public static void applyHullmodToAllPlayerShips(String hullmodId){
        if (Global.getSector().getPlayerFleet() != null) {
            List<FleetMemberAPI> playerFleetList = Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy();
            for (FleetMemberAPI fleetMember : playerFleetList) {
                ShipVariantAPI shipVariant=fleetMember.getVariant();
                shipVariant.addMod(hullmodId);
            }
        }
    }
}
