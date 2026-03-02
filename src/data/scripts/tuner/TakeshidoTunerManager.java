package data.scripts.tuner;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TakeshidoTunerManager {
    public static final String DATA_KEY = "takeshido_tuner_installed";
    public static final String DATA_KEY_MEMBER = DATA_KEY;
    public static final String DATA_KEY_VARIANT = "takeshido_tuner_installed_by_variant";
    private static final String DATA_KEY_VARIANT_PURGED = "takeshido_tuner_variant_purged";
    public static final String TAG_BUILTIN_PREFIX = "tuner_builtin:";
    public static final String TAG_BLOCK_PREFIX = "tuner_block:";
    public static final String TAG_BLOCK_TYPE_PREFIX = "tuner_block_type:";
    public static final String RACE_HULLMOD_ID = "takeshido_racemod";
    public static final String TUNER_HULLMOD_ID = "takeshido_tuner";

    public static class UpgradeTotals {
        public float maxSpeedMult = 1f;
        public float maxSpeedFlat = 0f;
        public float accelMult = 1f;
        public float accelFlat = 0f;
        public float decelMult = 1f;
        public float decelFlat = 0f;
        public float maxTurnRateMult = 1f;
        public float maxTurnRateFlat = 0f;
        public float turnAccelMult = 1f;
        public float turnAccelFlat = 0f;

        public void applyStatMod(TakeshidoTunerConfig.StatMod mod) {
            if (mod == null || mod.stat == null || mod.mode == null) return;
            String stat = mod.stat.toLowerCase(Locale.ROOT);
            boolean mult = "mult".equalsIgnoreCase(mod.mode);
            if ("maxspeed".equals(stat)) {
                if (mult) maxSpeedMult *= mod.value; else maxSpeedFlat += mod.value;
            } else if ("acceleration".equals(stat)) {
                if (mult) accelMult *= mod.value; else accelFlat += mod.value;
            } else if ("deceleration".equals(stat)) {
                if (mult) decelMult *= mod.value; else decelFlat += mod.value;
            } else if ("maxturnrate".equals(stat)) {
                if (mult) maxTurnRateMult *= mod.value; else maxTurnRateFlat += mod.value;
            } else if ("turnacceleration".equals(stat)) {
                if (mult) turnAccelMult *= mod.value; else turnAccelFlat += mod.value;
            }
        }

        public void applyToStats(MutableShipStatsAPI stats, String id) {
            if (stats == null) return;
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);

            stats.getMaxSpeed().modifyMult(id, maxSpeedMult);
            stats.getMaxSpeed().modifyFlat(id, maxSpeedFlat);
            stats.getAcceleration().modifyMult(id, accelMult);
            stats.getAcceleration().modifyFlat(id, accelFlat);
            stats.getDeceleration().modifyMult(id, decelMult);
            stats.getDeceleration().modifyFlat(id, decelFlat);
            stats.getMaxTurnRate().modifyMult(id, maxTurnRateMult);
            stats.getMaxTurnRate().modifyFlat(id, maxTurnRateFlat);
            stats.getTurnAcceleration().modifyMult(id, turnAccelMult);
            stats.getTurnAcceleration().modifyFlat(id, turnAccelFlat);
        }
    }

    public enum UpgradeStatus {
        AVAILABLE,
        INSTALLED,
        BUILTIN,
        BLOCKED,
        INCOMPATIBLE
    }

    public static List<FleetMemberAPI> getEligiblePlayerCars() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return new ArrayList<>();
        List<FleetMemberAPI> out = new ArrayList<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member == null || member.isFighterWing()) continue;
            ShipVariantAPI variant = member.getVariant();
            if (variant == null) continue;
            if (!variant.hasHullMod(RACE_HULLMOD_ID)) continue;
            out.add(member);
        }
        return out;
    }

    public static void ensureTunerHullmod(FleetMemberAPI member) {
        if (member == null || member.getVariant() == null) return;
        ShipVariantAPI variant = member.getVariant();
        if (!variant.hasHullMod(RACE_HULLMOD_ID)) return;
        if (!variant.hasHullMod(TUNER_HULLMOD_ID)) {
            try {
                variant.addPermaMod(TUNER_HULLMOD_ID);
            } catch (Throwable t) {
                variant.addMod(TUNER_HULLMOD_ID);
            }
        }
    }

    public static UpgradeTotals computeTotals(FleetMemberAPI member) {
        if (member == null) return new UpgradeTotals();
        Set<String> installed = getInstalledUpgrades(member);
        return computeTotals(installed);
    }

    public static UpgradeTotals computeTotals(Collection<String> upgradeIds) {
        UpgradeTotals totals = new UpgradeTotals();
        if (upgradeIds == null || upgradeIds.isEmpty()) return totals;
        TakeshidoTunerConfig cfg = TakeshidoTunerConfig.get();
        for (String id : upgradeIds) {
            TakeshidoTunerConfig.UpgradeSpec spec = cfg.getUpgrade(id);
            if (spec == null) continue;
            for (TakeshidoTunerConfig.StatMod mod : spec.mods) {
                totals.applyStatMod(mod);
            }
        }
        return totals;
    }

    public static Set<String> getInstalledUpgrades(FleetMemberAPI member) {
        Set<String> out = new LinkedHashSet<>();
        if (member == null) return out;
        ShipHullSpecAPI spec = member.getHullSpec();
        out.addAll(getBuiltInUpgrades(spec));

        out.addAll(getInstalledUpgradesNoBuiltIn(member));
        return out;
    }

    public static Set<String> getInstalledUpgradesNoBuiltIn(FleetMemberAPI member) {
        Set<String> out = new LinkedHashSet<>();
        if (member == null) return out;
        Map<String, List<String>> byMember = getMemberData();
        List<String> memberList = byMember.get(member.getId());
        addAllNormalized(out, memberList);
        return out;
    }

    public static Set<String> getInstalledUpgradesForVariant(ShipVariantAPI variant, ShipHullSpecAPI hullSpec) {
        Set<String> out = new LinkedHashSet<>();
        if (hullSpec != null) {
            out.addAll(getBuiltInUpgrades(hullSpec));
        }
        return out;
    }

    public static Set<String> getInstalledUpgradesForVariantNoBuiltIn(ShipVariantAPI variant) {
        return new LinkedHashSet<>();
    }

    public static boolean installUpgrade(FleetMemberAPI member, String upgradeId) {
        if (member == null || upgradeId == null || upgradeId.trim().isEmpty()) return false;
        String id = upgradeId.trim().toLowerCase(Locale.ROOT);
        boolean changed = false;

        Map<String, List<String>> byMember = getMemberData();
        List<String> memberList = byMember.get(member.getId());
        if (memberList == null) {
            memberList = new ArrayList<>();
            byMember.put(member.getId(), memberList);
        }
        if (!memberList.contains(id)) {
            memberList.add(id);
            changed = true;
        }
        return changed;
    }

    public static boolean uninstallUpgrade(FleetMemberAPI member, String upgradeId) {
        if (member == null || upgradeId == null || upgradeId.trim().isEmpty()) return false;
        String id = upgradeId.trim().toLowerCase(Locale.ROOT);
        boolean changed = false;

        Map<String, List<String>> byMember = getMemberData();
        List<String> memberList = byMember.get(member.getId());
        if (memberList != null) {
            for (int i = memberList.size() - 1; i >= 0; i--) {
                String cur = memberList.get(i);
                if (cur != null && cur.trim().toLowerCase(Locale.ROOT).equals(id)) {
                    memberList.remove(i);
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static UpgradeStatus getUpgradeStatus(FleetMemberAPI member, TakeshidoTunerConfig.UpgradeSpec spec) {
        if (member == null || spec == null) return UpgradeStatus.BLOCKED;
        Set<String> builtIn = getBuiltInUpgrades(member.getHullSpec());
        if (builtIn.contains(spec.id)) return UpgradeStatus.BUILTIN;
        Set<String> installed = getInstalledUpgradesNoBuiltIn(member);
        if (installed.contains(spec.id)) return UpgradeStatus.INSTALLED;
        if (isBlocked(member, spec)) return UpgradeStatus.BLOCKED;
        if (isIncompatible(spec, getInstalledUpgrades(member))) return UpgradeStatus.INCOMPATIBLE;
        return UpgradeStatus.AVAILABLE;
    }

    public static boolean isBlocked(FleetMemberAPI member, TakeshidoTunerConfig.UpgradeSpec spec) {
        if (member == null || spec == null) return true;
        ShipHullSpecAPI hull = member.getHullSpec();
        if (hull == null) return true;
        Set<String> blocked = getBlockedUpgrades(hull);
        if (blocked.contains(spec.id)) return true;
        Set<String> blockedTypes = getBlockedTypes(hull);
        return spec.type != null && blockedTypes.contains(spec.type);
    }

    public static boolean isIncompatible(TakeshidoTunerConfig.UpgradeSpec spec, Set<String> installed) {
        if (spec == null || installed == null || installed.isEmpty()) return false;
        TakeshidoTunerConfig cfg = TakeshidoTunerConfig.get();
        for (String otherId : installed) {
            if (otherId == null) continue;
            if (otherId.equals(spec.id)) continue;
            TakeshidoTunerConfig.UpgradeSpec other = cfg.getUpgrade(otherId);
            if (other != null && other.type != null && other.type.equals(spec.type) && spec.type != null && !spec.type.isEmpty()) {
                return true;
            }
            if (spec.incompatible.contains(otherId)) return true;
            if (other != null && other.incompatible.contains(spec.id)) return true;
        }
        return false;
    }

    public static int getUpgradePrice(FleetMemberAPI member, TakeshidoTunerConfig.UpgradeSpec spec) {
        if (member == null || spec == null) return 0;
        ShipHullSpecAPI hull = member.getHullSpec();
        float base = hull != null ? hull.getBaseValue() : member.getBaseValue();
        return Math.round(base * spec.priceMult + spec.priceFlat);
    }

    public static Set<String> getBuiltInUpgrades(ShipHullSpecAPI spec) {
        Set<String> out = new LinkedHashSet<>();
        if (spec == null) return out;
        Set<String> tags = spec.getTags();
        if (tags == null) return out;
        for (String tag : tags) {
            if (tag == null) continue;
            String t = tag.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(TAG_BUILTIN_PREFIX)) {
                String id = t.substring(TAG_BUILTIN_PREFIX.length()).trim();
                if (!id.isEmpty()) out.add(id);
            }
        }
        return out;
    }

    public static Set<String> getBlockedUpgrades(ShipHullSpecAPI spec) {
        Set<String> out = new LinkedHashSet<>();
        if (spec == null) return out;
        Set<String> tags = spec.getTags();
        if (tags == null) return out;
        for (String tag : tags) {
            if (tag == null) continue;
            String t = tag.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(TAG_BLOCK_PREFIX)) {
                String id = t.substring(TAG_BLOCK_PREFIX.length()).trim();
                if (!id.isEmpty()) out.add(id);
            }
        }
        return out;
    }

    public static Set<String> getBlockedTypes(ShipHullSpecAPI spec) {
        Set<String> out = new LinkedHashSet<>();
        if (spec == null) return out;
        Set<String> tags = spec.getTags();
        if (tags == null) return out;
        for (String tag : tags) {
            if (tag == null) continue;
            String t = tag.trim().toLowerCase(Locale.ROOT);
            if (t.startsWith(TAG_BLOCK_TYPE_PREFIX)) {
                String type = t.substring(TAG_BLOCK_TYPE_PREFIX.length()).trim();
                if (!type.isEmpty()) out.add(type);
            }
        }
        return out;
    }

    private static void addAllNormalized(Set<String> out, List<String> list) {
        if (out == null || list == null) return;
        for (String id : list) {
            if (id != null && !id.trim().isEmpty()) {
                out.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getMemberData() {
        if (Global.getSector() == null) return new LinkedHashMap<>();
        // One-time purge of legacy variant-scoped upgrades to avoid cross-ship bleed.
        Object purged = Global.getSector().getPersistentData().get(DATA_KEY_VARIANT_PURGED);
        if (!(purged instanceof Boolean) || !((Boolean) purged)) {
            Global.getSector().getPersistentData().remove(DATA_KEY_VARIANT);
            Global.getSector().getPersistentData().put(DATA_KEY_VARIANT_PURGED, true);
        }
        Object data = Global.getSector().getPersistentData().get(DATA_KEY_MEMBER);
        if (data instanceof Map) {
            return (Map<String, List<String>>) data;
        }
        Map<String, List<String>> map = new LinkedHashMap<>();
        Global.getSector().getPersistentData().put(DATA_KEY_MEMBER, map);
        return map;
    }

    public static FleetMemberAPI resolveFleetMember(MutableShipStatsAPI stats) {
        if (stats == null) return null;
        try {
            Method m = stats.getClass().getMethod("getFleetMember");
            Object result = m.invoke(stats);
            if (result instanceof FleetMemberAPI) return (FleetMemberAPI) result;
        } catch (Exception ignored) {
        }
        try {
            Object entity = stats.getEntity();
            if (entity instanceof com.fs.starfarer.api.combat.ShipAPI) {
                return ((com.fs.starfarer.api.combat.ShipAPI) entity).getFleetMember();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
