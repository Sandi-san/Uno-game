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
import com.srebot.uno.classes.Player;

import java.lang.reflect.Type;

public class PlayerDeserializer implements JsonDeserializer<Player> {
    @Override
    public Player deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        int id = jsonObject.get("id").getAsInt();
        String name = jsonObject.get("name").getAsString();
        int score = jsonObject.get("score").getAsInt();

        //if Hand in DB is null: set null, else: copy
        Hand hand = null;
        if (jsonObject.has("hand") && !jsonObject.get("hand").isJsonNull()) {
            hand = context.deserialize(jsonObject.get("hand"), Hand.class);
        }

        return new Player(id, name, score, hand);
    }
}
