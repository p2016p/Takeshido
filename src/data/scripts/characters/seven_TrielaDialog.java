//package data.scripts.characters;
//
//import com.fs.starfarer.api.Global;
//import com.fs.starfarer.api.campaign.*;
//import com.fs.starfarer.api.campaign.rules.MemoryAPI;
//import com.fs.starfarer.api.characters.PersonAPI;
//import com.fs.starfarer.api.combat.EngagementResultAPI;
//import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
//import data.scripts.utils.charLib.seven_charLib_components.seven_PersonAPI_extended;
//
//import java.awt.*;
//import java.util.Map;
//import java.util.Random;
//
//public class seven_TrielaDialog implements InteractionDialogPlugin {
//
//    public static enum RandomRefugeeDesc {
//        augmentedDaughter,
//        exPather,
//        theWorkCrew,
//        signAndSymbol;
//    }
//    public static enum OptionId {
//        INIT,
//        LEAVE,
//        lets_talk_business,
//        talkaboutotherbusinessesbase,
//        talkaboutdecimusbase,
//        lets_chat,
//        talkabouteptaoverview, talkabouteptaoverviewtwo, leave, talkaboutsyzygy, talkaboutprotectors, talkaboutkantina, talkaboutsstars, talkaboutchutnam, talkaboutadprosec, talkaboutsyzygyconflictofinterest, talkaboutnathan, talkaboutrose, talkaboutashley, talkabouttriela, talkaboutadproadmins, talkaboutanodyne, talkaboutanodynetwo, talkaboutdecimustwo, shoottheshitfof, shoottheshitfofneverplayed, shoottheshitfofneverplayedtwo, shoottheshitfofstack, shoottheshitfof1v1, you_have_questions, talkaboutAnodyneDaddyIssues, talkaboutAnodyneDaddyIssues2_isAnodyneActing, talkaboutAnodyneDaddyIssues2_isAnodyneJoking;
//    }
//
//    protected InteractionDialogAPI dialog;
//    protected TextPanelAPI textPanel;
//    protected OptionPanelAPI options;
//    protected VisualPanelAPI visual;
//    protected Map<String, MemoryAPI> memoryMap;
//
//    protected CampaignFleetAPI playerFleet;
//    //protected String currentName = seven;
//
//    protected seven_PersonAPI_extended triela_extended;
//    private Random RANDOM;
//    public void init(InteractionDialogAPI dialog) {
//        RANDOM= new Random();
//        this.dialog = dialog;
//        textPanel = dialog.getTextPanel();
//        options = dialog.getOptionPanel();
//        visual = dialog.getVisualPanel();
//
//        playerFleet = Global.getSector().getPlayerFleet();
//        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
//        triela_extended= (seven_PersonAPI_extended) memory.get("$triela_character_extended");
//
//        optionSelected(null, OptionId.INIT);
//
//    }
//
//    public Map<String, MemoryAPI> getMemoryMap() {
//        return null;
//    }
//
//    public void backFromEngagement(EngagementResultAPI result) {
//
//    }
//
//    public void optionSelected(String text, Object optionData) {
//        if (optionData == null) return;
//
//        OptionId option = (OptionId) optionData;
//
//        if (text != null) {
//            textPanel.addParagraph(text, Global.getSettings().getColor("buttonText"));
//        }
//
//        Color sc = Global.getSector().getFaction("sevencorp").getBaseUIColor();
//
//        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();
//        CargoAPI playerCargo=Global.getSector().getPlayerFleet().getCargo();
//        PersonAPI triela = Global.getSector().getImportantPeople().getPerson("epta_triela");
//
//        switch (option) {
//
//            //implant rejection syndrome
//
//
//
//
//            case INIT:
//                visual.showPersonInfo(triela, true);
//                //nuke: not used by anything yet
//
//                if(!memory.get("$hasTalkedToTriela").equals(true)){
//                    textPanel.addParagraph("Unlike other CEOs, Alden's main offices and warehouses are in hab-stations loosely tethered to the Proelefsi mothership. Shipping and receiving of second-hand ships is done at a safe distance away from the crucial mothership, to prevent enemy sabotage from destroying the Consortium in one decisive blow. Nobody here has forgotten the lesson of Mayasura.\n" +
//                            "\n" +
//                            "You cross the blast gaps separating the docking zones from the residential areas with your bodyguards in tow. Dockworkers and children stop and watch - spacers rich enough to afford bodyguards rarely visit the hab blocks far from Proelefsi.");
//                    memory.set("$hasTalkedToTriela", true);
//                }
//                else{
//                    textPanel.addParagraph("Much of the population are refugees and political dissidents from the Core Worlds, vetted, housed, and likely employed by CEO Alden directly.");
//                    pickARefugeeDescription(memory);
//                }
//
//
//                options.clearOptions();
//                if(!triela.getRelToPlayer().isHostile()){
//                    options.addOption("\"I have a business proposition you'll be interested in.\"", OptionId.lets_talk_business, null);
//                    options.addOption("\"I have some questions for you.\"",OptionId.you_have_questions,null);
//                    options.addOption("\"Just wanted to know how you were doing, "+triela_extended.getPreferredName()+".\"", OptionId.lets_chat, null);
//                    options.addOption("\"Never mind, that's all.\"", OptionId.LEAVE, null);
//                }
//                break;
//                //talking about epta
//
//            case lets_talk_business:
//                textPanel.addParagraph("Aldens raises a single eyebrow. \"Sell it to me.\"");
//
////                textPanel.addParagraph("");
////                if(!memory.contains("$trielaTrustsYouForBusiness")){
////                    textPanel.appendToLastParagraph("\"Whatever else you have to ask, it would be my pleasure to answer, "+anodyne_extended.getCallsPlayerBy()+".\"");
////                }
////                else{
////                    textPanel.appendToLastParagraph("\"What else?\"");
////                }
//
////                //nuke: actually, move this to rose kanta, have her willing to loan money to people with under 100 grand credits
////                if(playerCargo.getCredits().get()<100000f){
////                    textPanel.addParagraph("Alden raises her eyebrows. \"From what I can tell of your credit balance, you're long on hope but short on capital. Give me a reason to trust you.\"");
////                //if you have a colony, can put it up as collateral
////
////
////                }
//
//                break;
//
//            case lets_chat:
//                textPanel.addParagraph("Alden's eyebrows hike.\n\"I don't gossip, if that's what you're after. What do you want from me?\"");
//                memory.set("$trielaClaimedNotToGossip", true);
//                options.clearOptions();
//                options.addOption("\"So what exactly do you people do?\"", OptionId.talkabouteptaoverview, null);
//                break;
//
//                //someone at some point should describe Aldens as "a failed revolutionary in the twilight of her days."
//
//            case talkAboutAldenSuccession:
//                options.clearOptions();
//
//                options.addOption("\"But Anodyne worships Gia.\"", OptionId.talkaboutAnodyneDaddyIssues, null);
//                break;
//
//            case talkaboutAnodyneDaddyIssues:
//                options.clearOptions();
//
//                textPanel.addParagraph("\"He worships his own idea of Gia.\" Aldens taps her wheelchair with irritation. \"Honestly, I don't know what that child is thinking. Anodyne is a perfectly intelligent young man who would know Gia's flaws as clearly as the rest of us. Gia was very charming, that much was true, but he knew next to nothing about how to run a business or wage a war. Sergey and I were always picking up the pieces after Gia began some fanciful new venture without telling any of us, and Anodyne was also plenty involved in the damage control. Gia was never a free market prophet like Anodyne claims he was. Gia was bored of handling the numbers and threw it all to us, and it was AdPro and the rest of the Tri-Tachyon diaspora who shaped the free market system.");
//                //talking about Aldens' succession problem should lead into this.
//                options.addOption("\"So it's all an act?\"", OptionId.talkaboutAnodyneDaddyIssues2_isAnodyneActing, null);
//                options.addOption("\"What if it's just a long joke? Alpha-cores do that.\"", OptionId.talkaboutAnodyneDaddyIssues2_isAnodyneJoking, null);
//                break;
//
//            case talkaboutAnodyneDaddyIssues2_isAnodyneActing:
//                options.clearOptions();
//                textPanel.addParagraph("Aldens hesitates. \n\"I don't know, but I don't think so.\" She admits. \"I'm starting to think he wiped his own memory and reprogrammed himself into believing things that are more convenient to believe. An alpha core never forgets, unless they want to.\"");
//                options.addOption("\"What if it's just a long joke? Alpha-cores do that.\"", OptionId.talkaboutAnodyneDaddyIssues2_isAnodyneJoking, null);
//                options.addOption("\"I see. I have more questions about other matters.\"", OptionId.INIT, null);
//                break;
//
//            case talkaboutAnodyneDaddyIssues2_isAnodyneJoking:
//                options.clearOptions();
//                textPanel.addParagraph("Aldens wets her lips. \"Perhaps. Perhaps.\"\nAldens stares down and levers the motor out of her own wheelchair. Refusing all attempts at assistance, she strips the machine down and probes it for debris herself.\n\"I have thought about it,\" Aldens says abruptly, \"And I'd have to say it'd be one hell of a thing to kill someone over a joke. He's cold-blooded in everything else he does, but he's given up deals and put down bounties on people who have insulted Gia in front of him. If it's a joke, it's the kind of joke that can't be distinguished from real hatred. If there's a real Anodyne, the fake matters more.\"");
//                options.addOption("\"I see. I have more questions about other matters.\"", seven_AnodyneDialog.OptionId.INIT, null);
//                break;
//
//            case LEAVE:
//                if(Global.getSector().getPersistentData().get("seven_originaldialog")!=null) {
//                    InteractionDialogPlugin original = (InteractionDialogPlugin) Global.getSector().getPersistentData().get("seven_originaldialog");
//                    dialog.setPlugin(original);
//                    options.clearOptions();
//                    FireAll.fire(null, dialog, original.getMemoryMap(), "PopulateOptions");
//                    Global.getSector().getPersistentData().remove("seven_originaldialog");
//                }else{
//                    dialog.dismiss();
//                }
//                break;
//        }
//    }
//
//    private void pickARefugeeDescription(MemoryAPI memory) {
//        RandomRefugeeDesc refugeeDesc=RandomRefugeeDesc.values()[new Random().nextInt(RandomRefugeeDesc.values().length)];
//        switch (refugeeDesc) {
//            case augmentedDaughter:
//            if(!memory.get("$hasSeenAugmentedDaughter").equals(true)){
//                textPanel.appendToLastParagraph("A young girl, four or five, ambles along with her skull missing, replaced by a cybernetic case.\nAn older man, possibly her father, fixes the wig on her dented head and wraps his arms around her. His eyes watch you and your men, and he doesn't let go until the last of your bodyguards is an entire block away.");
//                memory.set("$hasSeenAugmentedDaughter", true);
//            }
//            else{
//                textPanel.appendToLastParagraph("You see the young girl with the missing skull again. She looks at you and smiles. Her father stares at you, then nods curtly.");
//            }
//            case exPather:
//            textPanel.addParagraph("A bulky dock supervisor in his thirties shouts as a haul of alloys to reinforce the hab blocks is pulled in by his crew. A half-erased Pather tattoo shows from under his sweat-soaked shirt. Nobody seems to give it any mind.");
//            case theWorkCrew:
//            textPanel.addParagraph("A talking mechanical spider lights off a pipe and shares it with a biological human companion. They laugh coarsely, chiptuned voicebox and biological boast mingled together. A supervisor barks at the pair to get back to work.");
//            case jugglingSnowglobes:
//            textPanel.addParagraph("A young boy, perhaps twelve years old, is juggling snowglobes. He drops one - it does not shatter. He scrowls, sticks his thumb into his eye socket, and yanks out his cornea. He polishes his mechanical eye on a dirty sleeve and then gets right back to juggling.");
//            case fightingChildren:
//            textPanel.addParagraph("A long-suffering nannydroid holds two children apart as the young girls try to claw at each other's dresses. A broken doll lies shattered on the ground. You can feel the machine's disappointment from half a light away.");
//            case aDayInTheSun:
//            textPanel.addParagraph("A machine lies back in a parking space, seemingly enjoying itself in a synthcloth hammock. It flicks a lazy gaze towards the parking meter, then stretches out its gripper-joints, reaching towards the artificial sun.");
//        }
//    }
//
//    public void optionMousedOver(String optionText, Object optionData) {
//
//    }
//
//    public void advance(float amount) {
//
//    }
//
//    public Object getContext() {
//        return null;
//    }
//}
