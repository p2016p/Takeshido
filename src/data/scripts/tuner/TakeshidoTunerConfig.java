package data.scripts.tuner;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TakeshidoTunerConfig {
    public static final String PATH = "data/config/takeshido_tuner_upgrades.json";

    private static TakeshidoTunerConfig INSTANCE;

    public static TakeshidoTunerConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static void clearCache() {
        INSTANCE = null;
    }

    public final Map<String, UpgradeSpec> upgrades = new LinkedHashMap<>();
    public final List<UpgradeSpec> upgradeList = new ArrayList<>();

    public UpgradeSpec getUpgrade(String id) {
        if (id == null) return null;
        return upgrades.get(id.toLowerCase(Locale.ROOT));
    }

    private static TakeshidoTunerConfig load() {
        TakeshidoTunerConfig cfg = new TakeshidoTunerConfig();
        try {
            JSONObject root = Global.getSettings().loadJSON(PATH);
            JSONArray arr = root.optJSONArray("upgrades");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.optJSONObject(i);
                    if (obj == null) continue;
                    UpgradeSpec spec = new UpgradeSpec();
                    spec.id = obj.optString("id", "").trim().toLowerCase(Locale.ROOT);
                    if (spec.id.isEmpty()) continue;
                    spec.name = obj.optString("name", spec.id);
                    spec.type = obj.optString("type", "").trim().toLowerCase(Locale.ROOT);
                    spec.description = obj.optString("description", "").trim();
                    spec.icon = obj.optString("icon", "").trim();
                    spec.priceMult = (float) obj.optDouble("priceMult", 0f);
                    spec.priceFlat = (float) obj.optDouble("priceFlat", 0f);

                    JSONArray incompat = obj.optJSONArray("incompatible");
                    if (incompat != null) {
                        for (int j = 0; j < incompat.length(); j++) {
                            String id = incompat.optString(j, "").trim().toLowerCase(Locale.ROOT);
                            if (!id.isEmpty()) spec.incompatible.add(id);
                        }
                    }

                    JSONArray mods = obj.optJSONArray("mods");
                    if (mods != null) {
                        for (int j = 0; j < mods.length(); j++) {
                            JSONObject mod = mods.optJSONObject(j);
                            if (mod == null) continue;
                            StatMod sm = new StatMod();
                            sm.stat = mod.optString("stat", "").trim();
                            sm.mode = mod.optString("mode", "").trim();
                            sm.value = (float) mod.optDouble("value", 0f);
                            if (!sm.stat.isEmpty() && !sm.mode.isEmpty()) {
                                spec.mods.add(sm);
                            }
                        }
                    }

                    cfg.upgrades.put(spec.id, spec);
                    cfg.upgradeList.add(spec);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + PATH, e);
        }
        return cfg;
    }

    public static class UpgradeSpec {
        public String id;
        public String name;
        public String type;
        public String description;
        public String icon;
        public float priceMult;
        public float priceFlat;
        public final List<String> incompatible = new ArrayList<>();
        public final List<StatMod> mods = new ArrayList<>();
    }

    public static class StatMod {
        public String stat;
        public String mode;
        public float value;
    }
}
