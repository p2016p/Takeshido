
package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class seven_commission_epta extends BaseHullMod {

    
    public static final float DISSIPATION = 1.05f;
    public static final float CAPACITY = 1.05f;
    
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		
            stats.getFluxDissipation().modifyMult(id, 1f * DISSIPATION);
            stats.getFluxCapacity().modifyMult(id, 1f * CAPACITY);
            
        }
            
               
    public String getDescriptionParam(int index, HullSize hullSize) {
    //if (index == 0) return "" + (int)RANGE_PENALTY_PERCENT + "%";
        return null;
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



        LabelAPI label = tooltip.addPara("%s specializes in maintaining %s and their crews know some tricks that can %s of any ship in the sector.", opad, t,
        "" + "Epta Tech",
        "" + "high capacity flux grids",
        "" + "increase the flux capacity and dissipation");
	label.setHighlight("" + "Epta Tech",
            "" + "high capacity flux grids",
            "" + "increase the flux capacity and dissipation");
	label.setHighlightColors(et, e, e);


        tooltip.addSectionHeading("Modifies:", Alignment.MID, opad);
        
        label = tooltip.addPara( "%s by %s; ", opad, t,
        "" + "Increases flux capacity and dissipation",
        "" + "5%");
	label.setHighlight("" + "Increases flux capacity and dissipation",
        "" + "5%");
	label.setHighlightColors(e, b);
    
    }
}