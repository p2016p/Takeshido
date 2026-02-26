package data.scripts.utils.interactionUI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import data.scripts.racing.TakeshidoRacingConfig;
import data.scripts.racing.TakeshidoRacingManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class takeshido_RacecoordinatorDialog implements InteractionDialogPlugin {

    private static final String COORDINATOR_BIFF = "takeshido_racecoordinator_silverstone";
    private static final String COORDINATOR_BURT = "takeshido_racecoordinator_nino";
    private static final String COORDINATOR_LIZZIE = "takeshido_racecoordinator_oashisu";

    private enum CoordinatorFlavor {
        BIFF,
        BURT,
        LIZZIE,
        GENERIC
    }
    public static enum OptionId {
        INIT,
        CONT,
        IMPROMPTU,
        TOURNAMENT,
        BET,
        ABOUT_IMPROMPTU,
        ABOUT_TOURNAMENT,
        ABOUT_BETTING,
        CATEGORY_SPORT,
        CATEGORY_SUPER,
        CATEGORY_HYPER,
        START_TOURNAMENT,
        START_NEXT_RACE,
        VIEW_STATUS,
        BET_CATEGORY_SPORT,
        BET_CATEGORY_SUPER,
        BET_CATEGORY_HYPER,
        BET_BACK_RACERS,
        BET_BACK_AMOUNT,
        BET_CONFIRM
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    protected Map<String, MemoryAPI> memoryMap;
    protected boolean selectingTournament = false;
    protected boolean greeted = false;
    protected String betCategoryId;
    protected List<TakeshidoRacingManager.RacerDefinition> betRacers = new ArrayList<>();
    protected Map<String, Float> betOddsByRacerId = new HashMap<>();
    protected TakeshidoRacingManager.RacerDefinition betChoice;
    protected int betAmount = 0;
    protected float betOdds = 1f;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        memoryMap = new HashMap<String, MemoryAPI>();

        showCoordinator();
        optionSelected(null, OptionId.INIT);
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }

    public void backFromEngagement(com.fs.starfarer.api.combat.EngagementResultAPI result) {
        showRaceResultMessage();
        showMainMenu();
    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        if (optionData instanceof TakeshidoRacingManager.RacerDefinition) {
            handleBetRacerSelected((TakeshidoRacingManager.RacerDefinition) optionData);
            return;
        }
        if (optionData instanceof Integer) {
            handleBetAmountSelected((Integer) optionData);
            return;
        }

        OptionId option = (OptionId) optionData;
        switch (option) {
            case INIT:
                showMainMenu();
                break;
            case IMPROMPTU:
                showImpromptuSignupText();
                showCategoryMenu(false);
                break;
            case TOURNAMENT:
                showTournamentSignupText();
                showCategoryMenu(true);
                break;
            case BET:
                showBetting();
                break;
            case BET_CATEGORY_SPORT:
                showBetRacerList("sportscar");
                break;
            case BET_CATEGORY_SUPER:
                showBetRacerList("supercar");
                break;
            case BET_CATEGORY_HYPER:
                showBetRacerList("hypercar");
                break;
            case BET_BACK_RACERS:
                showBetRacerList(betCategoryId);
                break;
            case BET_BACK_AMOUNT:
                showBetAmountMenu();
                break;
            case BET_CONFIRM:
                confirmBet();
                break;
            case ABOUT_IMPROMPTU:
                showImpromptuInfo();
                break;
            case ABOUT_TOURNAMENT:
                showTournamentInfo();
                break;
            case ABOUT_BETTING:
                showBettingInfo();
                break;
            case CATEGORY_SPORT:
                handleCategorySelected("sportscar");
                break;
            case CATEGORY_SUPER:
                handleCategorySelected("supercar");
                break;
            case CATEGORY_HYPER:
                handleCategorySelected("hypercar");
                break;
            case START_NEXT_RACE:
                startNextTournamentRace();
                break;
            case VIEW_STATUS:
                showTournamentStatus();
                break;
            case CONT:
                closeDialog();
                break;
            default:
                showMainMenu();
                break;
        }
    }

    private void showMainMenu() {
        options.clearOptions();
        if (!greeted) {
            showCoordinatorIntro();
            greeted = true;
        }
        textPanel.addParagraph("\"Questions, a quick race, the tournament circuit... or are you just here to bet?\"");
        selectingTournament = false;

        options.addOption("Enter an impromptu race", OptionId.IMPROMPTU, null);
        options.addOption("Bet on a race", OptionId.BET, null);
        options.addOption("Ask about impromptu races", OptionId.ABOUT_IMPROMPTU, null);
        options.addOption("Ask about tournaments", OptionId.ABOUT_TOURNAMENT, null);
        options.addOption("Ask about betting", OptionId.ABOUT_BETTING, null);

        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state == null) {
            options.addOption("Sign up for a tournament", OptionId.TOURNAMENT, null);
        } else {
            options.addOption("View tournament status", OptionId.VIEW_STATUS, null);
            MarketAPI market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
            if (TakeshidoRacingManager.isTournamentRaceAvailable(state, market)) {
                options.addOption("Start the next tournament race", OptionId.START_NEXT_RACE, null);
            }
        }

        options.addOption("Leave", OptionId.CONT, null);
    }

    private void showRaceResultMessage() {
        Object data = Global.getSector().getPersistentData().get(TakeshidoRacingManager.LAST_RACE_DIALOG_MESSAGE_KEY);
        if (data instanceof String) {
            String msg = (String) data;
            if (msg != null && !msg.trim().isEmpty()) {
                textPanel.addParagraph(msg);
            }
            Global.getSector().getPersistentData().remove(TakeshidoRacingManager.LAST_RACE_DIALOG_MESSAGE_KEY);
        }
    }

    private void showCategoryMenu(boolean isTournament) {
        options.clearOptions();
        textPanel.addParagraph(isTournament ? "Pick a tournament class." : "Pick a class for the impromptu race.");
        selectingTournament = isTournament;

        options.addOption("Sportscar", OptionId.CATEGORY_SPORT, null);
        options.addOption("Supercar", OptionId.CATEGORY_SUPER, null);
        options.addOption("Hypercar", OptionId.CATEGORY_HYPER, null);
        options.addOption("Back", OptionId.INIT, null);
    }

    private void handleCategorySelected(String categoryId) {
        if (!selectingTournament) {
            textPanel.addParagraph("You follow the coordinator down to the staging bays, engines rumbling as crews wave you onto the grid.");
            pickShipForRace(categoryId, false);
            return;
        }

        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state != null) {
            textPanel.addParagraph("You are already enrolled in a tournament.");
            options.clearOptions();
            options.addOption("Back", OptionId.INIT, null);
            return;
        }

        TakeshidoRacingManager.startTournament(dialog, categoryId);
        if (TakeshidoRacingManager.getTournamentState() != null) {
            textPanel.addParagraph("The coordinator taps your details into a battered terminal, assigns your number, and waves you onto the tournament list.");
            textPanel.addParagraph("You're registered. Report in for each race as the schedule advances.");
        }

        options.clearOptions();
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showTournamentStatus() {
        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state == null) {
            textPanel.addParagraph("No active tournament.");
            options.clearOptions();
            options.addOption("Back", OptionId.INIT, null);
            return;
        }

        TakeshidoRacingConfig.CategorySpec cat = TakeshidoRacingConfig.get().getCategory(state.categoryId);
        String label = cat != null ? cat.label : state.categoryId;
        textPanel.addParagraph("Category: " + label);
        textPanel.addParagraph("Race " + (state.currentRaceIndex + 1) + " of " + state.racesPerTournament);

        MarketAPI market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
        boolean ready = TakeshidoRacingManager.isTournamentRaceAvailable(state, market);
        if (ready) {
            textPanel.addParagraph("The next race is ready to start here.");
        } else {
            boolean locationReady = market != null && state.nextMarketId != null && state.nextMarketId.equals(market.getId());
            if (!locationReady && state.nextMarketId != null) {
                MarketAPI target = Global.getSector().getEconomy().getMarket(state.nextMarketId);
                String loc = target != null ? target.getName() : "the scheduled venue";
                textPanel.addParagraph("The next race is scheduled at " + loc + ".");
            } else {
                com.fs.starfarer.api.campaign.CampaignClockAPI clock = Global.getSector().getClock();
                int currentDay = clock != null ? (clock.getCycle() * 360) + ((clock.getMonth() - 1) * 30) + (clock.getDay() - 1) : 0;
                int daysLeft = state.nextRaceDay - currentDay;
                if (daysLeft < 0) daysLeft = 0;
                textPanel.addParagraph("The next race opens in " + daysLeft + " days.");
            }
        }

        options.clearOptions();
        if (ready) {
            options.addOption("Start the next tournament race", OptionId.START_NEXT_RACE, null);
        }
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showImpromptuInfo() {
        options.clearOptions();
        textPanel.addParagraph("Impromptu races are one-offs. Pick a class, field your own car, and run it immediately against local drivers.");
        textPanel.addParagraph("Payouts are based on placement, and the grid is whatever's available that day.");
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showTournamentInfo() {
        options.clearOptions();
        textPanel.addParagraph("Tournaments are multi-race circuits. You run one race per week across the set schedule.");
        textPanel.addParagraph("Points are awarded by finish position, and prizes are paid after the final race.");
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showBettingInfo() {
        options.clearOptions();
        textPanel.addParagraph("Betting is based on odds set by the local bookies. Winners and podiums pay out best, but long shots can surprise.");
        textPanel.addParagraph("If you're not racing, you can still watch the action from the stands and take a cut when your pick delivers.");
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showBetting() {
        options.clearOptions();
        betCategoryId = null;
        betRacers.clear();
        betOddsByRacerId.clear();
        betChoice = null;
        betAmount = 0;
        betOdds = 1f;

        textPanel.addParagraph("You step into the stands as the next grid assembles, the smell of hot engines and scorched rubber hanging over the track.");
        textPanel.addParagraph("\"Pick the class you're betting on,\" the coordinator says, flicking through the odds board.");

        options.addOption("Sportscar", OptionId.BET_CATEGORY_SPORT, null);
        options.addOption("Supercar", OptionId.BET_CATEGORY_SUPER, null);
        options.addOption("Hypercar", OptionId.BET_CATEGORY_HYPER, null);
        options.addOption("Back", OptionId.INIT, null);
    }

    private void showBetRacerList(String categoryId) {
        options.clearOptions();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            showBetting();
            return;
        }
        betCategoryId = categoryId;
        betChoice = null;
        betAmount = 0;
        betOdds = 1f;

        betRacers = TakeshidoRacingManager.buildBetRacers(categoryId);
        betOddsByRacerId.clear();

        if (betRacers == null || betRacers.isEmpty()) {
            textPanel.addParagraph("No eligible racers found for that class.");
            options.addOption("Back", OptionId.BET, null);
            return;
        }

        betOddsByRacerId.putAll(computeBetOdds(betRacers));

        TakeshidoRacingConfig.CategorySpec cat = TakeshidoRacingConfig.get().getCategory(categoryId);
        String label = cat != null ? cat.label : categoryId;
        textPanel.addParagraph("The coordinator pulls up the " + label + " grid and rattles off the odds.");

        for (TakeshidoRacingManager.RacerDefinition def : betRacers) {
            float odds = getOddsFor(def);
            textPanel.addParagraph(buildRacerInfoLine(def, odds));
        }

        for (TakeshidoRacingManager.RacerDefinition def : betRacers) {
            String name = def != null && def.name != null && !def.name.isEmpty() ? def.name : "Racer";
            float odds = getOddsFor(def);
            options.addOption(name + " (" + formatOdds(odds) + "x)", def, null);
        }
        options.addOption("Back", OptionId.BET, null);
    }

    private void handleBetRacerSelected(TakeshidoRacingManager.RacerDefinition def) {
        if (def == null) {
            showBetRacerList(betCategoryId);
            return;
        }
        betChoice = def;
        betOdds = getOddsFor(def);
        showBetAmountMenu();
    }

    private void showBetAmountMenu() {
        options.clearOptions();
        if (betChoice == null) {
            showBetRacerList(betCategoryId);
            return;
        }

        String name = betChoice.name != null && !betChoice.name.isEmpty() ? betChoice.name : "your pick";
        textPanel.addParagraph("\"How much are you putting down on " + name + "?\"");
        textPanel.addParagraph("Odds: " + formatOdds(betOdds) + "x");

        int credits = getPlayerCredits();
        int[] amounts = new int[]{5000, 10000, 25000, 50000, 100000, 250000};
        boolean any = false;
        for (int amount : amounts) {
            if (amount <= credits) {
                int payout = Math.round(amount * betOdds);
                options.addOption("Bet " + formatCredits(amount) + " (pays " + formatCredits(payout) + ")", amount, null);
                any = true;
            }
        }

        if (!any) {
            textPanel.addParagraph("You don't have enough credits to place a bet right now.");
        }

        options.addOption("Back", OptionId.BET_BACK_RACERS, null);
    }

    private void handleBetAmountSelected(int amount) {
        betAmount = Math.max(0, amount);
        showBetConfirm();
    }

    private void showBetConfirm() {
        options.clearOptions();
        if (betChoice == null || betAmount <= 0) {
            showBetAmountMenu();
            return;
        }

        String name = betChoice.name != null && !betChoice.name.isEmpty() ? betChoice.name : "your pick";
        int payout = Math.round(betAmount * betOdds);
        textPanel.addParagraph("You're betting " + formatCredits(betAmount) + " credits on " + name + " at " + formatOdds(betOdds) + "x.");
        textPanel.addParagraph("Potential payout: " + formatCredits(payout) + " credits. Confirm?");

        options.addOption("Place bet", OptionId.BET_CONFIRM, null);
        options.addOption("Back", OptionId.BET_BACK_AMOUNT, null);
    }

    private void confirmBet() {
        if (betChoice == null || betAmount <= 0 || betCategoryId == null || betRacers == null || betRacers.isEmpty()) {
            showBetting();
            return;
        }

        int credits = getPlayerCredits();
        if (credits < betAmount) {
            textPanel.addParagraph("You don't have enough credits for that bet.");
            showBetAmountMenu();
            return;
        }

        Global.getSector().getPlayerFleet().getCargo().getCredits().add(-betAmount);
        textPanel.addParagraph("The coordinator nods, logs the ticket, and waves you toward the viewing platform.");

        TakeshidoRacingManager.startBetRace(
                dialog,
                betCategoryId,
                betRacers,
                betChoice.racerId,
                betChoice.name,
                betAmount,
                betOdds
        );
    }

    private Map<String, Float> computeBetOdds(List<TakeshidoRacingManager.RacerDefinition> racers) {
        Map<String, Float> out = new HashMap<>();
        if (racers == null || racers.isEmpty()) return out;
        float avg = 0f;
        for (TakeshidoRacingManager.RacerDefinition def : racers) {
            if (def != null) avg += def.skill;
        }
        avg /= Math.max(1, racers.size());

        for (TakeshidoRacingManager.RacerDefinition def : racers) {
            if (def == null) continue;
            float delta = avg - def.skill;
            float odds = clamp(1.2f + delta * 2.0f, 1.1f, 4.0f);
            out.put(def.racerId, odds);
        }
        return out;
    }

    private String buildRacerInfoLine(TakeshidoRacingManager.RacerDefinition def, float odds) {
        if (def == null) return "Unknown racer.";
        String name = def.name != null && !def.name.isEmpty() ? def.name : "Racer";
        String carInfo = getCarInfo(def);
        String skillLine = formatSkill(def.skill) + " (" + skillDescriptor(def.skill) + ")";
        return name + " - " + carInfo + " | Skill " + skillLine + " | Odds " + formatOdds(odds) + "x.";
    }

    private String getCarInfo(TakeshidoRacingManager.RacerDefinition def) {
        ShipHullSpecAPI spec = null;
        if (def.variantId != null && !def.variantId.isEmpty() && Global.getSettings().doesVariantExist(def.variantId)) {
            ShipVariantAPI var = Global.getSettings().getVariant(def.variantId);
            if (var != null) spec = var.getHullSpec();
        } else if (def.hullId != null && !def.hullId.isEmpty()) {
            spec = Global.getSettings().getHullSpec(def.hullId);
        }

        if (spec == null) {
            if (def.variantId != null && !def.variantId.isEmpty()) return def.variantId;
            if (def.hullId != null && !def.hullId.isEmpty()) return def.hullId;
            return "Unknown car";
        }

        String name = spec.getHullName();
        String designation = spec.getDesignation();
        if (designation != null && !designation.isEmpty()) {
            return name + " (" + designation + ")";
        }
        return name;
    }

    private String skillDescriptor(float skill) {
        if (skill >= 0.82f) return "bookies' favorite";
        if (skill >= 0.72f) return "front-runner";
        if (skill >= 0.62f) return "mid-pack";
        return "long shot";
    }

    private String formatSkill(float skill) {
        return String.format(Locale.ROOT, "%.0f%%", clamp(skill, 0f, 1f) * 100f);
    }

    private String formatOdds(float odds) {
        return String.format(Locale.ROOT, "%.2f", odds);
    }

    private int getPlayerCredits() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return 0;
        return (int) Math.floor(Global.getSector().getPlayerFleet().getCargo().getCredits().get());
    }

    private String formatCredits(int credits) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, credits));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float getOddsFor(TakeshidoRacingManager.RacerDefinition def) {
        if (def == null) return 1f;
        Float odds = betOddsByRacerId.get(def.racerId);
        return odds != null ? odds : 1f;
    }

    private void startNextTournamentRace() {
        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state == null) {
            textPanel.addParagraph("No active tournament.");
            options.clearOptions();
            options.addOption("Back", OptionId.INIT, null);
            return;
        }

        MarketAPI market = dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
        if (!TakeshidoRacingManager.isTournamentRaceAvailable(state, market)) {
            boolean locationReady = market != null && state.nextMarketId != null && state.nextMarketId.equals(market.getId());
            if (!locationReady && state.nextMarketId != null) {
                MarketAPI target = Global.getSector().getEconomy().getMarket(state.nextMarketId);
                String loc = target != null ? target.getName() : "the scheduled venue";
                textPanel.addParagraph("The next race is scheduled at " + loc + ".");
            } else {
                com.fs.starfarer.api.campaign.CampaignClockAPI clock = Global.getSector().getClock();
                int currentDay = clock != null ? (clock.getCycle() * 360) + ((clock.getMonth() - 1) * 30) + (clock.getDay() - 1) : 0;
                int daysLeft = state.nextRaceDay - currentDay;
                if (daysLeft < 0) daysLeft = 0;
                textPanel.addParagraph("The next race opens in " + daysLeft + " days.");
            }
            options.clearOptions();
            options.addOption("Back", OptionId.INIT, null);
            return;
        }

        pickShipForRace(state.categoryId, true);
    }

    private void pickShipForRace(final String categoryId, final boolean tournament) {
        List<FleetMemberAPI> eligible = TakeshidoRacingManager.getEligiblePlayerShips(categoryId);
        if (eligible.isEmpty()) {
            textPanel.addParagraph("You don't have a ship in that class.");
            options.clearOptions();
            options.addOption("Back", OptionId.INIT, null);
            return;
        }

        dialog.showFleetMemberPickerDialog(
                "Select your race car",
                "Confirm",
                "Cancel",
                4,
                8,
                150f,
                true,
                false,
                eligible,
                new FleetMemberPickerListener() {
                    @Override
                    public void pickedFleetMembers(List<FleetMemberAPI> members) {
                        if (members == null || members.isEmpty()) {
                            optionSelected(null, OptionId.INIT);
                            return;
                        }
                        FleetMemberAPI chosen = members.get(0);
                        if (tournament) {
                            TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
                            TakeshidoRacingManager.startTournamentRace(dialog, state, chosen);
                        } else {
                            TakeshidoRacingManager.startImpromptuRace(dialog, categoryId, chosen);
                        }
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        optionSelected(null, OptionId.INIT);
                    }
                }
        );
    }

    private void closeDialog() {
        if (Global.getSector().getPersistentData().get("takeshido_originaldialog") != null) {
            InteractionDialogPlugin original = (InteractionDialogPlugin) Global.getSector().getPersistentData().get("takeshido_originaldialog");
            dialog.setPlugin(original);
            options.clearOptions();
            FireAll.fire(null, dialog, original.getMemoryMap(), "PopulateOptions");
            Global.getSector().getPersistentData().remove("takeshido_originaldialog");
        } else {
            dialog.dismiss();
        }
    }

    private void showCoordinator() {
        if (visual == null) return;
        PersonAPI person = null;
        MarketAPI market = null;
        if (dialog != null && dialog.getInteractionTarget() != null) {
            market = dialog.getInteractionTarget().getMarket();
            if (market != null) {
                person = TakeshidoRacingManager.getRaceCoordinatorForMarket(market);
            }
            if (person == null) {
                person = dialog.getInteractionTarget().getActivePerson();
            }
        }
        if (person != null) {
            visual.showPersonInfo(person);
        }
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void advance(float amount) {
    }

    public Object getContext() {
        return null;
    }

    private CoordinatorFlavor getCoordinatorFlavor() {
        String id = getCoordinatorId();
        if (COORDINATOR_BIFF.equals(id)) return CoordinatorFlavor.BIFF;
        if (COORDINATOR_BURT.equals(id)) return CoordinatorFlavor.BURT;
        if (COORDINATOR_LIZZIE.equals(id)) return CoordinatorFlavor.LIZZIE;
        return CoordinatorFlavor.GENERIC;
    }

    private String getCoordinatorId() {
        MarketAPI market = dialog != null && dialog.getInteractionTarget() != null ? dialog.getInteractionTarget().getMarket() : null;
        PersonAPI person = market != null ? TakeshidoRacingManager.getRaceCoordinatorForMarket(market) : null;
        if (person == null && dialog != null && dialog.getInteractionTarget() != null) {
            person = dialog.getInteractionTarget().getActivePerson();
        }
        return person != null ? person.getId() : null;
    }

    private void showCoordinatorIntro() {
        switch (getCoordinatorFlavor()) {
            case BIFF:
                textPanel.addParagraph("You hear bad things about this station's coordination officer, from those who will talk. There is no end to the rumors and sweaty brows surrounding him. Some say that he's run the races since the beginning. Others say that he used to be more benevolent, but will not explain in what way - some suggest that he lost a friend or a family member. You can't find anyone willing to talk about him for long.");
                textPanel.addParagraph("This wing's coordination office has a suite of stains and odors and discarded needles in the hallways leading up to its entrance. You detect the sweet, seductive whiff of an illegal narcotic, as well as that of greasy meat and the after-jets of gauss discharge. The threshold is wide open with no door, at the end of a wide court. Another bar has been set up here, being patronized by men and women with glittering hands, eyes and teeth, some of which have themselves been ripped at and scorched over by blades and gunfire. Vials of rainbow-colored substance change hands beneath tables, as do small weapons, whispers and intimate gestures. Many of those present notice and observe your party with interest.");
                textPanel.addParagraph("Heading for the office, you find the place nearly lit by a few hot-red lamps, embedded in the walls. Smoke and vapor from inhalants being used by the patrons makes a haze from it which is difficult to see through. Your entrance is initially blocked by a squad of men, first sitting around and chattering by the office corners, who you had assumed to be either customers or vagrants. They step around and in front of your group just as you're about to cross inside. Their faces are all wrapped up in scarves and rusted helmets. Some draw out guns and knives they had been concealing and ready them.");
                textPanel.addParagraph("\"They're okay, Ajax,\" you hear. \"They've got class.\"");
                textPanel.addParagraph("One of the foremost men inclines his head and moves. The others part along with him, back to their places, and you go inside, toward the voice.");
                textPanel.addParagraph("You can hear the wet thudding of metal-against-meat from a back room, along with crying out and whimpering. It's blocked by a red-white wall and the presence of a very large man, standing with a dark, tubular inhaler in his hand. He stands with his back to you as his other hand manipulates a touch-screen mounted on the wall, striking buttons and marking characters with three heavy fingers.");
                textPanel.addParagraph("The screen darkens as he makes a final stroke and turns to you. He breathes something in out of his inhaler and blows it out to the side, making a cloud of white smoke that glitters gently and reflects the light. He slips it back into the pocket of his coat, which is heavy and black and appears to be made out of skin. A finger starts to play with the gold chain on his neck. His eyebrow lifts slightly at your appearance.");
                textPanel.addParagraph("\"Come on in,\" he says. \"Don't be shy.\"");
                break;
            case BURT:
                textPanel.addParagraph("Even before you arrive at his office, you hear that this wing's race-coordinator is difficult to get a read on. He's reputed to have worn a number of hats in the past; among them: anti-Diktat resistance fighter, gunrunner, thief, and concubine. Some of the security forces you bribe for information about him are more enthusiastic than others - a younger one, who blushes when she talks about him, and an older one, who laughs about a time he saw the man defenestrate an organ trafficker. Your inquiry among the Kanta associates on the station reveal a similar range of opinion, although it is more broadly positive among the buccaneers, and one of them offers to show you where he works.");
                textPanel.addParagraph("The office he guides you to is very personal, with a physical wooden door that you step inside. The interior is low and round. A large oval window casts in the station's gentle evening light, which is currently shining on the promenade over which it is situated.");
                textPanel.addParagraph("The coordinator reclines at his desk, his feet crossed atop it. His upper lip is darkened by a smooth mustache, and his hair is a little messy. He wears a parted shirt, a large jacket thrown over the back of his chair. One of your bodyguards privately informs you that a scan has revealed a pistol taped underneath his desk. He's poring over a Tripad there, holding his mouth and tapping the arm of his chair as he reads it.");
                textPanel.addParagraph("Once you enter, he looks up. He takes in your appearance, shuts off the Tripad and sets it down on the desk. He doesn't get up.");
                textPanel.addParagraph("\"Howdy,\" he says. \"Are you a contender?\"");
                break;
            case LIZZIE:
                textPanel.addParagraph("You hear good things about this station's race coordinator. She has a digital profile on the station's intranet that displays her credentials and shows you where she's from. The profile picture is of a smiling woman in a vest and suit, wearing the pin of a Westernessse racing club. She looks very young, and holds her hands on her hips.");
                textPanel.addParagraph("The passage to this wing's coordination office immediately saturates you with heavily processed air, filtered through a number of high-grade purifiers. A patron going the way you're coming, a pirate with heavy scarring, an old musty coat, and a chewing on a spitted rodent, does not have any scent you can detect. The edges of the area, the furniture, the lights, the electronics are all smoothed down and stripped of texture. You and your company enter through a clear, sliding glass door, which opens for you into a softly lit office lit by false-skylights overhead, projecting a rainy day. The coordinator herself sits behind a black desk made of Gilead oak, upon which is mounted a range of Tripads that rises past her head. She attends to them all with six arms; four appear to be fully bionic, mounted on her torso and waist. She wears a navy blue jacket and white shirt, both cut for her unique anatomy. Her mouth is pressed together in a thin line and her eyes move smoothly between different concerns on different pads.");
                textPanel.addParagraph("She addresses you without turning her gaze from her work. \"Is this a prior concern?\" she asks.");
                break;
            default:
                textPanel.addParagraph("The coordinator runs the operation from a cramped control bay above the infield, a mess of telemetry screens and old race posters.");
                textPanel.addParagraph("They glance up as you enter, tracking you with the practiced focus of someone who never misses a lap.");
                break;
        }
    }

    private void showImpromptuSignupText() {
        switch (getCoordinatorFlavor()) {
            case BIFF:
                textPanel.addParagraph("You tell him that you're there to sign up for an impromptu race, and he feeds the appropriate form to your Tripad. You fill it out and send it back to him.");
                textPanel.addParagraph("\"Remember to show,\" he says, taking a hit. \"It makes me look a certain way, if you don't.\"");
                break;
            case BURT:
                textPanel.addParagraph("You tell him that you're there to sign up for an impromptu race, and he feeds the appropriate form to your Tripad. You fill it out and send it back to him.");
                textPanel.addParagraph("\"Good luck,\" he says. \"Not that you'll need it.\"");
                break;
            case LIZZIE:
                textPanel.addParagraph("You tell her that you're there to sign up for an impromptu race, and she feeds the appropriate form to your Tripad. You fill it out and send it back to her.");
                textPanel.addParagraph("\"Arrive at the specified time,\" she says. \"You will not be accommodated if the launch is still closed.\"");
                break;
            default:
                break;
        }
    }

    private void showTournamentSignupText() {
        switch (getCoordinatorFlavor()) {
            case BIFF:
                textPanel.addParagraph("You tell him that you're there to sign up for a tournament, and he feeds the appropriate form to your Tripad. You fill it out and hand it back to him.");
                textPanel.addParagraph("\"Talk to me later if you're interested in 'falling behind',\" he says, turning back to his screen. \"I've got some friends who would pay for it. It's all a circus, anyway.\"");
                break;
            case BURT:
                textPanel.addParagraph("You tell him that you're there to sign up for a tournament, and he feeds the appropriate form to your Tripad. You fill it out and hand it back to him.");
                textPanel.addParagraph("\"Don't be afraid to make friends,\" he says. \"You're all racing on the same track. And mostly for the same reason.\"");
                break;
            case LIZZIE:
                textPanel.addParagraph("You tell her that you're there to sign up for a tournament, and she feeds the appropriate form to your Tripad. You fill it out and hand it back to her.");
                textPanel.addParagraph("\"Thank you for participating,\" she says. \"Your performance will be very lucrative for us. Would you like to add a death or mutilation plan to your account? Contestants receive a discount on our most generous package.\"");
                break;
            default:
                break;
        }
    }
}
