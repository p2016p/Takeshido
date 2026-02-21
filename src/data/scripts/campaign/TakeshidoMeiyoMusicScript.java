package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TakeshidoMeiyoMusicScript implements EveryFrameScript {
    private static final String SYSTEM_ID = "Meiyo";
    private static final String SYSTEM_NAME = "Meiyo";
    private static final String SYSTEM_NAME_ALT = "Meiyo Star System";
    private static final List<String> TRACK_IDS = Arrays.asList(
            "takeshido_meiyo_courseselect1",
            "takeshido_meiyo_events",
            "takeshido_meiyo_gtmode5"
    );
    private static final float TRACK_SWITCH_GRACE = 1.5f;
    private static final float LOG_INTERVAL = 5f;
    private static final float TRACK_FALLBACK_DURATION = 180f;
    private static final Map<String, Float> TRACK_DURATIONS = new HashMap<>();
    private static final Random RNG = new Random();

    private int index = 0;
    private String currentTrackId = null;
    private float sinceStart = 0f;
    private boolean wasInMeiyo = false;
    private float logTimer = 0f;
    private float currentTrackDuration = -1f;

    static {
        TRACK_DURATIONS.put("takeshido_meiyo_courseselect1", 189f);
        TRACK_DURATIONS.put("takeshido_meiyo_events", 193f);
        TRACK_DURATIONS.put("takeshido_meiyo_gtmode5", 208f);
    }

    public static void ensure() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        if (!sector.hasScript(TakeshidoMeiyoMusicScript.class)) {
            sector.addScript(new TakeshidoMeiyoMusicScript());
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Added Meiyo music script.");
        }
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null) return;
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) {
            logOccasionally(amount, "Player fleet is null, skipping music update.");
            return;
        }

        LocationAPI location = player.getContainingLocation();
        if (location == null) {
            logOccasionally(amount, "Player location is null, skipping music update.");
            return;
        }
        logLocationOccasionally(amount, location);
        boolean inMeiyo = isInMeiyoSystem(location);
        SoundPlayerAPI soundPlayer = Global.getSoundPlayer();

        if (!inMeiyo) {
            if (wasInMeiyo) {
                Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Left Meiyo system. Stopping custom music.");
                stopCustomMusic(soundPlayer);
            }
            wasInMeiyo = false;
            return;
        }

        if (soundPlayer == null) {
            logOccasionally(amount, "Sound player is null, cannot play music.");
            return;
        }

        soundPlayer.setSuspendDefaultMusicPlayback(true);

        if (!wasInMeiyo) {
            String name = location instanceof StarSystemAPI ? ((StarSystemAPI) location).getName() : String.valueOf(location);
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Entered Meiyo system. Location=" + name);
            wasInMeiyo = true;
            currentTrackId = null;
            try {
                soundPlayer.playCustomMusic(0, 0, null, true);
            } catch (Exception e) {
                Global.getLogger(TakeshidoMeiyoMusicScript.class).warn("Failed to clear custom music on entry.", e);
            }
            pickRandomIndex();
            startTrack(soundPlayer);
            return;
        }

        wasInMeiyo = true;
        if (currentTrackId == null) {
            startTrack(soundPlayer);
            return;
        }

        if (getCurrentMusicIdSafe() == null) {
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info("No music playing in Meiyo. Restarting with a random track.");
            currentTrackId = null;
            pickRandomIndex();
            startTrack(soundPlayer);
            return;
        }

        sinceStart += amount;
        if (currentTrackDuration > 0f && sinceStart >= Math.max(1f, currentTrackDuration - TRACK_SWITCH_GRACE)) {
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Track duration reached. Advancing. currentTrack=" + currentTrackId + ", duration=" + currentTrackDuration);
            advanceIndex();
            startTrack(soundPlayer);
        }
    }

    private boolean isInMeiyoSystem(LocationAPI location) {
        if (!(location instanceof StarSystemAPI)) return false;
        StarSystemAPI system = (StarSystemAPI) location;
        String name = system.getName();
        String id = system.getId();
        if (id != null && SYSTEM_ID.equalsIgnoreCase(id)) return true;
        if (name == null) return false;
        if (SYSTEM_NAME.equalsIgnoreCase(name)) return true;
        if (SYSTEM_NAME_ALT.equalsIgnoreCase(name)) return true;
        return name.toLowerCase().startsWith(SYSTEM_NAME.toLowerCase());
    }

    private void advanceIndex() {
        if (TRACK_IDS.isEmpty()) return;
        index = (index + 1) % TRACK_IDS.size();
    }

    private void pickRandomIndex() {
        if (TRACK_IDS.isEmpty()) return;
        index = RNG.nextInt(TRACK_IDS.size());
    }

    private void startTrack(SoundPlayerAPI soundPlayer) {
        if (TRACK_IDS.isEmpty()) return;
        currentTrackId = TRACK_IDS.get(index);
        sinceStart = 0f;
        currentTrackDuration = getTrackDuration(currentTrackId);
        try {
            soundPlayer.playCustomMusic(0, 0, currentTrackId, false);
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Starting Meiyo track: " + currentTrackId + " (duration=" + currentTrackDuration + "s)");
        } catch (Exception e) {
            Global.getLogger(TakeshidoMeiyoMusicScript.class).warn("Failed to play music track " + currentTrackId, e);
        }
    }

    private void stopCustomMusic(SoundPlayerAPI soundPlayer) {
        currentTrackId = null;
        sinceStart = 0f;
        currentTrackDuration = -1f;
        if (soundPlayer == null) return;
        try {
            soundPlayer.playCustomMusic(0, 0, null, true);
        } catch (Exception e) {
            Global.getLogger(TakeshidoMeiyoMusicScript.class).warn("Failed to stop custom music", e);
        }
        soundPlayer.setSuspendDefaultMusicPlayback(false);
    }

    private void logOccasionally(float amount, String message) {
        logTimer += amount;
        if (logTimer >= LOG_INTERVAL) {
            logTimer = 0f;
            Global.getLogger(TakeshidoMeiyoMusicScript.class).info(message);
        }
    }

    private void logLocationOccasionally(float amount, LocationAPI location) {
        logTimer += amount;
        if (logTimer < LOG_INTERVAL) return;
        logTimer = 0f;

        String locClass = location.getClass().getSimpleName();
        String locName = location instanceof StarSystemAPI ? ((StarSystemAPI) location).getName() : String.valueOf(location);
        String locId = location instanceof StarSystemAPI ? ((StarSystemAPI) location).getId() : "n/a";
        String musicId = getCurrentMusicIdSafe();
        if (musicId == null) musicId = "null";
        Global.getLogger(TakeshidoMeiyoMusicScript.class).info("Meiyo music tick. location=" + locName + " (" + locClass + "), id=" + locId + ", inMeiyo=" + isInMeiyoSystem(location) + ", currentTrack=" + currentTrackId + ", playing=" + musicId + ", duration=" + currentTrackDuration + ", t=" + sinceStart);
    }

    private String getCurrentMusicIdSafe() {
        SoundPlayerAPI sp = Global.getSoundPlayer();
        if (sp == null) return null;
        String id = sp.getCurrentMusicId();
        if (id == null) return null;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return null;
        if ("nothing".equalsIgnoreCase(trimmed)) return null;
        return id;
    }

    private float getTrackDuration(String trackId) {
        if (trackId == null) return TRACK_FALLBACK_DURATION;
        Float duration = TRACK_DURATIONS.get(trackId);
        return duration != null ? duration : TRACK_FALLBACK_DURATION;
    }
}
