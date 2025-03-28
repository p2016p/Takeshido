package data.scripts.utils.interactionUI;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.util.MutableValue;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;

import java.awt.*;
import java.util.Map;

public class seven_RacecoordinatorDialog implements InteractionDialogPlugin {

    public static enum OptionId {
        INIT,
        CONT,
        askaboutraces, askaboutracers, askabouttrack, racer1, racer2, racer3, racer4, racer5, racer6, makebet, betracer1, betracer2, betracer3, betracer4, betracer5, betracer6, money0, money1, money2, money3, money4, money5, watchracetune;
    }

    protected InteractionDialogAPI dialog;
    protected TextPanelAPI textPanel;
    protected OptionPanelAPI options;
    protected VisualPanelAPI visual;
    protected Map<String, MemoryAPI> memoryMap;

    protected CampaignFleetAPI playerFleet;

    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();

        playerFleet = Global.getSector().getPlayerFleet();

        optionSelected(null, OptionId.INIT);
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void backFromEngagement(EngagementResultAPI result) {

    }

    public void optionSelected(String text, Object optionData) {
        if (optionData == null) return;

        OptionId option = (OptionId) optionData;

        if (text != null) {
            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
        }

        Color sc = Global.getSector().getFaction("sevencorp").getBaseUIColor();

        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        PersonAPI pilot = null;
        FleetMemberAPI car = null;
        Object[][] racerarray;
        float playermoney = playerFleet.getCargo().getCredits().get();

        switch (option) {
            case INIT:
                //visual.showPersonInfo((PersonAPI) memory.get("$racecoordinator"));

                options.clearOptions();
                if(memory.contains("$recentracedate")&&memory.get("$recentracedate").equals(Global.getSector().getClock().getDay())){
                    textPanel.addParagraph("We've already had our race for today though, you'll have to come back tomorrow of you wanna bet.");
                    options.addOption("\"Bye\"", OptionId.CONT, null);
                }else {
                    if(!memory.contains("$seven_pilotupdatedate")||!memory.get("$seven_pilotupdatedate").equals(Global.getSector().getClock().getDay())) {
                        WeightedRandomPicker<String> cars = new WeightedRandomPicker<>();
                        cars.add("seven_hobi_custom", 10);
                        cars.add("seven_350x_custom", 8);
                        cars.add("seven_camirillo_custom", 6);
                        cars.add("seven_BR97_custom", 4);
                        cars.add("seven_NMW_G3_custom", 2);
                        cars.add("seven_vroomicorn_custom", 1);
                        cars.add("seven_bonta_custom", 0.5f);
                        cars.add("seven_bionda_custom", 0.5f);

                        Object[][] racerarrayinit = new Object[6][2];
                        for (int i = 0; i < 6; i++) {
                            racerarrayinit[i][0] = OfficerManagerEvent.createOfficer(Global.getSector().getFaction("pirates"), MathUtils.getRandomNumberInRange(3, 8));
                            racerarrayinit[i][1] = Global.getFactory().createFleetMember(FleetMemberType.SHIP, cars.pick() + "_Hull");
                        }

                        memory.set("$racerarray", racerarrayinit);
                        memory.set("$seven_pilotupdatedate", Global.getSector().getClock().getDay());
                    }
                    options.addOption("\"What races?\"", OptionId.askaboutraces, null);
                    options.addOption("\"Who's racing?\"", OptionId.askaboutracers, null);
                    //options.addOption("\"Which track are they racing on?\"", OptionId.askabouttrack, null);
                    options.addOption("\"I would like to place a bet.\"", OptionId.makebet, null);
                    options.addOption("\"Bye\"", OptionId.CONT, null);
                }
                break;

                //what is this
            case askaboutraces:
                textPanel.addParagraph("\"Ah you really are a bit of a skidmark huh. Well that's alright; basically what we've got here is a series of races that you can bet on, and I'm talking real racing, old earth style, not this new wave digital crap. You bet on the winner and you can walk away with a nice chunk of change, but it takes a bit of knowhow about the tracks and teams to really be consistent with it.\"");

                options.clearOptions();
                options.addOption("\"Alright, I'll think about it\"", OptionId.INIT, null);
                break;

            case askaboutracers:
                textPanel.addParagraph("\"We've got\n");
                racerarray = (Object[][]) memory.get("$racerarray");
                for (int i = 0; i < 6; i++) {
                    if(i==6){
                        textPanel.addParagraph("and" + ((PersonAPI) racerarray[i][0]).getName().getFullName() + " in a " + ((FleetMemberAPI) racerarray[i][1]).getHullSpec().getHullName() + ".\"");
                    }else {
                        textPanel.addParagraph(((PersonAPI) racerarray[i][0]).getName().getFullName() + " in a " + ((FleetMemberAPI) racerarray[i][1]).getHullSpec().getHullName() + ",");
                    }
                }

                options.clearOptions();
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[0][0]).getName().getFullName()+" and their car\"", OptionId.racer1, null);
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[1][0]).getName().getFullName()+" and their car\"", OptionId.racer2, null);
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[2][0]).getName().getFullName()+" and their car\"", OptionId.racer3, null);
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[3][0]).getName().getFullName()+" and their car\"", OptionId.racer4, null);
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[4][0]).getName().getFullName()+" and their car\"", OptionId.racer5, null);
                options.addOption("\"Tell me more about "+((PersonAPI) racerarray[5][0]).getName().getFullName()+" and their car\"", OptionId.racer6, null);
                break;

            case racer1:
            case racer2:
            case racer3:
            case racer4:
            case racer5:
            case racer6:
                int c = 1;
                if(option.equals(OptionId.racer1)) c = 1;
                else if(option.equals(OptionId.racer2)) c = 2;
                else if(option.equals(OptionId.racer3)) c = 3;
                else if(option.equals(OptionId.racer4)) c = 4;
                else if(option.equals(OptionId.racer5)) c = 5;
                else if(option.equals(OptionId.racer6)) c = 6;

                pilot = (PersonAPI) ((Object[][]) memory.get("$racerarray"))[c-1][0];
                car = (FleetMemberAPI) ((Object[][]) memory.get("$racerarray"))[c-1][1];
                visual.showPersonInfo(pilot);

                String skill = "average";
                if(pilot.getStats().getLevel()<=2) skill = "mediocre";
                else if (pilot.getStats().getLevel()<=4) skill = "average";
                else if (pilot.getStats().getLevel()<=6) skill = "skilled";
                else skill = "phenomenal";

                String upkeep = "well maintained";
                if(car.getVariant().hasDMods()) upkeep = "poorly maintained";

                String carstyle = "good car";
                if(car.getHullSpec().getHullId().equals("seven_hobi_custom")) carstyle = "a jack of all trades, possessing decent speed, acceleration, and cornering ability, but not really excelling in any one area";
                else if(car.getHullSpec().getHullId().equals("seven_350x_Custom")) carstyle = "an older model with decent offroading capabilities, but not much else going for it";
                else if(car.getHullSpec().getHullId().equals("seven_camirillo_Custom")) carstyle = "a bulky ponderous vehicle without much going for it besides a good quarter mile and an overengineered frame";
                else if(car.getHullSpec().getHullId().equals("seven_BR97_Custom")) carstyle = "a car with a unique drivetrain that allows it to excel in extremely technical city roads";
                else if(car.getHullSpec().getHullId().equals("seven_NMW_G3_Custom")) carstyle = "a powerful car with great handling and speed without any obvious flaws";
                else if(car.getHullSpec().getHullId().equals("seven_vroomicorn_Custom")) carstyle = "a car with ridiculous low range acceleration and decent handling, but relatively poor top speed";
                else if(car.getHullSpec().getHullId().equals("seven_bonta_Custom")) carstyle = "a stellar car with phenomenal performance across the board";
                else if(car.getHullSpec().getHullId().equals("seven_350x_Custom")) carstyle = "a phenomenal car with stellar performance across the board";

                float victoryodds = pilot.getStats().getLevel()/8f;

                textPanel.addParagraph( pilot.getName().getFullName() + " is a " + skill + " racer and drives a " + upkeep + " " + car.getHullSpec().getHullName() + ". The " + car.getHullSpec().getHullName() + " is " + carstyle + " and " + pilot.getName().getFullName() + " has a " + victoryodds + " percent chance of winning.");

                options.clearOptions();
                options.addOption("\"Tell me about some of the other racers.\"", OptionId.askaboutracers, null);
                options.addOption("\"Tell me about something else.\"", OptionId.INIT, null);

                break;

            case makebet:
                racerarray = (Object[][]) memory.get("$racerarray");
                textPanel.addParagraph("\"Excellent, who would you like to bet on?\"");

                options.clearOptions();
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[0][0]).getName().getFullName()+"\"", OptionId.betracer1, null);
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[1][0]).getName().getFullName()+"\"", OptionId.betracer2, null);
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[2][0]).getName().getFullName()+"\"", OptionId.betracer3, null);
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[3][0]).getName().getFullName()+"\"", OptionId.betracer4, null);
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[4][0]).getName().getFullName()+"\"", OptionId.betracer5, null);
                options.addOption("\"I'll bet on "+((PersonAPI) racerarray[5][0]).getName().getFullName()+"\"", OptionId.betracer6, null);

                break;

            case betracer1:
            case betracer2:
            case betracer3:
            case betracer4:
            case betracer5:
            case betracer6:
                int bo = 1;
                if(option.equals(OptionId.betracer1)) bo = 1;
                else if(option.equals(OptionId.betracer2)) bo = 2;
                else if(option.equals(OptionId.betracer3)) bo = 3;
                else if(option.equals(OptionId.betracer4)) bo = 4;
                else if(option.equals(OptionId.betracer5)) bo = 5;
                else if(option.equals(OptionId.betracer6)) bo = 6;

                memory.set("$seven_racerbeton",bo);

                textPanel.addParagraph("\"got it, and how much do you wanna put down?\"");

                options.clearOptions();
                options.addOption("\"all in ("+Math.min(playermoney,2500000f)+" credits)\"", OptionId.money0, null);
                if(playermoney>=1000f) options.addOption("\"1000 credits\"", OptionId.money1, null);
                if(playermoney>=5000f) options.addOption("\"5000 credits\"", OptionId.money2, null);
                if(playermoney>=25000f) options.addOption("\"25000 credits\"", OptionId.money3, null);
                if(playermoney>=100000f) options.addOption("\"100000 credits\"", OptionId.money4, null);
                if(playermoney>=500000f) options.addOption("\"500000 credits\"", OptionId.money5, null);
                options.addOption("\"Nevermind, I changed my mind.\"", OptionId.INIT, null);

                break;

            case money0:
            case money1:
            case money2:
            case money3:
            case money4:
            case money5:
                float ba = 0f;
                if(option.equals(OptionId.money0)) ba = Math.min(playermoney,2500000f);
                else if(option.equals(OptionId.money1)) ba = 1000f;
                else if(option.equals(OptionId.money2)) ba = 5000f;
                else if(option.equals(OptionId.money3)) ba = 25000f;
                else if(option.equals(OptionId.money4)) ba = 100000f;
                else if(option.equals(OptionId.money5)) ba = 500000f;
                playerFleet.getCargo().getCredits().subtract(ba);
                memory.set("$seven_racerbetamount",ba);

                textPanel.addParagraph("\"Alrighty, your bet is locked in. You can come down to the surface to watch in person or you can tune in from your ship, race starts at noon.\"");

                options.clearOptions();
                options.addOption("tune in from your ship", OptionId.watchracetune, null);

                break;

            case watchracetune:
                racerarray = (Object[][]) memory.get("$racerarray");
                PersonAPI winner = (PersonAPI)racerarray[MathUtils.getRandomNumberInRange(0,5)][0];
                textPanel.addParagraph("You pull up the race on your holo projector. Six racing machines of undoubtedly high caliber line up at the starting line and rocket off a few moments later, spraying asphalt and hot air at the haphazardly arranged crowd behind them. \n The race is fast and dangerous with many sharp turns that your brain can barely keep up with just as a spectator. \n Within minutes the racers rocket over the finish line and a winner is chosen.");
                textPanel.addParagraph(winner.getName().getFullName()+" walks up to the podium to collect their trophy and winnings, chugging champagne and flipping wads of credits at the provocatively dressed women that stand trackside.");
                if(winner.equals(memory.get("$seven_racerbeton"))){
                    playerFleet.getCargo().getCredits().add(Math.max((Float) memory.get("$seven_racerbetamount")*1.2f,5000f));
                    textPanel.addParagraph("Your bet winnings totalling "+Math.max((Float) memory.get("$seven_racerbetamount")*1.2f,5000f)+" are wired to your account shortly afterwards");
                }else{
                    textPanel.addParagraph("Unfortunately this bet didn't quite pan out for you, better luck next time");
                }
                memory.set("$recentracedate",Global.getSector().getClock().getDay());

                options.clearOptions();
                options.addOption("close the race holo", OptionId.CONT, null);

                break;

            case CONT:
                if(Global.getSector().getPersistentData().get("seven_originaldialog")!=null) {
                    InteractionDialogPlugin original = (InteractionDialogPlugin) Global.getSector().getPersistentData().get("seven_originaldialog");
                    dialog.setPlugin(original);
                    options.clearOptions();
                    FireAll.fire(null, dialog, original.getMemoryMap(), "PopulateOptions");
                    Global.getSector().getPersistentData().remove("seven_originaldialog");
                }else{
                    dialog.dismiss();
                }
                break;
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
