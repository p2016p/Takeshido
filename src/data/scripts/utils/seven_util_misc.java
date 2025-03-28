package data.scripts.utils;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;

//import exerelin.campaign.SectorManager;

public class seven_util_misc extends BaseModPlugin {
    private static org.apache.log4j.Logger log = Global.getLogger(seven_util_misc.class);

    public static float shieldpowerdecaypercent = 0.0007f;
    public static float shieldpowerdecayflat = 1.2f;

    public static void addGlobalSuppressedHullMod(final String hullmodId) {
        for (String id : Global.getSettings().getAllVariantIds()) {
            ShipVariantAPI variant = Global.getSettings().getVariant(id);
            variant.addSuppressedMod(hullmodId);
        }
    }

    public static boolean playerHasCommodity(String id)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null)
            return false;
        List<CargoStackAPI> playerCargoStacks = playerFleet.getCargo().getStacksCopy();

        for (CargoStackAPI cargoStack : playerCargoStacks) {
            if (cargoStack.isCommodityStack() && cargoStack.getCommodityId().equals(id) && cargoStack.getSize() > 0) {
                return true;
            }
        }

        return false;
    }

    public static void removePlayerCommodity(String id)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null)
            return;
        List<CargoStackAPI> playerCargoStacks = playerFleet.getCargo().getStacksCopy();

        for (CargoStackAPI cargoStack : playerCargoStacks) {
            if (cargoStack.isCommodityStack() && cargoStack.getCommodityId().equals(id)) {
                cargoStack.subtract(1);
                if (cargoStack.getSize() <= 0)
                    playerFleet.getCargo().removeStack(cargoStack);
                return;
            }
        }
    }

    public static void addPlayerCommodity(String commodityId, int amount)
    {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null)
            return;
        CargoAPI playerFleetCargo = playerFleet.getCargo();
        playerFleetCargo.addCommodity(commodityId, amount);
    }

    public static void createAIPersona(PersonAPI commander, ShipAPI ship)
    {
        PersonAPI AI;
        String AIName;
        if(Global.getSector().getPersistentData().get("seven_AIPersona_"+ship.getCaptain().getId()) instanceof PersonAPI) {
            AI = (PersonAPI)Global.getSector().getPersistentData().get("seven_AIPersona_"+ship.getCaptain().getId());
            AIName = (String)Global.getSector().getPersistentData().get("seven_AIPersona_"+"name_"+ship.getCaptain().getId());
        }
        else{
            AI = Misc.getAICoreOfficerPlugin("alpha_core").createPerson("alpha_core", "player", Misc.random);
            AIName = OfficerManagerEvent.createOfficer(Global.getSector().getFaction("remnant"),1, true).getName().getFullName();
            Global.getSector().getPersistentData().put("seven_AIPersona_"+ship.getCaptain().getId(),AI);
            Global.getSector().getPersistentData().put("seven_AIPersona_"+"name_"+ship.getCaptain().getId(),AIName);
        }
    }

    public static void createDuo(PersonAPI commander)
    {
        String duoName = getDuoName();
        Global.getSector().getPersistentData().put("AICore_DuoName_"+commander.getId(),duoName);
    }

    public static boolean hasDuo(PersonAPI commander)
    {
        return Global.getSector().getPersistentData().get("AICore_DuoName_"+commander.getId()) instanceof String;
    }

    public static void createAnanke(PersonAPI commander, ShipAPI ship)
    {
        PersonAPI AI;
        String AIName;
        if(Global.getSector().getPersistentData().get("seven_Ananke_"+ship.getCaptain().getId()) instanceof PersonAPI) {
            AI = (PersonAPI)Global.getSector().getPersistentData().get("seven_Ananke_"+ship.getCaptain().getId());
            AIName = (String)Global.getSector().getPersistentData().get("seven_Ananke_"+"name_"+ship.getCaptain().getId());
        }
        else{
            AI = Misc.getAICoreOfficerPlugin("omega_core").createPerson("omega_core", "player", Misc.random);
            AIName = "Ananke";
            Global.getSector().getPersistentData().put("seven_Ananke_"+ship.getCaptain().getId(),AI);
            Global.getSector().getPersistentData().put("seven_Ananke_"+"name_"+ship.getCaptain().getId(),AIName);
        }
    }

    public static void createAnankeDuo(PersonAPI commander)
    {
        String duoName = getDuoName();
        Global.getSector().getPersistentData().put("Ananke_DuoName_"+commander.getId(),duoName);
    }

    public static boolean hasAnankeDuo(PersonAPI commander)
    {
        return Global.getSector().getPersistentData().get("Ananke_DuoName_"+commander.getId()) instanceof String;
    }

    public static String getDuoName()
    {
        return "dynamic duo";
    }

    public static void changeshieldcolor(ShipAPI ship, float tostat, Object source){
        ShieldAPI shield = ship.getShield();
        int blue = 0;
        int green = 0;
        int red = 0;

        if (tostat > 1.5f) {
            blue = Math.round(250);
            green = Math.round(7 * (tostat * tostat * tostat * tostat * tostat * tostat));
            red = Math.round(60 * (tostat * tostat * tostat));
        }else if(tostat > 1.25f){
            blue = Math.round(175 * (tostat));
            green = Math.round(175 / (tostat * tostat));
            red = Math.round(60 * (tostat * tostat * tostat));
        }else{
            blue = Math.round(175 * (tostat));
            green = Math.round(175 / (tostat * tostat));
            red = Math.round(250 / (tostat * tostat * tostat));
        }

        if (blue > 250) blue = (250);
        if (red > 250) red = (250);
        if (green > 250) green = (250);

        shield.setInnerColor(new Color(red, green, blue, 100));

        ship.setVentCoreColor(new Color (red, green, blue, 255));
        ship.setVentFringeColor(new Color (red, green, blue, 255));
        ship.setOverloadColor(new Color (red, green, blue, 255));
        shield.setRingColor(new Color (red, green, blue, 15));

        ship.fadeToColor(source, new Color(140,140,140,255), 0.1f, 0.1f, tostat - 1f);
        ship.setWeaponGlow(tostat - 1f, new Color(red, green, blue,255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        ship.setJitterUnder(source, new Color(red, green, blue,255), tostat - 1f, 8, 0f, 12f);

        float extendLengthFraction = 1f * tostat * (tostat * 0.7f) ;
        float extendWidthFraction = 1f * (tostat);
        float extendGlowFraction = 0.28f;
        float effectlevel = 2f;
        float maxBlend = 0.35f;
        Color engine_color = new Color(red, green, blue, 255);
        Color particles = new Color(red, green, blue, 10);

        ship.getEngineController().extendFlame(source, extendLengthFraction, extendWidthFraction, extendGlowFraction);
        ship.getEngineController().getFlameColorShifter().setBase(engine_color);
        ship.getEngineController().fadeToOtherColor(source, engine_color, particles, effectlevel, maxBlend);

        if (tostat > 1.25f) {
            int framecap = 50;
            if(tostat > 1.5) framecap = 30;

            if(MathUtils.getRandomNumberInRange(1,framecap)>=framecap){
                Global.getCombatEngine().spawnEmpArc(ship,
                        MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.65f),
                        ship,
                        ship,
                        DamageType.ENERGY, //Damage type
                        0f,
                        0f,
                        9999f, //Max range
                        "mote_attractor_impact_normal", //Impact sound
                        ship.getCollisionRadius() / 70f * 7f - 5f, // thickness of the lightning bolt
                        new Color (red, green, blue, 160), //Central color
                        new Color (red, green, blue, 125) //Fringe Color);
                );
            }
        }


        //if (tostat < 1.25f) {
        //            ShieldAPI shield = ship.getShield();
        //            int blue = Math.round(175 * (tostat));
        //            int green = Math.round(175 / (tostat * tostat));
        //            int red = Math.round(250 / (tostat * tostat * tostat));
        //
        //            if (blue > 250) blue = (250);
        //            if (red > 250) red = (250);
        //            if (green > 250) green = (250);
        //
        //            shield.setInnerColor(new Color (red, green, blue, 100));
        //
        //            ship.setVentCoreColor(new Color (red, green, blue, 255));
        //            ship.setVentFringeColor(new Color (red, green, blue, 255));
        //            ship.setOverloadColor(new Color (red, green, blue, 255));
        //            shield.setRingColor(new Color (red, green, blue, 15));
        //
        //
        //        ship.fadeToColor(this, new Color(140,140,140,255), 0.1f, 0.1f, tostat - 1f);
        //	ship.setWeaponGlow(tostat - 1f, new Color(red, green, blue,255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        //	ship.setJitterUnder(this, new Color(red, green, blue,255), tostat - 1f, 8, 0f, 12f);
        //
        //            float extendLengthFraction = 1f * tostat * (tostat * 0.7f) ;
        //            float extendWidthFraction = 1f * (tostat);
        //            float extendGlowFraction = 0.28f;
        //            float effectlevel = 2f;
        //            float maxBlend = 0.35f;
        //            Color engine_color = new Color(red, green, blue, 255);
        //            Color particles = new Color(red, green, blue, 10);
        //
        //            ship.getEngineController().extendFlame(this, extendLengthFraction, extendWidthFraction, extendGlowFraction);
        //            ship.getEngineController().getFlameColorShifter().setBase(engine_color);
        //            ship.getEngineController().fadeToOtherColor(this, engine_color, particles, effectlevel, maxBlend);
        //        }
        //
        //        if (tostat > 1.25f) {
        //            ShieldAPI shield = ship.getShield();
        //            int blue = Math.round(175 * (tostat));
        //            int green = Math.round(175 / (tostat * tostat));
        //            int red = Math.round(60 * (tostat * tostat * tostat));
        //
        //            if (blue > 250) blue = (250);
        //            if (red > 250) red = (250);
        //            if (green > 250) green = (250);
        //
        //            shield.setInnerColor(new Color (red, green, blue, 100));
        //
        //            ship.setVentCoreColor(new Color (red, green, blue, 255));
        //            ship.setVentFringeColor(new Color (red, green, blue, 255));
        //            ship.setOverloadColor(new Color (red, green, blue, 255));
        //            shield.setRingColor(new Color (red, green, blue, 15));
        //
        //            ship.fadeToColor(this, new Color(140,140,140,255), 0.1f, 0.1f, tostat - 1f);
        //	ship.setWeaponGlow(tostat - 1f, new Color(red, green, blue,255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        //	ship.setJitterUnder(this, new Color(red, green, blue,255), tostat - 1f, 8, 0f, 12f);
        //
        //            float extendLengthFraction = 1f * tostat * (tostat * 0.7f) ;
        //            float extendWidthFraction = 1f * (tostat);
        //            float extendGlowFraction = 0.28f;
        //            float effectlevel = 2f;
        //            float maxBlend = 0.35f;
        //            Color engine_color = new Color(red, green, blue, 255);
        //            Color particles = new Color(red, green, blue, 10);
        //
        //            ship.getEngineController().extendFlame(this, extendLengthFraction, extendWidthFraction, extendGlowFraction);
        //            ship.getEngineController().getFlameColorShifter().setBase(engine_color);
        //            ship.getEngineController().fadeToOtherColor(this, engine_color, particles, effectlevel, maxBlend);
        //
        //            if (shield.isOn()) {
        //                frame += 1;
        //                if (frame == 50) {
        //                    for (int i = 0; i < 2; i++) {
        //                        //  engine.spawnEmpArcVisual(shield.getLocation(),ship,MathUtils.getPointOnCircumference(shield.getLocation(),shield.getRadius(),activearc),ship,arcsize * 0.5f,LIGHTNING_CORE_COLOR,hit);
        //                        //Global.getCombatEngine().spawnEmpArcVisual(MathUtils.getMidpoint(targetData.target.getLocation(), MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 2f, MathUtils.getRandomNumberInRange(0f, 360f))), targetData.target, MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 0.45f, MathUtils.getRandomNumberInRange(0f, 360f)), targetData.target, targetData.target.getCollisionRadius() / 70f * 7f - 5f, LIGHTNING_CORE_COLOR, hit);
        //                        Global.getCombatEngine().spawnEmpArc(ship,
        //                                MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.65f),
        //                                ship,
        //                                ship,
        //                                DamageType.ENERGY, //Damage type
        //                                0f,
        //                                0f,
        //                                9999f, //Max range
        //                                "mote_attractor_impact_normal", //Impact sound
        //                                ship.getCollisionRadius() / 70f * 7f - 5f, // thickness of the lightning bolt
        //                                new Color (red, green, blue, 160), //Central color
        //                                new Color (red, green, blue, 125) //Fringe Color);
        //                        );
        //                        frame = 0;
        //                    }
        //                }
        //            }
        //
        //            if (shield.isOff()) {
        //                frame += 1;
        //                if (frame == 50) {
        //                    for (int i = 0; i < 2; i++) {
        //
        //
        //                        //  engine.spawnEmpArcVisual(shield.getLocation(),ship,MathUtils.getPointOnCircumference(shield.getLocation(),shield.getRadius(),activearc),ship,arcsize * 0.5f,LIGHTNING_CORE_COLOR,hit);
        //                        //Global.getCombatEngine().spawnEmpArcVisual(MathUtils.getMidpoint(targetData.target.getLocation(), MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 2f, MathUtils.getRandomNumberInRange(0f, 360f))), targetData.target, MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 0.45f, MathUtils.getRandomNumberInRange(0f, 360f)), targetData.target, targetData.target.getCollisionRadius() / 70f * 7f - 5f, LIGHTNING_CORE_COLOR, hit);
        //                        Global.getCombatEngine().spawnEmpArc(ship , MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.65f),
        //                                ship,
        //                                ship,
        //                                DamageType.ENERGY, //Damage type
        //                                0f,
        //                                0f,
        //                                9999f, //Max range
        //                                "mote_attractor_impact_normal", //Impact sound
        //                                ship.getCollisionRadius() / 70f * 7f - 5f, // thickness of the lightning bolt
        //                                new Color (red, green, blue, 160), //Central color
        //                                new Color (red, green, blue, 125) //Fringe Color);
        //                        );
        //                        frame = 0;
        //                    }
        //                }
        //            }
        //        }
        //
        //        if (tostat > 1.5f) {
        //
        //            ShieldAPI shield = ship.getShield();
        //            int blue = Math.round(250);
        //            int green = Math.round(7 *(tostat * tostat * tostat * tostat * tostat * tostat));
        //            int red = Math.round(60 * (tostat * tostat * tostat));
        //
        //            if (blue > 250) blue = (250);
        //            if (red > 250) red = (250);
        //            if (green > 250) green = (250);
        //
        //            shield.setInnerColor(new Color (red, green, blue, 100));
        //
        //            ship.setVentCoreColor(new Color (red, green, blue, 255));
        //            ship.setVentFringeColor(new Color (red, green, blue, 255));
        //            ship.setOverloadColor(new Color (red, green, blue, 255));
        //            shield.setRingColor(new Color (red, green, blue, 15));
        //
        //            ship.fadeToColor(this, new Color(140,140,140,255), 0.1f, 0.1f, tostat - 1f);
        //	ship.setWeaponGlow(tostat - 1f, new Color(red, green, blue,255), EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY, WeaponAPI.WeaponType.MISSILE));
        //	ship.setJitterUnder(this, new Color(red, green, blue,255), tostat - 1f, 8, 0f, 12f);
        //
        //            float extendLengthFraction = 1f * tostat * (tostat * 0.7f) ;
        //            float extendWidthFraction = 1f * (tostat);
        //            float extendGlowFraction = 0.28f;
        //            float effectlevel = 2f;
        //            float maxBlend = 0.35f;
        //            Color engine_color = new Color(red, green, blue, 255);
        //            Color particles = new Color(red, green, blue, 10);
        //
        //            ship.setJitterShields(true);
        //            ship.getEngineController().extendFlame(this, extendLengthFraction, extendWidthFraction, extendGlowFraction);
        //            ship.getEngineController().getFlameColorShifter().setBase(engine_color);
        //            ship.getEngineController().fadeToOtherColor(this, engine_color, particles, effectlevel, maxBlend);
        //
        //            if (shield.isOn()) {
        //                frame2 += 1;
        //                if (frame2 == 30) {
        //                    for (int i = 0; i < 2; i++) {
        //
        //                        //  engine.spawnEmpArcVisual(shield.getLocation(),ship,MathUtils.getPointOnCircumference(shield.getLocation(),shield.getRadius(),activearc),ship,arcsize * 0.5f,LIGHTNING_CORE_COLOR,hit);
        //                        //Global.getCombatEngine().spawnEmpArcVisual(MathUtils.getMidpoint(targetData.target.getLocation(), MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 2f, MathUtils.getRandomNumberInRange(0f, 360f))), targetData.target, MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 0.45f, MathUtils.getRandomNumberInRange(0f, 360f)), targetData.target, targetData.target.getCollisionRadius() / 70f * 7f - 5f, LIGHTNING_CORE_COLOR, hit);
        //                        Global.getCombatEngine().spawnEmpArc(ship , MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.65f), ship, ship,
        //                                DamageType.ENERGY, //Damage type
        //                                0f,
        //                                0f,
        //                                9999f, //Max range
        //                                "mote_attractor_impact_normal", //Impact sound
        //                                ship.getCollisionRadius() / 70f * 7f - 5f, // thickness of the lightning bolt
        //                                new Color (red, green, blue, 160), //Central color
        //                                new Color (red, green, blue, 125) //Fringe Color);
        //                        );
        //                        frame2 = 0;
        //                    }
        //                }
        //            }
        //
        //            if (shield.isOff()) {
        //                frame2 += 1;
        //                if (frame2 == 30) {
        //                    for (int i = 0; i < 2; i++) {
        //
        //                        //  engine.spawnEmpArcVisual(shield.getLocation(),ship,MathUtils.getPointOnCircumference(shield.getLocation(),shield.getRadius(),activearc),ship,arcsize * 0.5f,LIGHTNING_CORE_COLOR,hit);
        //                        //Global.getCombatEngine().spawnEmpArcVisual(MathUtils.getMidpoint(targetData.target.getLocation(), MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 2f, MathUtils.getRandomNumberInRange(0f, 360f))), targetData.target, MathUtils.getPointOnCircumference(targetData.target.getLocation(), targetData.target.getCollisionRadius() * 0.45f, MathUtils.getRandomNumberInRange(0f, 360f)), targetData.target, targetData.target.getCollisionRadius() / 70f * 7f - 5f, LIGHTNING_CORE_COLOR, hit);
        //                        Global.getCombatEngine().spawnEmpArc(ship , MathUtils.getRandomPointOnCircumference(ship.getLocation(), ship.getCollisionRadius() * 0.65f), ship, ship,
        //                                DamageType.ENERGY, //Damage type
        //                                0f,
        //                                0f,
        //                                9999f, //Max range
        //                                "mote_attractor_impact_normal", //Impact sound
        //                                ship.getCollisionRadius() / 70f * 7f - 5f, // thickness of the lightning bolt
        //                                new Color (red, green, blue, 160), //Central color
        //                                new Color (red, green, blue, 125) //Fringe Color);
        //                        );
        //                        frame2 = 0;
        //                    }
        //                }
        //            }
        //        }
    }


}