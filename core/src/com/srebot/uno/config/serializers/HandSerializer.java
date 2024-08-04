package com.srebot.uno.config.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Hand;

import java.lang.reflect.Type;

public class HandSerializer implements JsonSerializer<Hand> {
    @Override
    public JsonElement serialize(Hand hand, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("indexFirst", hand.getIndexFirst());
        jsonObject.addProperty("indexLast", hand.getIndexLast());

        // Serialize the cards list directly without "items", "size", and "ordered"
        jsonObject.add("cards", context.serialize(hand.getCards().items));

        return jsonObject;
    }
}