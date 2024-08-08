package com.srebot.uno.config.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;

import java.lang.reflect.Type;

public class CardSerializer implements JsonSerializer<Card> {
    @Override
    public JsonElement serialize(Card card, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        // Only include the fields that you want to serialize
        jsonObject.addProperty("priority", card.getPriority());
        jsonObject.addProperty("value", card.getValue());
        jsonObject.addProperty("color", card.getColor());
        jsonObject.addProperty("texture", card.getTexture());

        // Exclude "position", "bounds", and "isHighlighted"

        return jsonObject;
    }
}




