package data.scripts.world.systems.takeshido;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.takeshido_util_sysgen;
import data.scripts.world.ids.takeshido_Industries;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class takeshido_meiyoGen {

    public void generate(SectorAPI sector) {
        StarSystemAPI system = sector.createStarSystem("Meiyo");
        system.getLocation().set(-1000, -11200);
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.addTag(Tags.THEME_CORE_POPULATED);

        PlanetAPI meiyoStar = system.initStar(
                "takeshido_meiyo",
                "star_yellow",
                600f,
                650f
        );

        meiyoStar.setName("Meiyo");
        system.setLightColor(new Color(226, 230, 156));

        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 0, new Color(180, 190, 190, 110), 220f, 2800f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 1, new Color(200, 200, 200, 150), 220f, 2900f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 2, new Color(180, 190, 190, 110), 220f, 3000f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 3, new Color(200, 200, 200, 150), 220f, 3100f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 4, new Color(180, 190, 190, 110), 220f, 3200f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 5, new Color(200, 200, 200, 150), 220f, 3300f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 6, new Color(180, 190, 190, 110), 220f, 3400f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 7, new Color(200, 200, 200, 150), 220f, 3500f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 8, new Color(180, 190, 190, 110), 220f, 3600f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 9, new Color(200, 200, 200, 150), 220f, 3700f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 10, new Color(180, 190, 190, 110), 220f, 3800f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 11, new Color(200, 200, 200, 150), 220f, 3900f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 12, new Color(180, 190, 190, 110), 220f, 4000f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 13, new Color(200, 200, 200, 150), 220f, 4100f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_dust0", 256f, 14, new Color(180, 190, 190, 110), 220f, 4200f, 360f, Terrain.RING, null);
        system.addRingBand(meiyoStar, "misc", "rings_asteroids0", 256f, 15, new Color(200, 200, 200, 150), 220f, 4300f, 360f, Terrain.RING, null);

        PlanetAPI oashisu = system.addPlanet(
                "takeshido_oashisu",
                meiyoStar,
                "Oashisu",
                "terran-eccentric",
                280f,
                100f,
                3600f,
                360f
        );
        oashisu.setCustomDescriptionId("takeshido_oashisuxxx");

        MarketAPI oashisuMarket = takeshido_util_sysgen.addMarketplace(
                "takeshido",
                oashisu,
                null,
                "Oashisu",
                5,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_5,
                                Conditions.TERRAN,
                                Conditions.HABITABLE,
                                Conditions.FARMLAND_RICH,
                                Conditions.ORGANICS_COMMON,
                                Conditions.FREE_PORT,
                                Conditions.ORGANIZED_CRIME,
                                Conditions.RUINS_EXTENSIVE
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                Submarkets.SUBMARKET_BLACK
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.FARMING,
                                Industries.LIGHTINDUSTRY,
                                Industries.GROUNDDEFENSES,
                                Industries.PATROLHQ,
                                Industries.WAYSTATION,
                                Industries.ORBITALWORKS,
                                Industries.ORBITALSTATION_MID,
                                takeshido_Industries.TAKESHIDO_RACEWAYS
                        )
                ),
                true,
                false
        );

        oashisuMarket.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);

        PlanetAPI nino = system.addPlanet(
                "takeshido_nino",
                meiyoStar,
                "Nino",
                "jungle",
                120f,
                90f,
                3200f,
                360f
        );

        MarketAPI ninoMarket = takeshido_util_sysgen.addMarketplace(
                "takeshido",
                nino,
                null,
                "Nino",
                4,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_4,
                                Conditions.JUNGLE,
                                Conditions.HABITABLE,
                                Conditions.FARMLAND_ADEQUATE,
                                Conditions.ORGANICS_ABUNDANT,
                                Conditions.FREE_PORT,
                                Conditions.ORGANIZED_CRIME
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                Submarkets.SUBMARKET_BLACK
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.FARMING,
                                Industries.LIGHTINDUSTRY,
                                Industries.GROUNDDEFENSES,
                                Industries.PATROLHQ,
                                Industries.WAYSTATION,
                                takeshido_Industries.TAKESHIDO_RACEWAYS
                        )
                ),
                true,
                false
        );

        PlanetAPI silverstone = system.addPlanet(
                "takeshido_silverstone",
                meiyoStar,
                "Silverstone",
                "cryovolcanic",
                20f,
                85f,
                4200f,
                360f
        );

        MarketAPI silverstoneMarket = takeshido_util_sysgen.addMarketplace(
                "takeshido",
                silverstone,
                null,
                "Silverstone",
                4,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_4,
                                Conditions.VERY_HOT,
                                Conditions.HABITABLE,
                                Conditions.VERY_COLD,
                                Conditions.VOLATILES_ABUNDANT,
                                Conditions.ORE_ULTRARICH,
                                Conditions.FREE_PORT,
                                Conditions.ORGANIZED_CRIME,
                                Conditions.VICE_DEMAND
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Submarkets.SUBMARKET_OPEN,
                                Submarkets.SUBMARKET_STORAGE,
                                Submarkets.SUBMARKET_BLACK
                        )
                ),
                new ArrayList<>(
                        Arrays.asList(
                                Industries.POPULATION,
                                Industries.MEGAPORT,
                                Industries.MINING,
                                Industries.FUELPROD,
                                Industries.GROUNDDEFENSES,
                                Industries.PATROLHQ,
                                Industries.WAYSTATION,
                                takeshido_Industries.TAKESHIDO_RACEWAYS
                        )
                ),
                true,
                false
        );

        silverstoneMarket.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);

        JumpPointAPI jumpPoint = Global.getFactory().createJumpPoint(
                "takeshido_meiyo_inner_jump",
                "Inner Jump Point"
        );
        jumpPoint.setCircularOrbit(meiyoStar, 134f, 3461f, 213f);
        jumpPoint.setStandardWormholeToHyperspaceVisual();
        system.addEntity(jumpPoint);

        system.autogenerateHyperspaceJumpPoints(false, true);
        cleanup(system);
    }

    private void cleanup(StarSystemAPI system) {
        HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;
        float radius = system.getMaxRadiusInHyperspace();

        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, radius + minRadius * 0.5f, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0f, radius + minRadius, 0f, 360f, 0.25f);
    }
}
