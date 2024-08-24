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
import com.srebot.uno.config.deserializers.HandDeserializer;
import com.srebot.uno.config.deserializers.PlayerDeserializer;
import com.srebot.uno.config.serializers.CardSerializer;
import com.srebot.uno.config.deserializers.DateDeserializer;
import com.srebot.uno.config.deserializers.DeckDeserializer;
import com.srebot.uno.config.serializers.DeckSerializer;
import com.srebot.uno.config.serializers.HandSerializer;
import com.srebot.uno.config.serializers.PlayerSerializer;

import java.util.Date;

public class GameService {
    private final Gson gson;
    public GameService() {
        //INIT GSON w/ de/serializers
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Deck.class, new DeckSerializer());
        gsonBuilder.registerTypeAdapter(Deck.class, new DeckDeserializer());
        gsonBuilder.registerTypeAdapter(Hand.class, new HandSerializer());
        gsonBuilder.registerTypeAdapter(Hand.class, new HandDeserializer());
        gsonBuilder.registerTypeAdapter(Player.class, new PlayerSerializer());
        gsonBuilder.registerTypeAdapter(Player.class, new PlayerDeserializer());
        gsonBuilder.registerTypeAdapter(Card.class, new CardSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    //callback metoda, ker so http funkcije async in ne podpirajo regular return
    public interface GameCreateCallback {
        void onSuccess(GameData game);
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
        Gdx.app.log("CREATE GAME:",jsonData);
        request.setContent(jsonData);
        //TODO: GDX.POST?
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        // Handle the response
                        GameData game = gson.fromJson(responseJson, GameData.class);
                        callback.onSuccess(game);
                    } catch (GdxRuntimeException e) {
                        // Handle the error (invalid JSON, etc.)
                        e.printStackTrace();
                        callback.onFailure(e);
                    }
                } else {
                    // Handle non-201 response codes
                    Gdx.app.log("createGame", "Invalid status code response: "+statusCode);
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

    public interface GameUpdatePlayersCallback {
        void onSuccess(GameData game);
        void onFailure(Throwable t);
    }

    public void updateGameWithPlayer(GameUpdatePlayersCallback callback, int gameId, Player player) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PUT)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" + gameId+"/players")
                .header("Content-Type", "application/json")
                .build();

        //String jsonData = gson.toJson(gameData.getPlayers()); // Serialize your game data here
        String jsonData = gson.toJson(player); // Serialize your game data here
        Gdx.app.log("UPDATE PLAYER ON GAME:", jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    GameData game = gson.fromJson(responseJson, GameData.class);
                    Gdx.app.postRunnable(() -> {
                        // Perform actions on the main thread
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            // Handle the error
                            Gdx.app.log("updateGameWithPlayer", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("updateGameWithPlayer", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to update game. Status code: " + statusCode));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("FAILED", "CANNOT CONNECT TO SERVER");
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("CANCELLED", "REQUEST CANCELLED");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public interface GameUpdateCallback {
        void onSuccess(GameData game);
        void onFailure(Throwable t);
    }

    public void updateGame(GameUpdateCallback callback, int gameId, GameData gameData) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PUT)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" + gameId)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(gameData);
        Gdx.app.log("UPDATE GAME:", jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    GameData game = gson.fromJson(responseJson, GameData.class);
                    Gdx.app.postRunnable(() -> {
                        // Perform actions on the main thread
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            // Handle the error
                            Gdx.app.log("updateGame", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("updateGame", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to update game. Status code: " + statusCode));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("FAILED", "CANNOT CONNECT TO SERVER");
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("CANCELLED", "REQUEST CANCELLED");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
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
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    Player[] players = gson.fromJson(responseJson, Player[].class);
                    Gdx.app.postRunnable(() -> {
                        // Perform actions on the main thread
                        if (players != null) {
                            callback.onSuccess(players);
                        } else {
                            // Handle the error
                            Gdx.app.log("fetchGamePlayers", "Failed to parse game data - players");
                            callback.onFailure(new Exception("Failed to parse game data - players"));
                        }
                    });
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("fetchGamePlayers", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to fetch players. Status code: " + statusCode));
                }
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

    public interface FetchGameTurnCallback {
        void onSuccess(int gameTurn);
        void onFailure(Throwable t);
    }
    public void fetchGameTurn(FetchGameTurnCallback callback, int gameId){
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" +gameId+"/turn")
                .header("Content-Type", "application/json")
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    int fetchedTurn = 0;
                    fetchedTurn = gson.fromJson(responseJson, Integer.class);
                    int finalFetchedTurn = fetchedTurn;
                        Gdx.app.postRunnable(() -> {
                            // Perform actions on the main thread
                            if (finalFetchedTurn != 0) {
                                callback.onSuccess(finalFetchedTurn);
                            } else {
                                // Handle the error
                                Gdx.app.log("fetchGameTurn", "Failed to parse game data - turn");
                                callback.onFailure(new Exception("Failed to parse game data - turn"));
                            }
                        });
                    Gdx.app.log("fetchGameTurn", "Thread interrupted: " + Thread.currentThread().isInterrupted());
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("fetchGameTurn", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to fetch game turn. Status code: " + statusCode));
                }
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
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    try{
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
                                Gdx.app.log("fetchGames", "Failed to parse game data");
                                callback.onFailure(new Exception("Failed to parse game data"));
                            }
                        });
                    }
                    catch (Exception e){
                        Gdx.app.log("fetchGames", "Failed to parse json: "+e);
                        callback.onFailure(new Exception("Failed to parse json: "+e));
                    }
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("fetchGames", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to fetch games. Status code: " + statusCode));
                }
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
    public interface FetchGameCallback {
        void onSuccess(GameData games);
        void onFailure(Throwable t);
    }

    public void fetchGame(int gameId, FetchGameCallback callback){
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL+"/"+gameId)
                .header("Content-Type", "application/json")
                .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    GameData game = gson.fromJson(responseJson, GameData.class);
                    // Handle the GameData array (e.g., update the UI)
                    Gdx.app.postRunnable(() -> {
                        // Perform actions on the main thread
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            // Handle the error
                            Gdx.app.log("fetchGame", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("fetchGame", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to fetch game. Status code: " + statusCode));
                }
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
        void onSuccess(Player player);
        void onFailure(Throwable t);
    }
    /** Get player from database with unique name */
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
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("PLAYER:", responseJson);
                    Player player = gson.fromJson(responseJson, Player.class);
                    if(player!=null) {
                        Gdx.app.postRunnable(() -> {
                            callback.onSuccess(player);
                        });
                    } else {
                        Gdx.app.postRunnable(() ->{
                            callback.onFailure(new Exception("Failed to parse player data"));
                        });
                    }
                }
                else{
                    // Handle non-200 response codes
                    Gdx.app.log("fetchPlayerByName", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to fetch player. Status code: " + statusCode));
                }
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
        Gdx.app.log("CREATE PLAYER:",jsonData);
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
                    // Handle non-201 response codes
                    Gdx.app.log("createPlayer", "Invalid status code response: "+statusCode);
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
