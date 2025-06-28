package com.srebot.uno.config.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.srebot.uno.classes.Card;

import java.lang.reflect.Type;

public class CardDeserializer implements JsonDeserializer<Card> {
    @Override
    public Card deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int id = jsonObject.get("id").getAsInt();
        int priority = jsonObject.get("priority").getAsInt();
        int value = jsonObject.get("value").getAsInt();
        String color = jsonObject.get("color").getAsString();
        String texture = jsonObject.get("texture").getAsString();

        return new Card(id, priority, value, color, texture);
    }
}
