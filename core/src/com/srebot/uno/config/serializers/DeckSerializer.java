package com.srebot.uno.config.serializers;

import com.badlogic.gdx.utils.Array;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;

import java.lang.reflect.Type;

public class DeckSerializer implements JsonSerializer<Deck> {
    @Override
    public JsonElement serialize(Deck deck, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", deck.getId());
        jsonObject.addProperty("size", deck.getSize());

        //TODO: does not filter null
        // Get the filtered list of cards
        Array<Card> filteredCards = new Array<>();
        for (Card card : deck.getCards()) {
            if (card != null) {
                filteredCards.add(card);
            }
        }

        // Serialize the cards list directly without "items", "size", and "ordered"
        jsonObject.add("cards", context.serialize(filteredCards.items));
        //jsonObject.add("cards", context.serialize(deck.getCards().items));

        return jsonObject;
    }

}




