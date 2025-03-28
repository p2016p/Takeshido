package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import java.awt.*;

public class seven_autorecall extends BaseHullMod {

    public static Color LIGHTNING_FRINGE_COLOR2 = new Color(155, 41, 217, 90);
    public static Color FRINGE_COLOR2 = new Color( 19, 26, 14, 250);

    public void advanceInCombat(ShipAPI ship, float amount) {
        if (!ship.isAlive())
            return;

        CombatEngineAPI engine = Global.getCombatEngine();

        if (ship.getCustomData().get("recallcharges") == null) ship.setCustomData("recallcharges", 4);
        if (ship.getCustomData().get("recalltimer") == null) ship.setCustomData("recalltimer", 0);

        if ((int) ship.getCustomData().get("recallcharges") >= 1 && (int) ship.getCustomData().get("recalltimer") == 0) {
            for (ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
                if (!ally.getVariant().hasHullMod("strikeCraft") && ally.getHullSize() != HullSize.FIGHTER) {
                    continue;
                } else {
                    if(ally.getHitpoints()/ally.getMaxHitpoints()<0.4f || ally.getFluxTracker().isOverloaded()){
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.addNebulaParticle(ally.getLocation(),new Vector2f(0f,0f), 200f, 1f, 1.2f, 0.5f, 1.25f,LIGHTNING_FRINGE_COLOR2);
                        engine.addNebulaParticle(ship.getLocation(),new Vector2f(0f,0f), 150f, 1f, 1.2f, 0.5f, 1.25f,FRINGE_COLOR2);
                        ally.getLocation().set(MathUtils.getRandomPointOnCircumference(ship.getLocation(),150f));
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.spawnEmpArcVisual(ally.getLocation(),ally,MathUtils.getRandomPointInCircle(ally.getLocation(),20f),ally,10,FRINGE_COLOR2,LIGHTNING_FRINGE_COLOR2);
                        engine.addNebulaParticle(ally.getLocation(),new Vector2f(0f,0f), 200f, 1f, 1.2f, 0.5f, 1.25f,LIGHTNING_FRINGE_COLOR2);
                        engine.addNebulaParticle(ally.getLocation(),new Vector2f(0f,0f), 150f, 1f, 1.2f, 0.5f, 1.25f,FRINGE_COLOR2);
                        ship.setCustomData("recallcharges", MathUtils.clamp((int)ship.getCustomData().get("recallcharges")-1,0,4));
                        ship.setCustomData("recalltimer", 5);
                        if(ally.getHitpoints()/ally.getMaxHitpoints()<0.4f) ally.setHitpoints(ally.getMaxHitpoints()*0.45f);
                        if(ally.getFluxTracker().isOverloaded()) ally.getFluxTracker().stopOverload();
                        if(ally.getWing()!=null) ally.getWing().orderReturn(ally);
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 3f;
        float opad = 10f;
        Color m = Misc.getMissileMountColor();
        Color e = Misc.getHighlightColor();
        Color b = Misc.getHighlightColor();
        Color t = Misc.getHighlightColor();
        Color l = Misc.getDesignTypeColor("Low Tech");
        Color md = Misc.getDesignTypeColor("Midline");
        Color h = Misc.getDesignTypeColor("High Tech");
        Color et = Misc.getDesignTypeColor("Epta Tech");
        Color p = Misc.getHighlightColor();
        Color w = Misc.getDesignTypeColor("seven white");
        Color prt = Misc.getDesignTypeColor("Pirate");

        Color bad = Misc.getNegativeHighlightColor();


        LabelAPI label = tooltip.addPara("Utilizes experimental '%s' technology to forcibly teleport damaged fighters and strike craft back to the carrier when %s. Stores up to %s and regains %s every %s.", opad, t,
                "" + "Phase Grabber",
                "" + "damaged or overloaded",
                "" + "4 charges",
                "" + "1 charge",
                "" + "10 seconds");
        label.setHighlight(
                "" + "Phase Grabber",
                "" + "damaged or overloaded",
                "" + "4 charges",
                "" + "1 charge",
                "" + "10 seconds");
        label.setHighlightColors(e, bad, b, b, b);
    }
}
