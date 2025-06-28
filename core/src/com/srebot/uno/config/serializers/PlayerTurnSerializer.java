package com.srebot.uno.config.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Player;
import com.srebot.uno.classes.PlayerTurn;

import java.lang.reflect.Type;

/** For serializing Player (and current turn of Game) and all its connected objects */
public class PlayerTurnSerializer implements JsonSerializer<PlayerTurn> {
    @Override
    public JsonElement serialize(PlayerTurn playerTurn, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", playerTurn.getId());
        jsonObject.addProperty("name", playerTurn.getName());
        jsonObject.addProperty("score", playerTurn.getScore());

        //serialize the Cards list directly without "items", "size", and "ordered"
        jsonObject.add("hand", context.serialize(playerTurn.getHand()));
        //add current turn into property to serialize and pass into backend
        jsonObject.addProperty("currentTurn", playerTurn.getCurrentTurn());

        return jsonObject;
    }
}