package com.srebot.uno.config;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.srebot.uno.classes.GameData;

public class GameService {
    private static final String SERVER_URL = "http://localhost:3000/";
    private static final String GAME_URL = "game";

    public void createGame() {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest request = requestBuilder.newRequest().method(Net.HttpMethods.POST)
                .url(SERVER_URL+GAME_URL)
                .header("Content-Type", "application/json")
                .build();

        String jsonData = new Json().toJson(new GameData()); // Serialize your game data here

        request.setContent(jsonData);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String responseJson = httpResponse.getResultAsString();
                    JsonValue response = new Json().fromJson(null, responseJson);
                    // Handle the response
                } else {
                    // Handle error
                }
            }

            @Override
            public void failed(Throwable t) {
                // Handle error
            }

            @Override
            public void cancelled() {
                // Handle cancellation
            }
        });
    }
}
