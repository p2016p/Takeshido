package data.scripts.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.loading.Description;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class seven_util_descriptions {

    private static org.apache.log4j.Logger log = Global.getLogger(seven_util_descriptions.class);
    private void replaceDescriptionOfEpta(String descID, String newDesc) throws JSONException, IOException {
        replaceDescriptionOfSpecificMod("seven_nexus", descID, newDesc);
    }


//    private void prependOntoDescriptionOfEpta(String descID, String newDesc){
//        replaceDescriptionOfSpecificMod("seven_nexus", descID, newDesc);
//    }


    //nuke: modified version of the method from WhichMod
    //nuke: thanks to ruddy for pointing out this path of investigation

    //nuke: my specific rationale for having to input a specific mod is performance, as in not having to loop through every single mod

    private void replaceDescriptionOfSpecificMod(String modID, String descID, String newDesc) throws JSONException, IOException {
        SettingsAPI settings = Global.getSettings();
        JSONArray csvData;
        // try loading descriptions.csv; not every mod has one
        try
        {
            csvData = settings.loadCSV("data/strings/descriptions.csv", modID);
            for (int i = 0; i < csvData.length(); i++) {
                try {
                    JSONObject row = csvData.getJSONObject(i);
                    String id = row.getString("id");
                    String type = row.getString("type");
                    if(id.equals("descID")){
                        Description.Type descType = getType(type);
                        Description desc = settings.getDescription(id, descType);
                        desc.setText1(newDesc);
                    }
                } catch (JSONException e) {
                    log.info("Epta has failed while reading descriptions.csv from " + modID);
                }
            }
        } catch (JSONException e)
        {
            log.info("Epta Consortium has attempted to load a descriptions.csv from " + modID + " but failed.");
        }
    }

    @Nullable
    public Description.Type getType(String type)
    {
        switch (type)
        {
            case "SHIP":
                return Description.Type.SHIP;
            case "WEAPON":
                return Description.Type.WEAPON;
            case "ASTEROID":
                return Description.Type.ASTEROID;
            case "SHIP_SYSTEM":
                return Description.Type.SHIP_SYSTEM;
            case "CUSTOM":
                return Description.Type.CUSTOM;
            case "ACTION_TOOLTIP":
                return Description.Type.ACTION_TOOLTIP;
            case "FACTION":
                return Description.Type.FACTION;
            case "PLANET":
                return Description.Type.PLANET;
            case "RESOURCE":
                return Description.Type.RESOURCE;
            case "TERRAIN":
                return Description.Type.TERRAIN;
            default:
                return null;
        }
    }
}
