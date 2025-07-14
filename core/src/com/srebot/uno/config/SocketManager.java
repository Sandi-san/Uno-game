package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

/** Class for managing Socket.IO connections */
public class SocketManager {
    private Socket socket;
    private GameSocketListener listener;

    /** Interface listener for class */
    public interface GameSocketListener{
        void onPlayerListener();
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

                //notify listener
                if(listener!=null)
                    listener.onPlayerListener();
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
