package com.srebot.uno.config.serializers;

import com.badlogic.gdx.utils.Array;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Hand;

import java.lang.reflect.Type;

public class HandSerializer implements JsonSerializer<Hand> {
    @Override
    public JsonElement serialize(Hand hand, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();


        jsonObject.addProperty("id", hand.getId());
        jsonObject.addProperty("indexFirst", hand.getIndexFirst());
        jsonObject.addProperty("indexLast", hand.getIndexLast());

        // Get the filtered list of cards
        Array<Card> filteredCards = new Array<>();
        for (Card card : hand.getCards()) {
            if (card != null) {
                filteredCards.add(card);
            }
        }

        // Serialize the cards list directly without "items", "size", and "ordered"
        jsonObject.add("cards", context.serialize(filteredCards.items));
        //jsonObject.add("cards", context.serialize(hand.getCards().items));

        return jsonObject;
    }
}