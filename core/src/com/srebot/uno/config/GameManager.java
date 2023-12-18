package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.srebot.uno.classes.PlayerData;

import java.util.ArrayList;
import java.util.List;

public class GameManager {
    public static final GameManager INSTANCE = new GameManager();
    private static final String PLAYER_NAME = "currentPlayer";
    private static final String CARD_PRESET = "cardPreset";
    private static final String STARTER_PLAYER = "starterPlayer";
    private static final String CARD_ORDER = "cardOrder";
    private static final boolean PLAY_SOUND = true;
    private static final boolean PLAY_MUSIC = true;
    private static final int SCORE_PLAYER = 0;

    private Json json = new Json();
    private final Preferences PREFS;

    //DOBI VREDNOSTI IZ NASTAVITEV
    private String namePref;
    private String presetPref;
    private String starterPref;
    private String orderPref;
    private boolean soundPref;
    private boolean musicPref;

    private int scorePlayer;
    private PlayerData playersData;

    public GameManager() {
        PREFS = Gdx.app.getPreferences("GameSettings");
        //DOBI VREDNOSTI IZ NASTAVITEV
        namePref = PREFS.getString("currentPlayer","Player 1");
        presetPref = PREFS.getString("cardPreset","All");
        starterPref = PREFS.getString("starterPlayer","Player");
        orderPref = PREFS.getString("cardOrder","Clockwise");
        soundPref = PREFS.getBoolean("soundEnabled", true);
        musicPref = PREFS.getBoolean("musicEnabled", true);
    }
    public String getNamePref() {
        return namePref;
    }
    public String getPresetPref() {
        return presetPref;
    }
    public String getStarterPref() {
        return starterPref;
    }
    public String getOrderPref() {
        return orderPref;
    }
    public boolean getSoundPref(){
        return soundPref;
    }
    public boolean getMusicPref(){
        return musicPref;
    }

    public void setNamePref(String namePref) {
        this.namePref = namePref;
        PREFS.putString("currentPlayer", namePref);
    }
    public void setPresetPref(String presetPref) {
        this.presetPref = presetPref;
        PREFS.putString("cardPreset", presetPref);
    }
    public void setStarterPref(String starterPref) {
        this.starterPref = starterPref;
        PREFS.putString("starterPlayer", starterPref);
    }
    public void setOrderPref(String orderPref) {
        this.orderPref = orderPref;
        PREFS.putString("cardOrder", orderPref);
    }
    public void setSoundPref(boolean soundPref) {
        this.soundPref = soundPref;
        PREFS.putBoolean("soundEnabled", soundPref);
    }
    public void setMusicPref(boolean musicPref) {
        this.musicPref = musicPref;
        PREFS.putBoolean("musicEnabled", musicPref);
    }
    public void savePrefs(){
        PREFS.flush();
    }

    public void appendToJson(String namePref, int scorePlayer){
        // Load existing data from the JSON file
        List<PlayerData> playerDataList = loadFromJson();

        // Check if the player with the given name already exists
        PlayerData existingPlayer = getPlayerByName(playerDataList, namePref);

        if (existingPlayer != null) {
            // If the player exists, update the score
            existingPlayer.setScore(existingPlayer.getScore() + scorePlayer);
        } else {
            // If the player doesn't exist, create a new entry
            PlayerData newPlayer = new PlayerData(namePref, scorePlayer);
            playerDataList.add(newPlayer);
        }

        // Save the updated data back to the JSON file
        saveDataToJsonFile(playerDataList);
    }
    public List<PlayerData> loadFromJson() {
        List<PlayerData> playerDataList = new ArrayList<>();

        try {
            String jsonData = Gdx.files.local("playerData.json").readString();
            JsonValue root = new JsonReader().parse(jsonData);

            for (JsonValue entry = root.child(); entry != null; entry = entry.next()) {
                String playerName = entry.getString("namePref");
                int playerScore = entry.getInt("scorePlayer");
                playerDataList.add(new PlayerData(playerName, playerScore));
            }

        } catch (Exception e) {
            // Handle exceptions (file not found, parse error, etc.)
            e.printStackTrace();
        }

        return playerDataList;
    }


    public void saveDataToJsonFile(List<PlayerData> playerDataList) {
        try {
            // Serialize the list of PlayerData to JSON
            String jsonData = json.toJson(playerDataList);

            // Write the JSON data to the file
            Gdx.files.local("playerData.json").writeString(jsonData, false);
        } catch (Exception e) {
            // Handle exceptions (write error, etc.)
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerByName(List<PlayerData> playerDataList, String name) {
        for (PlayerData playerData : playerDataList) {
            if (playerData.getName().equals(name)) {
                return playerData;
            }
        }
        return null;
    }
}
