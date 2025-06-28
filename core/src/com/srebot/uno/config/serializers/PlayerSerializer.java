package com.srebot.uno.config.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.srebot.uno.classes.Player;

import java.lang.reflect.Type;

/** For serializing Player and all its connected objects */
public class PlayerSerializer implements JsonSerializer<Player> {
    @Override
    public JsonElement serialize(Player player, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("id", player.getId());
        jsonObject.addProperty("name", player.getName());
        jsonObject.addProperty("score", player.getScore());

        //serialize Cards list directly without "items", "size", and "ordered"
        jsonObject.add("hand", context.serialize(player.getHand()));

        return jsonObject;
    }
}