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

public class DeckDeserializer implements JsonDeserializer<Deck> {
    @Override
    public Deck deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int size = jsonObject.get("size").getAsInt();
        JsonArray cardsArray = jsonObject.getAsJsonArray("cards");

        Array<Card> cards = new Array<>();
        if (cardsArray != null) {
            for (JsonElement cardElement : cardsArray) {
                Card card = context.deserialize(cardElement, Card.class);
                cards.add(card);
            }
        }

        return new Deck(size, cards);
    }
}
