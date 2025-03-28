package data.scripts.world.specialTaskForceNamers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import exerelin.ExerelinConstants;
import exerelin.campaign.intel.specialforces.namer.SpecialForcesNamer;
import exerelin.utilities.NexUtils;
import exerelin.utilities.StringHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class sevencorp_EptaNamer implements SpecialForcesNamer {

    public static final String FILE_PATH = "data/config/exerelin/specialForcesNames.json";

    public static final List<String> ORG_NAME = new ArrayList<>();
    public static final List<String> ORG_TYPE = new ArrayList<>();
    public static final List<String> FACTION_SIGNIFIER = new ArrayList<>();
    public static final String FORMAT;

    static {
        try {
            JSONObject json = Global.getSettings().getMergedJSONForMod(FILE_PATH, ExerelinConstants.MOD_ID);
            JSONArray org_name = json.getJSONArray("diktat_names1");
            JSONArray org_type = json.getJSONArray("diktat_names2");
            JSONArray faction_signifier = json.getJSONArray("diktat_names2");
            ORG_NAME.addAll(NexUtils.JSONArrayToArrayList(org_name));
            ORG_TYPE.addAll(NexUtils.JSONArrayToArrayList(org_type));
            FACTION_SIGNIFIER.addAll(NexUtils.JSONArrayToArrayList(faction_signifier));
            FORMAT = json.getString("diktat_nameFormat");
        }
        catch (IOException | JSONException ex) {
            throw new RuntimeException("Failed to load Epta Consortium special forces namer", ex);
        }
    }

    @Override
    public String getFleetName(CampaignFleetAPI fleet, MarketAPI origin, PersonAPI commander) {
        String one = NexUtils.getRandomListElement(ORG_NAME);
        String two = NexUtils.getRandomListElement(ORG_TYPE);
        String prepos = NexUtils.getRandomListElement(FACTION_SIGNIFIER);

        String name = StringHelper.substituteToken(FORMAT, "$one", one);
        name = StringHelper.substituteToken(name, "$preposition", prepos);
        name = StringHelper.substituteToken(name, "$two", two);

        return name;
    }
}
