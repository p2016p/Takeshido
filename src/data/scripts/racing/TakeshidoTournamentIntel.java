package data.scripts.racing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TakeshidoTournamentIntel extends BaseIntelPlugin {
    private final String tournamentId;

    public TakeshidoTournamentIntel(String tournamentId) {
        this.tournamentId = tournamentId;
        this.important = true;
    }

    public String getTournamentId() {
        return tournamentId;
    }

    @Override
    public String getName() {
        return "Takeshido Racing Tournament";
    }

    @Override
    public String getIcon() {
        return "graphics/icons/intel/mission.png";
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state == null || !state.tournamentId.equals(tournamentId)) {
            info.addPara("Tournament data unavailable.", 0f);
            return;
        }

        TakeshidoRacingConfig config = TakeshidoRacingConfig.get();
        TakeshidoRacingConfig.CategorySpec cat = config.getCategory(state.categoryId);
        String catLabel = cat != null ? cat.label : state.categoryId;

        info.addPara("Category: " + catLabel, 0f);
        info.addPara("Race " + (state.currentRaceIndex + 1) + " of " + state.racesPerTournament, 0f);

        if (state.currentRaceIndex < state.racesPerTournament) {
            CampaignClockAPI clock = Global.getSector().getClock();
            int currentDay = clock != null ? (clock.getCycle() * 360) + ((clock.getMonth() - 1) * 30) + (clock.getDay() - 1) : 0;
            int daysLeft = state.nextRaceDay - currentDay;
            if (daysLeft < 0) daysLeft = 0;
            MarketAPI market = state.nextMarketId != null ? Global.getSector().getEconomy().getMarket(state.nextMarketId) : null;
            String loc = market != null ? market.getName() : "unknown";
            info.addPara("Next race: in " + daysLeft + " days on " + loc, 0f);
        }

        addStandings(info, state);
    }

    private void addStandings(TooltipMakerAPI info, TakeshidoRacingManager.TournamentState state) {
        List<TakeshidoRacingManager.TournamentRacer> racers = new ArrayList<>(state.racers);
        Collections.sort(racers, new Comparator<TakeshidoRacingManager.TournamentRacer>() {
            @Override
            public int compare(TakeshidoRacingManager.TournamentRacer a, TakeshidoRacingManager.TournamentRacer b) {
                return Integer.compare(b.points, a.points);
            }
        });

        info.addPara("Standings:", 10f);
        float pad = 3f;
        for (int i = 0; i < racers.size(); i++) {
            TakeshidoRacingManager.TournamentRacer r = racers.get(i);
            String line = (i + 1) + ". " + r.name + " - " + r.points + " pts";
            Color color = r.isPlayer ? Global.getSettings().getColor("textFriendColor") : null;
            if (color != null) {
                info.addPara(line, pad, color, line);
            } else {
                info.addPara(line, pad);
            }
        }
    }

    @Override
    public SectorEntityToken getMapLocation(com.fs.starfarer.api.ui.SectorMapAPI map) {
        TakeshidoRacingManager.TournamentState state = TakeshidoRacingManager.getTournamentState();
        if (state == null || !state.tournamentId.equals(tournamentId)) return null;
        if (state.nextMarketId == null) return null;
        MarketAPI market = Global.getSector().getEconomy().getMarket(state.nextMarketId);
        if (market == null) return null;
        return market.getPrimaryEntity();
    }


    @Override
    public IntelInfoPlugin.IntelSortTier getSortTier() {
        return IntelInfoPlugin.IntelSortTier.TIER_2;
    }

    @Override
    public String getSortString() {
        return "Takeshido Tournament";
    }
}
