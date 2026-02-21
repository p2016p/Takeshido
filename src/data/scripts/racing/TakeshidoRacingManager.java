package data.scripts.racing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.ListMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TakeshidoRacingManager {
    public static final String RACE_HULLMOD_ID = "takeshido_racemod";
    public static final String ACTIVE_RACE_ID_KEY = "takeshido_active_race_id";
    public static final String ACTIVE_TOURNAMENT_KEY = "takeshido_active_tournament";
    public static final String RACE_RESULT_PREFIX = "takeshido_race_result_";
    public static final String RACE_FLEET_MEMORY_KEY = "$takeshido_race_id";
    public static final String LAST_RACE_DIALOG_MESSAGE_KEY = "takeshido_last_race_dialog_message";
    public static final String ROSTER_FLEET_KEY = "takeshido_race_roster_fleet";
    public static final String ROSTER_FLEET_SIGNATURE_KEY = "takeshido_race_roster_signature";
    public static final float ROSTER_TARGET_CR = 0.7f;

    private static final Random RANDOM = new Random();
    private static final Map<String, RaceContext> ACTIVE_RACES = new HashMap<>();

    public static void ensureCampaignPlugin() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;
        sector.unregisterPlugin(TakeshidoRacingCampaignPlugin.PLUGIN_ID);
        sector.registerPlugin(new TakeshidoRacingCampaignPlugin());
    }

    public static void ensureRaceCoordinators() {
        SectorAPI sector = Global.getSector();
        if (sector == null) return;

        ensureRaceCoordinatorForMarket("takeshido_oashisu_market", "takeshido_racecoordinator_oashisu");
        ensureRaceCoordinatorForMarket("takeshido_nino_market", "takeshido_racecoordinator_nino");
        ensureRaceCoordinatorForMarket("takeshido_silverstone_market", "takeshido_racecoordinator_silverstone");
    }

    private static void ensureRaceCoordinatorForMarket(String marketId, String personId) {
        MarketAPI market = Global.getSector().getEconomy().getMarket(marketId);
        if (market == null) return;

        MemoryAPI mem = market.getMemoryWithoutUpdate();
        String key = "$takeshido_racecoordinator_id";
        if (mem.contains(key)) {
            String id = mem.getString(key);
            if (id != null && Global.getSector().getImportantPeople().getPerson(id) != null) return;
        }

        FactionAPI faction = market.getFaction();
        PersonAPI person = faction.createRandomPerson();
        person.setId(personId);
        person.setFaction(faction.getId());
        person.setRankId(Ranks.CITIZEN);
        person.setPostId(Ranks.POST_PORTMASTER);
        person.setImportance(PersonImportance.MEDIUM);
        person.setName(new FullName(market.getName(), "Race Coordinator", FullName.Gender.ANY));

        market.addPerson(person);
        market.getCommDirectory().addPerson(person);
        Global.getSector().getImportantPeople().addPerson(person);
        Global.getSector().getImportantPeople().getData(person).getLocation().setMarket(market);
        Global.getSector().getImportantPeople().checkOutPerson(person, "permanent_staff");

        mem.set(key, person.getId());
    }

    public static List<FleetMemberAPI> getEligiblePlayerShips(String categoryId) {
        TakeshidoRacingConfig.CategorySpec category = TakeshidoRacingConfig.get().getCategory(categoryId);
        if (category == null) return Collections.emptyList();
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return Collections.emptyList();

        List<FleetMemberAPI> eligible = new ArrayList<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member == null || member.isFighterWing()) continue;
            ShipHullSpecAPI spec = member.getHullSpec();
            if (spec == null) continue;
            if (!spec.getHullId().startsWith("takeshido_")) continue;
            if (category.matchesDesignation(spec.getDesignation())) {
                eligible.add(member);
            }
        }
        return eligible;
    }

    public static TournamentState getTournamentState() {
        Object data = Global.getSector().getPersistentData().get(ACTIVE_TOURNAMENT_KEY);
        if (data instanceof TournamentState) {
            TournamentState state = (TournamentState) data;
            if (state.nextRaceDay <= 0) {
                CampaignClockAPI clock = Global.getSector().getClock();
                if (clock != null) {
                    state.nextRaceDay = getAbsoluteDay(clock) + 7;
                }
            }
            return state;
        }
        return null;
    }

    public static void startImpromptuRace(InteractionDialogAPI dialog, String categoryId, FleetMemberAPI playerShip) {
        if (dialog == null || playerShip == null) return;

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        RaceContext ctx = new RaceContext();
        ctx.raceId = Global.getSector().genUID();
        ctx.type = RaceType.IMPROMPTU;
        ctx.categoryId = categoryId;
        ctx.lapsToWin = 1;
        ctx.expectedRacers = Math.max(2, config.tournament.racersPerRace);
        ctx.playerRacerId = "player";
        ctx.playerFleetMember = playerShip;
        ctx.playerOriginalCaptain = playerShip.getCaptain();
        ctx.playerWasFlagship = playerShip.isFlagship();
        ctx.playerShipName = playerShip.getShipName();
        ctx.playerMemberId = playerShip.getId();

        MarketAPI market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
        String marketId = market != null ? market.getId() : null;
        ctx.marketId = marketId;

        List<String> tracks = config.getTracksForMarket(marketId);
        ctx.trackId = pickRandom(tracks, "austin");

        ctx.aiRacers.addAll(generateAIRacers(categoryId, ctx.expectedRacers - 1));
        if (ctx.aiRacers.isEmpty()) {
            dialog.getTextPanel().addParagraph("No eligible AI racers found for this class.");
            return;
        }
        ctx.expectedRacers = 1 + ctx.aiRacers.size();

        for (RacerDefinition def : ctx.aiRacers) {
            if (def.member != null) {
                ctx.aiFleetMembers.add(def.member);
            }
        }

        beginRace(dialog, ctx);
    }

    public static void startTournament(InteractionDialogAPI dialog, String categoryId) {
        if (dialog == null) return;
        if (getTournamentState() != null) return;

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        TournamentState state = new TournamentState();
        state.tournamentId = Global.getSector().genUID();
        state.categoryId = categoryId;
        state.racesPerTournament = config.tournament.racesPerTournament;
        state.racersPerRace = Math.max(2, config.tournament.racersPerRace);
        state.currentRaceIndex = 0;

        state.trackIds = buildTrackList(config.tournament.trackListDefault, state.racesPerTournament);
        if (state.trackIds.isEmpty()) {
            state.trackIds.add("austin");
        }

        TournamentRacer player = new TournamentRacer();
        player.racerId = "player";
        player.isPlayer = true;
        player.name = Global.getSector().getPlayerPerson().getName().getFullName();
        state.racers.add(player);

        List<RacerDefinition> ai = generateAIRacers(categoryId, state.racersPerRace - 1);
        if (ai.isEmpty()) {
            dialog.getTextPanel().addParagraph("No eligible AI racers found for this class.");
            return;
        }
        for (RacerDefinition def : ai) {
            TournamentRacer r = new TournamentRacer();
            r.racerId = def.racerId;
            r.name = def.name;
            r.variantId = def.variantId;
            r.hullId = def.hullId;
            r.skill = def.skill;
            state.racers.add(r);
        }

        CampaignClockAPI clock = Global.getSector().getClock();
        state.nextRaceTimestamp = clock.getTimestamp();
        state.nextRaceDay = getAbsoluteDay(clock) + 7;
        state.nextMarketId = config.findMarketForTrack(state.trackIds.get(0));
        if (state.nextMarketId == null) {
            MarketAPI market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
            if (market != null) state.nextMarketId = market.getId();
        }

        Global.getSector().getPersistentData().put(ACTIVE_TOURNAMENT_KEY, state);

        IntelManagerAPI intel = Global.getSector().getIntelManager();
        intel.addIntel(new TakeshidoTournamentIntel(state.tournamentId), true);
    }

    public static void startTournamentRace(InteractionDialogAPI dialog, TournamentState state, FleetMemberAPI playerShip) {
        if (dialog == null || state == null || playerShip == null) return;
        if (state.currentRaceIndex < 0 || state.currentRaceIndex >= state.trackIds.size()) return;

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        RaceContext ctx = new RaceContext();
        ctx.raceId = Global.getSector().genUID();
        ctx.type = RaceType.TOURNAMENT;
        ctx.tournamentId = state.tournamentId;
        ctx.categoryId = state.categoryId;
        ctx.lapsToWin = 1;
        ctx.expectedRacers = Math.max(2, state.racersPerRace);
        ctx.playerRacerId = "player";
        ctx.playerFleetMember = playerShip;
        ctx.playerOriginalCaptain = playerShip.getCaptain();
        ctx.playerWasFlagship = playerShip.isFlagship();
        ctx.playerShipName = playerShip.getShipName();
        ctx.playerMemberId = playerShip.getId();
        ctx.trackId = state.trackIds.get(state.currentRaceIndex);
        ctx.marketId = state.nextMarketId;

        for (TournamentRacer r : state.racers) {
            if (r.isPlayer) continue;
            RacerDefinition def = new RacerDefinition();
            def.racerId = r.racerId;
            def.name = r.name;
            def.variantId = r.variantId != null ? r.variantId : r.hullId;
            def.hullId = r.hullId;
            def.skill = r.skill;
            CampaignFleetAPI roster = getOrCreateRosterFleet();
            if (roster != null) {
                FleetMemberAPI member = getRosterMember(roster, def.racerId, def.variantId);
                if (member != null) {
                    def.member = member;
                    def.captain = member.getCaptain();
                }
            }
            ctx.aiRacers.add(def);
        }
        if (ctx.aiRacers.isEmpty()) return;
        ctx.expectedRacers = 1 + ctx.aiRacers.size();

        for (RacerDefinition def : ctx.aiRacers) {
            if (def.member != null) {
                ctx.aiFleetMembers.add(def.member);
            }
        }

        beginRace(dialog, ctx);
    }

    public static boolean isTournamentRaceAvailable(TournamentState state, MarketAPI market) {
        if (state == null) return false;
        CampaignClockAPI clock = Global.getSector().getClock();
        boolean timeReady = clock != null && getAbsoluteDay(clock) >= state.nextRaceDay;
        boolean locationReady = market != null && state.nextMarketId != null && state.nextMarketId.equals(market.getId());
        return timeReady && locationReady;
    }

    public static void beginRace(InteractionDialogAPI dialog, RaceContext ctx) {
        if (dialog == null || ctx == null) return;

        ensureCampaignPlugin();
        ACTIVE_RACES.put(ctx.raceId, ctx);
        Global.getSector().getPersistentData().put(ACTIVE_RACE_ID_KEY, ctx.raceId);

        CampaignFleetAPI playerFleet = Global.getFactory().createEmptyFleet(Global.getSector().getPlayerFaction(), true);
        CampaignFleetAPI enemyFleet = Global.getFactory().createEmptyFleet(Global.getSector().getFaction("takeshido"), false);

        SectorEntityToken target = dialog.getInteractionTarget();
        if (target != null) {
            playerFleet.setContainingLocation(target.getContainingLocation());
            enemyFleet.setContainingLocation(target.getContainingLocation());
            playerFleet.setLocation(target.getLocation().x, target.getLocation().y);
            enemyFleet.setLocation(target.getLocation().x, target.getLocation().y);
        }

        enemyFleet.getMemoryWithoutUpdate().set(RACE_FLEET_MEMORY_KEY, ctx.raceId);
        enemyFleet.getMemoryWithoutUpdate().set("$isRaceFleet", true);

        Global.getSector().addTransientListener(new TakeshidoRaceBattleListener(ctx.raceId));

        // Ensure fleets are in-play and targetable by the dialog battle flow.
        if (target != null && target.getContainingLocation() != null) {
            target.getContainingLocation().addEntity(playerFleet);
            target.getContainingLocation().addEntity(enemyFleet);
        } else if (Global.getSector().getPlayerFleet() != null) {
            Global.getSector().getPlayerFleet().getContainingLocation().addEntity(playerFleet);
            Global.getSector().getPlayerFleet().getContainingLocation().addEntity(enemyFleet);
        }

        com.fs.starfarer.api.combat.BattleCreationContext bcc =
                new com.fs.starfarer.api.combat.BattleCreationContext(playerFleet, FleetGoal.ATTACK, enemyFleet, FleetGoal.ATTACK);
        bcc.setPlayerCommandPoints((int) Global.getSector().getPlayerFleet().getCommanderStats().getCommandPoints().getModifiedValue());
        bcc.objectivesAllowed = false;
        bcc.aiRetreatAllowed = false;
        bcc.fightToTheLast = true;
        bcc.enemyDeployAll = true;

        dialog.getVisualPanel().fadeVisualOut();
        dialog.startBattle(bcc);
    }

    public static RaceContext getActiveRace(String raceId) {
        return ACTIVE_RACES.get(raceId);
    }

    public static void storeRaceResult(String raceId, List<String> finishOrder, List<String> dnfMemberIds, boolean endedByTimeout, boolean endedByPlayerDNF) {
        if (raceId == null) return;
        RaceResult result = new RaceResult();
        result.raceId = raceId;
        result.finishOrderMemberIds = new ArrayList<>(finishOrder);
        if (dnfMemberIds != null) {
            result.dnfMemberIds = new ArrayList<>(dnfMemberIds);
        }
        result.endedByTimeout = endedByTimeout;
        result.endedByPlayerDNF = endedByPlayerDNF;
        Global.getSector().getPersistentData().put(RACE_RESULT_PREFIX + raceId, result);
    }

    public static RaceResult getRaceResult(String raceId) {
        if (raceId == null) return null;
        Object data = Global.getSector().getPersistentData().get(RACE_RESULT_PREFIX + raceId);
        if (data instanceof RaceResult) return (RaceResult) data;
        return null;
    }

    public static void clearRaceResult(String raceId) {
        if (raceId == null) return;
        Global.getSector().getPersistentData().remove(RACE_RESULT_PREFIX + raceId);
    }

    public static void processRaceResult(String raceId) {
        RaceContext ctx = ACTIVE_RACES.remove(raceId);
        Global.getSector().getPersistentData().remove(ACTIVE_RACE_ID_KEY);
        if (ctx == null) return;

        RaceResult result = getRaceResult(raceId);
        restorePlayerState(ctx);
        restoreRosterMembers(ctx);
        if (result == null) return;

        if (ctx.type == RaceType.IMPROMPTU) {
            handleImpromptuResult(ctx, result);
        } else if (ctx.type == RaceType.TOURNAMENT) {
            handleTournamentResult(ctx, result);
        }

        clearRaceResult(raceId);
    }

    private static void handleImpromptuResult(RaceContext ctx, RaceResult result) {
        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        List<String> classified = getClassifiedFinishOrder(result);
        int position = getPlacement(classified, ctx.playerMemberId);
        int reward = getRewardForPosition(config.rewards.impromptu, position);
        if (reward > 0) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(reward);
            String msg = "Impromptu race result: " + positionString(position) + ". Payout: " + reward + " credits.";
            Global.getSector().getCampaignUI().addMessage(msg);
            setLastDialogMessage(msg);
        } else {
            String msg = "Impromptu race result: " + positionString(position) + ". No payout.";
            Global.getSector().getCampaignUI().addMessage(msg);
            setLastDialogMessage(msg);
        }
    }

    private static void handleTournamentResult(RaceContext ctx, RaceResult result) {
        TournamentState state = getTournamentState();
        if (state == null || !state.tournamentId.equals(ctx.tournamentId)) return;

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        List<Integer> points = config.tournament.points;

        List<String> classified = getClassifiedFinishOrder(result);
        for (int i = 0; i < classified.size(); i++) {
            String memberId = classified.get(i);
            String racerId = ctx.memberIdToRacerId.get(memberId);
            if (racerId == null) continue;
            TournamentRacer racer = state.getRacer(racerId);
            if (racer == null) continue;
            int pts = (i < points.size()) ? points.get(i) : 0;
            racer.points += pts;
        }

        int playerPos = getPlacement(classified, ctx.playerMemberId);
        String msg = "Tournament race result: " + positionString(playerPos) + ".";
        Global.getSector().getCampaignUI().addMessage(msg);
        setLastDialogMessage(msg);

        state.currentRaceIndex++;
        if (state.currentRaceIndex >= state.racesPerTournament) {
            finalizeTournament(state);
            return;
        }

        scheduleNextTournamentRace(state);
        sendTournamentUpdate(state, "Race completed");
        Global.getSector().getPersistentData().put(ACTIVE_TOURNAMENT_KEY, state);
    }

    private static void restorePlayerState(RaceContext ctx) {
        if (ctx == null || ctx.playerFleetMember == null) return;
        FleetMemberAPI member = ctx.playerFleetMember;
        if (ctx.playerOriginalCaptain != null) {
            member.setCaptain(ctx.playerOriginalCaptain);
        }
        member.setFlagship(ctx.playerWasFlagship);
    }

    private static void restoreRosterMembers(RaceContext ctx) {
        if (ctx == null || ctx.aiFleetMembers.isEmpty()) return;
        for (FleetMemberAPI member : ctx.aiFleetMembers) {
            resetRosterMember(member);
        }
    }

    private static void finalizeTournament(TournamentState state) {
        List<TournamentRacer> standings = new ArrayList<>(state.racers);
        Collections.sort(standings, new Comparator<TournamentRacer>() {
            @Override
            public int compare(TournamentRacer a, TournamentRacer b) {
                return Integer.compare(b.points, a.points);
            }
        });

        int playerIndex = -1;
        for (int i = 0; i < standings.size(); i++) {
            if (standings.get(i).isPlayer) {
                playerIndex = i;
                break;
            }
        }

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        int reward = getRewardForPosition(config.rewards.tournament, playerIndex >= 0 ? playerIndex + 1 : -1);
        if (reward > 0) {
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(reward);
            String msg = "Tournament complete: " + positionString(playerIndex + 1) + ". Prize: " + reward + " credits.";
            Global.getSector().getCampaignUI().addMessage(msg);
            setLastDialogMessage(msg);
        } else {
            String msg = "Tournament complete: " + positionString(playerIndex + 1) + ".";
            Global.getSector().getCampaignUI().addMessage(msg);
            setLastDialogMessage(msg);
        }

        sendTournamentUpdate(state, "Tournament complete");
        endTournamentIntel(state);

        Global.getSector().getPersistentData().remove(ACTIVE_TOURNAMENT_KEY);
    }

    private static void scheduleNextTournamentRace(TournamentState state) {
        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        String nextTrack = state.trackIds.get(state.currentRaceIndex);
        String nextMarket = config.findMarketForTrack(nextTrack);
        if (nextMarket == null) nextMarket = state.nextMarketId;

        CampaignClockAPI clock = Global.getSector().getClock();
        float days = 7f;
        state.nextRaceTimestamp = clock.getTimestamp();
        state.nextRaceDay = getAbsoluteDay(clock) + (int) days;
        state.nextMarketId = nextMarket;
    }

    private static int getAbsoluteDay(CampaignClockAPI clock) {
        if (clock == null) return 0;
        int cycle = clock.getCycle();
        int month = clock.getMonth();
        int day = clock.getDay();
        if (month < 1) month = 1;
        if (day < 1) day = 1;
        return (cycle * 360) + ((month - 1) * 30) + (day - 1);
    }



    private static void sendTournamentUpdate(TournamentState state, String reason) {
        if (state == null) return;
        IntelManagerAPI intel = Global.getSector().getIntelManager();
        for (Object obj : intel.getIntel(TakeshidoTournamentIntel.class)) {
            if (obj instanceof TakeshidoTournamentIntel) {
                TakeshidoTournamentIntel t = (TakeshidoTournamentIntel) obj;
                if (state.tournamentId.equals(t.getTournamentId())) {
                    t.sendUpdateIfPlayerHasIntel(reason, false);
                    break;
                }
            }
        }
    }

    private static void endTournamentIntel(TournamentState state) {
        if (state == null) return;
        IntelManagerAPI intel = Global.getSector().getIntelManager();
        for (Object obj : intel.getIntel(TakeshidoTournamentIntel.class)) {
            if (obj instanceof TakeshidoTournamentIntel) {
                TakeshidoTournamentIntel t = (TakeshidoTournamentIntel) obj;
                if (state.tournamentId.equals(t.getTournamentId())) {
                    intel.removeIntel(t);
                    break;
                }
            }
        }
    }

    private static int getPlacement(List<String> finishOrder, String memberId) {
        if (finishOrder == null || memberId == null) return -1;
        for (int i = 0; i < finishOrder.size(); i++) {
            if (memberId.equals(finishOrder.get(i))) return i + 1;
        }
        return -1;
    }

    private static int getRewardForPosition(List<Integer> rewards, int position) {
        if (position <= 0 || rewards == null) return 0;
        if (position > rewards.size()) return 0;
        return rewards.get(position - 1);
    }

    private static String positionString(int position) {
        if (position <= 0) return "DNF";
        if (position == 1) return "1st";
        if (position == 2) return "2nd";
        if (position == 3) return "3rd";
        return position + "th";
    }

    private static void setLastDialogMessage(String message) {
        if (message == null || message.trim().isEmpty()) return;
        Global.getSector().getPersistentData().put(LAST_RACE_DIALOG_MESSAGE_KEY, message);
    }

    private static List<String> getClassifiedFinishOrder(RaceResult result) {
        List<String> classified = new ArrayList<>();
        if (result == null || result.finishOrderMemberIds == null) return classified;
        classified.addAll(result.finishOrderMemberIds);
        if (result.dnfMemberIds != null && !result.dnfMemberIds.isEmpty()) {
            classified.removeAll(result.dnfMemberIds);
        }
        return classified;
    }

    private static List<String> buildTrackList(List<String> source, int count) {
        List<String> out = new ArrayList<>();
        if (source == null || source.isEmpty()) return out;
        for (int i = 0; i < count; i++) {
            out.add(source.get(i % source.size()));
        }
        return out;
    }

    private static String pickRandom(List<String> list, String fallback) {
        if (list == null || list.isEmpty()) return fallback;
        return list.get(RANDOM.nextInt(list.size()));
    }

    public static ShipVariantAPI cloneVariantWithRaceHullmod(ShipVariantAPI base) {
        if (base == null) return null;
        ShipVariantAPI variant = base.clone();
        if (!variant.hasHullMod(RACE_HULLMOD_ID)) {
            variant.addMod(RACE_HULLMOD_ID);
        }
        return variant;
    }

    public static FleetMemberAPI createFleetMemberForHull(String hullId, String shipName) {
        ShipVariantAPI variant = createVariantForHull(hullId);
        if (variant == null) return null;
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        if (shipName != null) member.setShipName(shipName);
        return member;
    }

    public static ShipVariantAPI createVariantForHull(String hullId) {
        if (hullId == null) return null;
        ListMap<String> map = Global.getSettings().getHullIdToVariantListMap();
        List<String> variants = map != null ? map.getList(hullId) : null;
        String variantId = null;
        if (variants != null && !variants.isEmpty()) {
            for (String id : variants) {
                if (id != null && id.endsWith("_standard")) {
                    variantId = id;
                    break;
                }
            }
            if (variantId == null) variantId = variants.get(0);
        }

        if (variantId != null && Global.getSettings().doesVariantExist(variantId)) {
            ShipVariantAPI variant = Global.getSettings().getVariant(variantId).clone();
            if (!variant.hasHullMod(RACE_HULLMOD_ID)) {
                variant.addMod(RACE_HULLMOD_ID);
            }
            return variant;
        }

        ShipHullSpecAPI spec = Global.getSettings().getHullSpec(hullId);
        if (spec == null) return null;
        ShipVariantAPI variant = Global.getSettings().createEmptyVariant(hullId + "_Hull", spec);
        variant.addMod(RACE_HULLMOD_ID);
        return variant;
    }

    private static String getDesignationForVariant(String variantId) {
        if (variantId == null || variantId.trim().isEmpty()) return null;
        if (!Global.getSettings().doesVariantExist(variantId)) return null;
        ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
        if (variant == null || variant.getHullSpec() == null) return null;
        return variant.getHullSpec().getDesignation();
    }

    public static FleetMemberAPI createFleetMemberForVariant(String variantId, String shipName) {
        if (variantId == null || variantId.trim().isEmpty()) return null;
        if (!Global.getSettings().doesVariantExist(variantId)) return null;
        ShipVariantAPI variant = Global.getSettings().getVariant(variantId).clone();
        if (!variant.hasHullMod(RACE_HULLMOD_ID)) {
            variant.addMod(RACE_HULLMOD_ID);
        }
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
        if (shipName != null) member.setShipName(shipName);
        return member;
    }

    private static List<RacerDefinition> generateAIRacers(String categoryId, int count) {
        TakeshidoRacingConfig.CategorySpec category = TakeshidoRacingConfig.get().getCategory(categoryId);
        if (category == null) return Collections.emptyList();

        List<RacerDefinition> fromRoster = generateAIRacersFromRoster(categoryId, count);
        if (!fromRoster.isEmpty()) return fromRoster;

        List<String> eligible = new ArrayList<>();
        for (ShipHullSpecAPI spec : Global.getSettings().getAllShipHullSpecs()) {
            if (spec == null) continue;
            if (!spec.getHullId().startsWith("takeshido_")) continue;
            if (!category.matchesDesignation(spec.getDesignation())) continue;
            eligible.add(spec.getHullId());
        }

        if (eligible.isEmpty()) return Collections.emptyList();

        List<RacerDefinition> racers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RacerDefinition def = new RacerDefinition();
            def.name = "Racer " + (i + 1);
            def.racerId = def.name;
            def.hullId = eligible.get(RANDOM.nextInt(eligible.size()));
            def.skill = 0.6f + RANDOM.nextFloat() * 0.3f;
            def.captain = createRaceCaptain(def.name, null);
            racers.add(def);
        }
        return racers;
    }

    private static List<RacerDefinition> generateAIRacersFromRoster(String categoryId, int count) {
        TakeshidoRacingConfig cfg = TakeshidoRacingConfig.get();
        if (cfg.racers.isEmpty()) return Collections.emptyList();

        CampaignFleetAPI roster = getOrCreateRosterFleet();
        if (roster == null) return Collections.emptyList();

        List<TakeshidoRacingConfig.RacerProfile> pool = new ArrayList<>();
        for (TakeshidoRacingConfig.RacerProfile rp : cfg.racers) {
            if (rp == null || rp.variantId == null || rp.variantId.isEmpty()) continue;
            String designation = getDesignationForVariant(rp.variantId);
            TakeshidoRacingConfig.CategorySpec cat = TakeshidoRacingConfig.get().getCategory(categoryId);
            if (cat != null && designation != null && cat.matchesDesignation(designation)) {
                pool.add(rp);
            }
        }
        if (pool.isEmpty()) return Collections.emptyList();

        Collections.shuffle(pool, RANDOM);
        List<RacerDefinition> racers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TakeshidoRacingConfig.RacerProfile rp = pool.get(i % pool.size());
            RacerDefinition def = new RacerDefinition();
            def.name = rp.name != null && !rp.name.isEmpty() ? rp.name : ("Racer " + (i + 1));
            def.racerId = def.name;
            def.variantId = rp.variantId;
            def.skill = rp.skill > 0f ? rp.skill : 0.7f;
            FleetMemberAPI member = getRosterMember(roster, def.racerId, def.variantId);
            if (member != null) {
                def.member = member;
                def.captain = member.getCaptain();
                if (def.captain == null) {
                    def.captain = createRaceCaptain(rp);
                }
            } else {
                def.captain = createRaceCaptain(rp);
            }
            racers.add(def);
        }
        return racers;
    }

    private static CampaignFleetAPI getOrCreateRosterFleet() {
        Object data = Global.getSector().getPersistentData().get(ROSTER_FLEET_KEY);
        Object sigData = Global.getSector().getPersistentData().get(ROSTER_FLEET_SIGNATURE_KEY);
        String desiredSignature = buildRosterSignature();
        if (data instanceof CampaignFleetAPI && sigData instanceof String) {
            if (desiredSignature != null && desiredSignature.equals(sigData)) {
                return (CampaignFleetAPI) data;
            }
        }

        FactionAPI faction = Global.getSector().getFaction("takeshido");
        CampaignFleetAPI fleet = Global.getFactory().createEmptyFleet(faction, true);
        fleet.setName("Takeshido Race Roster");
        fleet.setNoFactionInName(true);
        fleet.setTransponderOn(false);

        FleetDataAPI fleetData = fleet.getFleetData();
        int index = 0;
        for (TakeshidoRacingConfig.RacerProfile rp : TakeshidoRacingConfig.get().racers) {
            if (rp == null || rp.variantId == null || rp.variantId.isEmpty()) continue;
            FleetMemberAPI member = createFleetMemberForVariant(rp.variantId, rp.name);
            if (member == null) continue;
            member.setId("roster_" + index);
            PersonAPI captain = createRaceCaptain(rp);
            if (captain != null) member.setCaptain(captain);
            fleetData.addFleetMember(member);
            resetRosterMember(member);
            index++;
        }

        Global.getSector().getPersistentData().put(ROSTER_FLEET_KEY, fleet);
        Global.getSector().getPersistentData().put(ROSTER_FLEET_SIGNATURE_KEY, desiredSignature);
        return fleet;
    }

    private static String buildRosterSignature() {
        StringBuilder sb = new StringBuilder();
        for (TakeshidoRacingConfig.RacerProfile rp : TakeshidoRacingConfig.get().racers) {
            if (rp == null) continue;
            if (rp.variantId == null || rp.variantId.trim().isEmpty()) continue;
            if (sb.length() > 0) sb.append(";");
            sb.append(rp.name != null ? rp.name.trim() : "");
            sb.append("|").append(rp.variantId.trim());
            sb.append("|").append(rp.personality != null ? rp.personality.trim() : "");
            sb.append("|").append(rp.skill);
            sb.append("|").append(rp.officerLevel);
            if (rp.skills != null && !rp.skills.isEmpty()) {
                sb.append("|");
                for (int i = 0; i < rp.skills.size(); i++) {
                    TakeshidoRacingConfig.RacerSkill skill = rp.skills.get(i);
                    if (skill == null || skill.id == null) continue;
                    if (i > 0) sb.append(",");
                    sb.append(skill.id.trim()).append(":").append(skill.level);
                }
            }
        }
        return sb.toString();
    }

    private static FleetMemberAPI getRosterMember(CampaignFleetAPI roster, String racerName, String variantId) {
        if (roster == null) return null;
        List<FleetMemberAPI> members = roster.getFleetData().getMembersListCopy();
        if (racerName != null && !racerName.isEmpty()) {
            for (FleetMemberAPI m : members) {
                if (m != null && m.getCaptain() != null && racerName.equals(m.getCaptain().getName().getFullName())) return m;
            }
        }
        if (variantId != null && !variantId.isEmpty()) {
            for (FleetMemberAPI m : members) {
                if (m != null && m.getVariant() != null && variantId.equals(m.getVariant().getHullVariantId())) return m;
            }
        }
        return null;
    }

    private static void resetRosterMember(FleetMemberAPI member) {
        if (member == null) return;
        member.setFlagship(false);
        if (member.getStatus() != null) {
            member.getStatus().repairFully();
            member.getStatus().setHullFraction(1f);
            member.getStatus().repairArmorAllCells(1f);
            member.getStatus().resetDamageTaken();
        }
        if (member.getCrewComposition() != null) {
            float crew = member.getNeededCrew();
            if (crew <= 0f) crew = member.getMinCrew();
            if (crew <= 0f) crew = 1f;
            member.getCrewComposition().setCrew(crew);
        }
        if (member.getRepairTracker() != null) {
            member.getRepairTracker().setMothballed(false);
            member.getRepairTracker().setCrashMothballed(false);
            float max = member.getRepairTracker().getMaxCR();
            float target = ROSTER_TARGET_CR;
            if (max > 0f && target > max) target = max;
            member.getRepairTracker().setCR(target);
            member.getRepairTracker().setCRPriorToMothballing(target);
        }
        member.updateStats();
        member.setStatUpdateNeeded(true);
    }

    private static PersonAPI createRaceCaptain(String name, String personality) {
        TakeshidoRacingConfig.RacerProfile profile = new TakeshidoRacingConfig.RacerProfile();
        profile.name = name;
        profile.personality = personality;
        return createRaceCaptain(profile);
    }

    private static PersonAPI createRaceCaptain(TakeshidoRacingConfig.RacerProfile profile) {
        FactionAPI faction = Global.getSector() != null ? Global.getSector().getFaction("takeshido") : null;
        FactionAPI fallback = Global.getSector() != null ? Global.getSector().getPlayerFaction() : null;
        PersonAPI person = faction != null ? faction.createRandomPerson() : (fallback != null ? fallback.createRandomPerson() : null);
        if (person == null) return null;
        if (profile.personality != null && !profile.personality.trim().isEmpty()) {
            person.setPersonality(profile.personality.trim());
        } else {
            person.setPersonality(Personalities.STEADY);
        }
        person.setRankId(Ranks.SPACE_CAPTAIN);
        if (profile.name != null && !profile.name.trim().isEmpty()) {
            person.setName(makeName(profile.name.trim()));
        }
        if (profile.officerLevel > 0) {
            person.getStats().setLevel(profile.officerLevel);
        }
        if (profile.skills != null) {
            for (TakeshidoRacingConfig.RacerSkill skill : profile.skills) {
                if (skill == null || skill.id == null || skill.id.trim().isEmpty()) continue;
                person.getStats().setSkillLevel(skill.id.trim(), skill.level);
            }
        }
        return person;
    }

    private static FullName makeName(String name) {
        String first = name;
        String last = "";
        int idx = name.indexOf(' ');
        if (idx > 0 && idx < name.length() - 1) {
            first = name.substring(0, idx);
            last = name.substring(idx + 1);
        }
        return new FullName(first, last, FullName.Gender.ANY);
    }

    public static class RaceContext {
        public String raceId;
        public RaceType type;
        public String categoryId;
        public String trackId;
        public int lapsToWin;
        public int expectedRacers;
        public String marketId;
        public String tournamentId;
        public String playerRacerId;
        public String playerMemberId;
        public String playerShipName;
        public FleetMemberAPI playerFleetMember;
        public PersonAPI playerOriginalCaptain;
        public boolean playerWasFlagship;
        public final List<RacerDefinition> aiRacers = new ArrayList<>();
        public final List<FleetMemberAPI> aiFleetMembers = new ArrayList<>();
        public final Map<String, String> memberIdToRacerId = new LinkedHashMap<>();
    }

    public enum RaceType {
        IMPROMPTU,
        TOURNAMENT
    }

    public static class RacerDefinition {
        public String racerId;
        public String name;
        public String hullId;
        public String variantId;
        public float skill;
        public PersonAPI captain;
        public FleetMemberAPI member;
    }

    public static class RaceResult {
        public String raceId;
        public List<String> finishOrderMemberIds = new ArrayList<>();
        public List<String> dnfMemberIds = new ArrayList<>();
        public boolean endedByTimeout = false;
        public boolean endedByPlayerDNF = false;
    }

    public static class TournamentState {
        public String tournamentId;
        public String categoryId;
        public List<String> trackIds = new ArrayList<>();
        public int racesPerTournament;
        public int racersPerRace;
        public int currentRaceIndex;
        public long nextRaceTimestamp;
        public int nextRaceDay;
        public String nextMarketId;
        public final List<TournamentRacer> racers = new ArrayList<>();

        public TournamentRacer getRacer(String racerId) {
            if (racerId == null) return null;
            for (TournamentRacer r : racers) {
                if (racerId.equals(r.racerId)) return r;
            }
            return null;
        }
    }

    public static class TournamentRacer {
        public String racerId;
        public String name;
        public String hullId;
        public String variantId;
        public float skill;
        public int points;
        public boolean isPlayer;
    }

    public static class TakeshidoRaceBattleListener extends BaseCampaignEventListener {
        private final String raceId;

        public TakeshidoRaceBattleListener(String raceId) {
            super(false);
            this.raceId = raceId;
        }

        @Override
        public void reportBattleFinished(CampaignFleetAPI fleet, BattleAPI battle) {
            processRaceResult(raceId);
            Global.getSector().removeListener(this);
        }
    }
}
