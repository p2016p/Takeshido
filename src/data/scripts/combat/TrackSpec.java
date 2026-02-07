package data.scripts.combat;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

public class TrackSpec {
    public String id;
    public String name;

    public Integer lapsToWin;
    public int startIndex = 0;


    public float checkpointRadius = 350f;
    public float edgeMarkerSpacing = 200f;

    public float wallAsteroidSpacing = 180f;
    public int wallAsteroidType = 0;
    public float wallAsteroidTrackBuffer = 40f;

    public GridSpec grid = new GridSpec();

    public List<Vector2f> checkpoints = new ArrayList<>();

    public String centerlineCsv = null;
    public String racelineCsv = null;

    public float unitsPerMeter = 100f;
    public float rotationDeg = 0f;
    public boolean autoCenter = true;
    public float offsetX = 0f;
    public float offsetY = 0f;

    public boolean useCsvWidths = true;
    public float widthScale = 1f;

    public float checkpointSpacing = 900f; // used only if no "checkpoints" array is provided

    // Resolved geometry
    public List<Vector2f> centerline = new ArrayList<>();
    public List<Vector2f> raceline = new ArrayList<>();
    public List<Float> racelineS = new ArrayList<>();
    public List<Float> racelinePsi = new ArrayList<>();
    public List<Float> racelineKappa = new ArrayList<>();
    public List<Float> racelineVx = new ArrayList<>();
    public List<Float> wLeft = new ArrayList<>();
    public List<Float> wRight = new ArrayList<>();



    public static class GridSpec {
        public int columns = 4;
        public float rowSpacing = 320f;
        public float edgeMargin = 80f;
        public float aheadOffset = 900f;
    }
}

