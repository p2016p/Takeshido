package data.scripts.world;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.world.systems.takeshido.takeshido_meiyoGen;

public class takeshido_takeshidoGen implements SectorGeneratorPlugin {

    @Override
    public void generate(SectorAPI sector) {

        new takeshido_meiyoGen().generate(sector);

        FactionAPI takeshido = sector.getFaction("takeshido");

        takeshido.setRelationship(Factions.HEGEMONY, -0.49f);
        takeshido.setRelationship(Factions.PLAYER, -0.15f);
        takeshido.setRelationship(Factions.PIRATES, 0.75f);
        takeshido.setRelationship(Factions.INDEPENDENT, 0.1f);
        takeshido.setRelationship(Factions.TRITACHYON, -0.25f);
        takeshido.setRelationship(Factions.KOL, 0.5f);
        takeshido.setRelationship(Factions.LUDDIC_PATH, 0.5f);
        takeshido.setRelationship(Factions.LUDDIC_CHURCH, -0.75f);
        takeshido.setRelationship(Factions.PERSEAN, -0.25f);
        takeshido.setRelationship(Factions.LIONS_GUARD, -0.25f);
        takeshido.setRelationship(Factions.DIKTAT, -0.25f);

        //environment
        takeshido.setRelationship(Factions.REMNANTS, RepLevel.HOSTILE);
        takeshido.setRelationship(Factions.DERELICT, RepLevel.HOSTILE);

        // mod factions
        takeshido.setRelationship("HMI", 0.5f);

        
    }
}