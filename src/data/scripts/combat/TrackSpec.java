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
    public float wallOffsetExtra = 0f;
    public float edgeMarkerSpacing = 200f;

    public GridSpec grid = new GridSpec();

    public List<Vector2f> checkpoints = new ArrayList<>();

    public static class GridSpec {
        public int columns = 4;
        public float desiredLaneSpacing = 260f;
        public float rowSpacing = 320f;
        public float edgeMargin = 80f;
        public float widthMult = 1f;
        public float aheadOffset = 900f;
    }
}

