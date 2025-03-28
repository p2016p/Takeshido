package data.weapons.projAI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//credit to VIC for the basics on this

public class seven_sminos_rocket_AI implements MissileAIPlugin, GuidedMissileAI {

    private final MissileAPI missile;
    private final IntervalUtil timer = new IntervalUtil(0.05f, 0.15f);
    private CombatEngineAPI engine;
    private CombatEntityAPI target;

    private float rotation = 0f;

    private Vector2f firstLoc;

    public seven_sminos_rocket_AI(MissileAPI missile, ShipAPI launchingShip) {
        if (engine != Global.getCombatEngine()) {
            this.engine = Global.getCombatEngine();
        }
        this.missile = missile;
        missile.getSpriteAPI().setAdditiveBlend();
    }



    @Override
    public void advance(float amount) {

        setTarget(acquireTargetIfNeeded());

        //skip the AI if the game is paused, the missile is engineless or fading
        if (engine.isPaused()) {
            return;
        }
        if (firstLoc == null) {
            firstLoc = new Vector2f(missile.getLocation());
        }
        //init vars

        if(getTarget()!=null) {
            missile.giveCommand(ShipCommand.ACCELERATE);
            missile.giveCommand(VectorUtils.getAngle(missile.getLocation(), getTarget().getLocation()) < 0 ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
        }
    }

    private CombatEntityAPI acquireTargetIfNeeded(){

        if((target instanceof ShipAPI && !((ShipAPI)target).isAlive()) || (target == null)){

        }
        return target;
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.target = target;
    }
}