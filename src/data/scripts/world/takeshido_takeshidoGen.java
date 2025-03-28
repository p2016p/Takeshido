package data.scripts.world;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.world.systems.bushido.bushido_meiyoGen;

public class takeshido_takeshidoGen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {

        new bushido_meiyoGen().generate(sector);

        FactionAPI bushido = sector.getFaction("takeshido");

        bushido.setRelationship(Factions.HEGEMONY, -0.49f);
        bushido.setRelationship(Factions.PLAYER, -0.15f);
        bushido.setRelationship(Factions.PIRATES, 0.75f);
        bushido.setRelationship(Factions.INDEPENDENT, 0.1f);
        bushido.setRelationship(Factions.TRITACHYON, -0.25f);
        bushido.setRelationship(Factions.KOL, 0.5f);
        bushido.setRelationship(Factions.LUDDIC_PATH, 0.5f);
        bushido.setRelationship(Factions.LUDDIC_CHURCH, -0.75f);
        bushido.setRelationship(Factions.PERSEAN, -0.25f);
        bushido.setRelationship(Factions.LIONS_GUARD, -0.25f);
        bushido.setRelationship(Factions.DIKTAT, -0.25f);

        //environment
        bushido.setRelationship(Factions.REMNANTS, RepLevel.HOSTILE);
        bushido.setRelationship(Factions.DERELICT, RepLevel.HOSTILE);

        // mod factions
        bushido.setRelationship("vic", 0f);
        bushido.setRelationship("ironsentinel", -0.75f);
        bushido.setRelationship("ironshell", -0.75f);

        bushido.setRelationship("sylphon", -0.4f);
        bushido.setRelationship("Coalition", -0.3f);
        bushido.setRelationship("tiandong", 0f);
        bushido.setRelationship("kadur_remnant", -0.75f);
        bushido.setRelationship("blackrock_driveyards", 0.25f);
		bushido.setRelationship("interstellarimperium", -0.25f);
		bushido.setRelationship("HMI", 0.5f);
		bushido.setRelationship("al_ars", -0.25f);
        bushido.setRelationship("mayorate", -0.25f);
        bushido.setRelationship("SCY", -0.25f);
        bushido.setRelationship("blade_breakers", 0f);
        bushido.setRelationship("dassault_mikoyan", 0f);
        bushido.setRelationship("diableavionics", 0f);
        bushido.setRelationship("ORA", 0.25f);
        bushido.setRelationship("gmda", -0.25f);
        bushido.setRelationship("gmda_patrol", -0.25f);

        bushido.setRelationship("tahlan_legioinfernalis", 0.5f);
        bushido.setRelationship("yrxp", 0f);

        bushido.setRelationship("cabal", 0.5f);


        bushido.setRelationship("shadow_industry", -0.6f);
        bushido.setRelationship("roider", -0.6f);
        bushido.setRelationship("exipirated", -0.6f);
        bushido.setRelationship("draco", -0.6f);
        bushido.setRelationship("fang", -0.6f);
        bushido.setRelationship("junk_pirates", -0.6f);
        bushido.setRelationship("junk_pirates_hounds", -0.6f);
        bushido.setRelationship("junk_pirates_junkboy s", -0.6f);
        bushido.setRelationship("junk_pirates_technicians", -0.6f);
        bushido.setRelationship("the_cartel", -0.6f);
        bushido.setRelationship("nullorder", -0.6f);
        bushido.setRelationship("templars", -0.6f);
        bushido.setRelationship("crystanite_pir", -0.6f);
        bushido.setRelationship("infected", -0.6f);
        bushido.setRelationship("new_galactic_order", -0.6f);
        bushido.setRelationship("TF7070_D3C4", -0.6f);
        bushido.setRelationship("minor_pirate_1", -0.6f);
        bushido.setRelationship("minor_pirate_2", -0.6f);
        bushido.setRelationship("minor_pirate_3", -0.6f);
        bushido.setRelationship("minor_pirate_4", -0.6f);
        bushido.setRelationship("minor_pirate_5", -0.6f);
        bushido.setRelationship("minor_pirate_6", -0.6f);
        
    }
}