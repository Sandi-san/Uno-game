package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.srebot.uno.classes.Authorization;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.classes.PlayerTurn;
import com.srebot.uno.config.deserializers.HandDeserializer;
import com.srebot.uno.config.deserializers.PlayerDeserializer;
import com.srebot.uno.config.serializers.CardSerializer;
import com.srebot.uno.config.deserializers.DateDeserializer;
import com.srebot.uno.config.deserializers.DeckDeserializer;
import com.srebot.uno.config.serializers.DeckSerializer;
import com.srebot.uno.config.serializers.HandSerializer;
import com.srebot.uno.config.serializers.PlayerSerializer;
import com.srebot.uno.config.serializers.PlayerTurnSerializer;

import java.util.Date;

/** Class to communicate with server/backend */
public class GameService {
    private final Gson gson;
    public GameService() {
        //initialize GSON (json) with de/serializers
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Deck.class, new DeckSerializer());
        gsonBuilder.registerTypeAdapter(Deck.class, new DeckDeserializer());
        gsonBuilder.registerTypeAdapter(Hand.class, new HandSerializer());
        gsonBuilder.registerTypeAdapter(Hand.class, new HandDeserializer());
        gsonBuilder.registerTypeAdapter(Player.class, new PlayerSerializer());
        gsonBuilder.registerTypeAdapter(Player.class, new PlayerDeserializer());
        gsonBuilder.registerTypeAdapter(Card.class, new CardSerializer());
        gsonBuilder.registerTypeAdapter(Date.class, new DateDeserializer());
        gsonBuilder.registerTypeAdapter(PlayerTurn.class, new PlayerTurnSerializer());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    /** Callback methods, used for returning async http functions */
    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onFailure(Throwable t);
    }

    /** Method for setting auth request parameters as register route */
    public void registerUser(String name, String password, AuthCallback callback) {
        sendAuthRequest(GameConfig.SERVER_URL + GameConfig.AUTH_URL + "/register", name, password, callback);
    }

    /** Method for setting auth request parameters as login route */
    public void loginUser(String name, String password, AuthCallback callback) {
        sendAuthRequest(GameConfig.SERVER_URL + GameConfig.AUTH_URL + "/login", name, password, callback);
    }

    /** Method for sending auth request for registration or login */
    private void sendAuthRequest(String url, String name, String password, AuthCallback callback) {
        Authorization authRequest = new Authorization(name, password);
        String jsonData = gson.toJson(authRequest);

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(url)
                .header("Content-Type", "application/json")
                .build();

        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200 || statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        Authorization.AuthorizationResponse authResponse = gson.fromJson(responseJson, Authorization.AuthorizationResponse.class);
                        Gdx.app.postRunnable(() -> callback.onSuccess(authResponse.getAccessToken()));
                    } catch (Exception e) {
                        e.printStackTrace();
                        Gdx.app.postRunnable(() -> callback.onFailure(e));
                    }
                } else {
                    Gdx.app.log("AUTH", "Status code: " + statusCode);
                    //Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Failed with status code " + statusCode)));

                    //parse message from http json result and send back to MenuScreen for display
                    JsonObject jsonObject = JsonParser.parseString(httpResponse.getResultAsString()).getAsJsonObject();
                    String message = String.valueOf(jsonObject.get("message"));
                    Gdx.app.log("AUTH", "Message: " + message);
                    Gdx.app.postRunnable(() -> callback.onFailure(new Exception(message)));
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.log("AUTH", "FAILED to connect");
                Gdx.app.postRunnable(() -> callback.onFailure(t));
            }

            @Override
            public void cancelled() {
                Gdx.app.log("AUTH", "Request cancelled");
                Gdx.app.postRunnable(() -> callback.onFailure(new Exception("Request cancelled")));
            }
        });
    }

    public interface GameCreateCallback {
        void onSuccess(GameData game);
        void onFailure(Throwable t);
    }

    /** Method for creating new Game object in backend (with all connected objects) */
    public void createGame(GameCreateCallback callback, GameData gameData) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(gameData); //serialize game data
        Gdx.app.log("CREATE GAME:",jsonData);
        request.setContent(jsonData);   //set data to send through http
        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {    //response was correct
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        //get the Game object from response and return to MP screen class (callback)
                        GameData game = gson.fromJson(responseJson, GameData.class);
                        Gdx.app.postRunnable(() -> {
                            callback.onSuccess(game);
                        });
                    } catch (GdxRuntimeException e) {
                        e.printStackTrace();
                        callback.onFailure(e);
                    }
                } else {
                    //handle non-201 response codes
                    Gdx.app.log("createGame", "Invalid status code response: "+statusCode);
                    callback.onFailure(new Exception("Failed to create player. Status code: " + statusCode));
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

    public interface GameUpdatePlayersCallback {
        void onSuccess(GameData game);
        void onFailure(Throwable t);
    }

    /** Update Game object by connecting Player object to it */
    public void updateGameWithPlayer(GameUpdatePlayersCallback callback, int gameId, Player player) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PUT)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" + gameId+"/players")
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(player);
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
                        //perform actions on the main thread
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            Gdx.app.log("updateGameWithPlayer", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
                    //handle non-200 response codes
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

    public interface GameUpdatePlayerRemoveCallback {
        void onSuccess(GameData game);
        void onFailure(Throwable t);
    }

    /** Update Game object by disconnecting Player object */
    public void updateGameRemovePlayer(GameUpdatePlayerRemoveCallback callback, int gameId, Player player) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PUT)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" + gameId+"/player/"+player.getId())
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(player);
        Gdx.app.log("REMOVE PLAYER FROM GAME:", jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    //save response as GameData, unless response is null, in which case Game does not exist on backend/was deleted
                    GameData game = null;
                    if(!responseJson.equals(""))
                        game = gson.fromJson(responseJson, GameData.class);
                    GameData finalGame = game;
                    Gdx.app.postRunnable(() -> {
                        if (finalGame != null) {
                            callback.onSuccess(finalGame);
                        } else {
                            Gdx.app.log("updateGameWithPlayer", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
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

    /** Update Game object by disconnecting Player object and updating Game's turn variable */
    public void updateGameRemovePlayerTurn(GameUpdatePlayerRemoveCallback callback, int gameId, Player player, int currentTurn) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.PUT)
                .url(GameConfig.SERVER_URL + GameConfig.GAME_URL + "/" + gameId+"/player/"+player.getId()+"/turn")
                .header("Content-Type", "application/json")
                .build();

        PlayerTurn playerTurn = new PlayerTurn(player.getId(),player.getName(),player.getScore(),player.getHand(),currentTurn);
        String jsonData = gson.toJson(playerTurn);
        Gdx.app.log("REMOVE PLAYER & UPDATE GAME:", jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    Gdx.app.log("DATA:", responseJson);
                    //save response as GameData, unless response is null, in which case Game does not exist on backend/was deleted
                    GameData game = null;
                    if(!responseJson.equals(""))
                        game = gson.fromJson(responseJson, GameData.class);
                    GameData finalGame = game;
                    Gdx.app.postRunnable(() -> {
                        if (finalGame != null) {
                            callback.onSuccess(finalGame);
                        } else {
                            Gdx.app.log("updateGameWithPlayerTurn", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
                    Gdx.app.log("updateGameWithPlayerTurn", "Invalid status code response: "+statusCode);
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

    /** Update Game object with new data */
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
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            Gdx.app.log("updateGame", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
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

    /** Fetch Players connected to Game */
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
                        if (players != null) {
                            callback.onSuccess(players);
                        } else {
                            Gdx.app.log("fetchGamePlayers", "Failed to parse game data - players");
                            callback.onFailure(new Exception("Failed to parse game data - players"));
                        }
                    });
                }
                else{
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

    public interface FetchPlayersScoresCallback {
        void onSuccess(Player[] players);
        void onFailure(Throwable t);
    }

    /** Fetch scores of all Players */
    public void fetchPlayersScores(FetchPlayersScoresCallback callback){
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(GameConfig.SERVER_URL + GameConfig.PLAYER_URL + "/scores")
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
                        if (players != null) {
                            callback.onSuccess(players);
                        } else {
                            Gdx.app.log("fetchPlayersScores", "Failed to parse game data - players");
                            callback.onFailure(new Exception("Failed to parse game data - players"));
                        }
                    });
                }
                else{
                    Gdx.app.log("fetchPlayersScores", "Invalid status code response: "+statusCode);
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
        void onSuccess(GameData gameTurn);
        void onFailure(Throwable t);
    }

    /** Fetch turn and discard Deck from Game object */
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
                    GameData game = gson.fromJson(responseJson, GameData.class);
                    Gdx.app.postRunnable(() -> {
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            Gdx.app.log("fetchGame", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
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

    public interface FetchGamesCallback {
        void onSuccess(GameData[] games);
        void onFailure(Throwable t);
    }

    /** Fetch all Games on backend */
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
                        Gdx.app.postRunnable(() -> {
                            if (games != null) {
                                callback.onSuccess(games);
                            } else {
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

    public interface FetchGameCallback {
        void onSuccess(GameData games);
        void onFailure(Throwable t);
    }

    /** Fetch specific Game object */
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
                    Gdx.app.postRunnable(() -> {
                        if (game != null) {
                            callback.onSuccess(game);
                        } else {
                            Gdx.app.log("fetchGame", "Failed to parse game data");
                            callback.onFailure(new Exception("Failed to parse game data"));
                        }
                    });
                }
                else{
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

    /** Get Player from backend with unique name */
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

    /** Create new Player on backend */
    public void createPlayer(PlayerCreateCallback callback, Player player) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(GameConfig.SERVER_URL + GameConfig.PLAYER_URL)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = gson.toJson(player);
        Gdx.app.log("CREATE PLAYER:",jsonData);
        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 201) {
                    String responseJson = httpResponse.getResultAsString();
                    try {
                        Player response = gson.fromJson(responseJson, Player.class);
                        int playerId = response.getId();
                        callback.onSuccess(playerId);
                    } catch (GdxRuntimeException e) {
                        e.printStackTrace();
                        callback.onFailure(e);
                    }
                } else {
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
