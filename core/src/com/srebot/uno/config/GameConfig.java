package com.srebot.uno.config;

public class GameConfig {
    //GAME WINDOW
    public static final float WIDTH = 800f;
    public static final float HEIGHT = 600f;
    //GAME HUD
    public static final float HUD_WIDTH = 800f;
    public static final float HUD_HEIGHT = 600f;
    //WORLD UNITS
    public static final float WORLD_WIDTH = 80f;
    public static final float WORLD_HEIGHT = 60f;

    //CARD RATIO (za intro)
    public static final float CARD_WIDTH_RATIO = 0.7f;
    public static final float CARD_HEIGHT_RATIO = 1f;
    //CARD SIZE IN WORLD UNITS
    public static final float CARD_HEIGHT = 16f;
    public static final float CARD_WIDTH = 11.2f;
    //CARD SIZE SMALL
    public static final float CARD_HEIGHT_SM = 12f;
    public static final float CARD_WIDTH_SM = 8.4f;

    //TEXT SIZE RATIO
    public static final float TEXT_WIDTH = WIDTH*0.25f;
    public static final float TEXT_HEIGHT = HEIGHT*0.25f;

    //BUTTON SIZE IN WORLD UNITS
    public static final float BUTTON_WIDTH = 20f;
    public static final float BUTTON_HEIGHT = 14f;

    //MAX CARDS TO SHOW IN PLAYER HANDS AT ONE TIME
    public static final int MAX_CARDS_SHOW = 7; //7
    public static final int MAX_CARDS_SHOW_SM = 9;

    //MULTIPLAYER/SERVER
    public static final String SERVER_URL = "http://localhost:3000/";
    public static final String GAME_URL = "game";
    public static final String PLAYER_URL = "player";
    public static final String HAND_URL = "hand";
    public static final String CARD_URL = "card";
    public static final String DECK_URL = "deck";
}
