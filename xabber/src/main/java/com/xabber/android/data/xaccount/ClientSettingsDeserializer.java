package com.xabber.android.data.xaccount;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 21.07.17.
 */

public class ClientSettingsDeserializer implements JsonDeserializer<AuthManager.ListClientSettingsDTO> {

    private List<AuthManager.ClientSettingsDTO> items;
    private List<AuthManager.DeletedDTO> deleted;

    @Override
    public AuthManager.ListClientSettingsDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        items = new ArrayList<>();
        deleted = new ArrayList<>();

        JsonArray array = json.getAsJsonObject().get("settings_data").getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement jsonElement = array.get(i);
            AuthManager.ClientSettingsDTO settingsItem = context.deserialize(jsonElement, AuthManager.ClientSettingsDTO.class);
            items.add(settingsItem);
        }

        JsonElement orderData = json.getAsJsonObject().get("order_data").getAsJsonObject();
        AuthManager.OrderDataDTO orderDataDTO = context.deserialize(orderData, AuthManager.OrderDataDTO.class);

        JsonArray deletedArray = json.getAsJsonObject().get("deleted").getAsJsonArray();
        for (int i = 0; i < deletedArray.size(); i++) {
            JsonElement jsonElement = deletedArray.get(i);
            AuthManager.DeletedDTO deletedDTO = context.deserialize(jsonElement, AuthManager.DeletedDTO.class);
            deleted.add(deletedDTO);
        }

        return new AuthManager.ListClientSettingsDTO(items, orderDataDTO, deleted);
    }
}
