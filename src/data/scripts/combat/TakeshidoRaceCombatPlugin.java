package data.scripts.combat;

import java.awt.Color;
import java.util.*;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lwjgl.opengl.GL11;

import data.scripts.ai.TakeshidoRaceShipAI;
import data.scripts.racing.TakeshidoRacingManager;

public class TakeshidoRaceCombatPlugin extends BaseEveryFrameCombatPlugin {

    private static org.apache.log4j.Logger log = Global.getLogger(TakeshidoRaceCombatPlugin.class);

    private final String raceHullmodId;
    private final int lapsToWin;
    private final int expectedRacers;
    private final boolean isDeathRace;
    private final String trackId;
    private final Map<String, Float> skillOverrides;
    private final String raceId;
    private boolean spectatorOnly = false;
    private boolean contextLoaded = false;
    private String betRacerId = null;
    private String spectatorMemberId = null;
    private ShipAPI betShip = null;
    private ShipAPI spectatorShip = null;
    private boolean spectatingApplied = false;
    private TakeshidoRaceShipAI spectatorAI;
    private boolean spectatorInitialized = false;

    // to figure out what checkpoint we start at
    public int startIndex = 0;

    // Wall asteroid tuning
    private float wallAsteroidTrackBuffer = 40f;


    // Starting grid tuning
    private int gridColumns = 4;              // keep 4
    private float rowSpacing = 320f;          // constant is fine
    private float gridEdgeMargin = 80f;       // safety gap from the walls
    private float aheadOffset = 900f;


    private static final String OFFTRACK_MOD_ID = "takeshido_offtrack";
    private static final String START_LOCK_ID = "takeshido_start_lock";
    private static final String SPECTATOR_LOCK_ID = "takeshido_spectator_lock";
    private static final float WALL_PUSH_PER_SEC = 260f;
    private static final float WALL_PUSH_MULT = 1.0f;
    private static final float WALL_LATERAL_DAMP = 0.7f;
    private static final float CAR_REPEL_BUFFER = 8f;
    private static final float CAR_REPEL_PER_SEC = 120f;
    private static final float MAX_RACE_SECONDS = 600f;
    private static final float RACE_BACKGROUND_UNLOAD_DELAY_AFTER_END = 1.25f;
    private static final String RACE_MUSIC_FORCE_KEY = "takeshido_race_music_id";
    private static final String DEFAULT_SCREEN_LOCKED_BACKGROUND = "graphics/backgrounds/deserttrack.png";
    private static final float DEFAULT_SCREEN_LOCKED_BACKGROUND_ALPHA = 1f;
    private static final float TRACK_LOCKED_BACKGROUND_PADDING = 2400f;
    private static final float TRACK_LOCKED_MAP_PADDING = 800f;
    private static final float DEFAULT_TRACK_LOCKED_TILE_WORLD_WIDTH = 1024f;
    private static final boolean ENABLE_BACKGROUND_DUAL_UV_BLEND = true;
    private static final String DEFAULT_TRACK_RIBBON_TEXTURE = "graphics/tracks/asphalt.png";
    private static final float DEFAULT_TRACK_RIBBON_TILE_WORLD_LENGTH = 128f;
    private static final float DEFAULT_TRACK_RIBBON_WIDTH_REPEAT_SCALE = 1f;
    private static final float DEFAULT_TRACK_RIBBON_ALPHA = 1f;
    private static final float DEFAULT_TRACK_RIBBON_FEATHER_WIDTH = 8f;
    private static final float DEFAULT_TRACK_SHOULDER_WIDTH = 15f;
    private static final String DEFAULT_TRACK_SHOULDER_TEXTURE = DEFAULT_SCREEN_LOCKED_BACKGROUND;
    private static final float DEFAULT_TRACK_SHOULDER_TILE_WORLD_LENGTH = 256f;
    private static final float DEFAULT_TRACK_SHOULDER_WIDTH_REPEAT_SCALE = 1f;
    private static final float DEFAULT_TRACK_SHOULDER_ALPHA_INNER = 0.85f;
    private static final float DEFAULT_TRACK_SHOULDER_ALPHA_OUTER = 0.25f;
    private static final boolean ENABLE_BACKGROUND_MACRO_NOISE = true;
    private static final String DEFAULT_BACKGROUND_MACRO_NOISE_TEXTURE = "graphics/backgrounds/macro_noise.png";
    private static final List<String> RACE_MUSIC_IDS = Arrays.asList(
            "takeshido_race_kissyougoodbye",
            "takeshido_race_driveyoucrazy",
            "takeshido_race_freedomtowin",
            "takeshido_race_getcloser",
            "takeshido_race_greenmonster"
    );

    // How harsh the penalty is
    private float offTrackMaxSpeedMult = 0.20f;  // 20% top speed when off track
    private float offTrackAccelMult    = 0.35f;  // optional, helps them actually slow down



    private CombatEngineAPI engine;
    private boolean setupComplete = false;

    private RaceState race;

    // race timing/state
    private boolean countdownActive = false;
    private float countdownTimer = 0f;
    private float countdownDelayTimer = 0f;
    private int lastCountdownSecond = -1;
    private boolean countdownSoundPlayed = false;
    private boolean enginesSuppressed = false;
    private boolean raceMusicStarted = false;
    private boolean raceFinishPlayed = false;
    private boolean postCountdownAIEnsured = false;
    private boolean resultsStored = false;
    private float raceTimer = 0f;
    private String currentRaceMusicId = null;
    private final Random raceMusicRandom = new Random();
    private float setupWaitTimer = 0f;
    private boolean combatEndTriggered = false;
    private float combatEndTimer = 0f;
    private boolean raceBackgroundInitialized = false;
    private boolean raceBackgroundLoaded = false;
    private boolean raceBackgroundReleased = false;
    private boolean raceBackgroundDisabledStarfield = false;
    private boolean originalRenderStarfield = true;
    private String raceBackgroundSpritePath = null;
    private ScreenLockedBackgroundRenderer raceBackgroundRenderer = null;
    private ScreenLockedBackgroundRenderer raceBackgroundSecondaryRenderer = null;
    private BackgroundMacroNoiseRenderer raceBackgroundNoiseRenderer = null;
    private boolean raceBackgroundNoiseLoaded = false;
    private String raceBackgroundNoiseTexturePath = null;
    private Vector2f raceBackgroundCenter = new Vector2f(0f, 0f);
    private float raceBackgroundWidth = 12000f;
    private float raceBackgroundHeight = 12000f;
    private float raceBackgroundTileWorldWidth = DEFAULT_TRACK_LOCKED_TILE_WORLD_WIDTH;
    private String trackShoulderTexturePath = DEFAULT_TRACK_SHOULDER_TEXTURE;
    private boolean trackRibbonInitialized = false;
    private boolean trackRibbonLoaded = false;
    private final List<TrackBandRenderer> trackRibbonRenderers = new ArrayList<>();
    private final Set<String> trackRibbonTexturePaths = new LinkedHashSet<>();
    private boolean trackDecalsInitialized = false;
    private boolean trackDecalsLoaded = false;
    private final List<TrackDecalRenderer> trackDecalRenderers = new ArrayList<>();
    private final Set<String> trackDecalTexturePaths = new LinkedHashSet<>();

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = null;
        this.raceId = null;
    }

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId,
                                     Map<String, Float> skillOverrides) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = skillOverrides;
        this.raceId = null;
    }

    public TakeshidoRaceCombatPlugin(String raceHullmodId, int lapsToWin, int expectedRacers, boolean isDeathRace, String trackId,
                                     Map<String, Float> skillOverrides, String raceId) {
        this.raceHullmodId = raceHullmodId;
        this.lapsToWin = Math.max(1, lapsToWin);
        this.expectedRacers = Math.max(1, expectedRacers);
        this.isDeathRace = isDeathRace;
        this.trackId = trackId;
        this.skillOverrides = skillOverrides;
        this.raceId = raceId;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.setDoNotEndCombat(true);
        this.race = new RaceState(lapsToWin);
        this.race.isDeathRace = isDeathRace;
        engine.getCustomData().put("takeshido_race_state", race);
        syncRaceContext();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) return;
        if (engine.isCombatOver()) {
            releaseRaceBackgroundResources();
            return;
        }
        if (combatEndTriggered && !raceBackgroundReleased) {
            combatEndTimer += Math.max(0f, amount);
            if (combatEndTimer >= RACE_BACKGROUND_UNLOAD_DELAY_AFTER_END) {
                releaseRaceBackgroundResources();
            }
        }
        if (engine.isPaused()) return;

        applySpectatorLock();

        // Keep attempting setup until all ships are actually present
        if (!setupComplete) {
            setupWaitTimer += amount;
            setupComplete = trySetup();
            if (!setupComplete) return; // don't run race logic until setup is real
        }

        ensurePlayerAutopilotRaceAI();

        if (!isDeathRace) {
            // USE_SYSTEM is a ShipCommand; blocking it stops both AI and player from activating the ship system.
            for (ShipAPI s : race.racers.keySet()) {
                if (s == null || s.isHulk()) continue;
                s.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                s.blockCommandForOneFrame(ShipCommand.FIRE);
            }
        }

        if (countdownActive) {
            updateCountdown(amount);
            return;
        }

        ensureSpectatingTarget();
        if (spectatorOnly && spectatorAI != null) {
            if (engine.getCombatUI() == null || !engine.getCombatUI().isAutopilotOn()) {
                spectatorAI.advance(amount);
            }
        }

        updateRaceProgress();

        raceTimer += amount;
        if (!race.finished && raceTimer >= MAX_RACE_SECONDS) {
            endRaceByTimeout();
        }

        applyOffTrackPenalty();
        applySoftWall(amount);
        // applySoftCarRepulsion(amount); // disabled: allow normal ship-ship collisions

        ShipAPI player = engine.getPlayerShip();
        if (player != null) {
            RacerState ps = race.racers.get(player);
            if (ps != null) {
                String msg = "Lap " + (ps.lap + 1) + "/" + race.lapsToWin +
                        "   CP " + (ps.nextCheckpoint + 1) + "/" + race.checkpoints.size();
                engine.maintainStatusForPlayerShip(this, null, "Race", msg, false);
            }
        }

        if (race.finished) {
            if (!combatEndTriggered) {
                finalizeFinishOrder();
                storeResultsIfNeeded();
                playRaceFinish();
                combatEndTriggered = true;
                combatEndTimer = 0f;
                engine.endCombat(1f, race.winnerSide);
            }
        } else if (raceMusicStarted && !raceFinishPlayed) {
            ensureRaceMusicLoop();
        }
    }

    private boolean trySetup() {
        ShipAPI player = engine.getPlayerShip();
        if (player == null) return false;
        syncRaceContext();

        List<ShipAPI> cars = new ArrayList<>();
        for (ShipAPI s : engine.getShips()) {
            if (isRacer(s)) cars.add(s);
        }

        int needed = expectedRacers;
        if (setupWaitTimer > 2f) {
            needed = Math.min(expectedRacers, Math.max(2, cars.size()));
        }
        // Wait until all racers exist before doing one-time placement/AI swap
        if (cars.size() < needed) return false;

        TrackSpec track = data.scripts.combat.TrackLoader.get(trackId);
        setupRaceBackground(track);

        this.wallAsteroidTrackBuffer = track.wallAsteroidTrackBuffer;

        race.checkpoints = track.checkpoints;
        race.checkpointRadius = track.checkpointRadius;

        if (track.centerline != null && !track.centerline.isEmpty()) {
            race.centerline = track.centerline;
            race.wLeft = track.wLeft;
            race.wRight = track.wRight;
            race.useWidths = track.useCsvWidths && track.wLeft.size() == track.centerline.size() && track.wRight.size() == track.centerline.size();
        } else {
            // fallback: treat checkpoints as centerline
            race.centerline = race.checkpoints;
            race.useWidths = false;
        }
        if (track.raceline != null) {
            race.raceline = new ArrayList<Vector2f>(track.raceline);
            race.racelineS = new ArrayList<Float>(track.racelineS);
            race.racelinePsi = new ArrayList<Float>(track.racelinePsi);
            race.racelineKappa = new ArrayList<Float>(track.racelineKappa);
            race.racelineVx = new ArrayList<Float>(track.racelineVx);
        } else {
            race.raceline = new ArrayList<Vector2f>();
            race.racelineS = new ArrayList<Float>();
            race.racelinePsi = new ArrayList<Float>();
            race.racelineKappa = new ArrayList<Float>();
            race.racelineVx = new ArrayList<Float>();
        }
        race.unitsPerMeter = track.unitsPerMeter;

// If you're not using CSV widths, keep your existing corridor definition:
        race.corridorHalfWidth = race.checkpointRadius;


// If you want per-mission laps to override JSON, keep lapsToWin as-is.
// If you want the track to define laps when mission passes 0, do:
// if (this.lapsToWin <= 0 && track.lapsToWin != null) race.lapsToWin = track.lapsToWin;

        race.startIndex = ((track.startIndex % race.checkpoints.size()) + race.checkpoints.size()) % race.checkpoints.size();


        // Grid tuning from track
        this.gridColumns = track.grid.columns;
        this.rowSpacing = track.grid.rowSpacing;
        this.gridEdgeMargin = track.grid.edgeMargin;
        this.aheadOffset = track.grid.aheadOffset;

        race.gates = buildGates();
        setupTrackDecals(track);
        setupTrackRibbon();



        // Place everyone on the same starting grid near the track edge
        placeStartingGrid(cars);

        if (!isRacer(player)) {
            placeSpectatorShip(player);
        }


        // Stop them from shooting if its not a deathrace
        if (!isDeathRace) {
            applyNonDeathRaceLockout(cars);
        }

        // Initialize racer state + AI for non-player racers
        race.racers.clear();
        for (ShipAPI s : cars) {
            RacerState rs = new RacerState();
            rs.skill = getSkillForShip(s);
            race.racers.put(s, rs);

            int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
            rs.nextCheckpoint = firstTarget;
            rs.lastPos = new Vector2f(s.getLocation());

            if (s != player) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            } else if (!spectatorOnly && s.getShipAI() != null) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            }
        }


        startCountdown();
        return true;
    }

    private void setupRaceBackground(TrackSpec track) {
        if (engine == null || raceBackgroundInitialized) return;
        raceBackgroundInitialized = true;

        String spritePath = track != null ? track.screenLockedBackground : null;
        if (spritePath == null || spritePath.trim().isEmpty()) {
            spritePath = DEFAULT_SCREEN_LOCKED_BACKGROUND;
        }
        if (spritePath == null || spritePath.trim().isEmpty()) return;

        trackShoulderTexturePath = spritePath;

        float alpha = track != null ? track.screenLockedBackgroundAlpha : DEFAULT_SCREEN_LOCKED_BACKGROUND_ALPHA;
        alpha = clamp(alpha, 0f, 1f);
        computeTrackLockedBackgroundBounds(track);
        raceBackgroundTileWorldWidth = track != null
                ? Math.max(128f, track.screenLockedBackgroundTileWorldWidth)
                : DEFAULT_TRACK_LOCKED_TILE_WORLD_WIDTH;

        raceBackgroundSpritePath = spritePath;
        SpriteAPI sprite = null;
        try {
            Global.getSettings().loadTexture(raceBackgroundSpritePath);
            sprite = Global.getSettings().getSprite(raceBackgroundSpritePath);
            if (sprite == null) {
                log.warn("Race background sprite not found after load: " + raceBackgroundSpritePath);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to load race background texture: " + raceBackgroundSpritePath, ex);
            return;
        }

        try {

            raceBackgroundRenderer = new ScreenLockedBackgroundRenderer(
                    sprite,
                    alpha,
                    raceBackgroundCenter,
                    raceBackgroundWidth,
                    raceBackgroundHeight,
                    raceBackgroundTileWorldWidth
            );
            engine.addLayeredRenderingPlugin(raceBackgroundRenderer);
            raceBackgroundLoaded = true;
            setupSecondaryBackgroundBlend(track, sprite);
            setupRaceMacroNoise(track);

            boolean disableStarfield = track == null || track.screenLockedBackgroundDisableStarfield;
            if (disableStarfield) {
                originalRenderStarfield = engine.isRenderStarfield();
                raceBackgroundDisabledStarfield = true;
                engine.setRenderStarfield(false);
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize race background: " + raceBackgroundSpritePath, ex);
        }
    }

    private void setupRaceMacroNoise(TrackSpec track) {
        if (!ENABLE_BACKGROUND_MACRO_NOISE || engine == null || raceBackgroundNoiseRenderer != null) return;

        raceBackgroundNoiseTexturePath = DEFAULT_BACKGROUND_MACRO_NOISE_TEXTURE;
        SpriteAPI noiseSprite;
        try {
            Global.getSettings().loadTexture(raceBackgroundNoiseTexturePath);
            noiseSprite = Global.getSettings().getSprite(raceBackgroundNoiseTexturePath);
            if (noiseSprite == null) {
                log.warn("Macro noise sprite not found: " + raceBackgroundNoiseTexturePath);
                return;
            }
        } catch (Exception ex) {
            log.warn("Failed to load macro noise texture: " + raceBackgroundNoiseTexturePath, ex);
            return;
        }

        String seedSource = track != null && track.id != null ? track.id : trackId;
        long seed = seedSource != null ? seedSource.hashCode() : 0L;
        List<BackgroundMacroNoiseRenderer.OctaveSpec> octaves = buildMacroNoiseOctaves(seed);
        if (octaves.isEmpty()) return;

        raceBackgroundNoiseRenderer = new BackgroundMacroNoiseRenderer(
                noiseSprite,
                raceBackgroundCenter,
                raceBackgroundWidth,
                raceBackgroundHeight,
                octaves
        );
        engine.addLayeredRenderingPlugin(raceBackgroundNoiseRenderer);
        raceBackgroundNoiseLoaded = true;
    }

    private void setupSecondaryBackgroundBlend(TrackSpec track, SpriteAPI sprite) {
        if (!ENABLE_BACKGROUND_DUAL_UV_BLEND || engine == null || sprite == null || raceBackgroundSecondaryRenderer != null) return;

        String seedSource = track != null && track.id != null ? track.id : trackId;
        long seed = seedSource != null ? seedSource.hashCode() : 0L;
        Random rand = new Random(seed * 1103515245L + 12345L);

        float tileScale = 1.9f + rand.nextFloat() * 0.7f; // 1.9x .. 2.6x base tile scale
        float secondaryTileWorldWidth = Math.max(128f, raceBackgroundTileWorldWidth * tileScale);

        float offsetMag = secondaryTileWorldWidth * 0.45f;
        float offsetX = (rand.nextFloat() * 2f - 1f) * offsetMag;
        float offsetY = (rand.nextFloat() * 2f - 1f) * offsetMag;
        float alpha = 0.08f + rand.nextFloat() * 0.03f;

        Vector2f secondaryCenter = new Vector2f(raceBackgroundCenter.x + offsetX, raceBackgroundCenter.y + offsetY);
        raceBackgroundSecondaryRenderer = new ScreenLockedBackgroundRenderer(
                sprite,
                alpha,
                secondaryCenter,
                raceBackgroundWidth,
                raceBackgroundHeight,
                secondaryTileWorldWidth
        );
        engine.addLayeredRenderingPlugin(raceBackgroundSecondaryRenderer);
    }

    private List<BackgroundMacroNoiseRenderer.OctaveSpec> buildMacroNoiseOctaves(long seed) {
        List<BackgroundMacroNoiseRenderer.OctaveSpec> octaves = new ArrayList<BackgroundMacroNoiseRenderer.OctaveSpec>();
        Random rand = new Random(seed * 31L + 0x9E3779B97F4A7C15L);

        float base = Math.max(256f, raceBackgroundTileWorldWidth);
        float[] tileScales = new float[] {2.2f, 1.3f, 0.8f};
        float[] darkAlphas = new float[] {0.05f, 0.03f, 0.02f};
        float[] lightAlphas = new float[] {0.14f, 0.09f, 0.06f};

        for (int i = 0; i < tileScales.length; i++) {
            float tileWidth = base * tileScales[i];
            float offsetX = (rand.nextFloat() * 2f - 1f) * tileWidth;
            float offsetY = (rand.nextFloat() * 2f - 1f) * tileWidth;
            float rotation = rand.nextFloat() * 360f;
            octaves.add(new BackgroundMacroNoiseRenderer.OctaveSpec(
                    tileWidth,
                    darkAlphas[i],
                    lightAlphas[i],
                    offsetX,
                    offsetY,
                    rotation
            ));
        }
        return octaves;
    }

    private void setupTrackDecals(TrackSpec track) {
        if (engine == null || trackDecalsInitialized) return;
        trackDecalsInitialized = true;
        if (track == null || track.decals == null || track.decals.isEmpty()) return;

        List<TrackDecalRenderer.DecalInstance> instances = new ArrayList<TrackDecalRenderer.DecalInstance>();
        for (TrackSpec.DecalSpec decal : track.decals) {
            if (decal == null || decal.sprite == null || decal.sprite.trim().isEmpty()) continue;

            SpriteAPI sprite = loadTrackDecalSprite(decal.sprite);
            if (sprite == null) continue;

            float width = Math.max(16f, decal.width);
            float height = Math.max(16f, decal.height);
            float alpha = clamp(decal.alpha, 0f, 1f);
            instances.add(new TrackDecalRenderer.DecalInstance(
                    sprite,
                    new Vector2f(decal.x, decal.y),
                    width,
                    height,
                    decal.angle,
                    alpha
            ));
        }

        if (instances.isEmpty()) return;

        TrackDecalRenderer renderer = new TrackDecalRenderer(instances);
        trackDecalRenderers.add(renderer);
        engine.addLayeredRenderingPlugin(renderer);
        trackDecalsLoaded = true;
    }

    private void setupTrackRibbon() {
        if (engine == null || trackRibbonInitialized) return;
        trackRibbonInitialized = true;
        if (race == null || race.centerline == null || race.centerline.size() < 2) return;

        List<Vector2f> leftBoundary = buildTrackRibbonEdgePolyline(true, 0f);
        List<Vector2f> rightBoundary = buildTrackRibbonEdgePolyline(false, 0f);
        List<Vector2f> leftInner = buildTrackRibbonEdgePolyline(true, DEFAULT_TRACK_RIBBON_FEATHER_WIDTH);
        List<Vector2f> rightInner = buildTrackRibbonEdgePolyline(false, DEFAULT_TRACK_RIBBON_FEATHER_WIDTH);
        List<Vector2f> leftShoulderOuter = buildTrackRibbonEdgePolyline(true, -DEFAULT_TRACK_SHOULDER_WIDTH);
        List<Vector2f> rightShoulderOuter = buildTrackRibbonEdgePolyline(false, -DEFAULT_TRACK_SHOULDER_WIDTH);

        if (!edgesCompatible(leftBoundary, rightBoundary, leftInner, rightInner, leftShoulderOuter, rightShoulderOuter)) {
            log.warn("Track ribbon geometry invalid for track " + trackId +
                    " (leftBoundary=" + leftBoundary.size() +
                    ", rightBoundary=" + rightBoundary.size() + ")");
            return;
        }

        String shoulderTexturePath = trackShoulderTexturePath;
        if (shoulderTexturePath == null || shoulderTexturePath.trim().isEmpty()) {
            shoulderTexturePath = DEFAULT_TRACK_SHOULDER_TEXTURE;
        }

        SpriteAPI shoulderSprite = loadTrackRibbonSprite(shoulderTexturePath);
        if (shoulderSprite == null && !DEFAULT_TRACK_SHOULDER_TEXTURE.equals(shoulderTexturePath)) {
            log.warn("Failed to load track shoulder texture '" + shoulderTexturePath + "' for track " + trackId +
                    "; falling back to default shoulder texture.");
            shoulderSprite = loadTrackRibbonSprite(DEFAULT_TRACK_SHOULDER_TEXTURE);
        }
        if (shoulderSprite != null) {
            addTrackRibbonBand(
                    shoulderSprite,
                    leftShoulderOuter,
                    leftBoundary,
                    DEFAULT_TRACK_SHOULDER_TILE_WORLD_LENGTH,
                    DEFAULT_TRACK_SHOULDER_WIDTH_REPEAT_SCALE,
                    DEFAULT_TRACK_SHOULDER_ALPHA_OUTER,
                    DEFAULT_TRACK_SHOULDER_ALPHA_INNER,
                    1f
            );
            addTrackRibbonBand(
                    shoulderSprite,
                    rightBoundary,
                    rightShoulderOuter,
                    DEFAULT_TRACK_SHOULDER_TILE_WORLD_LENGTH,
                    DEFAULT_TRACK_SHOULDER_WIDTH_REPEAT_SCALE,
                    DEFAULT_TRACK_SHOULDER_ALPHA_INNER,
                    DEFAULT_TRACK_SHOULDER_ALPHA_OUTER,
                    1f
            );
        }

        SpriteAPI asphaltSprite = loadTrackRibbonSprite(DEFAULT_TRACK_RIBBON_TEXTURE);
        if (asphaltSprite == null) {
            log.warn("Track ribbon asphalt texture missing; shoulders only for track " + trackId);
            trackRibbonLoaded = !trackRibbonRenderers.isEmpty();
            return;
        }

        addTrackRibbonBand(
                asphaltSprite,
                leftBoundary,
                leftInner,
                DEFAULT_TRACK_RIBBON_TILE_WORLD_LENGTH,
                Math.max(0.25f, DEFAULT_TRACK_RIBBON_WIDTH_REPEAT_SCALE * 0.5f),
                0f,
                1f,
                DEFAULT_TRACK_RIBBON_ALPHA
        );
        addTrackRibbonBand(
                asphaltSprite,
                leftInner,
                rightInner,
                DEFAULT_TRACK_RIBBON_TILE_WORLD_LENGTH,
                DEFAULT_TRACK_RIBBON_WIDTH_REPEAT_SCALE,
                1f,
                1f,
                DEFAULT_TRACK_RIBBON_ALPHA
        );
        addTrackRibbonBand(
                asphaltSprite,
                rightInner,
                rightBoundary,
                DEFAULT_TRACK_RIBBON_TILE_WORLD_LENGTH,
                Math.max(0.25f, DEFAULT_TRACK_RIBBON_WIDTH_REPEAT_SCALE * 0.5f),
                1f,
                0f,
                DEFAULT_TRACK_RIBBON_ALPHA
        );

        trackRibbonLoaded = !trackRibbonRenderers.isEmpty();
    }

    private SpriteAPI loadTrackDecalSprite(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        try {
            Global.getSettings().loadTexture(path);
            SpriteAPI sprite = Global.getSettings().getSprite(path);
            if (sprite != null) {
                trackDecalTexturePaths.add(path);
                return sprite;
            }
            log.warn("Track decal sprite not found: " + path);
        } catch (Exception ex) {
            log.warn("Failed to load track decal texture: " + path, ex);
        }
        return null;
    }

    private SpriteAPI loadTrackRibbonSprite(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        try {
            Global.getSettings().loadTexture(path);
            SpriteAPI sprite = Global.getSettings().getSprite(path);
            if (sprite != null) {
                trackRibbonTexturePaths.add(path);
                return sprite;
            }
            log.warn("Track ribbon sprite not found: " + path);
        } catch (Exception ex) {
            log.warn("Failed to load track ribbon texture: " + path, ex);
        }
        return null;
    }

    private void addTrackRibbonBand(SpriteAPI sprite,
                                    List<Vector2f> edgeA,
                                    List<Vector2f> edgeB,
                                    float tileWorldLength,
                                    float widthRepeats,
                                    float edgeAlphaA,
                                    float edgeAlphaB,
                                    float alphaMult) {
        TrackBandRenderer renderer = new TrackBandRenderer(
                sprite,
                edgeA,
                edgeB,
                tileWorldLength,
                widthRepeats,
                edgeAlphaA,
                edgeAlphaB,
                alphaMult
        );
        trackRibbonRenderers.add(renderer);
        engine.addLayeredRenderingPlugin(renderer);
    }

    private boolean edgesCompatible(List<Vector2f>... edges) {
        if (edges == null || edges.length == 0) return false;
        int expected = -1;
        for (List<Vector2f> edge : edges) {
            if (edge == null || edge.size() < 2) return false;
            if (expected < 0) {
                expected = edge.size();
            } else if (edge.size() != expected) {
                return false;
            }
        }
        return true;
    }

    private void releaseRaceBackgroundResources() {
        if (raceBackgroundReleased) return;
        raceBackgroundReleased = true;

        if (raceBackgroundNoiseRenderer != null) {
            raceBackgroundNoiseRenderer.expire();
            raceBackgroundNoiseRenderer = null;
        }
        if (raceBackgroundNoiseLoaded && raceBackgroundNoiseTexturePath != null) {
            try {
                Global.getSettings().unloadTexture(raceBackgroundNoiseTexturePath);
            } catch (Exception ex) {
                log.warn("Failed to unload macro noise texture: " + raceBackgroundNoiseTexturePath, ex);
            }
        }
        raceBackgroundNoiseLoaded = false;
        raceBackgroundNoiseTexturePath = null;

        for (TrackDecalRenderer renderer : trackDecalRenderers) {
            if (renderer != null) renderer.expire();
        }
        trackDecalRenderers.clear();

        for (String path : trackDecalTexturePaths) {
            try {
                Global.getSettings().unloadTexture(path);
            } catch (Exception ex) {
                log.warn("Failed to unload track decal texture: " + path, ex);
            }
        }
        trackDecalTexturePaths.clear();
        trackDecalsLoaded = false;

        for (TrackBandRenderer renderer : trackRibbonRenderers) {
            if (renderer != null) renderer.expire();
        }
        trackRibbonRenderers.clear();

        for (String path : trackRibbonTexturePaths) {
            try {
                Global.getSettings().unloadTexture(path);
            } catch (Exception ex) {
                log.warn("Failed to unload track ribbon texture: " + path, ex);
            }
        }
        trackRibbonTexturePaths.clear();
        trackRibbonLoaded = false;

        if (raceBackgroundRenderer != null) {
            raceBackgroundRenderer.expire();
            raceBackgroundRenderer = null;
        }
        if (raceBackgroundSecondaryRenderer != null) {
            raceBackgroundSecondaryRenderer.expire();
            raceBackgroundSecondaryRenderer = null;
        }
        if (raceBackgroundDisabledStarfield && engine != null) {
            engine.setRenderStarfield(originalRenderStarfield);
            raceBackgroundDisabledStarfield = false;
        }
        if (raceBackgroundLoaded && raceBackgroundSpritePath != null) {
            try {
                Global.getSettings().unloadTexture(raceBackgroundSpritePath);
            } catch (Exception ex) {
                log.warn("Failed to unload race background texture: " + raceBackgroundSpritePath, ex);
            }
        }
        raceBackgroundLoaded = false;
    }

    private void computeTrackLockedBackgroundBounds(TrackSpec track) {
        if (track == null) return;

        List<Vector2f> points = track.centerline != null && !track.centerline.isEmpty()
                ? track.centerline
                : track.checkpoints;
        if (points == null || points.isEmpty()) return;

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        boolean hasPerPointWidths =
                track.useCsvWidths &&
                track.centerline != null &&
                track.wLeft != null &&
                track.wRight != null &&
                track.wLeft.size() == track.centerline.size() &&
                track.wRight.size() == track.centerline.size() &&
                points == track.centerline;

        float edgeMargin = track.grid != null ? track.grid.edgeMargin : 80f;
        float fallbackRadius = track.checkpointRadius + track.wallAsteroidTrackBuffer + edgeMargin;
        if (fallbackRadius < 200f) fallbackRadius = 200f;

        for (int i = 0; i < points.size(); i++) {
            Vector2f p = points.get(i);
            if (p == null) continue;

            float radius = fallbackRadius;
            if (hasPerPointWidths) {
                float left = track.wLeft.get(i);
                float right = track.wRight.get(i);
                radius = Math.max(left, right) + track.wallAsteroidTrackBuffer + edgeMargin;
                if (radius < 200f) radius = 200f;
            }
            radius += TRACK_LOCKED_BACKGROUND_PADDING;

            minX = Math.min(minX, p.x - radius);
            minY = Math.min(minY, p.y - radius);
            maxX = Math.max(maxX, p.x + radius);
            maxY = Math.max(maxY, p.y + radius);
        }

        // Ensure the tiled background also covers the combat map extents.
        if (engine != null) {
            float halfMapWidth = Math.max(0f, engine.getMapWidth() * 0.5f) + TRACK_LOCKED_MAP_PADDING;
            float halfMapHeight = Math.max(0f, engine.getMapHeight() * 0.5f) + TRACK_LOCKED_MAP_PADDING;
            minX = Math.min(minX, -halfMapWidth);
            maxX = Math.max(maxX, halfMapWidth);
            minY = Math.min(minY, -halfMapHeight);
            maxY = Math.max(maxY, halfMapHeight);
        }

        if (minX >= maxX || minY >= maxY) return;

        raceBackgroundCenter = new Vector2f((minX + maxX) * 0.5f, (minY + maxY) * 0.5f);
        raceBackgroundWidth = Math.max(2000f, maxX - minX);
        raceBackgroundHeight = Math.max(2000f, maxY - minY);
    }


    private List<GateSpec> buildGates() {
        List<GateSpec> gates = new ArrayList<>();
        if (race.centerline == null || race.centerline.size() < 2 || race.checkpoints == null) return gates;

        for (Vector2f cp : race.checkpoints) {
            ClosestPoint closest = findClosestPointOnCenterline(cp);
            if (closest == null) continue;

            Vector2f a = race.centerline.get(closest.segIndex);
            Vector2f b = race.centerline.get((closest.segIndex + 1) % race.centerline.size());

            Vector2f tangent = unit(b.x - a.x, b.y - a.y);
            Vector2f normal = new Vector2f(-tangent.y, tangent.x);

            float wl = getWallLeftWidth(closest.segIndex, closest.t);
            float wr = getWallRightWidth(closest.segIndex, closest.t);

            gates.add(new GateSpec(new Vector2f(closest.point), tangent, normal, wl, wr));
        }

        return gates;
    }

    private float getLeftWidth(int segIndex, float tSeg) {
        if (!race.useWidths || race.wLeft.size() < race.centerline.size()) return race.corridorHalfWidth;
        int j = (segIndex + 1) % race.centerline.size();
        float wlA = race.wLeft.get(segIndex);
        float wlB = race.wLeft.get(j);
        return (wlA + (wlB - wlA) * tSeg);
    }

    private float getRightWidth(int segIndex, float tSeg) {
        if (!race.useWidths || race.wRight.size() < race.centerline.size()) return race.corridorHalfWidth;
        int j = (segIndex + 1) % race.centerline.size();
        float wrA = race.wRight.get(segIndex);
        float wrB = race.wRight.get(j);
        return (wrA + (wrB - wrA) * tSeg);
    }

    private float getWallLeftWidth(int segIndex, float tSeg) {
        return getLeftWidth(segIndex, tSeg) + wallAsteroidTrackBuffer;
    }

    private float getWallRightWidth(int segIndex, float tSeg) {
        return getRightWidth(segIndex, tSeg) + wallAsteroidTrackBuffer;
    }

    private ClosestPoint findClosestPointOnCenterline(Vector2f p) {
        if (race.centerline == null || race.centerline.size() < 2) return null;

        float best = Float.MAX_VALUE;
        ClosestPoint bestPoint = null;

        for (int i = 0; i < race.centerline.size(); i++) {
            Vector2f a = race.centerline.get(i);
            Vector2f b = race.centerline.get((i + 1) % race.centerline.size());
            ClosestPoint cp = closestPointOnSegment(p, a, b, i);
            if (cp != null && cp.distSq < best) {
                best = cp.distSq;
                bestPoint = cp;
            }
        }

        return bestPoint;
    }

    private ClosestPoint closestPointOnSegment(Vector2f p, Vector2f a, Vector2f b, int segIndex) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;
        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) return null;

        float apx = p.x - a.x;
        float apy = p.y - a.y;
        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        Vector2f point = new Vector2f(a.x + abx * t, a.y + aby * t);
        float dx = p.x - point.x;
        float dy = p.y - point.y;
        float distSq = dx * dx + dy * dy;

        return new ClosestPoint(segIndex, t, point, distSq);
    }

    private List<Vector2f> buildTrackRibbonEdgePolyline(boolean leftSide, float offsetFromBoundary) {
        List<Vector2f> out = new ArrayList<>();
        if (race.centerline == null) return out;
        int n = race.centerline.size();
        if (n < 2) return out;

        for (int i = 0; i < n; i++) {
            Vector2f prev = race.centerline.get((i - 1 + n) % n);
            Vector2f cur = race.centerline.get(i);
            Vector2f next = race.centerline.get((i + 1) % n);
            if (prev == null || cur == null || next == null) continue;

            Vector2f t1 = unit(cur.x - prev.x, cur.y - prev.y);
            Vector2f t2 = unit(next.x - cur.x, next.y - cur.y);
            Vector2f n1 = new Vector2f(-t1.y, t1.x);
            Vector2f n2 = new Vector2f(-t2.y, t2.x);

            Vector2f miter = unit(n1.x + n2.x, n1.y + n2.y);
            float dot = miter.x * n1.x + miter.y * n1.y;
            if (Math.abs(dot) < 0.1f) {
                miter = n1;
                dot = 1f;
            }

            float width = leftSide ? getLeftWidth(i, 0f) : getRightWidth(i, 0f);
            // Positive offset moves inward, negative offset moves outward.
            float edgeDistance = Math.max(5f, width - offsetFromBoundary);
            float miterScale = edgeDistance / dot;
            float maxMiter = edgeDistance * 2f;
            if (miterScale > maxMiter) miterScale = maxMiter;
            if (miterScale < -maxMiter) miterScale = -maxMiter;

            float sideSign = leftSide ? 1f : -1f;
            out.add(new Vector2f(
                    cur.x + miter.x * miterScale * sideSign,
                    cur.y + miter.y * miterScale * sideSign
            ));
        }

        return out;
    }

    private Vector2f unit(float x, float y) {
        float len = (float) Math.sqrt(x * x + y * y);
        if (len < 0.0001f) return new Vector2f(1f, 0f);
        return new Vector2f(x / len, y / len);
    }




    private void applyOffTrackPenalty() {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return;

        for (ShipAPI ship : race.racers.keySet()) {
            if (ship == null || ship.isHulk()) continue;

            RacerState rs = race.racers.get(ship);
            if (rs == null || rs.finished) continue;

            // Always clear first just to make sure cuz i had some issue with it before
            ship.getMutableStats().getMaxSpeed().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getAcceleration().unmodify(OFFTRACK_MOD_ID);
            ship.getMutableStats().getDeceleration().unmodify(OFFTRACK_MOD_ID);

            float distSq = distanceToTrackCenterlineSq(ship.getLocation());
            ClosestPoint cp = findClosestPointOnCenterline(ship.getLocation());
            boolean offTrack = true;

            if (cp != null) {
                Vector2f a = race.centerline.get(cp.segIndex);
                Vector2f b = race.centerline.get((cp.segIndex + 1) % race.centerline.size());
                Vector2f tangent = unit(b.x - a.x, b.y - a.y);
                Vector2f normal = new Vector2f(-tangent.y, tangent.x);

                float wl = getWallLeftWidth(cp.segIndex, cp.t);
                float wr = getWallRightWidth(cp.segIndex, cp.t);

                float dx = ship.getLocation().x - cp.point.x;
                float dy = ship.getLocation().y - cp.point.y;
                float side = dx * normal.x + dy * normal.y;
                float allowed = side >= 0f ? wl : wr;

                offTrack = Math.abs(side) > allowed;
            } else {
                float halfWidthSq = race.corridorHalfWidth * race.corridorHalfWidth;
                offTrack = distSq > halfWidthSq;
            }


            if (offTrack) {
                ship.getMutableStats().getMaxSpeed().modifyMult(OFFTRACK_MOD_ID, offTrackMaxSpeedMult);
                ship.getMutableStats().getAcceleration().modifyMult(OFFTRACK_MOD_ID, offTrackAccelMult);
                ship.getMutableStats().getDeceleration().modifyMult(OFFTRACK_MOD_ID, offTrackAccelMult);

                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(
                            "Takeshido Offroad",
                            null,
                            "Off track",
                            "Top speed heavily reduced",
                            true
                    );
                }
            }
        }
    }

    private float distanceToTrackCenterlineSq(Vector2f p) {
        int n = race.centerline.size();
        float best = Float.MAX_VALUE;

        for (int i = 0; i < n; i++) {
            Vector2f a = race.centerline.get(i);
            Vector2f b = race.centerline.get((i + 1) % n);
            float d2 = distancePointToSegmentSq(p, a, b);
            if (d2 < best) best = d2;
        }
        return best;
    }

    private float distancePointToSegmentSq(Vector2f p, Vector2f a, Vector2f b) {
        float abx = b.x - a.x;
        float aby = b.y - a.y;

        float apx = p.x - a.x;
        float apy = p.y - a.y;

        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) {
            float dx = p.x - a.x;
            float dy = p.y - a.y;
            return dx * dx + dy * dy;
        }

        float t = (apx * abx + apy * aby) / abLen2;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;

        float cx = a.x + abx * t;
        float cy = a.y + aby * t;

        float dx = p.x - cx;
        float dy = p.y - cy;
        return dx * dx + dy * dy;
    }




    private boolean isRacer(ShipAPI s) {
        if (s == null || s.isHulk()) return false;
        if (spectatorOnly && spectatorMemberId != null && s.getFleetMember() != null
                && spectatorMemberId.equals(s.getFleetMember().getId())) {
            return false;
        }
        try {
            return s.getVariant() != null && s.getVariant().hasHullMod(raceHullmodId);
        } catch (Throwable t) {
            return false;
        }
    }

    private void updateRaceProgress() {
        if (race.checkpoints == null || race.checkpoints.isEmpty()) return;

        ShipAPI player = engine.getPlayerShip();
        if (!spectatorOnly && player != null && isRacer(player) && player.isHulk()) {
            race.endedByPlayerDNF = true;
            race.winnerSide = FleetSide.ENEMY;
            recordDNF(player);
            race.finished = true;
            return;
        }

        for (Map.Entry<ShipAPI, RacerState> e : race.racers.entrySet()) {
            ShipAPI ship = e.getKey();
            RacerState rs = e.getValue();

            if (rs.finished) continue;
            if (ship == null) continue;
            if (ship.isHulk()) {
                rs.finished = true;
                recordDNF(ship);
                if (race.finishOrderMemberIds.size() >= race.racers.size()) {
                    race.finished = true;
                }
                continue;
            }

            boolean crossed = false;
            GateSpec gate = (race.gates != null && race.gates.size() == race.checkpoints.size())
                    ? race.gates.get(rs.nextCheckpoint)
                    : null;

            if (gate != null && rs.lastPos != null) {
                Vector2f cur = ship.getLocation();
                float prevS = dotDelta(rs.lastPos, gate.center, gate.tangent);
                float curS = dotDelta(cur, gate.center, gate.tangent);

                if (prevS < 0f && curS >= 0f) {
                    float lat = dotDelta(cur, gate.center, gate.normal);
                    float pad = Math.max(20f, ship.getCollisionRadius() * 0.25f);
                    if (lat <= gate.wLeft + pad && lat >= -gate.wRight - pad) {
                        crossed = true;
                    }
                }
            } else {
                Vector2f cp = race.checkpoints.get(rs.nextCheckpoint);
                if (distance(ship.getLocation(), cp) <= race.checkpointRadius) {
                    crossed = true;
                }
            }

            if (crossed) {
                rs.nextCheckpoint++;
                if (rs.nextCheckpoint >= race.checkpoints.size()) rs.nextCheckpoint = 0;

                // Lap increments when you pass CP0 (start/finish), i.e. next becomes 1
                int firstTarget = (race.startIndex + 1) % race.checkpoints.size();
                if (rs.nextCheckpoint == firstTarget) {
                    rs.lap++;
                    if (rs.lap >= race.lapsToWin) {
                        rs.finished = true;
                        recordFinish(ship);

                        if (race.finishOrderMemberIds.size() == 1) {
                            race.winnerSide = (ship == engine.getPlayerShip()) ? FleetSide.PLAYER : FleetSide.ENEMY;
                            engine.addFloatingText(
                                    new Vector2f(ship.getLocation()),
                                    (ship == engine.getPlayerShip() ? "You win!" : "You lost!"),
                                    30f, Color.WHITE, ship, 1f, 1f
                            );
                        }

                        if (ship == engine.getPlayerShip()) {
                            race.finished = true;
                        }

                        if (race.finishOrderMemberIds.size() >= race.racers.size()) {
                            race.finished = true;
                        }
                    }
                }
            }

            if (rs.lastPos == null) {
                rs.lastPos = new Vector2f(ship.getLocation());
            } else {
                rs.lastPos.set(ship.getLocation());
            }
        }
    }

    private void recordFinish(ShipAPI ship) {
        if (ship == null) return;
        if (ship.getFleetMember() == null) return;
        String id = ship.getFleetMember().getId();
        if (id == null) return;
        if (!race.finishOrderMemberIds.contains(id)) {
            race.finishOrderMemberIds.add(id);
        }
    }

    private void recordDNF(ShipAPI ship) {
        if (ship == null) return;
        if (ship.getFleetMember() == null) return;
        String id = ship.getFleetMember().getId();
        if (id == null) return;
        if (!race.finishOrderMemberIds.contains(id)) {
            race.finishOrderMemberIds.add(id);
        }
        if (!race.dnfMemberIds.contains(id)) {
            race.dnfMemberIds.add(id);
        }
    }

    private void endRaceByTimeout() {
        if (race.finished) return;
        race.endedByTimeout = true;
        race.finished = true;
        if (!race.finishOrderMemberIds.isEmpty()) {
            String first = race.finishOrderMemberIds.get(0);
            ShipAPI player = engine.getPlayerShip();
            if (player != null && player.getFleetMember() != null) {
                race.winnerSide = first.equals(player.getFleetMember().getId()) ? FleetSide.PLAYER : FleetSide.ENEMY;
            }
        }
    }

    private void finalizeFinishOrder() {
        if (race.racers == null || race.racers.isEmpty()) return;

        List<ShipAPI> remaining = new ArrayList<>();
        for (Map.Entry<ShipAPI, RacerState> e : race.racers.entrySet()) {
            ShipAPI ship = e.getKey();
            if (ship == null || ship.getFleetMember() == null) continue;
            String id = ship.getFleetMember().getId();
            if (id == null) continue;
            if (!race.finishOrderMemberIds.contains(id)) {
                remaining.add(ship);
            }
        }

        if (!remaining.isEmpty()) {
            Collections.sort(remaining, new Comparator<ShipAPI>() {
                @Override
                public int compare(ShipAPI a, ShipAPI b) {
                    RacerState ra = race.racers.get(a);
                    RacerState rb = race.racers.get(b);
                    float pa = getProgressScore(a, ra);
                    float pb = getProgressScore(b, rb);
                    return Float.compare(pb, pa);
                }
            });

            for (ShipAPI ship : remaining) {
                recordFinish(ship);
            }
        }

        if (!race.finishOrderMemberIds.isEmpty()) {
            ShipAPI player = engine.getPlayerShip();
            if (player != null && player.getFleetMember() != null) {
                String first = race.finishOrderMemberIds.get(0);
                race.winnerSide = first.equals(player.getFleetMember().getId()) ? FleetSide.PLAYER : FleetSide.ENEMY;
            }
        }
    }

    private float getProgressScore(ShipAPI ship, RacerState rs) {
        if (ship == null || rs == null || race.checkpoints == null) return 0f;
        int cpCount = race.checkpoints.size();
        int lastCheckpoint = rs.nextCheckpoint - 1;
        if (lastCheckpoint < 0) lastCheckpoint = cpCount - 1;
        float progress = rs.lap * cpCount + lastCheckpoint;
        return progress;
    }

    private void storeResultsIfNeeded() {
        if (resultsStored) return;
        if (raceId == null || raceId.trim().isEmpty()) return;
        resultsStored = true;
        TakeshidoRacingManager.storeRaceResult(raceId, race.finishOrderMemberIds, race.dnfMemberIds, race.endedByTimeout, race.endedByPlayerDNF);
        TakeshidoRacingManager.processRaceResult(raceId);
    }

    private void applyNonDeathRaceLockout(List<ShipAPI> cars) {
        for (ShipAPI s : cars) {
            if (s == null) continue;
            disableAllWeapons(s);
            forceHoldFire(s);
        }
    }


    private void disableAllWeapons(ShipAPI s) {
        // WeaponAPI.disable() exists and permanently disables that weapon for the battle. :contentReference[oaicite:2]{index=2}
        for (com.fs.starfarer.api.combat.WeaponAPI w : s.getAllWeapons()) {
            if (w != null){
                w.disable(true);
            }
        }
    }

    private void forceHoldFire(ShipAPI s) {
        // In case any weapon slips through (decorative/system weapons, etc.)
        // HOLD_FIRE is a ShipCommand. :contentReference[oaicite:3]{index=3}
        s.blockCommandForOneFrame(com.fs.starfarer.api.combat.ShipCommand.HOLD_FIRE);
    }

    private Vector2f startPoint() {
        if (race.checkpoints == null || race.checkpoints.isEmpty()) return new Vector2f(0f, 0f);
        return race.checkpoints.get(race.startIndex);
    }

    private void startCountdown() {
        countdownActive = true;
        countdownDelayTimer = 3f;
        countdownTimer = 3f;
        lastCountdownSecond = -1;
        countdownSoundPlayed = false;
        enginesSuppressed = false;
        raceMusicStarted = false;
        raceTimer = 0f;
        resultsStored = false;
        applyStartLockout();
    }

    private void updateCountdown(float amount) {
        if (!countdownActive) return;

        applyStartLockout();
        if (countdownDelayTimer > 0f) {
            countdownDelayTimer -= amount;
            if (!enginesSuppressed) {
                setEngineFlameLevel(0f);
                enginesSuppressed = true;
            }
            return;
        }

        if (!countdownSoundPlayed && Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playCustomMusic(0, 0, "takeshido_countdown", false);
            countdownSoundPlayed = true;
        }

        if (enginesSuppressed) {
            setEngineFlameLevel(1f);
            enginesSuppressed = false;
        }

        countdownTimer -= amount;

        int currentSecond = (int) Math.ceil(Math.max(0f, countdownTimer));
        if (currentSecond != lastCountdownSecond) {
            lastCountdownSecond = currentSecond;
            String text = currentSecond > 0 ? String.valueOf(currentSecond) : "GO!";
            Vector2f center = engine.getViewport().getCenter();
            engine.addFloatingText(new Vector2f(center.x, center.y), text, 160f, Color.WHITE, null, 0f, 0f);
        }

        if (countdownTimer <= 0f) {
            countdownActive = false;
            releaseStartLockout();
            ensureRaceAI();
            startRaceMusic();
            engine.getCombatUI().addMessage(1, "Race start! Complete " + lapsToWin + " laps.");
        }
    }

    private void ensureRaceAI() {
        if (postCountdownAIEnsured || engine == null) return;
        postCountdownAIEnsured = true;

        ShipAPI player = engine.getPlayerShip();
        int firstTarget = (race.startIndex + 1) % race.checkpoints.size();

        for (ShipAPI s : engine.getShips()) {
            if (!isRacer(s)) continue;

            RacerState rs = race.racers.get(s);
            if (rs == null) {
                rs = new RacerState();
                rs.skill = getSkillForShip(s);
                rs.nextCheckpoint = firstTarget;
                rs.lastPos = new Vector2f(s.getLocation());
                race.racers.put(s, rs);
            }

            if (s != player) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            } else if (!spectatorOnly && s.getShipAI() != null) {
                s.setShipAI(new TakeshidoRaceShipAI(s, race, rs));
            }
        }
    }

    private void ensurePlayerAutopilotRaceAI() {
        if (spectatorOnly || engine == null || race == null) return;
        if (engine.getCombatUI() == null || !engine.getCombatUI().isAutopilotOn()) return;

        ShipAPI player = engine.getPlayerShip();
        if (player == null || player.getShipAI() == null) return;
        if (player.getShipAI() instanceof TakeshidoRaceShipAI) return;

        RacerState rs = race.racers != null ? race.racers.get(player) : null;
        if (rs == null) return;

        player.setShipAI(new TakeshidoRaceShipAI(player, race, rs));
    }

    private void startRaceMusic() {
        if (raceMusicStarted) return;
        raceMusicStarted = true;
        //Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
        currentRaceMusicId = pickRaceMusicId(null);
        Global.getSoundPlayer().playCustomMusic(0, 0, currentRaceMusicId, false);
    }

    private void playRaceFinish() {
        if (raceFinishPlayed || Global.getSoundPlayer() == null) return;
        raceFinishPlayed = true;
        //Global.getSoundPlayer().pauseCustomMusic();
        Global.getSoundPlayer().playCustomMusic(0, 0, "takeshido_race_finish", false);
    }

    private void ensureRaceMusicLoop() {
        if (engine == null || Global.getSoundPlayer() == null) return;
        String playing = getCurrentMusicIdSafe();
        if (playing != null) return;

        String next = pickRaceMusicId(currentRaceMusicId);
        currentRaceMusicId = next;
        Global.getSoundPlayer().playCustomMusic(0, 0, currentRaceMusicId, false);
    }

    private String pickRaceMusicId(String exclude) {
        String forced = getForcedRaceMusicId();
        if (forced != null) return forced;
        if (RACE_MUSIC_IDS.isEmpty()) return "takeshido_race";

        if (exclude == null || RACE_MUSIC_IDS.size() == 1) {
            return RACE_MUSIC_IDS.get(raceMusicRandom.nextInt(RACE_MUSIC_IDS.size()));
        }

        String pick = exclude;
        int guard = 0;
        while (pick.equals(exclude) && guard < 10) {
            pick = RACE_MUSIC_IDS.get(raceMusicRandom.nextInt(RACE_MUSIC_IDS.size()));
            guard++;
        }
        return pick;
    }

    private String getForcedRaceMusicId() {
        if (engine == null || engine.getCustomData() == null) return null;
        Object val = engine.getCustomData().get(RACE_MUSIC_FORCE_KEY);
        if (!(val instanceof String)) return null;
        String id = ((String) val).trim();
        return id.isEmpty() ? null : id;
    }

    private String getCurrentMusicIdSafe() {
        if (Global.getSoundPlayer() == null) return null;
        String id = Global.getSoundPlayer().getCurrentMusicId();
        if (id == null) return null;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return null;
        if ("nothing".equalsIgnoreCase(trimmed)) return null;
        return id;
    }

    private void applySoftWall(float amount) {
        if (race.centerline == null || race.centerline.size() < 2) return;

        for (ShipAPI ship : race.racers.keySet()) {
            if (ship == null || ship.isHulk()) continue;

            ClosestPoint cp = findClosestPointOnCenterline(ship.getLocation());
            if (cp == null) continue;

            Vector2f a = race.centerline.get(cp.segIndex);
            Vector2f b = race.centerline.get((cp.segIndex + 1) % race.centerline.size());
            Vector2f tangent = unit(b.x - a.x, b.y - a.y);
            Vector2f normal = new Vector2f(-tangent.y, tangent.x);

            float wl = getWallLeftWidth(cp.segIndex, cp.t);
            float wr = getWallRightWidth(cp.segIndex, cp.t);

            float dx = ship.getLocation().x - cp.point.x;
            float dy = ship.getLocation().y - cp.point.y;
            float side = dx * normal.x + dy * normal.y;
            float allowed = side >= 0f ? wl : wr;
            float penetration = Math.abs(side) - allowed;

            if (penetration <= 0f) continue;

            float sign = side >= 0f ? 1f : -1f;
            float push = Math.min(penetration * WALL_PUSH_MULT, WALL_PUSH_PER_SEC * amount);

            ship.getLocation().x -= normal.x * sign * push;
            ship.getLocation().y -= normal.y * sign * push;

            Vector2f vel = ship.getVelocity();
            if (vel != null) {
                float lateral = vel.x * normal.x + vel.y * normal.y;
                if (lateral * sign > 0f) {
                    vel.x -= normal.x * lateral * WALL_LATERAL_DAMP;
                    vel.y -= normal.y * lateral * WALL_LATERAL_DAMP;
                }
            }

            float face = ship.getFacing();
            float desired = (float) Math.toDegrees(Math.atan2(tangent.y, tangent.x));
            float delta = shortestRotation(face, desired);
            ship.setFacing(face + delta * 0.15f);
            ship.setAngularVelocity(ship.getAngularVelocity() * 0.5f);
        }
    }

    private void applySoftCarRepulsion(float amount) {
        if (amount <= 0f) return;
        if (race.racers == null || race.racers.isEmpty()) return;

        List<ShipAPI> cars = new ArrayList<>(race.racers.keySet());
        int n = cars.size();
        for (int i = 0; i < n; i++) {
            ShipAPI a = cars.get(i);
            if (a == null || a.isHulk()) continue;
            a.setCollisionClass(CollisionClass.NONE);
            for (int j = i + 1; j < n; j++) {
                ShipAPI b = cars.get(j);
                if (b == null || b.isHulk()) continue;
                b.setCollisionClass(CollisionClass.NONE);

                float dx = b.getLocation().x - a.getLocation().x;
                float dy = b.getLocation().y - a.getLocation().y;
                float r = a.getCollisionRadius() + b.getCollisionRadius() + CAR_REPEL_BUFFER;
                float distSq = dx * dx + dy * dy;
                if (distSq <= 0.0001f || distSq > r * r) continue;

                float dist = (float) Math.sqrt(distSq);
                float nx = dx / dist;
                float ny = dy / dist;
                float penetration = r - dist;
                float push = Math.min(penetration * 0.5f, CAR_REPEL_PER_SEC * amount);

                if (a.getVelocity() != null) {
                    a.getVelocity().x -= nx * push;
                    a.getVelocity().y -= ny * push;
                }
                if (b.getVelocity() != null) {
                    b.getVelocity().x += nx * push;
                    b.getVelocity().y += ny * push;
                }
            }
        }
    }

    private float shortestRotation(float fromDeg, float toDeg) {
        float a = normalizeAngle(toDeg) - normalizeAngle(fromDeg);
        if (a > 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    private float normalizeAngle(float a) {
        while (a < 0f) a += 360f;
        while (a >= 360f) a -= 360f;
        return a;
    }

    private void applyStartLockout() {
        float face = startFacing();
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            s.getVelocity().set(0f, 0f);
            s.setAngularVelocity(0f);
            s.setFacing(face);
            s.getMutableStats().getMaxSpeed().modifyMult(START_LOCK_ID, 0f);
            s.getMutableStats().getAcceleration().modifyMult(START_LOCK_ID, 0f);
            s.getMutableStats().getDeceleration().modifyMult(START_LOCK_ID, 0f);
        }
    }

    private void releaseStartLockout() {
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            s.getMutableStats().getMaxSpeed().unmodify(START_LOCK_ID);
            s.getMutableStats().getAcceleration().unmodify(START_LOCK_ID);
            s.getMutableStats().getDeceleration().unmodify(START_LOCK_ID);
        }
    }

    private void applySpectatorLock() {
        if (engine == null) return;
        syncRaceContext();
        ShipAPI player = engine.getPlayerShip();
        if (player == null) return;

        if (spectatorOnly) {
            player.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            player.blockCommandForOneFrame(ShipCommand.FIRE);
            player.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            player.blockCommandForOneFrame(ShipCommand.DECELERATE);
            player.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
            player.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
            player.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            player.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            lockSpectatorShip();
            return;
        }

        if (isRacer(player)) {
            player.getMutableStats().getMaxSpeed().unmodify(SPECTATOR_LOCK_ID);
            player.getMutableStats().getAcceleration().unmodify(SPECTATOR_LOCK_ID);
            player.getMutableStats().getDeceleration().unmodify(SPECTATOR_LOCK_ID);
            player.getMutableStats().getMaxTurnRate().unmodify(SPECTATOR_LOCK_ID);
            player.getMutableStats().getTurnAcceleration().unmodify(SPECTATOR_LOCK_ID);
            return;
        }

        player.getVelocity().set(0f, 0f);
        player.setAngularVelocity(0f);
        player.getMutableStats().getMaxSpeed().modifyMult(SPECTATOR_LOCK_ID, 0f);
        player.getMutableStats().getAcceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        player.getMutableStats().getDeceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        player.getMutableStats().getMaxTurnRate().modifyMult(SPECTATOR_LOCK_ID, 0f);
        player.getMutableStats().getTurnAcceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        player.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
        player.blockCommandForOneFrame(ShipCommand.FIRE);
        player.blockCommandForOneFrame(ShipCommand.ACCELERATE);
        player.blockCommandForOneFrame(ShipCommand.DECELERATE);
        player.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
        player.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        player.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
        player.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        player.setCollisionClass(CollisionClass.NONE);
    }

    private void syncRaceContext() {
        if (contextLoaded || raceId == null) return;
        TakeshidoRacingManager.RaceContext ctx = TakeshidoRacingManager.getActiveRace(raceId);
        if (ctx == null) return;
        spectatorOnly = ctx.spectatorOnly;
        betRacerId = ctx.betRacerId;
        spectatorMemberId = ctx.playerMemberId;
        contextLoaded = true;
    }

    private void ensureSpectatingTarget() {
        if (!spectatorOnly || spectatingApplied || engine == null) return;
        if (betRacerId == null || betRacerId.trim().isEmpty()) return;

        TakeshidoRacingManager.RaceContext ctx = TakeshidoRacingManager.getActiveRace(raceId);
        if (ctx == null) return;

        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.getFleetMember() == null) continue;
            String racerId = ctx.memberIdToRacerId.get(s.getFleetMember().getId());
            if (betRacerId.equals(racerId)) {
                betShip = s;
                break;
            }
        }

        if (betShip == null) return;

        engine.setPlayerShipExternal(betShip);
        RacerState rs = race.racers.get(betShip);
        if (rs != null) {
            spectatorAI = new TakeshidoRaceShipAI(betShip, race, rs);
            betShip.setShipAI(spectatorAI);
        }
        spectatingApplied = true;
    }

    private void lockSpectatorShip() {
        if (engine == null || spectatorMemberId == null) return;
        if (spectatorShip == null) {
            for (ShipAPI s : engine.getShips()) {
                if (s == null || s.getFleetMember() == null) continue;
                if (spectatorMemberId.equals(s.getFleetMember().getId())) {
                    spectatorShip = s;
                    break;
                }
            }
        }
        if (spectatorShip == null || spectatorShip.isHulk()) return;

        if (!spectatorInitialized) {
            placeSpectatorShip(spectatorShip);
            spectatorShip.setDoNotRenderWeapons(true);
            spectatorShip.setDoNotRenderShield(true);
            spectatorShip.setDoNotRenderVentingAnimation(true);
            spectatorShip.setRenderEngines(false);
            spectatorInitialized = true;
        }

        spectatorShip.getVelocity().set(0f, 0f);
        spectatorShip.setAngularVelocity(0f);
        spectatorShip.getMutableStats().getMaxSpeed().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getAcceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getDeceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getMaxTurnRate().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getTurnAcceleration().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getHullDamageTakenMult().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getArmorDamageTakenMult().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getEmpDamageTakenMult().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.getMutableStats().getShieldDamageTakenMult().modifyMult(SPECTATOR_LOCK_ID, 0f);
        spectatorShip.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
        spectatorShip.blockCommandForOneFrame(ShipCommand.FIRE);
        spectatorShip.blockCommandForOneFrame(ShipCommand.ACCELERATE);
        spectatorShip.blockCommandForOneFrame(ShipCommand.DECELERATE);
        spectatorShip.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
        spectatorShip.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        spectatorShip.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
        spectatorShip.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
        spectatorShip.setCollisionClass(CollisionClass.NONE);
    }


    private void setEngineFlameLevel(float level) {
        for (ShipAPI s : race.racers.keySet()) {
            if (s == null || s.isHulk()) continue;
            if (s.getEngineController() == null) continue;
            for (com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI e : s.getEngineController().getShipEngines()) {
                if (e == null) continue;
                s.getEngineController().setFlameLevel(e.getEngineSlot(), level);
            }
        }
    }

    private float startFacing() {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return 0f;
        int n = race.checkpoints.size();
        int start = ((race.startIndex % n) + n) % n;
        int next = (start + 1) % n;
        Vector2f cpStart = race.checkpoints.get(start);
        Vector2f cpNext = race.checkpoints.get(next);
        float dx = cpNext.x - cpStart.x;
        float dy = cpNext.y - cpStart.y;
        if (Math.abs(dx) < 0.0001f && Math.abs(dy) < 0.0001f) return 0f;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }


    private void placeStartingGrid(List<ShipAPI> cars) {
        if (race.checkpoints == null || race.checkpoints.size() < 2) return;

        int n = race.checkpoints.size();
        int start = ((race.startIndex % n) + n) % n;
        int next = (start + 1) % n;

        Vector2f cpStart = race.checkpoints.get(start);
        Vector2f cpNext  = race.checkpoints.get(next);


        // Travel direction at the start line: start -> next
        float dx = cpNext.x - cpStart.x;
        float dy = cpNext.y - cpStart.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) { dx = 1f; dy = 0f; len = 1f; }
        float dirX = dx / len;
        float dirY = dy / len;

        // Perpendicular for lane offsets
        float perpX = -dirY;
        float perpY = dirX;

        // Anchor: place the front row aheadOffset BEFORE the first target (cpNext),
        // on the centerline of the start segment.
        Vector2f anchor = new Vector2f(
                cpNext.x - dirX * aheadOffset,
                cpNext.y - dirY * aheadOffset
        );

        // Corridor half-width based on the actual track walls at the start segment
        float leftWidth = getWallLeftWidth(start, 0f);
        float rightWidth = getWallRightWidth(start, 0f);
        float safeHalfWidth = Math.max(0f, Math.min(leftWidth, rightWidth) - gridEdgeMargin);

        float cols = Math.max(2, gridColumns);
        float laneSpacing = (2f * safeHalfWidth) / (cols - 1f);
        laneSpacing = Math.max(80f, laneSpacing); // prevent collapsing into a stack

        float facing = (float) Math.toDegrees(Math.atan2(dirY, dirX));

        for (int i = 0; i < cars.size(); i++) {
            ShipAPI s = cars.get(i);
            if (s == null) continue;

            int row = i / gridColumns;
            int col = i % gridColumns;

            float centerCol = (gridColumns - 1) / 2f;
            float colOffset = (col - centerCol) * laneSpacing;

            // Rows extend backward along -dir, columns spread along perp
            Vector2f pos = new Vector2f(
                    anchor.x - dirX * (row * rowSpacing) + perpX * colOffset,
                    anchor.y - dirY * (row * rowSpacing) + perpY * colOffset
            );

            s.getLocation().set(pos);
            s.getVelocity().set(0f, 0f);
            s.setFacing(facing);
            s.setAngularVelocity(0f);
        }
    }

    private void placeSpectatorShip(ShipAPI player) {
        if (player == null || race.checkpoints == null || race.checkpoints.size() < 2) return;

        int n = race.checkpoints.size();
        int start = ((race.startIndex % n) + n) % n;
        int next = (start + 1) % n;

        Vector2f cpStart = race.checkpoints.get(start);
        Vector2f cpNext = race.checkpoints.get(next);

        float dx = cpNext.x - cpStart.x;
        float dy = cpNext.y - cpStart.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1f) { dx = 1f; dy = 0f; len = 1f; }
        float dirX = dx / len;
        float dirY = dy / len;
        float perpX = -dirY;
        float perpY = dirX;

        float leftWidth = getWallLeftWidth(start, 0f);
        float rightWidth = getWallRightWidth(start, 0f);
        float sideSign = (leftWidth >= rightWidth) ? 1f : -1f;
        float offset = Math.max(leftWidth, rightWidth) + gridEdgeMargin + 400f;

        Vector2f anchor = new Vector2f(
                cpStart.x - dirX * aheadOffset,
                cpStart.y - dirY * aheadOffset
        );

        Vector2f pos = new Vector2f(
                anchor.x + perpX * offset * sideSign,
                anchor.y + perpY * offset * sideSign
        );

        player.getLocation().set(pos);
        player.getVelocity().set(0f, 0f);
        player.setFacing((float) Math.toDegrees(Math.atan2(dirY, dirX)));
        player.setAngularVelocity(0f);
        player.setCollisionClass(CollisionClass.NONE);
    }





    private float distance(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float dotDelta(Vector2f p, Vector2f center, Vector2f axis) {
        float dx = p.x - center.x;
        float dy = p.y - center.y;
        return dx * axis.x + dy * axis.y;
    }

    private static class BackgroundMacroNoiseRenderer extends BaseCombatLayeredRenderingPlugin {
        private final SpriteAPI sprite;
        private final Vector2f center;
        private final float width;
        private final float height;
        private final List<OctaveSpec> octaves;
        private boolean expired = false;

        public BackgroundMacroNoiseRenderer(SpriteAPI sprite,
                                            Vector2f center,
                                            float width,
                                            float height,
                                            List<OctaveSpec> octaves) {
            super(CombatEngineLayers.BELOW_PLANETS);
            this.sprite = sprite;
            this.center = new Vector2f(center);
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);
            this.octaves = new ArrayList<OctaveSpec>(octaves);
        }

        public void expire() {
            expired = true;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (expired || sprite == null || viewport == null || octaves.isEmpty()) return;

            float rectMinX = center.x - width * 0.5f;
            float rectMaxX = center.x + width * 0.5f;
            float rectMinY = center.y - height * 0.5f;
            float rectMaxY = center.y + height * 0.5f;

            float viewMinX = viewport.getLLX();
            float viewMinY = viewport.getLLY();
            float viewMaxX = viewMinX + viewport.getVisibleWidth();
            float viewMaxY = viewMinY + viewport.getVisibleHeight();

            float drawMinX = Math.max(rectMinX, viewMinX);
            float drawMaxX = Math.min(rectMaxX, viewMaxX);
            float drawMinY = Math.max(rectMinY, viewMinY);
            float drawMaxY = Math.min(rectMaxY, viewMaxY);
            if (drawMinX >= drawMaxX || drawMinY >= drawMaxY) return;

            float vpAlpha = viewport.getAlphaMult();
            float spriteW = Math.max(1f, sprite.getWidth());
            float spriteH = Math.max(1f, sprite.getHeight());
            float aspect = spriteW / spriteH;

            sprite.bindTexture();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

            for (OctaveSpec octave : octaves) {
                if (octave == null) continue;

                float tileW = Math.max(64f, octave.tileWorldWidth);
                float tileH = Math.max(64f, tileW / aspect);
                float rad = (float) Math.toRadians(octave.rotationDeg);
                float cos = (float) Math.cos(rad);
                float sin = (float) Math.sin(rad);

                float darkAlpha = octave.darkAlpha * vpAlpha;
                if (darkAlpha > 0.001f) {
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    GL11.glColor4f(0f, 0f, 0f, darkAlpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    putNoiseVertex(drawMinX, drawMinY, center, octave, tileW, tileH, cos, sin);
                    putNoiseVertex(drawMaxX, drawMinY, center, octave, tileW, tileH, cos, sin);
                    putNoiseVertex(drawMaxX, drawMaxY, center, octave, tileW, tileH, cos, sin);
                    putNoiseVertex(drawMinX, drawMaxY, center, octave, tileW, tileH, cos, sin);
                    GL11.glEnd();
                }

                float lightAlpha = octave.lightAlpha * vpAlpha;
                if (lightAlpha > 0.001f) {
                    float radLight = rad + 0.67f;
                    float cosLight = (float) Math.cos(radLight);
                    float sinLight = (float) Math.sin(radLight);
                    OctaveSpec lightOctave = octave.withOffset(octave.offsetX + tileW * 0.19f, octave.offsetY - tileW * 0.14f);

                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    GL11.glColor4f(1f, 1f, 1f, lightAlpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    putNoiseVertex(drawMinX, drawMinY, center, lightOctave, tileW, tileH, cosLight, sinLight);
                    putNoiseVertex(drawMaxX, drawMinY, center, lightOctave, tileW, tileH, cosLight, sinLight);
                    putNoiseVertex(drawMaxX, drawMaxY, center, lightOctave, tileW, tileH, cosLight, sinLight);
                    putNoiseVertex(drawMinX, drawMaxY, center, lightOctave, tileW, tileH, cosLight, sinLight);
                    GL11.glEnd();
                }
            }

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        private static void putNoiseVertex(float x,
                                           float y,
                                           Vector2f center,
                                           OctaveSpec octave,
                                           float tileW,
                                           float tileH,
                                           float cos,
                                           float sin) {
            float dx = x - center.x - octave.offsetX;
            float dy = y - center.y - octave.offsetY;
            float rx = cos * dx + sin * dy;
            float ry = -sin * dx + cos * dy;
            GL11.glTexCoord2f(rx / tileW, ry / tileH);
            GL11.glVertex2f(x, y);
        }

        private static class OctaveSpec {
            private final float tileWorldWidth;
            private final float darkAlpha;
            private final float lightAlpha;
            private final float offsetX;
            private final float offsetY;
            private final float rotationDeg;

            private OctaveSpec(float tileWorldWidth,
                               float darkAlpha,
                               float lightAlpha,
                               float offsetX,
                               float offsetY,
                               float rotationDeg) {
                this.tileWorldWidth = tileWorldWidth;
                this.darkAlpha = darkAlpha;
                this.lightAlpha = lightAlpha;
                this.offsetX = offsetX;
                this.offsetY = offsetY;
                this.rotationDeg = rotationDeg;
            }

            private OctaveSpec withOffset(float offsetX, float offsetY) {
                return new OctaveSpec(tileWorldWidth, darkAlpha, lightAlpha, offsetX, offsetY, rotationDeg);
            }
        }
    }

    private static class TrackDecalRenderer extends BaseCombatLayeredRenderingPlugin {
        private final List<DecalInstance> decals;
        private boolean expired = false;

        public TrackDecalRenderer(List<DecalInstance> decals) {
            super(CombatEngineLayers.BELOW_SHIPS_LAYER);
            this.decals = new ArrayList<DecalInstance>(decals);
        }

        public void expire() {
            expired = true;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (expired || viewport == null || decals.isEmpty()) return;

            float viewMinX = viewport.getLLX();
            float viewMinY = viewport.getLLY();
            float viewMaxX = viewMinX + viewport.getVisibleWidth();
            float viewMaxY = viewMinY + viewport.getVisibleHeight();
            float vpAlpha = viewport.getAlphaMult();

            for (DecalInstance decal : decals) {
                if (decal == null || decal.sprite == null || decal.alpha <= 0f) continue;

                float halfW = decal.width * 0.5f;
                float halfH = decal.height * 0.5f;
                float pad = Math.max(halfW, halfH);
                if (decal.center.x + pad < viewMinX || decal.center.x - pad > viewMaxX ||
                        decal.center.y + pad < viewMinY || decal.center.y - pad > viewMaxY) {
                    continue;
                }

                decal.sprite.setNormalBlend();
                decal.sprite.setAlphaMult(decal.alpha * vpAlpha);
                decal.sprite.setAngle(decal.angle);
                decal.sprite.setSize(decal.width, decal.height);
                decal.sprite.renderAtCenter(decal.center.x, decal.center.y);
            }
        }

        private static class DecalInstance {
            private final SpriteAPI sprite;
            private final Vector2f center;
            private final float width;
            private final float height;
            private final float angle;
            private final float alpha;

            private DecalInstance(SpriteAPI sprite, Vector2f center, float width, float height, float angle, float alpha) {
                this.sprite = sprite;
                this.center = center;
                this.width = width;
                this.height = height;
                this.angle = angle;
                this.alpha = alpha;
            }
        }
    }

    private static class TrackBandRenderer extends BaseCombatLayeredRenderingPlugin {
        private final SpriteAPI sprite;
        private final List<Vector2f> edgeA;
        private final List<Vector2f> edgeB;
        private final List<Float> uCoords;
        private final float widthRepeats;
        private final float edgeAlphaA;
        private final float edgeAlphaB;
        private final float alpha;
        private boolean expired = false;

        public TrackBandRenderer(SpriteAPI sprite,
                                 List<Vector2f> edgeA,
                                 List<Vector2f> edgeB,
                                 float tileWorldLength,
                                 float widthRepeatScale,
                                 float edgeAlphaA,
                                 float edgeAlphaB,
                                 float alpha) {
            super(CombatEngineLayers.BELOW_SHIPS_LAYER);
            this.sprite = sprite;
            this.edgeA = new ArrayList<Vector2f>();
            this.edgeB = new ArrayList<Vector2f>();
            for (Vector2f p : edgeA) {
                this.edgeA.add(new Vector2f(p));
            }
            for (Vector2f p : edgeB) {
                this.edgeB.add(new Vector2f(p));
            }
            this.alpha = Math.max(0f, Math.min(1f, alpha));
            this.uCoords = buildUCoords(this.edgeA, this.edgeB, tileWorldLength);
            this.widthRepeats = computeWidthRepeats(this.sprite, this.edgeA, this.edgeB, tileWorldLength, widthRepeatScale);
            this.edgeAlphaA = Math.max(0f, Math.min(1f, edgeAlphaA));
            this.edgeAlphaB = Math.max(0f, Math.min(1f, edgeAlphaB));
        }

        public void expire() {
            expired = true;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (expired || sprite == null || viewport == null) return;
            int n = Math.min(edgeA.size(), edgeB.size());
            if (n < 2 || uCoords.size() < n + 1) return;

            sprite.bindTexture();

            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            float vpAlpha = alpha * viewport.getAlphaMult();

            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            for (int i = 0; i <= n; i++) {
                int idx = i % n;
                float u = uCoords.get(i);
                Vector2f l = edgeA.get(idx);
                Vector2f r = edgeB.get(idx);
                GL11.glColor4f(1f, 1f, 1f, vpAlpha * edgeAlphaA);
                GL11.glTexCoord2f(u, 0f);
                GL11.glVertex2f(l.x, l.y);
                GL11.glColor4f(1f, 1f, 1f, vpAlpha * edgeAlphaB);
                GL11.glTexCoord2f(u, widthRepeats);
                GL11.glVertex2f(r.x, r.y);
            }
            GL11.glEnd();

            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
        }

        private static List<Float> buildUCoords(List<Vector2f> left, List<Vector2f> right, float tileWorldLength) {
            List<Float> out = new ArrayList<Float>();
            int n = Math.min(left.size(), right.size());
            if (n < 2) return out;

            float repeatLen = Math.max(32f, tileWorldLength);
            float accum = 0f;
            out.add(0f);
            for (int i = 1; i <= n; i++) {
                int prev = i - 1;
                int curr = i % n;

                Vector2f cPrev = new Vector2f(
                        (left.get(prev).x + right.get(prev).x) * 0.5f,
                        (left.get(prev).y + right.get(prev).y) * 0.5f
                );
                Vector2f cCurr = new Vector2f(
                        (left.get(curr).x + right.get(curr).x) * 0.5f,
                        (left.get(curr).y + right.get(curr).y) * 0.5f
                );
                float dx = cCurr.x - cPrev.x;
                float dy = cCurr.y - cPrev.y;
                accum += (float) Math.sqrt(dx * dx + dy * dy);
                out.add(accum / repeatLen);
            }
            return out;
        }

        private static float computeWidthRepeats(SpriteAPI sprite,
                                                 List<Vector2f> edgeA,
                                                 List<Vector2f> edgeB,
                                                 float tileWorldLength,
                                                 float widthRepeatScale) {
            int n = Math.min(edgeA.size(), edgeB.size());
            if (n < 2) return 1f;

            float widthSum = 0f;
            for (int i = 0; i < n; i++) {
                Vector2f a = edgeA.get(i);
                Vector2f b = edgeB.get(i);
                float dx = b.x - a.x;
                float dy = b.y - a.y;
                widthSum += (float) Math.sqrt(dx * dx + dy * dy);
            }
            float avgWidth = widthSum / Math.max(1, n);
            float repeatLen = Math.max(32f, tileWorldLength);
            float scale = Math.max(0.1f, widthRepeatScale);

            float spriteW = Math.max(1f, sprite.getWidth());
            float spriteH = Math.max(1f, sprite.getHeight());
            float textureAspect = spriteW / spriteH;

            float repeats = (avgWidth * textureAspect / repeatLen) * scale;
            return Math.max(0.1f, repeats);
        }
    }

    private static class ScreenLockedBackgroundRenderer extends BaseCombatLayeredRenderingPlugin {
        private final SpriteAPI sprite;
        private final float alpha;
        private final Vector2f center;
        private final float width;
        private final float height;
        private final float tileWidth;
        private final float tileHeight;
        private boolean expired = false;

        public ScreenLockedBackgroundRenderer(SpriteAPI sprite, float alpha, Vector2f center, float width, float height, float tileWorldWidth) {
            super(CombatEngineLayers.BELOW_PLANETS);
            this.sprite = sprite;
            this.alpha = alpha;

            this.center = new Vector2f(center);
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);

            float spriteWidth = Math.max(1f, sprite.getWidth());
            float spriteHeight = Math.max(1f, sprite.getHeight());
            float spriteAspect = spriteWidth / spriteHeight;

            float tw = Math.max(64f, tileWorldWidth);
            float th = tw / spriteAspect;
            this.tileWidth = tw;
            this.tileHeight = Math.max(64f, th);
        }

        public void expire() {
            expired = true;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public void render(CombatEngineLayers layer, com.fs.starfarer.api.combat.ViewportAPI viewport) {
            if (expired || sprite == null || viewport == null) return;

            float rectMinX = center.x - width * 0.5f;
            float rectMaxX = center.x + width * 0.5f;
            float rectMinY = center.y - height * 0.5f;
            float rectMaxY = center.y + height * 0.5f;

            float viewMinX = viewport.getLLX();
            float viewMinY = viewport.getLLY();
            float viewMaxX = viewMinX + viewport.getVisibleWidth();
            float viewMaxY = viewMinY + viewport.getVisibleHeight();

            float drawMinX = Math.max(rectMinX, viewMinX - tileWidth);
            float drawMaxX = Math.min(rectMaxX, viewMaxX + tileWidth);
            float drawMinY = Math.max(rectMinY, viewMinY - tileHeight);
            float drawMaxY = Math.min(rectMaxY, viewMaxY + tileHeight);
            if (drawMinX >= drawMaxX || drawMinY >= drawMaxY) return;

            float startX = rectMinX + (float) Math.floor((drawMinX - rectMinX) / tileWidth) * tileWidth;
            float startY = rectMinY + (float) Math.floor((drawMinY - rectMinY) / tileHeight) * tileHeight;

            sprite.setNormalBlend();
            sprite.setAngle(0f);
            sprite.setAlphaMult(alpha * viewport.getAlphaMult());

            final float overlap = 1f;
            for (float x = startX; x < drawMaxX; x += tileWidth) {
                float w = Math.min(tileWidth, rectMaxX - x);
                if (w <= 0f) continue;

                for (float y = startY; y < drawMaxY; y += tileHeight) {
                    float h = Math.min(tileHeight, rectMaxY - y);
                    if (h <= 0f) continue;

                    float renderW = Math.max(1f, w + overlap);
                    float renderH = Math.max(1f, h + overlap);
                    sprite.setSize(renderW, renderH);
                    sprite.renderAtCenter(x + w * 0.5f, y + h * 0.5f);
                }
            }
        }
    }

    private static class GateSpec {
        public final Vector2f center;
        public final Vector2f tangent;
        public final Vector2f normal;
        public final float wLeft;
        public final float wRight;

        public GateSpec(Vector2f center, Vector2f tangent, Vector2f normal, float wLeft, float wRight) {
            this.center = center;
            this.tangent = tangent;
            this.normal = normal;
            this.wLeft = wLeft;
            this.wRight = wRight;
        }
    }

    private static class ClosestPoint {
        public final int segIndex;
        public final float t;
        public final Vector2f point;
        public final float distSq;

        public ClosestPoint(int segIndex, float t, Vector2f point, float distSq) {
            this.segIndex = segIndex;
            this.t = t;
            this.point = point;
            this.distSq = distSq;
        }
    }

    public static class RaceState {
        public int lapsToWin;
        public float checkpointRadius = 350f;
        public List<Vector2f> checkpoints = new ArrayList<>();
        public Map<ShipAPI, RacerState> racers = new LinkedHashMap<>();
        public boolean finished = false;
        public FleetSide winnerSide = FleetSide.PLAYER;
        public int startIndex;
        public boolean isDeathRace = false;

        public List<Vector2f> centerline = new ArrayList<>();
        public List<Vector2f> raceline = new ArrayList<>();
        public List<Float> racelineS = new ArrayList<>();
        public List<Float> racelinePsi = new ArrayList<>();
        public List<Float> racelineKappa = new ArrayList<>();
        public List<Float> racelineVx = new ArrayList<>();
        public float unitsPerMeter = 100f;
        public List<Float> wLeft = new ArrayList<>();
        public List<Float> wRight = new ArrayList<>();
        public boolean useWidths = false;
        public float corridorHalfWidth = 350f; // fallback
        public List<GateSpec> gates = new ArrayList<>();
        public List<String> finishOrderMemberIds = new ArrayList<>();
        public List<String> dnfMemberIds = new ArrayList<>();
        public boolean endedByTimeout = false;
        public boolean endedByPlayerDNF = false;


        public RaceState(int lapsToWin) {
            this.lapsToWin = lapsToWin;
        }
    }

    public static class RacerState {
        public int lap = 0;
        public int nextCheckpoint = 1;
        public boolean finished = false;
        public Vector2f lastPos;
        public float skill = 0f;
    }

    private float getSkillForShip(ShipAPI ship) {
        if (ship == null) return 0f;
        if (skillOverrides != null && ship.getFleetMember() != null) {
            String id = ship.getFleetMember().getId();
            Float override = skillOverrides.get(id);
            if (override != null) return clamp(override, 0f, 1f);
        }

        if (Global.getSector() == null) return 0f;
        if (ship.getCaptain() == null || ship.getCaptain().isDefault()) return 0f;
        String key = ship.getCaptain().getId();
        if (key == null || key.trim().isEmpty()) return 0f;

        Object data = Global.getSector().getPersistentData().get("takeshido_race_skills");
        if (!(data instanceof Map)) {
            Map<String, Float> map = new HashMap<>();
            map.put(key, 0f);
            Global.getSector().getPersistentData().put("takeshido_race_skills", map);
            return 0f;
        }

        @SuppressWarnings("unchecked")
        Map<String, Float> map = (Map<String, Float>) data;
        Float skill = map.get(key);
        if (skill == null) {
            map.put(key, 0f);
            return 0f;
        }
        return clamp(skill, 0f, 1f);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}


