package data.scripts.world.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class TakeshidoUsedCar extends BaseSubmarketPlugin {

    public TakeshidoUsedCar() {
    }

    @Override
    public void updateCargoPrePlayerInteraction()
    {
        //log.info("Days since update: " + sinceLastCargoUpdate);
        if (sinceLastCargoUpdate < 30) return;
        sinceLastCargoUpdate = 0f;

        CargoAPI cargo = getCargo();

        //pruneWeapons(1f);

        // 50% chance per stack in the cargo to remove that stack
        for (CargoStackAPI s : cargo.getStacksCopy()) {
            if (itemGenRandom.nextFloat() > 0.5f) {
                float qty = s.getSize();
                cargo.removeItems(s.getType(), s.getData(), qty );
            }
        }
        cargo.removeEmptyStacks();
        WeightedRandomPicker<String> cars = new WeightedRandomPicker<>();
        cars.add("takeshido_hobi_custom",10);
        cars.add("takeshido_350x_custom",8);
        cars.add("takeshido_camirillo_custom",6);
        cars.add("takeshido_BR97_custom",4);
        cars.add("takeshido_NMW_G3_custom",2);
        cars.add("takeshido_vroomicorn_custom",1);
        cars.add("takeshido_bonta_custom",0.5f);
        cars.add("takeshido_bionda_custom",0.5f);

        for (int i = 0; i < MathUtils.getRandomNumberInRange(4,10); i++) {
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, cars.pick()+"_Hull");
            cargo.getMothballedShips().addFleetMember(member);
        }
        cargo.sort();
    }

    @Override
    public float getTariff() {
        RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        float mult = 1f;
        switch (level)
        {
            case NEUTRAL:
                mult = .1f;
                break;
            case SUSPICIOUS:
                mult = .11f;
                break;
            case INHOSPITABLE:
                mult = .13f;
                break;
            case HOSTILE:
                mult = .15f;
                break;
            case VENGEFUL:
                mult = .18f;
                break;
            default:
                mult = 0.08f;
        }
        return mult;
    }

    @Override
    public boolean isBlackMarket() {
        return false;
    }
}

