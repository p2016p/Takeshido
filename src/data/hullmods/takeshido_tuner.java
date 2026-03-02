package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tuner.TakeshidoTunerConfig;
import data.scripts.tuner.TakeshidoTunerManager;
import data.scripts.racing.TakeshidoRacingManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class takeshido_tuner extends BaseHullMod {

    private static final String MOD_ID = "takeshido_tuner_mods";
    private static final String RACE_HULLMOD_ID = "takeshido_racemod";

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        if (stats == null || stats.getVariant() == null) return;
        if (!stats.getVariant().hasHullMod(RACE_HULLMOD_ID)) return;

        FleetMemberAPI member = TakeshidoTunerManager.resolveFleetMember(stats);
        Set<String> applied;
        if (member != null) {
            applied = TakeshidoTunerManager.getInstalledUpgradesNoBuiltIn(member);
        } else {
            applied = new java.util.LinkedHashSet<String>();
        }
        TakeshidoTunerManager.UpgradeTotals totals = TakeshidoTunerManager.computeTotals(applied);
        totals.applyToStats(stats, MOD_ID);
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && ship.getVariant() != null && ship.getVariant().hasHullMod(RACE_HULLMOD_ID);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return "Requires Takeshido Racing Spec.";
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return true;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float opad = 10f;
        float pad = 3f;
        Color h = Misc.getHighlightColor();

        tooltip.addSectionHeading("Tuner Upgrades", Alignment.MID, opad);

        if (ship == null || ship.getFleetMember() == null) {
            tooltip.addPara("No tuner data available.", opad);
            return;
        }

        FleetMemberAPI member = ship.getFleetMember();
        TakeshidoTunerConfig cfg = TakeshidoTunerConfig.get();
        Set<String> builtIn = TakeshidoTunerManager.getBuiltInUpgrades(member.getHullSpec());
        Set<String> installed = TakeshidoTunerManager.getInstalledUpgrades(member);

        List<TakeshidoTunerConfig.UpgradeSpec> applied = new ArrayList<>();
        for (TakeshidoTunerConfig.UpgradeSpec spec : cfg.upgradeList) {
            if (spec == null) continue;
            if (installed.contains(spec.id)) applied.add(spec);
        }

        if (applied.isEmpty()) {
            tooltip.addPara("No upgrades installed.", opad);
            return;
        }

        for (TakeshidoTunerConfig.UpgradeSpec spec : applied) {
            boolean isBuiltIn = builtIn.contains(spec.id);
            String status = isBuiltIn ? " (built-in)" : "";
            String effects = formatEffects(spec);
            LabelAPI label = tooltip.addPara("%s%s: %s", pad, h, spec.name, status, effects);
            label.setHighlight(spec.name);
            if (spec.description != null && !spec.description.isEmpty()) {
                tooltip.addPara(spec.description, pad);
            }
        }

        float score = TakeshidoRacingManager.computeTrackScoreForMember(member);
        List<String> tiers = TakeshidoRacingManager.getEligibleCategoryLabels(score);
        if (!tiers.isEmpty()) {
            String list = joinList(tiers);
            tooltip.addPara("Eligible races: %s", opad, h, list);
        }
    }

    private String formatEffects(TakeshidoTunerConfig.UpgradeSpec spec) {
        if (spec == null || spec.mods == null || spec.mods.isEmpty()) return "No stat changes.";
        List<String> parts = new ArrayList<>();
        for (TakeshidoTunerConfig.StatMod mod : spec.mods) {
            if (mod == null || mod.stat == null || mod.mode == null) continue;
            String label = statLabel(mod.stat);
            if (label == null) continue;
            if ("mult".equalsIgnoreCase(mod.mode)) {
                float pct = (mod.value - 1f) * 100f;
                if (Math.abs(pct) < 0.01f) continue;
                parts.add(String.format(Locale.ROOT, "%+.0f%% %s", pct, label));
            } else if ("flat".equalsIgnoreCase(mod.mode)) {
                if (Math.abs(mod.value) < 0.01f) continue;
                parts.add(String.format(Locale.ROOT, "%+.0f %s", mod.value, label));
            }
        }
        if (parts.isEmpty()) return "No stat changes.";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private String statLabel(String stat) {
        if (stat == null) return null;
        String s = stat.toLowerCase(Locale.ROOT);
        if ("maxspeed".equals(s)) return "max speed";
        if ("acceleration".equals(s)) return "acceleration";
        if ("deceleration".equals(s)) return "deceleration";
        if ("maxturnrate".equals(s)) return "max turn rate";
        if ("turnacceleration".equals(s)) return "turn acceleration";
        return null;
    }

    private String joinList(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
