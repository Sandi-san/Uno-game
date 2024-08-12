package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.serializers.CardSerializer;
import com.srebot.uno.config.serializers.DateDeserializer;
import com.srebot.uno.config.serializers.DeckSerializer;
import com.srebot.uno.config.serializers.HandSerializer;
import com.srebot.uno.config.serializers.PlayerSerializer;

import java.util.Date;

public class GameService {
    private final Gson gson;
    public GameService() {
        //INIT GSON z serializer
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Deck.class, new DeckSerializer());
        gsonBuilder.registerTypeAdapter(Hand.class, new HandSerializer());
        gsonBuilder.registerTypeAdapter(Player.class, new PlayerSerializer());
        gsonBuilder.registerTypeAdapter(Card.class, new CardSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    //callback metoda, ker so http funkcije async in ne podpirajo regular return
    public interface GameCreateCallback {
        void onSuccess(int gameId);
        void onFailure(Throwable t);
    }

    public void createGame(GameCreateCallback callback, GameData gameData) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(gameData); // Serialize your game data here
        //Gdx.app.log("CREATE GAME:",jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        // Handle the response
                        GameData response = gson.fromJson(responseJson, GameData.class);
                        int gameId = response.getId();
                        callback.onSuccess(gameId);
                    } catch (GdxRuntimeException e) {
                        // Handle the error (invalid JSON, etc.)
                        e.printStackTrace();
                        callback.onFailure(e);
                    }
                } else {
                    // Handle non-200 response codes
                    callback.onFailure(new Exception("Failed to create player. Status code: " + statusCode));
                }
            }

            @Override
            public void failed(Throwable t) {
                // Handle error
                t.printStackTrace();
                Gdx.app.log("FAILED","CANNOT CONNECT TO SERVER");
                callback.onFailure(t);
            }

            @Override
            public void cancelled() {
                // Handle cancellation
                callback.onFailure(new Exception("Request was cancelled"));
            }
        });
    }

    public void updateGame() {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PATCH)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL)
                .header("Content-Type", "application/json")
                .build();

        GameData gameData = new GameData();
        //String jsonData = gson.toJson(gameData.getPlayers()); // Serialize your game data here
        String jsonData = gson.toJson(gameData); // Serialize your game data here
        Gdx.app.log("DATA:",jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        GameData response = gson.fromJson(responseJson, GameData.class);
                        // Handle the response
                    } catch (GdxRuntimeException e) {
                        // Handle the error (invalid JSON, etc.)
                        e.printStackTrace();
                    }
                } else {
                    // Handle error
                }
            }

            @Override
            public void failed(Throwable t) {
                // Handle error
                t.printStackTrace();
                Gdx.app.log("FAILED","CANNOT CONNECT TO SERVER");
            }

            @Override
            public void cancelled() {
                // Handle cancellation
            }
        });
    }

    public interface FetchGamePlayersCallback {
        void onSuccess(Player[] players);
        void onFailure(Throwable t);
    }
    public void fetchGamePlayers(FetchGamePlayersCallback callback, int gameId){
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" +gameId+"/players")
                .header("Content-Type", "application/json")
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String responseJson = httpResponse.getResultAsString();
                Gdx.app.log("DATA:", responseJson);
                Player[] players = gson.fromJson(responseJson, Player[].class);
                Gdx.app.postRunnable(() -> {
                    // Perform actions on the main thread
                    if (players != null) {
                        callback.onSuccess(players);
                    } else {
                        // Handle the error
                        Gdx.app.log("GameService", "Failed to parse game data - players");
                        callback.onFailure(new Exception("Failed to parse game data - players"));
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("FAILED","CANNOT CONNECT TO SERVER");
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("CANCELLED","REQUEST CANCELLED");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }


    //callback metoda, ker so http funkcije async in ne podpirajo regular return
    public interface FetchGamesCallback {
        void onSuccess(GameData[] games);
        void onFailure(Throwable t);
    }

    public void fetchGames(FetchGamesCallback callback){
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL)
                .header("Content-Type", "application/json")
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String responseJson = httpResponse.getResultAsString();
                Gdx.app.log("DATA:", responseJson);
                GameData[] games = gson.fromJson(responseJson, GameData[].class);
                // Handle the GameData array (e.g., update the UI)
                Gdx.app.postRunnable(() -> {
                    // Perform actions on the main thread
                    if (games != null) {
                        // Update the UI with the fetched games
                        // e.g., gameList.setItems(games);
                        callback.onSuccess(games);
                    } else {
                        // Handle the error
                        Gdx.app.log("GameService", "Failed to parse game data");
                        callback.onFailure(new Exception("Failed to parse game data"));
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("FAILED","CANNOT CONNECT TO SERVER");
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("CANCELLED","REQUEST CANCELLED");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public interface PlayerFetchCallback {
        void onSuccess(Player player, Hand hand);
        void onFailure(Throwable t);
    }
    public void fetchPlayerByName(PlayerFetchCallback callback, String name) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.PLAYER_URL + "/name/"+name)
                .header("Content-Type", "application/json")
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                String responseJson = httpResponse.getResultAsString();
                Gdx.app.log("DATA:", responseJson);
                Player player = gson.fromJson(responseJson, Player.class);
                Hand hand = gson.fromJson(responseJson, Hand.class);
                Gdx.app.postRunnable(() -> {
                    if (player != null) {
                        callback.onSuccess(player,hand);
                    } else {
                        callback.onFailure(new Exception("Failed to parse player data"));
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    callback.onFailure(t);
                });
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> {
                    callback.onFailure(new Exception("Request cancelled"));
                });
            }
        });
    }

    public interface PlayerCreateCallback {
        void onSuccess(int playerId);
        void onFailure(Throwable t);
    }
    public void createPlayer(PlayerCreateCallback callback, Player player) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(GameConfig.SERVER_URL + GameConfig.PLAYER_URL)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(player); // Serialize your game data here
        //Gdx.app.log("CREATE PLAYER:",jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        // Handle the response
                        Player response = gson.fromJson(responseJson, Player.class);
                        int playerId = response.getId();
                        callback.onSuccess(playerId);
                    } catch (GdxRuntimeException e) {
                        // Handle the error (invalid JSON, etc.)
                        e.printStackTrace();
                        callback.onFailure(e);
                    }
                } else {
                    // Handle non-200 response codes
                    callback.onFailure(new Exception("Failed to create player. Status code: " + statusCode));
                }
            }

            @Override
            public void failed(Throwable t) {
                // Handle error
                t.printStackTrace();
                Gdx.app.log("FAILED","CANNOT CONNECT TO SERVER");
                callback.onFailure(t);
            }

            @Override
            public void cancelled() {
                // Handle cancellation
                callback.onFailure(new Exception("Request was cancelled"));
            }
        });
    }
}
