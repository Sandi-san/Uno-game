package com.srebot.uno.config.deserializers;

import com.badlogic.gdx.utils.Array;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Hand;

import java.lang.reflect.Type;

public class HandDeserializer implements JsonDeserializer<Hand> {
    @Override
    public Hand deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int indexFirst = jsonObject.get("indexFirst").getAsInt();
        int indexLast = jsonObject.get("indexLast").getAsInt();

        JsonArray cardsArray = jsonObject.getAsJsonArray("cards");
        Array<Card> cards = new Array<>();

        if (cardsArray != null) {
            for (JsonElement cardElement : cardsArray) {
                Card card = context.deserialize(cardElement, Card.class);
                cards.add(card);
            }
        }

        return new Hand(indexFirst, indexLast, cards);
    }
}
