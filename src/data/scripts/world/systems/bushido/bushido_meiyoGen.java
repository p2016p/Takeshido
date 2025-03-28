package data.scripts.world.systems.bushido;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import data.scripts.utils.seven_util_sysgen;
import data.scripts.world.ids.seven_Industries;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class bushido_meiyoGen {
  
    public void generate(SectorAPI sector) {

        StarSystemAPI system = sector.createStarSystem("Meiyo");
        system.getLocation().set(-1000, -11200);
        system.setBackgroundTextureFilename("graphics/backgrounds/background4.jpg");
        system.addTag(Tags.THEME_CORE_POPULATED);

     // Star
        PlanetAPI meiyo_star = system.initStar(
                "bushido_meiyo",
                "star_yellow",
                800f,
                820f);
        
        meiyo_star.setName("Meiyo");

        system.setLightColor(new Color(226, 230, 156));

        
     //// Aleatory Barren
        //        PlanetAPI meiyo_1 = system.addPlanet("bushido_kesseki",
        //        		meiyo_star,
        //                "Kesseki",
        //                "barren-bombarded",
        //                160f, //starting angle6
        //                90f, //size
        //                3550f, // orbit radius
        //                275f); // orbit time
        //        meiyo_1.setCustomDescriptionId("bushido_kessekixxx");
        //        PlanetConditionGenerator.generateConditionsForPlanet(meiyo_1, StarAge.AVERAGE);
        //
        //
        //
        //     // Another heaten productor
        //        PlanetAPI meiyo_2 = system.addPlanet("bushido_metarurijji",
        //        		meiyo_star,
        //                "Hitoreben",
        //                "rocky_metallic",
        //                100f, //starting angle
        //                120f, //size
        //                3000f, // orbit radius
        //                220f); // orbit time
        //        meiyo_2.setCustomDescriptionId("bushido_metarurijjixxx");
        //
        //        MarketAPI meiyo_2_market = seven_util_sysgen.addMarketplace("pirates",
        //                meiyo_2,
        //                null,
        //                "Hitoreben",
        //                5,
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Conditions.POPULATION_5,
        //                        Conditions.LOW_GRAVITY,
        //                        Conditions.VERY_HOT,
        //                        Conditions.RARE_ORE_ULTRARICH,
        //                        Conditions.ORE_ULTRARICH,
        //                        Conditions.FREE_PORT
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Submarkets.GENERIC_MILITARY,
        //                        Submarkets.SUBMARKET_OPEN,
        //                        Submarkets.SUBMARKET_STORAGE,
        //                        Submarkets.SUBMARKET_BLACK
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Industries.POPULATION,
        //                        Industries.MEGAPORT,
        //                        Industries.MINING,
        //                        //Industries.REFINING,
        //                        Industries.STARFORTRESS_MID,
        //                        Industries.HEAVYBATTERIES,
        //                        Industries.MILITARYBASE,
        //                        Industries.ORBITALWORKS,
        //                        Industries.WAYSTATION,
        //                        seven_Industries.BUSHIDO_RACEWAYS
        //                    )
        //                ),
        //                true,
        //                false);
        //
        //        meiyo_2_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.BETA_CORE);
        //        meiyo_2_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_2_market.addIndustry(Industries.ORBITALWORKS,new ArrayList<>(Arrays.asList(Items.CORRUPTED_NANOFORGE)));
        //        meiyo_2_market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.BETA_CORE);
        //        //meiyo_2_market.getIndustry(Industries.REFINING).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_2_market.getIndustry(Industries.STARFORTRESS_MID).setAICoreId(Commodities.ALPHA_CORE);
        //        meiyo_2_market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_2_market.addIndustry(Industries.MILITARYBASE,new ArrayList<>(Arrays.asList(Items.CRYOARITHMETIC_ENGINE)));
        //        meiyo_2_market.getIndustry(Industries.MILITARYBASE).setAICoreId(Commodities.GAMMA_CORE);

        
     // Excelling terran world, for classic racers and such
        PlanetAPI  meiyo_3 = system.addPlanet("bushido_oashisu",
        		meiyo_star,
                "Oashisu",
                "terran-eccentric",
                280f, //starting angle
                140f, //size
                4600f, // orbit radius
                442f); // orbit time
        meiyo_3.setCustomDescriptionId("bushido_oashisuxxx");  
        
        MarketAPI meiyo_3_market = seven_util_sysgen.addMarketplace("pirates",
                meiyo_3,
                null,
                "Oashisu",
                5,
                new ArrayList<>(
                    Arrays.asList(
                        Conditions.POPULATION_5,
                        Conditions.FARMLAND_RICH,
                        Conditions.HABITABLE,
                        //Conditions.MILD_CLIMATE,
                        Conditions.ORGANICS_COMMON,
                        Conditions.FREE_PORT,
                        Conditions.ORE_ABUNDANT,
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
                        Industries.MINING,
                        Industries.FUELPROD,
                        Industries.BATTLESTATION_MID,
                        Industries.GROUNDDEFENSES,
                        Industries.FARMING,
                        //Industries.LIGHTINDUSTRY,
                        Industries.PATROLHQ,
                        Industries.WAYSTATION,
                        seven_Industries.BUSHIDO_RACEWAYS
                    )
                ),
                true,
                false);
        
//        meiyo_3_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);
//        meiyo_3_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
        meiyo_3_market.getIndustry(Industries.FUELPROD).setAICoreId(Commodities.BETA_CORE);
        meiyo_3_market.getIndustry(Industries.BATTLESTATION_MID).setAICoreId(Commodities.GAMMA_CORE);
        meiyo_3_market.getIndustry(Industries.GROUNDDEFENSES).setAICoreId(Commodities.GAMMA_CORE);

        CustomCampaignEntityAPI  meiyo_station = system.addCustomEntity("bushido_nino_station", "Nino Station", "station_hightech3", "pirates");

        meiyo_station.setCircularOrbitPointingDown(meiyo_3,300,300,180);

        MarketAPI meiyo_station_market = seven_util_sysgen.addMarketplace("pirates",
                meiyo_station,
                null,
                "Nino Station",
                4,
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_4,
                                Conditions.INDUSTRIAL_POLITY,
                                Conditions.RUINS_VAST,
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
                                Industries.BATTLESTATION_MID,
                                Industries.GROUNDDEFENSES,
                                Industries.LIGHTINDUSTRY,
                                Industries.ORBITALWORKS,
                                Industries.WAYSTATION,
                                seven_Industries.BUSHIDO_RACEWAYS
                        )
                ),
                true,
                false);

        meiyo_station_market.getIndustry(Industries.ORBITALWORKS).setAICoreId(Commodities.ALPHA_CORE);
        meiyo_station_market.getIndustry(Industries.ORBITALWORKS).setSpecialItem(new SpecialItemData(Items.PRISTINE_NANOFORGE, null));


        //meiyo_3_market.getIndustry(Industries.FARMING).setAICoreId(Commodities.GAMMA_CORE);
//        meiyo_3_market.getIndustry(Industries.LIGHTINDUSTRY).setAICoreId(Commodities.GAMMA_CORE);
        
     //// First Moon of Oashisu
        //        PlanetAPI meiyo_4 = system.addPlanet("bushido_nosu",
        //        		meiyo_3,
        //                "Nosu",
        //                "barren",
        //                10f, //starting angle
        //                60f, //size
        //                900f, // orbit radius
        //                70f); // orbit time
        //        meiyo_4.setCustomDescriptionId("bushido_nosuxxx");
        //
        //        MarketAPI meiyo_4_market = seven_util_sysgen.addMarketplace("pirates",
        //                meiyo_4,
        //                null,
        //                "Nosu",
        //                4,
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Conditions.POPULATION_4,
        //                        Conditions.NO_ATMOSPHERE,
        //                        Conditions.LOW_GRAVITY,
        //                        Conditions.FREE_PORT,
        //                        Conditions.ORE_MODERATE,
        //                        Conditions.VOLATILES_TRACE,
        //                        Conditions.RARE_ORE_SPARSE
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Submarkets.SUBMARKET_OPEN,
        //                        Submarkets.SUBMARKET_STORAGE,
        //                        Submarkets.SUBMARKET_BLACK,
        //                        Submarkets.GENERIC_MILITARY
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Industries.POPULATION,
        //                        Industries.MEGAPORT,
        //                        Industries.MILITARYBASE,
        //                        Industries.MINING,
        //                        Industries.ORBITALSTATION,
        //                        Industries.HEAVYBATTERIES,
        //                        Industries.HEAVYINDUSTRY,
        //                        Industries.WAYSTATION,
        //                        seven_Industries.BUSHIDO_RACEWAYS
        //                    )
        //                ),
        //                true,
        //                false);
        //
        //        meiyo_4_market.getIndustry(Industries.POPULATION).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_4_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_4_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_4_market.getIndustry(Industries.ORBITALSTATION).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_4_market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_4_market.getIndustry(Industries.HEAVYINDUSTRY).setAICoreId(Commodities.BETA_CORE);
        //
        //
        //
        //     // Second Moon of Oashisu
        //        PlanetAPI meiyo_5 = system.addPlanet("bushido_minami",
        //        		meiyo_3,
        //                "Ninami",
        //                "barren",
        //                190f, //starting angle
        //                80f, //size
        //                600f, // orbit radius
        //                70f); // orbit time
        //        meiyo_5.setCustomDescriptionId("bushido_minamixxx");
        //
        //        MarketAPI meiyo_5_market = seven_util_sysgen.addMarketplace("pirates",
        //                meiyo_5,
        //                null,
        //                "Ninami",
        //                4,
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Conditions.POPULATION_4,
        //                        Conditions.NO_ATMOSPHERE,
        //                        Conditions.FREE_PORT,
        //                        Conditions.VOLATILES_TRACE,
        //                        Conditions.ORE_SPARSE,
        //                        Conditions.RARE_ORE_MODERATE
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Submarkets.SUBMARKET_OPEN,
        //                        Submarkets.SUBMARKET_STORAGE,
        //                        Submarkets.SUBMARKET_BLACK,
        //                        Submarkets.GENERIC_MILITARY
        //                    )
        //                ),
        //                new ArrayList<>(
        //                    Arrays.asList(
        //                        Industries.POPULATION,
        //                        Industries.MEGAPORT,
        //                        Industries.MILITARYBASE,
        //                        Industries.MINING,
        //                        Industries.ORBITALSTATION,
        //                        Industries.HEAVYBATTERIES,
        //                        Industries.HEAVYINDUSTRY,
        //                        Industries.WAYSTATION,
        //                        seven_Industries.BUSHIDO_RACEWAYS
        //                    )
        //                ),
        //                true,
        //                false);
        //
        ////        meiyo_5_market.getIndustry(Industries.POPULATION).setAICoreId(Commodities.GAMMA_CORE);
        ////        meiyo_5_market.getIndustry(Industries.MEGAPORT).setAICoreId(Commodities.GAMMA_CORE);
        ////        meiyo_5_market.getIndustry(Industries.MINING).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_5_market.getIndustry(Industries.ORBITALSTATION).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_5_market.getIndustry(Industries.HEAVYBATTERIES).setAICoreId(Commodities.GAMMA_CORE);
        //        meiyo_5_market.getIndustry(Industries.HEAVYINDUSTRY).setAICoreId(Commodities.BETA_CORE);

		float innerOrbitDistance = StarSystemGenerator.addOrbitingEntities(
                system, //star system variable, used to add entities
                meiyo_star, //focus object for entities to orbit
                StarAge.AVERAGE, //used by generator to decide which kind of planets to add
                2, //minimum number of entities
                5, //maximum number of entities
                5000, //the radius between the first generated entity and the focus object, in this case the star
                1, //used to assign roman numerals to the generated entities if not given special names
                true //generator will give unique names like "Ordog" instead of "Example Star System III"
        );
        
	 // Relay/Buoy/Array
        //nuke: the base id is not the type. the type is the top level string.
//        SectorEntityToken bushido_relay = system.addCustomEntity("comm_relay_sevencorp_bushido", // unique id
//                "Bushido Racer Radio (Comn Relay)", // name - if null, defaultName from custom_entities.json will be used
//                "comm_relay_sevencorp_bushido", // type of object, defined in custom_entities.json
//                "pirates"); // faction
//        bushido_relay.setCircularOrbitPointingDown( meiyo_star, 90f, 3900, 370);
//
//        SectorEntityToken bushido_buoy = system.addCustomEntity("nav_buoy_sevencorp_bushido", // unique id
//                "Bushido Speedster Post (Nav Buoy)", // name - if null, defaultName from custom_entities.json will be used
//                "nav_buoy_sevencorp_bushido", // type of object, defined in custom_entities.json
//                "pirates"); // faction
//        bushido_buoy.setCircularOrbitPointingDown( meiyo_star, 130f, 5290, 370);
//
//        SectorEntityToken bushido_array = system.addCustomEntity("sensor_array_sevencorp_bushido", // unique id
//                "Bushido Crash Avoidance Beacon (Sensor Array)", // name - if null, defaultName from custom_entities.json will be used
//                "sensor_array_sevencorp_bushido", // type of object, defined in custom_entities.json
//                "pirates"); // faction
//        bushido_array.setCircularOrbitPointingDown( meiyo_star, 200f, 5240, 370);
//

     // Inner system jump point
        JumpPointAPI jumpPoint1 = Global.getFactory().createJumpPoint("bushido_meiyo_inner_jump", "Inner Jump Point");
        jumpPoint1.setCircularOrbit(meiyo_star, 134, 3461, 213);
        
        system.addEntity(jumpPoint1);
		
     
     // generates hyperspace destinations for in-system jump points
		system.autogenerateHyperspaceJumpPoints(true, true);

     // Finally cleans up hyperspace
		cleanup(system);
	}

    //Shorthand function for cleaning up hyperspace
    private void cleanup(StarSystemAPI system){
    	HyperspaceTerrainPlugin plugin = (HyperspaceTerrainPlugin) Misc.getHyperspaceTerrain().getPlugin();
        NebulaEditor editor = new NebulaEditor(plugin);
        float minRadius = plugin.getTileSize() * 2f;
        float radius = system.getMaxRadiusInHyperspace();
        
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius * 0.5f, 0f, 360f);
        editor.clearArc(system.getLocation().x, system.getLocation().y, 0, radius + minRadius, 0f, 360f, 0.25f);
    }
}
