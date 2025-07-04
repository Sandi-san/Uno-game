package com.srebot.uno.config.deserializers;

import com.badlogic.gdx.utils.Array;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;

import java.lang.reflect.Type;

/** For deserializing Deck and all its connected objects */
public class DeckDeserializer implements JsonDeserializer<Deck> {
    @Override
    public Deck deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int id = jsonObject.get("id").getAsInt();
        int size = jsonObject.get("size").getAsInt();
        JsonArray cardsArray = jsonObject.getAsJsonArray("cards");

        //deserialize each Card in Deck separately
        Array<Card> cards = new Array<>();
        if (cardsArray != null) {
            for (JsonElement cardElement : cardsArray) {
                Card card = context.deserialize(cardElement, Card.class);
                cards.add(card);
            }
        }

        return new Deck(id, size, cards);
    }
}
