package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

public class TakeshidoLavCannonEveryFrame implements EveryFrameWeaponEffectPlugin {
    private static final float TARGET_WIDTH = 15f;
    private static final float TARGET_HEIGHT = 42f;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (weapon == null) return;

        scale(weapon.getSprite());
        scale(weapon.getUnderSpriteAPI());
        scale(weapon.getBarrelSpriteAPI());
        scale(weapon.getGlowSpriteAPI());
    }

    private static void scale(SpriteAPI sprite) {
        if (sprite == null) return;
        if (Math.abs(sprite.getWidth() - TARGET_WIDTH) > 0.01f || Math.abs(sprite.getHeight() - TARGET_HEIGHT) > 0.01f) {
            sprite.setSize(TARGET_WIDTH, TARGET_HEIGHT);
        }
        // Keep render pivot centered after resize; otherwise large-source sprites can appear offset.
        sprite.setCenter(TARGET_WIDTH * 0.5f, TARGET_HEIGHT * 0.5f);
    }
}
