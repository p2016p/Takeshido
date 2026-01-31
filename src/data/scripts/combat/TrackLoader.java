package data.scripts.combat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;

public class TrackLoader {
    private static final String PATH = "data/config/race_tracks.json";
    private static Map<String, TrackSpec> CACHE = null;

    public static TrackSpec get(String trackId) {
        if (CACHE == null) loadAll();
        TrackSpec spec = CACHE.get(trackId);
        if (spec == null) throw new RuntimeException("No track with id=" + trackId + " in " + PATH);
        return spec;
    }

    public static void loadAll() {
        CACHE = new LinkedHashMap<>();
        try {
            JSONObject root = Global.getSettings().loadJSON(PATH);
            JSONArray tracks = root.getJSONArray("tracks");
            for (int i = 0; i < tracks.length(); i++) {
                JSONObject t = tracks.getJSONObject(i);
                TrackSpec spec = parseTrack(t);
                CACHE.put(spec.id, spec);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tracks from " + PATH, e);
        }
    }

    private static TrackSpec parseTrack(JSONObject t) throws Exception {
        TrackSpec spec = new TrackSpec();
        spec.id = t.getString("id");
        spec.name = t.optString("name", spec.id);

        if (t.has("lapsToWin")) spec.lapsToWin = t.getInt("lapsToWin");

        spec.checkpointRadius = (float) t.optDouble("checkpointRadius", spec.checkpointRadius);
        spec.wallOffsetExtra = (float) t.optDouble("wallOffsetExtra", spec.wallOffsetExtra);
        spec.edgeMarkerSpacing = (float) t.optDouble("edgeMarkerSpacing", spec.edgeMarkerSpacing);
        spec.startIndex = t.optInt("startIndex", 0);


        JSONObject g = t.optJSONObject("grid");
        if (g != null) {
            spec.grid.columns = g.optInt("columns", spec.grid.columns);
            spec.grid.desiredLaneSpacing = (float) g.optDouble("desiredLaneSpacing", spec.grid.desiredLaneSpacing);
            spec.grid.rowSpacing = (float) g.optDouble("rowSpacing", spec.grid.rowSpacing);
            spec.grid.edgeMargin = (float) g.optDouble("edgeMargin", spec.grid.edgeMargin);
            spec.grid.widthMult = (float) g.optDouble("widthMult", spec.grid.widthMult);
            spec.grid.aheadOffset = (float) g.optDouble("aheadOffset", spec.grid.aheadOffset);
        }

        JSONArray cps = t.getJSONArray("checkpoints");
        for (int i = 0; i < cps.length(); i++) {
            JSONArray pair = cps.getJSONArray(i);
            float x = (float) pair.getDouble(0);
            float y = (float) pair.getDouble(1);
            spec.checkpoints.add(new Vector2f(x, y));
        }

        if (spec.checkpoints.size() < 2) {
            throw new RuntimeException("Track " + spec.id + " has <2 checkpoints");
        }

        return spec;
    }
}

