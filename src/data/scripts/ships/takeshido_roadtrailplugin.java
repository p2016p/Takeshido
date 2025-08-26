package data.scripts.ships;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class takeshido_roadtrailplugin implements EveryFrameCombatPlugin {

    private CombatEngineAPI engine;
    private ShipAPI targetShip;

    private final float ROAD_WIDTH = 45f;
    private final float FORWARD_LENGTH = 60f;
    private final float BACKWARD_LENGTH = 120f;
    private final float TOTAL_LENGTH = FORWARD_LENGTH + BACKWARD_LENGTH;

    private final Deque<Vector2f> trailPoints = new LinkedList<>();
    private final int MAX_POINTS = 60;

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isAlive() && ship.getHullSpec().getHullId().equals("your_ship_id")) {
                targetShip = ship;
                break;
            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine.isPaused() || targetShip == null) return;

        Vector2f shipLoc = new Vector2f(targetShip.getLocation());
        float angle = targetShip.getFacing();

        Vector2f rearOffset = Misc.getUnitVectorAtDegreeAngle(angle + 180f);
        rearOffset.scale(FORWARD_LENGTH);
        Vector2f trailPoint = Vector2f.add(shipLoc, rearOffset, null);

        trailPoints.addFirst(trailPoint);

        while (getTotalTrailLength() > TOTAL_LENGTH && trailPoints.size() > 2) {
            trailPoints.removeLast();
        }

        renderTrail();
    }

    private float getTotalTrailLength() {
        float total = 0f;
        Vector2f prev = null;
        for (Vector2f p : trailPoints) {
            if (prev != null) {
                total += Misc.getDistance(prev, p);
            }
            prev = p;
        }
        return total;
    }

    private void renderTrail() {
        if (trailPoints.size() < 2) return;

        Vector2f prev = null;
        for (Vector2f p : trailPoints) {
            if (prev != null) {
                renderSegment(prev, p);
            }
            prev = p;
        }
    }

    private void renderSegment(Vector2f from, Vector2f to) {
        Vector2f dir = Vector2f.sub(to, from, null);
        dir.normalise();
        Vector2f perp = new Vector2f(-dir.y, dir.x);
        perp.scale(ROAD_WIDTH / 2f);

        Color color = new Color(100, 200, 255, 80);

        engine.addSmoothParticle(from, new Vector2f(), ROAD_WIDTH, 1.0f, 0.1f, color);
        engine.addNebulaParticle(from, new Vector2f(), ROAD_WIDTH * 0.9f, 1.5f, 0.3f, 0.5f, 0.2f, color);
        engine.addHitParticle(from, new Vector2f(), ROAD_WIDTH, 0.8f, 0.1f, color);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        // Not used
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {
        // Not used
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        // Not used
    }
}
