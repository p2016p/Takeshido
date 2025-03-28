package data.weapons.weaponAI;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class seven_sminos_autofireAI implements AutofireAIPlugin {

    final float areaAngle = 30f * 0.9f;
    final float switchOffAngle = areaAngle * 0.5f;

    IntervalUtil interval = new IntervalUtil(0.05f, 0.15f);

    Float targetAngle;

    MissileData targetMissile = null;
    ShipAPI targetFighter = null;
    final WeaponAPI weapon;
    final boolean ignoreFlares;

    public seven_sminos_autofireAI(WeaponAPI weapon) {
        this.weapon = weapon;
        this.ignoreFlares = weapon.getShip().getMutableStats().getDynamic().getMod(Stats.PD_IGNORES_FLARES).getFlatBonus() >= 1;
    }

    @Override
    public void advance(float amount) {
        ShipAPI ship = weapon.getShip();

        if(!ship.getCustomData().containsKey("sminosmode")){
            ship.setCustomData("sminosmode","light");
        }

        if(ship.getCustomData().get("sminosmode").equals("light")) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {

                targetFighter = null;

                List<MissileAPI> missiles = CombatUtils.getMissilesWithinRange(weapon.getLocation(), weapon.getRange());
                ArrayList<MissileData> missilesInArc = new ArrayList<>();
                for (MissileAPI missile : missiles) {
                    if (ignoreFlares && (missile.isDecoyFlare() || missile.isFlare())) continue;
                    if (missile.getOwner() == weapon.getShip().getOwner()) continue;
                    if (missile.getCollisionClass() == CollisionClass.NONE) continue;
                    float angleToTarget = VectorUtils.getAngle(weapon.getLocation(), missile.getLocation());
                    float angle = Math.abs(MathUtils.getShortestRotation(angleToTarget, weapon.getSlot().getAngle() + weapon.getShip().getFacing()));
                    if (angle <= weapon.getArc() * 0.5 + 10) {
                        missilesInArc.add(new MissileData(missile, angleToTarget));
                    }
                }

                MissileData bestMissile = null;

                for (MissileData missile : missilesInArc) {
                    for (MissileData otherMissile : missilesInArc) {
                        if (otherMissile == missile) continue;
                        float angle = MathUtils.getShortestRotation(missile.angle, otherMissile.angle);
                        if (angle <= areaAngle && angle >= 0) {
                            missile.totalScore += otherMissile.score;
                            if (missile.lastAngle < angle) {
                                missile.lastAngle = angle;
                                missile.lastMissile = otherMissile;
                            }
                        }
                    }
                    if (bestMissile != null) {
                        if (bestMissile.totalScore < missile.totalScore) bestMissile = missile;
                    } else {
                        bestMissile = missile;
                    }
                }

                targetMissile = bestMissile;
                if (targetMissile == null) {
                    if (AIUtils.getNearestEnemy(weapon.getShip()) != null) {
                        targetFighter = AIUtils.getNearestEnemy(weapon.getShip());
                    }
                }
            }
        }else if(ship.getCustomData().get("sminosmode").equals("heavy")){
            ShipAPI bestenemy = null;
            for(ShipAPI enemy:WeaponUtils.getEnemiesInArc(weapon)){
                if(bestenemy==null){
                    bestenemy = enemy;
                }else if(enemy.getVelocity().length()<bestenemy.getVelocity().length()){
                    bestenemy = enemy;
                }
            }
            targetFighter = bestenemy;
        }
    }

    @Override
    public void forceOff() {

    }

    @Override
    public Vector2f getTarget() {
        if (targetMissile == null) {
            targetAngle = null;
            if (targetFighter != null){
                targetAngle = VectorUtils.getAngle(weapon.getLocation(), targetFighter.getLocation());
                return new Vector2f(targetFighter.getLocation());
            }
            return null;
        } else {
            targetAngle = targetMissile.angle + targetMissile.lastAngle;
            return MathUtils.getPointOnCircumference(
                    weapon.getLocation(),
                    MathUtils.getDistance(weapon.getLocation(), targetMissile.missile.getLocation()),
                    targetAngle);
        }
    }


    @Override
    public ShipAPI getTargetShip() {
        return targetFighter;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public boolean shouldFire() {
        if (targetAngle == null) return false;
        if (getTarget()!=null){
            if(getTargetShip() != null && MathUtils.getDistance(getTargetShip(),weapon.getShip())<=weapon.getRange() && MathUtils.getShortestRotation(VectorUtils.getAngle(weapon.getLocation(), getTargetShip().getLocation()),weapon.getShip().getFacing())<25f) return true;
            else if(getTargetMissile() != null && MathUtils.getDistance(getTargetMissile(),weapon.getShip())<=weapon.getRange()) return true;
        }
        return false;
        //return Math.abs(targetAngle - weapon.getCurrAngle()) <= switchOffAngle;
    }

    @Override
    public MissileAPI getTargetMissile() {
        if (targetMissile != null) {
            return targetMissile.missile;
        }
        return null;
    }

    static final class MissileData {

        public MissileData(MissileAPI missile, float angle){
            this.missile = missile;
            this.angle = angle;
            this.score = missile.getDamageAmount() / missile.getHitpoints();
            if (missile.getDamageType() == DamageType.FRAGMENTATION) this.score *= 0.25;
        }

        MissileAPI missile;
        float angle;
        float score;
        public float totalScore;
        public float lastAngle = 0;
        public MissileData lastMissile;

    }
}
