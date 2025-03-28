package data.scripts.world.campaignListeners;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;

public class repMatcherCampaignListener extends BaseCampaignEventListener implements EveryFrameScript {
    String factionRepAnchor;
    String factionRepFollower;

    public repMatcherCampaignListener(boolean permaRegister, String faction_RepAnchor, String faction_RepFollower) {
        super(permaRegister);
        factionRepAnchor=faction_RepAnchor;
        factionRepFollower=faction_RepAnchor;
    }

    protected boolean isDone;

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    //nuke: I need a function that adjusts factionRepFollower's reps to match factionRepAnchor. I'd prefer to do this through a script that only activates when a rep change happens, instead of an everyframe that's constantly checking for rep changes, for performance reasons.
    //nuke: I asked histidine who says he doesn't know any way other than everyframing it, and he's pretty smart, so everyframe it is.
    //nuke: I just hope the performance hit isn't too bad...if it sucks too much, I'll have it only run on periodic intervals.

    @Override
    public void advance(float amount) {

    }
}