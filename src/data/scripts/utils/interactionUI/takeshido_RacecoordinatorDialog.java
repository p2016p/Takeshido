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
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import data.scripts.racing.TakeshidoRacingConfig;
import data.scripts.racing.TakeshidoRacingManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class takeshido_RacecoordinatorDialog implements InteractionDialogPlugin {

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
        VIEW_STATUS
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    protected Map<String, MemoryAPI> memoryMap;
    protected boolean selectingTournament = false;
    protected boolean greeted = false;

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

        OptionId option = (OptionId) optionData;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        switch (option) {
            case INIT:
                showMainMenu();
                break;
            case IMPROMPTU:
                showCategoryMenu(false);
                break;
            case TOURNAMENT:
                showCategoryMenu(true);
                break;
            case BET:
                showBetting();
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
            textPanel.addParagraph("The coordinator runs the operation from a cramped control bay above the infield, a mess of telemetry screens and old race posters.");
            textPanel.addParagraph("They glance up as you enter, tracking you with the practiced focus of someone who never misses a lap.");
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
        textPanel.addParagraph("You step into the stands as the next grid assembles, the smell of hot engines and scorched rubber hanging over the track.");
        textPanel.addParagraph("Betting isn't available yet.");
        options.addOption("Back", OptionId.INIT, null);
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
        if (dialog != null && dialog.getInteractionTarget() != null) {
            person = dialog.getInteractionTarget().getActivePerson();
            if (person == null) {
                MarketAPI market = dialog.getInteractionTarget().getMarket();
                if (market != null) {
                    MemoryAPI mem = market.getMemoryWithoutUpdate();
                    String id = mem.getString("$takeshido_racecoordinator_id");
                    if (id != null) {
                        person = Global.getSector().getImportantPeople().getPerson(id);
                    }
                }
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
}
