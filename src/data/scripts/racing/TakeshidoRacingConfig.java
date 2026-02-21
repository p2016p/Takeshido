package data.scripts.racing;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TakeshidoRacingConfig {
    public static final String PATH = "data/config/takeshido_racing.json";

    private static TakeshidoRacingConfig INSTANCE;

    public static TakeshidoRacingConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static void clearCache() {
        INSTANCE = null;
    }

    public final Map<String, CategorySpec> categories = new LinkedHashMap<>();
    public final Map<String, List<String>> planetTracks = new LinkedHashMap<>();
    public final TournamentSpec tournament = new TournamentSpec();
    public final RewardsSpec rewards = new RewardsSpec();
    public final List<RacerProfile> racers = new ArrayList<>();

    public CategorySpec getCategory(String id) {
        if (id == null) return null;
        return categories.get(id.toLowerCase(Locale.ROOT));
    }

    public List<String> getTracksForMarket(String marketId) {
        if (marketId != null) {
            List<String> tracks = planetTracks.get(marketId);
            if (tracks != null && !tracks.isEmpty()) return tracks;
        }
        return tournament.trackListDefault;
    }

    public String findMarketForTrack(String trackId) {
        if (trackId == null) return null;
        for (Map.Entry<String, List<String>> entry : planetTracks.entrySet()) {
            for (String t : entry.getValue()) {
                if (trackId.equals(t)) return entry.getKey();
            }
        }
        return null;
    }

    private static TakeshidoRacingConfig load() {
        TakeshidoRacingConfig cfg = new TakeshidoRacingConfig();
        try {
            JSONObject root = Global.getSettings().loadJSON(PATH);

            JSONArray cats = root.optJSONArray("categories");
            if (cats != null) {
                for (int i = 0; i < cats.length(); i++) {
                    JSONObject c = cats.getJSONObject(i);
                    CategorySpec spec = new CategorySpec();
                    spec.id = c.optString("id", "").trim().toLowerCase(Locale.ROOT);
                    spec.label = c.optString("label", spec.id);
                    JSONArray des = c.optJSONArray("designations");
                    if (des != null) {
                        for (int d = 0; d < des.length(); d++) {
                            String val = des.getString(d);
                            if (val != null) spec.designations.add(val.toLowerCase(Locale.ROOT));
                        }
                    }
                    if (!spec.id.isEmpty()) {
                        cfg.categories.put(spec.id, spec);
                    }
                }
            }

            JSONObject planet = root.optJSONObject("planetTracks");
            if (planet != null) {
                for (Iterator<String> it = planet.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONArray arr = planet.optJSONArray(key);
                    if (arr == null) continue;
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        list.add(arr.getString(i));
                    }
                    cfg.planetTracks.put(key, list);
                }
            }

            JSONObject tour = root.optJSONObject("tournament");
            if (tour != null) {
                cfg.tournament.racesPerTournament = tour.optInt("racesPerTournament", cfg.tournament.racesPerTournament);
                cfg.tournament.racersPerRace = tour.optInt("racersPerRace", cfg.tournament.racersPerRace);
                JSONArray list = tour.optJSONArray("trackListDefault");
                if (list != null) {
                    cfg.tournament.trackListDefault.clear();
                    for (int i = 0; i < list.length(); i++) {
                        cfg.tournament.trackListDefault.add(list.getString(i));
                    }
                }
                JSONArray points = tour.optJSONArray("points");
                if (points != null) {
                    cfg.tournament.points.clear();
                    for (int i = 0; i < points.length(); i++) {
                        cfg.tournament.points.add(points.getInt(i));
                    }
                }
            }

            JSONObject rewards = root.optJSONObject("rewards");
            if (rewards != null) {
                JSONArray impromptu = rewards.optJSONArray("impromptu");
                if (impromptu != null) {
                    cfg.rewards.impromptu.clear();
                    for (int i = 0; i < impromptu.length(); i++) {
                        cfg.rewards.impromptu.add(impromptu.getInt(i));
                    }
                }
                JSONArray tourRewards = rewards.optJSONArray("tournament");
                if (tourRewards != null) {
                    cfg.rewards.tournament.clear();
                    for (int i = 0; i < tourRewards.length(); i++) {
                        cfg.rewards.tournament.add(tourRewards.getInt(i));
                    }
                }
            }

            JSONArray roster = root.optJSONArray("racers");
            if (roster != null) {
                cfg.racers.clear();
                for (int i = 0; i < roster.length(); i++) {
                    JSONObject r = roster.optJSONObject(i);
                    if (r == null) continue;
                    RacerProfile rp = new RacerProfile();
                    rp.name = r.optString("name", "").trim();
                    rp.variantId = r.optString("variantId", "").trim();
                    rp.personality = r.optString("personality", "").trim();
                    rp.skill = (float) r.optDouble("skill", 0.7f);
                    rp.officerLevel = r.optInt("officerLevel", 0);
                    JSONArray skills = r.optJSONArray("skills");
                    if (skills != null) {
                        for (int s = 0; s < skills.length(); s++) {
                            JSONObject skillObj = skills.optJSONObject(s);
                            if (skillObj == null) continue;
                            String id = skillObj.optString("id", "").trim();
                            float level = (float) skillObj.optDouble("level", 1f);
                            if (!id.isEmpty()) {
                                rp.skills.add(new RacerSkill(id, level));
                            }
                        }
                    }
                    if (!rp.variantId.isEmpty()) {
                        cfg.racers.add(rp);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + PATH, e);
        }

        return cfg;
    }

    public static class CategorySpec {
        public String id;
        public String label;
        public final List<String> designations = new ArrayList<>();

        public boolean matchesDesignation(String designation) {
            if (designation == null) return false;
            String d = designation.toLowerCase(Locale.ROOT);
            for (String allowed : designations) {
                if (d.equals(allowed)) return true;
            }
            return false;
        }
    }

    public static class TournamentSpec {
        public int racesPerTournament = 3;
        public int racersPerRace = 8;
        public final List<String> trackListDefault = new ArrayList<>();
        public final List<Integer> points = new ArrayList<>();
    }

    public static class RewardsSpec {
        public final List<Integer> impromptu = new ArrayList<>();
        public final List<Integer> tournament = new ArrayList<>();
    }

    public static class RacerProfile {
        public String name;
        public String variantId;
        public String personality;
        public float skill;
        public int officerLevel;
        public final List<RacerSkill> skills = new ArrayList<>();
    }

    public static class RacerSkill {
        public final String id;
        public final float level;

        public RacerSkill(String id, float level) {
            this.id = id;
            this.level = level;
        }
    }
}
