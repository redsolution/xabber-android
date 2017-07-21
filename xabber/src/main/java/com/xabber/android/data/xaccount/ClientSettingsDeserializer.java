package com.xabber.android.data.xaccount;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by valery.miller on 21.07.17.
 */

public class ClientSettingsDeserializer implements JsonDeserializer<AuthManager.ListClientSettingsDTO> {

    private List<AuthManager.ClientSettingsDTO> items;

    @Override
    public AuthManager.ListClientSettingsDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        items = new ArrayList<>();

        JsonArray array = json.getAsJsonArray();
        for (int i = 0; i < array.size(); i++) {
            JsonElement jsonElement = array.get(i);
            AuthManager.ClientSettingsDTO settingsItem = context.deserialize(jsonElement, AuthManager.ClientSettingsDTO.class);
            items.add(settingsItem);
        }
        return new AuthManager.ListClientSettingsDTO(items);
    }
}
