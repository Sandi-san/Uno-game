package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.srebot.uno.classes.Card;
import com.srebot.uno.config.deserializers.CardDeserializer;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.json.JSONObject;

/** Class for managing Socket.IO connections */
public class SocketManager {
    private Socket socket;
    private GameSocketListener listener;

    /** Interface listener for class */
    public interface GameSocketListener{
        void onPlayerChangedListener();
        void onTurnChangedListener(int fetchedTurn, Card fetchedTopCard, String fetchedState);
    }

    public SocketManager(GameSocketListener listener){
        this.listener = listener;
    }

    public void connect(int gameId) {
        try {
            socket = IO.socket("http://localhost:3000"); // or your server address

            socket.on(Socket.EVENT_CONNECT, args -> {
                Gdx.app.log("SocketManager","Connected to WebSocket");

                // Join game room
                socket.emit("joinGame", gameId);
            });

            // Listen for playerJoined updates
            socket.on("playerJoined", args -> {
                JSONObject data = (JSONObject) args[0];
                Gdx.app.log("SocketManager","Player joined: " + data);

                //trigger listener method
                if(listener!=null)
                    listener.onPlayerChangedListener();
            });

            //Listen for turnChanged updates
            socket.on("turnChanged", args -> {
                if (listener != null && args.length > 0) {
                    JSONObject data = (JSONObject) args[0];

                    Gdx.app.log("SocketManager","Turn changed: " + data);

                    int currentTurn = data.getInt("currentTurn");
                    String topCardJson = data.getJSONObject("topCard").toString();
                    String gameState = data.getString("gameState");

                    /*
                    Gdx.app.log("SocketManager","Turn: " + currentTurn);
                    Gdx.app.log("SocketManager","TopCard: " + topCardJson);
                    Gdx.app.log("SocketManager","State: " + gameState);
                    */

                    // Deserialize topCard using Gson
                    Gson gson = new GsonBuilder()
                            .registerTypeAdapter(Card.class, new CardDeserializer())
                            .create();

                    Card topCard = gson.fromJson(topCardJson, Card.class);

                    //trigger listener method
                    if(listener!=null)
                        listener.onTurnChangedListener(currentTurn, topCard, gameState);
                }
            });

            socket.connect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
        }
    }
}
