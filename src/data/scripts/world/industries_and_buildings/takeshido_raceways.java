package data.scripts.world.industries_and_buildings;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lwjgl.util.vector.Vector2f;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class takeshido_raceways extends BaseIndustry {

    private static final float ACCESSIBILITY_BONUS = 0.30f;
    private static final float GROUND_DEF_BONUS = 100f;
    private static final int INPUT_ORE = 1;
    private static final int INPUT_FUEL = 1;
    private static final int SHIP_OUTPUT = 1;

    // Target ~2 spawns per day
    private static final float FLEET_INTERVAL_MIN = 7.0f;
    private static final float FLEET_INTERVAL_MAX = 9.0f;
    private static final float RING_MIN = 2800f;
    private static final float RING_MAX = 4520f;
    private static final float RING_MARGIN = 50f;
    private static final float TARGET_BURN = 20f;
    private static final float LOOKAHEAD_DEG = 25f;
    private static final float ARRIVAL_DEG_THRESHOLD = 5f;

    private static List<String> strikecraftHulls = null;
    private final Random random = new Random();
    private float spawnTimer = 0f;
    private float nextSpawnInterval = getNextSpawnInterval();

    @Override
    public boolean isFunctional() {
        return super.isFunctional() && market != null && "takeshido".equals(market.getFactionId());
    }

    @Override
    public void apply() {
        super.apply(true);

        demand(Commodities.ORE, INPUT_ORE);
        demand(Commodities.FUEL, INPUT_FUEL);
        supply(Commodities.SHIPS, SHIP_OUTPUT);
        applyDeficitToProduction(1, getMaxDeficit(Commodities.ORE, Commodities.FUEL), Commodities.SHIPS);

        market.getAccessibilityMod().modifyFlat(getModId(), ACCESSIBILITY_BONUS, getNameForModifier());
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
                .modifyFlat(getModId(), GROUND_DEF_BONUS, getNameForModifier());

        if (!isFunctional()) {
            supply.clear();
            unapply();
        }
    }

    @Override
    public void unapply() {
        super.unapply();
        if (market == null) return;
        market.getAccessibilityMod().unmodify(getModId());
        market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodify(getModId());
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        if (Global.getSector().getEconomy().isSimMode()) return;
        if (!isFunctional()) return;

        float days = amount;
        spawnTimer += days;
        while (spawnTimer >= nextSpawnInterval) {
            spawnTimer -= nextSpawnInterval;
            spawnRaceFleet();
            nextSpawnInterval = getNextSpawnInterval();
        }
    }

    private void spawnRaceFleet() {
        if (market == null) return;
        StarSystemAPI system = market.getStarSystem();
        if (system == null) return;
        String systemId = system.getId();
        String systemName = system.getName();
        boolean isMeiyo = "Meiyo".equals(systemId)
                || "takeshido_meiyo".equals(systemId)
                || (systemName != null && systemName.contains("Meiyo"));
        if (!isMeiyo) return;

        List<MarketAPI> candidates = getMeiyoMarkets(system);
        if (candidates.size() < 2) return;

        MarketAPI destination = pickDestination(candidates, market, random);
        if (destination == null) return;

        List<String> hulls = getStrikecraftHulls();
        if (hulls.isEmpty()) return;

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet("takeshido", FleetTypes.TRADE, null);
        if (fleet == null) return;

        fleet.setName("Takeshido Racers");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(true);

        FleetDataAPI data = fleet.getFleetData();
        int count = 3 + random.nextInt(4); // 3-6 ships
        for (int i = 0; i < count; i++) {
            String hullId = hulls.get(random.nextInt(hulls.size()));
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, hullId + "_standard");
            if (member == null) continue;
            data.addFleetMember(member);
            if (i == 0) data.setFlagship(member);
            member.updateStats();
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
        }

        if (data.getMembersListCopy().isEmpty()) return;

        market.getContainingLocation().addEntity(fleet);
        fleet.setLocation(market.getPrimaryEntity().getLocation().x, market.getPrimaryEntity().getLocation().y);
        fleet.setFacing(random.nextFloat() * 360f);

        float currentMinBurn = data.getMinBurnLevel();
        float burnDelta = TARGET_BURN - currentMinBurn;
        if (burnDelta != 0f) {
            fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("takeshido_raceways", burnDelta);
        }

        fleet.addScript(new RingChaseScript(fleet, system, market.getPrimaryEntity(), destination.getPrimaryEntity()));
    }

    private static List<MarketAPI> getMeiyoMarkets(StarSystemAPI system) {
        List<MarketAPI> out = new ArrayList<>();
        for (MarketAPI m : Global.getSector().getEconomy().getMarkets(system)) {
            if (m == null) continue;
            if (!"takeshido".equals(m.getFactionId())) continue;
            out.add(m);
        }
        return out;
    }

    private static MarketAPI pickDestination(List<MarketAPI> markets, MarketAPI current, Random random) {
        List<MarketAPI> options = new ArrayList<>();
        for (MarketAPI m : markets) {
            if (m == null || m == current) continue;
            options.add(m);
        }
        if (options.isEmpty()) return null;
        return options.get(random.nextInt(options.size()));
    }

    private static List<String> getStrikecraftHulls() {
        if (strikecraftHulls != null) return strikecraftHulls;
        strikecraftHulls = new ArrayList<>();
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec == null) continue;
            if (spec.hasTag("takeshidostrikecraft")) {
                strikecraftHulls.add(spec.getHullId());
            }
        }
        return strikecraftHulls;
    }

    private float getNextSpawnInterval() {
        return FLEET_INTERVAL_MIN + random.nextFloat() * (FLEET_INTERVAL_MAX - FLEET_INTERVAL_MIN);
    }

    private static float getAngleDeg(Vector2f from, Vector2f to) {
        float angle = (float) Math.toDegrees(Math.atan2(to.y - from.y, to.x - from.x));
        return normalizeAngle(angle);
    }

    private static float normalizeAngle(float angle) {
        angle %= 360f;
        if (angle < 0) angle += 360f;
        return angle;
    }

    private static float getDistance(Vector2f a, Vector2f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vector2f getPointOnRing(Vector2f center, float radius, float angleDeg) {
        float rad = (float) Math.toRadians(angleDeg);
        float x = center.x + (float) Math.cos(rad) * radius;
        float y = center.y + (float) Math.sin(rad) * radius;
        return new Vector2f(x, y);
    }

    private static final class RingChaseScript implements EveryFrameScript {
        private final CampaignFleetAPI fleet;
        private final StarSystemAPI system;
        private final SectorEntityToken origin;
        private final SectorEntityToken destination;
        private final float ringRadius;
        private final float startAngle;
        private final float destAngle;
        private final float direction;
        private boolean done = false;

        private RingChaseScript(CampaignFleetAPI fleet, StarSystemAPI system,
                                SectorEntityToken origin, SectorEntityToken destination) {
            this.fleet = fleet;
            this.system = system;
            this.origin = origin;
            this.destination = destination;
            SectorEntityToken star = system == null ? null : system.getStar();
            Vector2f starLoc = star == null ? new Vector2f(0f, 0f) : star.getLocation();
            float originRadius = getDistance(starLoc, origin.getLocation());
            this.ringRadius = clamp(originRadius, RING_MIN + RING_MARGIN, RING_MAX - RING_MARGIN);
            this.startAngle = getAngleDeg(starLoc, origin.getLocation());
            this.destAngle = getAngleDeg(starLoc, destination.getLocation());
            float diff = normalizeAngle(destAngle - startAngle);
            this.direction = diff <= 180f ? 1f : -1f;
            float currentMinBurn = fleet.getFleetData().getMinBurnLevel();
            float burnDelta = TARGET_BURN - currentMinBurn;
            if (burnDelta != 0f) {
                fleet.getStats().getFleetwideMaxBurnMod().modifyFlat("takeshido_raceways", burnDelta);
            }
            fleet.setDoNotAdvanceAI(true);
        }

        @Override
        public void advance(float amount) {
            if (done || fleet == null || system == null || destination == null) {
                done = true;
                return;
            }
            SectorEntityToken star = system.getStar();
            if (star == null) {
                done = true;
                return;
            }
            Vector2f starLoc = star.getLocation();
            float currentAngle = getAngleDeg(starLoc, fleet.getLocation());
            float diffToDest = normalizeAngle(destAngle - currentAngle);
            float arcLeft = direction > 0 ? diffToDest : 360f - diffToDest;
            if (arcLeft <= ARRIVAL_DEG_THRESHOLD) {
                fleet.setDoNotAdvanceAI(false);
                fleet.clearAssignments();
                fleet.addAssignment(FleetAssignment.GO_TO_LOCATION_AND_DESPAWN, destination, 200f,
                        "Racing to " + destination.getName());
                done = true;
                return;
            }
            float targetAngle = normalizeAngle(currentAngle + direction * LOOKAHEAD_DEG);
            Vector2f target = getPointOnRing(starLoc, ringRadius, targetAngle);
            fleet.setMoveDestinationOverride(target.x, target.y);
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public boolean runWhilePaused() {
            return false;
        }
    }

}
