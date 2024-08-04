package com.srebot.uno.config.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Deck;

import java.lang.reflect.Type;

public class DeckSerializer implements JsonSerializer<Deck> {
    @Override
    public JsonElement serialize(Deck deck, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("size", deck.getSize());

        // Serialize the cards list directly without "items", "size", and "ordered"
        jsonObject.add("cards", context.serialize(deck.getCards().items));

        return jsonObject;
    }
}




