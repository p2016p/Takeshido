package data.scripts.combat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        // Build into a temp map so a partial failure doesn't leave a half-populated CACHE.
        Map<String, TrackSpec> temp = new LinkedHashMap<>();
        try {
            JSONObject root = Global.getSettings().loadJSON(PATH);

            JSONArray tracks = root.optJSONArray("tracks");
            if (tracks == null) {
                throw new RuntimeException("Top-level key \"tracks\" missing or not an array in " + PATH);
            }

            for (int i = 0; i < tracks.length(); i++) {
                JSONObject t = tracks.getJSONObject(i);
                TrackSpec spec = parseTrack(t);
                if (spec.id == null || spec.id.trim().isEmpty()) {
                    throw new RuntimeException("Track entry at index " + i + " has missing/blank id in " + PATH);
                }
                if (temp.containsKey(spec.id)) {
                    throw new RuntimeException("Duplicate track id \"" + spec.id + "\" in " + PATH);
                }
                temp.put(spec.id, spec);
            }

            CACHE = temp;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tracks from " + PATH, e);
        }
    }

    private static TrackSpec parseTrack(JSONObject t) throws Exception {
        TrackSpec spec = new TrackSpec();

        // ---- Identity / basics ----
        spec.id = t.getString("id");
        spec.name = t.optString("name", spec.id);

        if (t.has("lapsToWin")) spec.lapsToWin = t.getInt("lapsToWin");

        spec.checkpointRadius = (float) t.optDouble("checkpointRadius", spec.checkpointRadius);
        spec.edgeMarkerSpacing = (float) t.optDouble("edgeMarkerSpacing", spec.edgeMarkerSpacing);
        spec.wallAsteroidSpacing = (float) t.optDouble("wallAsteroidSpacing", spec.wallAsteroidSpacing);
        spec.wallAsteroidType = t.optInt("wallAsteroidType", spec.wallAsteroidType);
        spec.wallAsteroidTrackBuffer = (float) t.optDouble("wallAsteroidTrackBuffer", spec.wallAsteroidTrackBuffer);
        spec.startIndex = t.optInt("startIndex", 0);

        // ---- Grid ----
        JSONObject g = t.optJSONObject("grid");
        if (g != null) {
            spec.grid.columns = g.optInt("columns", spec.grid.columns);
            spec.grid.rowSpacing = (float) g.optDouble("rowSpacing", spec.grid.rowSpacing);
            spec.grid.edgeMargin = (float) g.optDouble("edgeMargin", spec.grid.edgeMargin);
            spec.grid.aheadOffset = (float) g.optDouble("aheadOffset", spec.grid.aheadOffset);
        }

        // ---- Optional explicit checkpoints (if present, we keep them) ----
        JSONArray cps = t.optJSONArray("checkpoints");
        if (cps != null) {
            for (int i = 0; i < cps.length(); i++) {
                JSONArray pair = cps.getJSONArray(i);
                if (pair.length() < 2) continue;
                float x = (float) pair.getDouble(0);
                float y = (float) pair.getDouble(1);
                spec.checkpoints.add(new Vector2f(x, y));
            }
        }

        // ---- Centerline CSV settings (optional) ----
        spec.centerlineCsv = t.optString("centerlineCsv", null);
        spec.racelineCsv = t.optString("racelineCsv", null);
        spec.unitsPerMeter = (float) t.optDouble("unitsPerMeter", spec.unitsPerMeter);
        spec.rotationDeg = (float) t.optDouble("rotationDeg", spec.rotationDeg);
        spec.autoCenter = t.optBoolean("autoCenter", spec.autoCenter);

        JSONArray off = t.optJSONArray("offset");
        if (off != null && off.length() >= 2) {
            spec.offsetX = (float) off.getDouble(0);
            spec.offsetY = (float) off.getDouble(1);
        }

        spec.useCsvWidths = t.optBoolean("useCsvWidths", spec.useCsvWidths);
        spec.widthScale = (float) t.optDouble("widthScale", spec.widthScale);

        spec.checkpointSpacing = (float) t.optDouble("checkpointSpacing", spec.checkpointSpacing);
        if (spec.checkpointSpacing <= 0f) spec.checkpointSpacing = 900f;

        // ---- Load centerline if specified ----
        if (spec.centerlineCsv != null && !spec.centerlineCsv.isEmpty()) {
            loadCenterlineCsvIntoSpec(spec);
        }
        if (spec.racelineCsv != null && !spec.racelineCsv.isEmpty()) {
            loadRacelineCsvIntoSpec(spec);
        }

        // ---- Auto-generate checkpoints only if none were explicitly provided ----
        if (spec.checkpoints.isEmpty() && !spec.centerline.isEmpty()) {
            List<Vector2f> gen = generateCheckpointsFromCenterline(spec.centerline, spec.checkpointSpacing);
            spec.checkpoints.addAll(gen);
        }

        // ---- Validate ----
        if (spec.checkpoints.size() < 2) {
            throw new RuntimeException(
                    "Track \"" + spec.id + "\" has <2 checkpoints. " +
                            "Provide \"checkpoints\" in JSON or provide a valid \"centerlineCsv\" to auto-generate them."
            );
        }

        return spec;
    }

    private static void loadCenterlineCsvIntoSpec(TrackSpec spec) throws Exception {
        String text = Global.getSettings().loadText(spec.centerlineCsv);

        // Handle BOM if present
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }

        String[] lines = text.split("\\r?\\n");
        if (lines.length <= 1) throw new RuntimeException("CSV has no data: " + spec.centerlineCsv);

        float cos = (float) Math.cos(Math.toRadians(spec.rotationDeg));
        float sin = (float) Math.sin(Math.toRadians(spec.rotationDeg));

        List<Vector2f> pts = new ArrayList<>();
        List<Float> wl = new ArrayList<>();
        List<Float> wr = new ArrayList<>();

        // Assume header on line 0; start at 1
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;

            String[] parts = line.split(",");
            if (parts.length < 2) continue;

            float x_m = Float.parseFloat(parts[0].trim());
            float y_m = Float.parseFloat(parts[1].trim());

            float wR_m = 0f;
            float wL_m = 0f;
            if (parts.length >= 4) {
                wR_m = Float.parseFloat(parts[2].trim());
                wL_m = Float.parseFloat(parts[3].trim());
            }

            float x = x_m * spec.unitsPerMeter;
            float y = y_m * spec.unitsPerMeter;

            // rotate about origin
            float xr = x * cos - y * sin;
            float yr = x * sin + y * cos;

            pts.add(new Vector2f(xr, yr));

            // widths: meters -> units, then scale up/down
            float wR = wR_m * spec.unitsPerMeter * spec.widthScale;
            float wL = wL_m * spec.unitsPerMeter * spec.widthScale;
            wr.add(wR);
            wl.add(wL);
        }

        if (pts.size() < 2) throw new RuntimeException("CSV produced <2 points: " + spec.centerlineCsv);

        // If last point duplicates first, drop it (prevents a zero-length wrap segment)
        if (pts.size() >= 3) {
            Vector2f first = pts.get(0);
            Vector2f last = pts.get(pts.size() - 1);
            if (dist(first, last) < 0.001f) {
                pts.remove(pts.size() - 1);
                wl.remove(wl.size() - 1);
                wr.remove(wr.size() - 1);
            }
        }

        // Optional auto-center (bbox center to 0,0)
        if (spec.autoCenter) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (Vector2f p : pts) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
            float cx = (minX + maxX) * 0.5f;
            float cy = (minY + maxY) * 0.5f;

            for (Vector2f p : pts) {
                p.x -= cx;
                p.y -= cy;
            }
        }

        // Apply user offset
        if (spec.offsetX != 0f || spec.offsetY != 0f) {
            for (Vector2f p : pts) {
                p.x += spec.offsetX;
                p.y += spec.offsetY;
            }
        }

        spec.centerline.clear();
        spec.wLeft.clear();
        spec.wRight.clear();
        spec.centerline.addAll(pts);
        spec.wLeft.addAll(wl);
        spec.wRight.addAll(wr);
    }

    private static void loadRacelineCsvIntoSpec(TrackSpec spec) throws Exception {
        String text = Global.getSettings().loadText(spec.racelineCsv);

        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }

        String[] lines = text.split("\\r?\\n");
        if (lines.length <= 1) throw new RuntimeException("CSV has no data: " + spec.racelineCsv);

        float cos = (float) Math.cos(Math.toRadians(spec.rotationDeg));
        float sin = (float) Math.sin(Math.toRadians(spec.rotationDeg));

        List<Vector2f> pts = new ArrayList<>();
        List<Float> sList = new ArrayList<>();
        List<Float> psiList = new ArrayList<>();
        List<Float> kappaList = new ArrayList<>();
        List<Float> vxList = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;

            String[] parts = line.split("[,;]");
            if (parts.length < 3) continue;

            float s_m = 0f;
            float psi_rad = 0f;
            float kappa = 0f;
            float vx = 0f;
            if (parts.length >= 1) s_m = Float.parseFloat(parts[0].trim());
            if (parts.length >= 4) psi_rad = Float.parseFloat(parts[3].trim());
            if (parts.length >= 5) kappa = Float.parseFloat(parts[4].trim());
            if (parts.length >= 6) vx = Float.parseFloat(parts[5].trim());

            float x_m = Float.parseFloat(parts[1].trim());
            float y_m = Float.parseFloat(parts[2].trim());

            float x = x_m * spec.unitsPerMeter;
            float y = y_m * spec.unitsPerMeter;

            float xr = x * cos - y * sin;
            float yr = x * sin + y * cos;

            pts.add(new Vector2f(xr, yr));
            sList.add(s_m);
            psiList.add(psi_rad);
            kappaList.add(kappa);
            vxList.add(vx);
        }

        if (pts.size() < 2) throw new RuntimeException("CSV produced <2 points: " + spec.racelineCsv);

        if (spec.autoCenter) {
            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (Vector2f p : pts) {
                if (p.x < minX) minX = p.x;
                if (p.x > maxX) maxX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.y > maxY) maxY = p.y;
            }
            float cx = (minX + maxX) * 0.5f;
            float cy = (minY + maxY) * 0.5f;

            for (Vector2f p : pts) {
                p.x -= cx;
                p.y -= cy;
            }
        }

        if (spec.offsetX != 0f || spec.offsetY != 0f) {
            for (Vector2f p : pts) {
                p.x += spec.offsetX;
                p.y += spec.offsetY;
            }
        }

        spec.raceline.clear();
        spec.raceline.addAll(pts);
        spec.racelineS.clear();
        spec.racelinePsi.clear();
        spec.racelineKappa.clear();
        spec.racelineVx.clear();
        spec.racelineS.addAll(sList);
        spec.racelinePsi.addAll(psiList);
        spec.racelineKappa.addAll(kappaList);
        spec.racelineVx.addAll(vxList);
    }

    private static List<Vector2f> generateCheckpointsFromCenterline(List<Vector2f> line, float spacing) {
        List<Vector2f> cps = new ArrayList<>();
        if (line.size() < 2) return cps;

        float acc = 0f;
        cps.add(new Vector2f(line.get(0)));

        Vector2f prev = new Vector2f(line.get(0));
        for (int i = 1; i < line.size(); i++) {
            Vector2f cur = line.get(i);
            float seg = dist(prev, cur);
            acc += seg;
            if (acc >= spacing) {
                cps.add(new Vector2f(cur));
                acc = 0f;
            }
            prev = cur;
        }

        // Ensure we don't end up with too few checkpoints on small/odd tracks
        if (cps.size() < 6 && line.size() >= 6) {
            int step = Math.max(1, line.size() / 12);
            for (int i = 0; i < line.size(); i += step) {
                cps.add(new Vector2f(line.get(i)));
            }
        }

        // De-dup in case the fallback added repeats
        cps = dedupeNearby(cps, 1f);

        return cps;
    }

    private static List<Vector2f> dedupeNearby(List<Vector2f> pts, float eps) {
        if (pts.size() < 2) return pts;
        float eps2 = eps * eps;
        List<Vector2f> out = new ArrayList<>();
        Vector2f last = null;
        for (Vector2f p : pts) {
            if (last == null) {
                out.add(p);
                last = p;
                continue;
            }
            float dx = p.x - last.x;
            float dy = p.y - last.y;
            if (dx * dx + dy * dy > eps2) {
                out.add(p);
                last = p;
            }
        }
        return out;
    }

    private static float dist(Vector2f a, Vector2f b) {
        float dx = a.x - b.x, dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}


