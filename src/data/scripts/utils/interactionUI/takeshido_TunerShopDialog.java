package data.scripts.utils.interactionUI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomDialogDelegate;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.util.Misc;
import data.scripts.tuner.TakeshidoTunerConfig;
import data.scripts.tuner.TakeshidoTunerManager;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class takeshido_TunerShopDialog implements InteractionDialogPlugin {

    private static final String BTN_CHANGE_CAR = "tuner_change_car";
    private static final String BTN_UPGRADE_PREFIX = "tuner_upgrade:";

    private enum OptionId {
        INIT,
        SELECT_CAR,
        OPEN_TUNER,
        LEAVE
    }

    private enum PendingAction {
        NONE,
        CHANGE_CAR
    }

    private enum UpgradeCategory {
        ASPIRATION,
        THRUSTER_BEARINGS,
        THRUSTER_GIMBALS,
        EXHAUST,
        ENGINE,
        DRIVETRAIN,
        TRANSMISSION,
        BODY
    }

    private static final UpgradeCategory[] CATEGORY_ORDER = new UpgradeCategory[]{
            UpgradeCategory.ASPIRATION,
            UpgradeCategory.THRUSTER_BEARINGS,
            UpgradeCategory.THRUSTER_GIMBALS,
            UpgradeCategory.EXHAUST,
            UpgradeCategory.ENGINE,
            UpgradeCategory.DRIVETRAIN,
            UpgradeCategory.TRANSMISSION,
            UpgradeCategory.BODY
    };

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    protected Map<String, MemoryAPI> memoryMap;

    protected FleetMemberAPI selectedCar;
    protected boolean openCarPickerRequested = false;
    protected boolean openTunerRequested = false;
    protected UpgradeCategory selectedCategory = UpgradeCategory.ENGINE;

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        memoryMap = new LinkedHashMap<String, MemoryAPI>();

        textPanel.addParagraph("You step into a dedicated tuning bay. Racks of calibrated parts line the walls and a spec board lights up as you arrive.");
        optionSelected(null, OptionId.INIT);
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return memoryMap;
    }

    @Override
    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        OptionId option = (OptionId) optionData;
        switch (option) {
            case INIT:
                showMainMenu();
                break;
            case SELECT_CAR:
                pickCar(true);
                break;
            case OPEN_TUNER:
                openTunerInterface();
                break;
            case LEAVE:
                closeDialog();
                break;
            default:
                showMainMenu();
                break;
        }
    }

    private void showMainMenu() {
        options.clearOptions();

        if (selectedCar == null) {
            textPanel.addParagraph("Select a race car to open the tuner interface.");
            options.addOption("Select a car", OptionId.SELECT_CAR, null);
            options.addOption("Leave", OptionId.LEAVE, null);
            return;
        }

        String hullName = selectedCar != null && selectedCar.getHullSpec() != null ? selectedCar.getHullSpec().getHullName() : "Unknown hull";
        textPanel.addParagraph("Selected car: " + selectedCar.getShipName() + " (" + hullName + ")");
        textPanel.addParagraph("Credits: " + formatCredits(getPlayerCredits()));

        options.addOption("Open tuner interface", OptionId.OPEN_TUNER, null);
        options.addOption("Select a different car", OptionId.SELECT_CAR, null);
        options.addOption("Leave", OptionId.LEAVE, null);
    }

    private void pickCar(final boolean reopenTunerAfterPick) {
        List<FleetMemberAPI> eligible = TakeshidoTunerManager.getEligiblePlayerCars();
        if (eligible.isEmpty()) {
            textPanel.addParagraph("No eligible cars found in your fleet.");
            showMainMenu();
            return;
        }

        dialog.showFleetMemberPickerDialog(
                "Select a car to tune",
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
                            if (reopenTunerAfterPick && selectedCar != null) {
                                openTunerRequested = true;
                            } else {
                                showMainMenu();
                            }
                            return;
                        }
                        selectedCar = members.get(0);
                        TakeshidoTunerManager.ensureTunerHullmod(selectedCar);
                        if (reopenTunerAfterPick) {
                            openTunerRequested = true;
                        } else {
                            showMainMenu();
                        }
                    }

                    @Override
                    public void cancelledFleetMemberPicking() {
                        if (reopenTunerAfterPick && selectedCar != null) {
                            openTunerRequested = true;
                        } else {
                            showMainMenu();
                        }
                    }
                }
        );
    }

    private void openTunerInterface() {
        if (selectedCar == null) {
            textPanel.addParagraph("Select a car first.");
            showMainMenu();
            return;
        }
        TakeshidoTunerManager.ensureTunerHullmod(selectedCar);
        dialog.showCustomDialog(1560f, 820f, new TunerCustomDialog(selectedCar));
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

    private int getPlayerCredits() {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return 0;
        return (int) Math.floor(Global.getSector().getPlayerFleet().getCargo().getCredits().get());
    }

    private String formatCredits(int credits) {
        return String.format(Locale.ROOT, "%,d", Math.max(0, credits));
    }

    private static void setButtonEnabled(ButtonAPI button, boolean enabled) {
        if (button == null) return;
        try {
            Method m = button.getClass().getMethod("setEnabled", boolean.class);
            m.invoke(button, enabled);
        } catch (Exception ignored) {
        }
    }

    private static UpgradeCategory getCategory(TakeshidoTunerConfig.UpgradeSpec spec) {
        if (spec == null || spec.type == null) return UpgradeCategory.ENGINE;
        String t = spec.type.toLowerCase(Locale.ROOT);
        if ("turbo".equals(t) || "supercharger".equals(t)) return UpgradeCategory.ASPIRATION;
        if ("thrusters".equals(t)) return UpgradeCategory.THRUSTER_BEARINGS;
        if ("gimbals".equals(t)) return UpgradeCategory.THRUSTER_GIMBALS;
        if ("engine_exhaust".equals(t) || "exhaust".equals(t)) return UpgradeCategory.EXHAUST;
        if (t.startsWith("engine")) return UpgradeCategory.ENGINE;
        if ("drivetrain".equals(t)) return UpgradeCategory.DRIVETRAIN;
        if ("transmission".equals(t)) return UpgradeCategory.TRANSMISSION;
        if ("aether".equals(t)) return UpgradeCategory.BODY;
        return UpgradeCategory.ENGINE;
    }

    private static String categoryTitle(UpgradeCategory category) {
        switch (category) {
            case ASPIRATION:
                return "Aspiration";
            case THRUSTER_BEARINGS:
                return "Thruster Bearings";
            case THRUSTER_GIMBALS:
                return "Thruster Gimbals";
            case EXHAUST:
                return "Exhaust";
            case ENGINE:
                return "Engine";
            case DRIVETRAIN:
                return "Drivetrain";
            case TRANSMISSION:
                return "Transmission";
            case BODY:
                return "Body";
            default:
                return "Upgrades";
        }
    }

    private static String categoryIconPath(UpgradeCategory category) {
        switch (category) {
            case ASPIRATION:
                return "graphics/tunerIcons/turbo.png";
            case THRUSTER_BEARINGS:
                return "graphics/tunerIcons/tires.png";
            case THRUSTER_GIMBALS:
                return "graphics/tunerIcons/suspension.png";
            case EXHAUST:
                return "graphics/tunerIcons/exhaust.png";
            case ENGINE:
                return "graphics/tunerIcons/engine.png";
            case DRIVETRAIN:
                return "graphics/tunerIcons/drivetrain.png";
            case TRANSMISSION:
                return "graphics/tunerIcons/transmission.png";
            case BODY:
                return "graphics/tunerIcons/body.png";
            default:
                return "graphics/hullmods/takeshido_default.png";
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float computeSpeedScore(float maxSpeed) {
        return (maxSpeed - 110f) / 15f;
    }

    private static float computeAccelScore(float accel) {
        if (accel <= 0f) return 0f;
        return (6f - (60f / accel)) * 2f;
    }

    private static float computeTurnScore(float maxTurnRate) {
        return (maxTurnRate - 50f) / 6f;
    }

    private static float toTenScale(float score) {
        return clamp(score, 0f, 10f);
    }

    private static String formatUpgradeEffects(TakeshidoTunerConfig.UpgradeSpec spec) {
        if (spec == null || spec.mods == null || spec.mods.isEmpty()) return "No stat changes.";
        StringBuilder sb = new StringBuilder();
        for (TakeshidoTunerConfig.StatMod mod : spec.mods) {
            if (mod == null || mod.stat == null || mod.mode == null) continue;
            String stat = mod.stat;
            if ("mult".equalsIgnoreCase(mod.mode)) {
                float pct = (mod.value - 1f) * 100f;
                if (Math.abs(pct) < 0.01f) continue;
                if (sb.length() > 0) sb.append("; ");
                sb.append(String.format(Locale.ROOT, "%+.0f%% %s", pct, stat));
            } else if ("flat".equalsIgnoreCase(mod.mode)) {
                if (Math.abs(mod.value) < 0.01f) continue;
                if (sb.length() > 0) sb.append("; ");
                sb.append(String.format(Locale.ROOT, "%+.0f %s", mod.value, stat));
            }
        }
        if (sb.length() == 0) return "No stat changes.";
        return sb.toString();
    }

    private static class UpgradeTooltipCreator implements TooltipMakerAPI.TooltipCreator {
        private final TakeshidoTunerConfig.UpgradeSpec spec;
        private final FleetMemberAPI member;

        private UpgradeTooltipCreator(TakeshidoTunerConfig.UpgradeSpec spec, FleetMemberAPI member) {
            this.spec = spec;
            this.member = member;
        }

        @Override
        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }

        @Override
        public float getTooltipWidth(Object tooltipParam) {
            return 360f;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            if (spec == null || tooltip == null) return;
            tooltip.addTitle(spec.name);
            if (spec.description != null && !spec.description.isEmpty()) {
                tooltip.addPara(spec.description, 6f);
            }
            tooltip.addSectionHeading("Effects", Alignment.MID, 8f);
            tooltip.addPara(formatUpgradeEffects(spec), 4f);

            TakeshidoTunerManager.UpgradeStatus status = member != null ? TakeshidoTunerManager.getUpgradeStatus(member, spec) : TakeshidoTunerManager.UpgradeStatus.BLOCKED;
            String statusText;
            if (status == TakeshidoTunerManager.UpgradeStatus.AVAILABLE) {
                int price = member != null ? TakeshidoTunerManager.getUpgradePrice(member, spec) : 0;
                statusText = "Price: " + String.format(Locale.ROOT, "%,d", price) + " credits";
            } else if (status == TakeshidoTunerManager.UpgradeStatus.BUILTIN) {
                statusText = "Built-in";
            } else if (status == TakeshidoTunerManager.UpgradeStatus.INSTALLED) {
                statusText = "Installed";
            } else if (status == TakeshidoTunerManager.UpgradeStatus.INCOMPATIBLE) {
                statusText = "Incompatible with current setup";
            } else {
                statusText = "Ineligible for this car";
            }
            tooltip.addPara(statusText, 8f, Misc.getHighlightColor(), statusText);
        }
    }

    private class TunerCustomDialog implements CustomDialogDelegate {
        private final FleetMemberAPI car;
        private CustomDialogCallback callback;

        private final Map<String, TakeshidoTunerConfig.UpgradeSpec> idToSpec = new LinkedHashMap<String, TakeshidoTunerConfig.UpgradeSpec>();
        private final Map<String, TakeshidoTunerManager.UpgradeStatus> idToStatus = new LinkedHashMap<String, TakeshidoTunerManager.UpgradeStatus>();
        private final Map<UpgradeCategory, ButtonAPI> categoryButtons = new LinkedHashMap<UpgradeCategory, ButtonAPI>();
        private final Map<UpgradeCategory, CustomPanelAPI> categoryPanels = new LinkedHashMap<UpgradeCategory, CustomPanelAPI>();
        private final Map<String, BuyRowState> buyRows = new LinkedHashMap<String, BuyRowState>();
        private PendingAction pendingAction = PendingAction.NONE;
        private LabelAPI creditsLabel;
        private StatBar overallBar;
        private StatBar speedBar;
        private StatBar accelBar;
        private StatBar turnBar;
        private float centerPanelX;
        private float centerPanelY;

        private TunerCustomDialog(FleetMemberAPI car) {
            this.car = car;
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel, CustomDialogCallback callback) {
            this.callback = callback;

            float width = 1560f;
            float height = 820f;
            TooltipMakerAPI ui = panel.createUIElement(width, height, false);

            ui.addSectionHeading("Takeshido Tuner Interface", Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, 0f);

            float leftX = 20f;
            float leftW = 240f;
            float centerX = leftX + leftW + 20f;
            float centerW = 760f;
            float rightX = centerX + centerW + 20f;
            float rightW = width - rightX - 20f;
            centerPanelX = centerX;

            creditsLabel = ui.addPara("Credits: %s", 0f, Misc.getHighlightColor(), formatCredits(getPlayerCredits()));
            creditsLabel.getPosition().inTL(rightX, 34f);
            creditsLabel.getPosition().setLocation(rightX, 34f);

            ButtonAPI changeCar = ui.addButton("Change Car", BTN_CHANGE_CAR, 150f, 28f, 0f);
            changeCar.getPosition().inTL(rightX + rightW - 160f, 28f);
            changeCar.getPosition().setLocation(rightX + rightW - 160f, 28f);

            ui.addPara("Car: %s", 6f, Misc.getHighlightColor(), car.getShipName());
            ui.getPrev().getPosition().inTL(rightX, 70f);
            ui.getPrev().getPosition().setLocation(rightX, 70f);

            List<FleetMemberAPI> shipList = Collections.singletonList(car);
            ui.addShipList(1, 1, 240f, Misc.getBasePlayerColor(), shipList, 0f);
            ui.getPrev().getPosition().inTL(rightX + (rightW - 240f) * 0.5f, 100f);
            ui.getPrev().getPosition().setLocation(rightX + (rightW - 240f) * 0.5f, 100f);

            if (car != null && car.getStats() != null) {
                float maxSpeed = car.getStats().getMaxSpeed().getModifiedValue();
                float accel = car.getStats().getAcceleration().getModifiedValue();
                float turn = car.getStats().getMaxTurnRate().getModifiedValue();

                float speedScore = toTenScale(computeSpeedScore(maxSpeed));
                float accelScore = toTenScale(computeAccelScore(accel));
                float turnScore = toTenScale(computeTurnScore(turn));
                float overallScore = toTenScale((speedScore + accelScore + turnScore) / 3f);

                float statPanelX = rightX + 20f;
                float statPanelY = 400f;
                float barW = 160f;
                float barH = 14f;
                float gap = 30f;

                overallBar = addStatBar(ui, statPanelX, statPanelY, "Overall", overallScore, barW, barH, overallScore, true);
                speedBar = addStatBar(ui, statPanelX, statPanelY + gap, "Top Speed", speedScore, barW, barH, maxSpeed, false);
                accelBar = addStatBar(ui, statPanelX, statPanelY + gap * 2f, "Acceleration", accelScore, barW, barH, accel, false);
                turnBar = addStatBar(ui, statPanelX, statPanelY + gap * 3f, "Handling", turnScore, barW, barH, turn, false);
            }

            float leftStartY = 110f;
            float leftGap = 54f;
            for (int i = 0; i < CATEGORY_ORDER.length; i++) {
                UpgradeCategory cat = CATEGORY_ORDER[i];
                float x = leftX;
                float y = leftStartY + i * leftGap;

                String catId = "tuner_cat:" + cat.name();
                ButtonAPI catButton = ui.addAreaCheckbox("", catId,
                        new Color(120, 120, 120, 80),
                        new Color(55, 55, 55, 110),
                        new Color(170, 170, 170, 130),
                        leftW - 20f, 44f, 0f);
                catButton.getPosition().setSize(leftW - 20f, 44f);
                catButton.getPosition().inTL(x, y);
                catButton.getPosition().setLocation(x, y);

                String icon = categoryIconPath(cat);
                ui.addImage(icon, 32f, 32f, 0f);
                ui.getPrev().getPosition().setSize(32f, 32f);
                ui.getPrev().getPosition().inTL(x + 8f, y + 6f);
                ui.getPrev().getPosition().setLocation(x + 8f, y + 6f);

                ui.addPara(categoryTitle(cat), 0f);
                ui.getPrev().getPosition().inTL(x + 48f, y + 12f);
                ui.getPrev().getPosition().setLocation(x + 48f, y + 12f);

                if (cat == selectedCategory) {
                    catButton.setChecked(true);
                    catButton.highlight();
                }
                categoryButtons.put(cat, catButton);
            }

            panel.addUIElement(ui).inTL(0f, 0f);

            float centerPanelH = height - 120f;
            float centerPanelY = 90f;
            this.centerPanelY = centerPanelY;
            for (UpgradeCategory cat : CATEGORY_ORDER) {
                CustomPanelAPI catPanel = panel.createCustomPanel(centerW, centerPanelH, new TunerPanelPlugin(this));
                TooltipMakerAPI catUi = catPanel.createUIElement(centerW, centerPanelH, true);

                catUi.addPara(categoryTitle(cat), 0f);
                catUi.getPrev().getPosition().inTL(0f, 0f);
                catUi.getPrev().getPosition().setLocation(0f, 0f);

                List<TakeshidoTunerConfig.UpgradeSpec> categoryUpgrades = new ArrayList<TakeshidoTunerConfig.UpgradeSpec>();
                for (TakeshidoTunerConfig.UpgradeSpec spec : TakeshidoTunerConfig.get().upgradeList) {
                    if (spec == null) continue;
                    if (getCategory(spec) == cat) {
                        categoryUpgrades.add(spec);
                    }
                }

                float listY = 30f;
                float rowH = 34f;
                float nameX = 0f;
                float statusX = 280f;
                float priceX = 440f;
                float buyX = 620f;

                if (categoryUpgrades.isEmpty()) {
                    catUi.addPara("No upgrades available for this category yet.", 0f);
                    catUi.getPrev().getPosition().inTL(0f, listY);
                    catUi.getPrev().getPosition().setLocation(0f, listY);
                } else {
                    for (int i = 0; i < categoryUpgrades.size(); i++) {
                        TakeshidoTunerConfig.UpgradeSpec spec = categoryUpgrades.get(i);
                        float y = listY + i * rowH;

                        catUi.addPara(spec.name, 0f);
                        catUi.getPrev().getPosition().inTL(nameX, y);
                        catUi.getPrev().getPosition().setLocation(nameX, y);

                        TakeshidoTunerManager.UpgradeStatus status = TakeshidoTunerManager.getUpgradeStatus(car, spec);
                        String statusText;
                        switch (status) {
                            case BUILTIN:
                                statusText = "Built-in";
                                break;
                            case INSTALLED:
                                statusText = "Installed";
                                break;
                            case INCOMPATIBLE:
                                statusText = "Incompatible";
                                break;
                            case AVAILABLE:
                                statusText = "Available";
                                break;
                            default:
                                statusText = "Ineligible";
                                break;
                        }
                        LabelAPI statusLabel = catUi.addPara(statusText, 0f);
                        statusLabel.getPosition().inTL(statusX, y);
                        statusLabel.getPosition().setLocation(statusX, y);

                        int price = TakeshidoTunerManager.getUpgradePrice(car, spec);
                        catUi.addPara(String.format(Locale.ROOT, "%,d", price), 0f);
                        catUi.getPrev().getPosition().inTL(priceX, y);
                        catUi.getPrev().getPosition().setLocation(priceX, y);

                        String buttonId = BTN_UPGRADE_PREFIX + spec.id;
                        ButtonAPI buy = catUi.addButton("Buy", buttonId, 70f, 22f, 0f);
                        buy.getPosition().inTL(buyX, y - 4f);
                        buy.getPosition().setLocation(buyX, y - 4f);

                        catUi.addTooltipTo(new UpgradeTooltipCreator(spec, car), buy, TooltipMakerAPI.TooltipLocation.RIGHT);

                        idToSpec.put(buttonId, spec);
                        idToStatus.put(buttonId, status);
                        BuyRowState row = new BuyRowState(spec, cat, buy, statusLabel, buyX, y - 4f, 70f, 22f);
                        buyRows.put(buttonId, row);
                        updateButtonForStatus(row, status);
                    }
                }

                catPanel.addUIElement(catUi).inTL(0f, 0f);
                if (cat == selectedCategory) {
                    catPanel.getPosition().inTL(centerX, centerPanelY);
                    catPanel.getPosition().setLocation(centerX, centerPanelY);
                    catPanel.setOpacity(1f);
                } else {
                    catPanel.getPosition().inTL(-10000f, -10000f);
                    catPanel.getPosition().setLocation(-10000f, -10000f);
                    catPanel.setOpacity(0f);
                }
                categoryPanels.put(cat, catPanel);
                panel.addComponent(catPanel);
            }
        }

        @Override
        public boolean hasCancelButton() {
            return false;
        }

        @Override
        public String getConfirmText() {
            return "Close";
        }

        @Override
        public String getCancelText() {
            return "Close";
        }

        @Override
        public void customDialogConfirm() {
            handleDismiss();
        }

        @Override
        public void customDialogCancel() {
            handleDismiss();
        }

        private void handleDismiss() {
            if (pendingAction == PendingAction.CHANGE_CAR) {
                pendingAction = PendingAction.NONE;
                pickCar(true);
                return;
            }
            if (openCarPickerRequested) {
                return;
            }
            showMainMenu();
        }

        private void setSelectedCategory(UpgradeCategory category) {
            if (category == null) return;
            selectedCategory = category;

            for (Map.Entry<UpgradeCategory, ButtonAPI> entry : categoryButtons.entrySet()) {
                ButtonAPI button = entry.getValue();
                if (button == null) continue;
                boolean active = entry.getKey() == selectedCategory;
                button.setChecked(active);
                if (active) {
                    button.highlight();
                } else {
                    button.unhighlight();
                }
            }

            for (Map.Entry<UpgradeCategory, CustomPanelAPI> entry : categoryPanels.entrySet()) {
                UpgradeCategory cat = entry.getKey();
                CustomPanelAPI panel = entry.getValue();
                if (panel == null) continue;
                if (cat == selectedCategory) {
                    panel.getPosition().inTL(centerPanelX, centerPanelY);
                    panel.getPosition().setLocation(centerPanelX, centerPanelY);
                    panel.setOpacity(1f);
                } else {
                    panel.getPosition().inTL(-10000f, -10000f);
                    panel.getPosition().setLocation(-10000f, -10000f);
                    panel.setOpacity(0f);
                }
            }
            refreshBuyRows();
        }

        private void refreshBuyRows() {
            for (Map.Entry<String, BuyRowState> entry : buyRows.entrySet()) {
                BuyRowState row = entry.getValue();
                if (row == null || row.button == null || row.spec == null) continue;

                TakeshidoTunerManager.UpgradeStatus status = TakeshidoTunerManager.getUpgradeStatus(car, row.spec);
                idToStatus.put(entry.getKey(), status);
                if (row.statusLabel != null) {
                    row.statusLabel.setText(statusToText(status));
                }

                int price = TakeshidoTunerManager.getUpgradePrice(car, row.spec);
                // Keep buttons interactive; purchase rules are enforced in handleInstantPurchase.
                updateButtonForStatus(row, status);
            }
        }

        private String statusToText(TakeshidoTunerManager.UpgradeStatus status) {
            if (status == null) return "Ineligible";
            switch (status) {
                case BUILTIN:
                    return "Built-in";
                case INSTALLED:
                    return "Installed";
                case INCOMPATIBLE:
                    return "Incompatible";
                case AVAILABLE:
                    return "Available";
                default:
                    return "Ineligible";
            }
        }

        private void handleInstantPurchase(TakeshidoTunerConfig.UpgradeSpec spec) {
            if (spec == null || car == null) return;
            TakeshidoTunerManager.UpgradeStatus status = TakeshidoTunerManager.getUpgradeStatus(car, spec);
            if (status != TakeshidoTunerManager.UpgradeStatus.AVAILABLE) {
                textPanel.addParagraph("That upgrade is no longer available for this car.");
                refreshBuyRows();
                return;
            }

            int price = TakeshidoTunerManager.getUpgradePrice(car, spec);
            int credits = getPlayerCredits();
            if (credits < price) {
                textPanel.addParagraph("Insufficient credits for " + spec.name + ".");
                refreshBuyRows();
                return;
            }

            Global.getSector().getPlayerFleet().getCargo().getCredits().add(-price);
            TakeshidoTunerManager.installUpgrade(car, spec.id);
            TakeshidoTunerManager.ensureTunerHullmod(car);
            car.updateStats();
            car.setStatUpdateNeeded(true);

            if (creditsLabel != null) {
                creditsLabel.setText("Credits: " + formatCredits(getPlayerCredits()));
            }

            textPanel.addParagraph("Installed " + spec.name + " for " + formatCredits(price) + " credits.");
            refreshBuyRows();
            updateStatBars();
        }

        private void handleRemoveUpgrade(TakeshidoTunerConfig.UpgradeSpec spec) {
            if (spec == null || car == null) return;
            TakeshidoTunerManager.UpgradeStatus status = TakeshidoTunerManager.getUpgradeStatus(car, spec);
            if (status != TakeshidoTunerManager.UpgradeStatus.INSTALLED) {
                textPanel.addParagraph("That upgrade cannot be removed.");
                refreshBuyRows();
                return;
            }

            TakeshidoTunerManager.uninstallUpgrade(car, spec.id);
            TakeshidoTunerManager.ensureTunerHullmod(car);
            car.updateStats();
            car.setStatUpdateNeeded(true);

            textPanel.addParagraph("Removed " + spec.name + ".");
            refreshBuyRows();
            updateStatBars();
        }

        private void updateStatBars() {
            if (car == null || car.getStats() == null) return;
            float maxSpeed = car.getStats().getMaxSpeed().getModifiedValue();
            float accel = car.getStats().getAcceleration().getModifiedValue();
            float turn = car.getStats().getMaxTurnRate().getModifiedValue();

            float speedScore = toTenScale(computeSpeedScore(maxSpeed));
            float accelScore = toTenScale(computeAccelScore(accel));
            float turnScore = toTenScale(computeTurnScore(turn));
            float overallScore = toTenScale((speedScore + accelScore + turnScore) / 3f);

            updateStatBar(overallBar, overallScore, overallScore);
            updateStatBar(speedBar, speedScore, maxSpeed);
            updateStatBar(accelBar, accelScore, accel);
            updateStatBar(turnBar, turnScore, turn);
        }

        private void updateStatBar(StatBar bar, float score, float displayValue) {
            if (bar == null || bar.fill == null || bar.valueLabel == null) return;
            float filledW = bar.barW * (score / 10f);
            bar.fill.getPosition().setSize(filledW, bar.barH);

            String valueText = bar.displayAsScore
                    ? String.format(Locale.ROOT, "%.1f/10", score)
                    : String.format(Locale.ROOT, "%.1f", displayValue);
            bar.valueLabel.setText(valueText);
        }

        private void updateButtonForStatus(BuyRowState row, TakeshidoTunerManager.UpgradeStatus status) {
            if (row == null || row.button == null) return;
            boolean show = status == TakeshidoTunerManager.UpgradeStatus.AVAILABLE
                    || status == TakeshidoTunerManager.UpgradeStatus.INSTALLED;
            if (!show) {
                row.button.setText("");
                setButtonEnabled(row.button, false);
                row.button.getPosition().setLocation(-10000f, -10000f);
                return;
            }

            String label = status == TakeshidoTunerManager.UpgradeStatus.INSTALLED ? "Remove" : "Buy";
            row.button.setText(label);
            setButtonEnabled(row.button, true);
            row.button.getPosition().setSize(row.buttonW, row.buttonH);
            row.button.getPosition().setLocation(row.buttonX, row.buttonY);
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return new TunerPanelPlugin(this);
        }
    }

    private class TunerPanelPlugin implements CustomUIPanelPlugin {
        private final TunerCustomDialog parent;

        private TunerPanelPlugin(TunerCustomDialog parent) {
            this.parent = parent;
        }

        @Override
        public void positionChanged(PositionAPI position) {
        }

        @Override
        public void renderBelow(float alphaMult) {
        }

        @Override
        public void render(float alphaMult) {
        }

        @Override
        public void advance(float amount) {
        }

        @Override
        public void processInput(List<InputEventAPI> events) {
        }

        @Override
        public void buttonPressed(Object buttonId) {
            if (!(buttonId instanceof String)) return;
            String id = (String) buttonId;

            if (BTN_CHANGE_CAR.equals(id)) {
                openCarPickerRequested = true;
                if (parent.callback != null) {
                    parent.callback.dismissCustomDialog(0);
                }
                return;
            }

            if (id.startsWith("tuner_cat:")) {
                String key = id.substring("tuner_cat:".length());
                try {
                    parent.setSelectedCategory(UpgradeCategory.valueOf(key));
                } catch (IllegalArgumentException ignored) {
                }
                return;
            }

            if (!id.startsWith(BTN_UPGRADE_PREFIX)) return;
            TakeshidoTunerConfig.UpgradeSpec spec = parent.idToSpec.get(id);
            if (spec == null) return;
            TakeshidoTunerManager.UpgradeStatus status = TakeshidoTunerManager.getUpgradeStatus(parent.car, spec);
            if (status == TakeshidoTunerManager.UpgradeStatus.AVAILABLE) {
                parent.handleInstantPurchase(spec);
            } else if (status == TakeshidoTunerManager.UpgradeStatus.INSTALLED) {
                parent.handleRemoveUpgrade(spec);
            }
        }
    }

    private void drawOutline(TooltipMakerAPI ui, float x, float y, float w, float h, float thickness, Color color) {
        drawRect(ui, x, y, w, thickness, color);
        drawRect(ui, x, y + h - thickness, w, thickness, color);
        drawRect(ui, x, y, thickness, h, color);
        drawRect(ui, x + w - thickness, y, thickness, h, color);
    }

    private StatBar addStatBar(TooltipMakerAPI ui, float x, float y, String label, float score, float barW, float barH, float displayValue, boolean displayAsScore) {
        ui.addPara(label, 0f);
        ui.getPrev().getPosition().inTL(x, y - 2f);
        ui.getPrev().getPosition().setLocation(x, y - 2f);

        float filledW = barW * (score / 10f);
        Color bg = new Color(60, 60, 60, 200);
        Color fg = new Color(245, 245, 245, 235);
        drawRect(ui, x + 90f, y, barW, barH, bg);
        UIComponentAPI fill = drawRect(ui, x + 90f, y, filledW, barH, fg);

        String valueText = displayAsScore
                ? String.format(Locale.ROOT, "%.1f/10", score)
                : String.format(Locale.ROOT, "%.1f", displayValue);
        LabelAPI valueLabel = ui.addPara(valueText, 0f, Misc.getHighlightColor(), valueText);
        valueLabel.getPosition().inTL(x + 90f + barW + 8f, y - 2f);
        valueLabel.getPosition().setLocation(x + 90f + barW + 8f, y - 2f);

        return new StatBar(fill, valueLabel, barW, barH, displayAsScore);
    }

    private UIComponentAPI drawRect(TooltipMakerAPI ui, float x, float y, float w, float h, Color color) {
        UIComponentAPI rect = ui.createRect(color, 1f);
        rect.getPosition().setSize(w, h);
        rect.getPosition().inTL(x, y);
        rect.getPosition().setLocation(x, y);
        ui.addCustomDoNotSetPosition(rect);
        return rect;
    }

    private static class BuyButtonState {
        private final ButtonAPI button;
        private final boolean enabled;

        private BuyButtonState(ButtonAPI button, boolean enabled) {
            this.button = button;
            this.enabled = enabled;
        }
    }

    private static class BuyRowState {
        private final TakeshidoTunerConfig.UpgradeSpec spec;
        private final UpgradeCategory category;
        private final ButtonAPI button;
        private final LabelAPI statusLabel;
        private final float buttonX;
        private final float buttonY;
        private final float buttonW;
        private final float buttonH;

        private BuyRowState(TakeshidoTunerConfig.UpgradeSpec spec, UpgradeCategory category, ButtonAPI button, LabelAPI statusLabel,
                            float buttonX, float buttonY, float buttonW, float buttonH) {
            this.spec = spec;
            this.category = category;
            this.button = button;
            this.statusLabel = statusLabel;
            this.buttonX = buttonX;
            this.buttonY = buttonY;
            this.buttonW = buttonW;
            this.buttonH = buttonH;
        }
    }


    @Override
    public void optionMousedOver(String optionText, Object optionData) {
    }

    @Override
    public void advance(float amount) {
        if (openCarPickerRequested) {
            openCarPickerRequested = false;
            pickCar(true);
            return;
        }
        if (openTunerRequested) {
            openTunerRequested = false;
            openTunerInterface();
        }
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public void backFromEngagement(EngagementResultAPI result) {
    }

    private static class StatBar {
        private final UIComponentAPI fill;
        private final LabelAPI valueLabel;
        private final float barW;
        private final float barH;
        private final boolean displayAsScore;

        private StatBar(UIComponentAPI fill, LabelAPI valueLabel, float barW, float barH, boolean displayAsScore) {
            this.fill = fill;
            this.valueLabel = valueLabel;
            this.barW = barW;
            this.barH = barH;
            this.displayAsScore = displayAsScore;
        }
    }
}
