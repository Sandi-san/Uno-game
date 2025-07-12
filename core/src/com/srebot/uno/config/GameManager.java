package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.srebot.uno.classes.Player;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/** Manages global application variables */
public class GameManager {
    public static final GameManager INSTANCE = new GameManager();
    private static final String PLAYER_NAME = "currentPlayer";
    private static final boolean PLAY_SOUND = true;
    private static final boolean PLAY_MUSIC = true;
    private static final int SCORE_PLAYER = 0;

    private Json json = new Json();
    private final Preferences PREFS;

    //Variables from local settings
    private String namePref;
    private boolean soundPref;
    private boolean musicPref;
    private float soundVPref;
    private float musicVPref;
    private boolean playIntroPref;

    private String access_token;
    private long tokenExpiration;

    public GameManager() {
        PREFS = Gdx.app.getPreferences("GameSettings");
        //load local settings variables
        namePref = PREFS.getString("currentPlayer","Player 1");
        soundPref = PREFS.getBoolean("soundEnabled", true);
        musicPref = PREFS.getBoolean("musicEnabled", true);
        soundVPref = PREFS.getFloat("soundVolume", 1f);
        musicVPref = PREFS.getFloat("musicVolume", 0.5f);
        playIntroPref = PREFS.getBoolean("introEnabled", true);

        access_token = PREFS.getString("access_token",null);
        tokenExpiration = PREFS.getLong("tokenExpiration",0);
    }
    public String getNamePref() {
        return namePref;
    }
    public boolean getSoundPref(){
        return soundPref;
    }
    public boolean getMusicPref(){
        return musicPref;
    }
    public float getSoundVolumePref(){
        return soundVPref;
    }
    public float getMusicVolumePref(){return musicVPref;}
    public boolean getPlayIntroPref() {return playIntroPref;}
    public String getAccessToken() {
        //compare token expiration and current time, if expired, set token to invalid
        if(getTokenExpiration() < new Date().getTime())
            return null;
        return access_token;
    }
    public long getTokenExpiration() {return tokenExpiration;}

    /** Set name for current Player */
    public void setNamePref(String namePref) {
        //Player name cannot be "Computer" (reserved for AI computers)
        if(!namePref.equals("Computer")) {
            this.namePref = namePref;
            PREFS.putString("currentPlayer", namePref);
        }
    }
    public void setSoundPref(boolean soundPref) {
        this.soundPref = soundPref;
        PREFS.putBoolean("soundEnabled", soundPref);
    }
    public void setMusicPref(boolean musicPref) {
        this.musicPref = musicPref;
        PREFS.putBoolean("musicEnabled", musicPref);
    }
    public void setSoundVolumePref(float soundVPref) {
        this.soundVPref = soundVPref;
        PREFS.putFloat("soundVolume", soundVPref);
    }
    public void setMusicVolumePref(float musicVPref) {
        this.musicVPref = musicVPref;
        PREFS.putFloat("musicVolume", musicVPref);
    }
    public void setPlayIntroPref(boolean playIntroPref) {
        this.playIntroPref = playIntroPref;
        PREFS.putBoolean("introEnabled", playIntroPref);
    }

    public void setAccessToken(String access_token){
        this.access_token = access_token;
        PREFS.putString("access_token", access_token);
    }
    public void setTokenExpiration(){
        //create new date with current time & add 2 hours to time (expiration must be same as in backend)
        this.tokenExpiration = new Date().getTime() + 2*3600000;
        PREFS.putLong("tokenExpiration", tokenExpiration);
    }

    public void savePrefs(){
        PREFS.flush();
    }

    /** Load settings variables from json file */
    public List<Player> loadFromJson() {
        List<Player> playerList = new ArrayList<>();

        try {
            String jsonData = Gdx.files.local("playerData.json").readString();
            JsonValue root = new JsonReader().parse(jsonData);

            //for each json entry, get name and score variable and create new Player object
            for (JsonValue entry = root.child(); entry != null; entry = entry.next()) {
                String playerName = entry.getString("name");
                int playerScore = entry.getInt("score");
                playerList.add(new Player(playerName, playerScore));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return playerList;
    }

    /** Merge Players from json into Players of current Game (playersData) */
    public List<Player> mergeJson(List<Player> playerList) {
        List<Player> playersFromJson = loadFromJson();
        //iterate from all Players from json and merge with playerDataList
        for (Player playerJson : playersFromJson) {
            boolean playerExists = false;
            for (Player existingPlayer : playerList) {
                if (existingPlayer != null) {
                    if (Objects.equals(playerJson.getName(), existingPlayer.getName())) {
                        //update Player's score in json if current Player's score is higher (updates local highscore)
                        if (playerJson.getScore() > existingPlayer.getScore()) {
                            existingPlayer.setScore(playerJson.getScore());
                        }
                        playerExists = true;
                        break;
                    }
                }
            }
            //if Player doesn't exist in json, add it
            if (!playerExists) {
                playerList.add(playerJson);
            }
        }
        return playerList;
    }

    /** Saves Player data to json */
    public void saveDataToJsonFile(List<Player> playerList) {
        try {
            //merge loadJson in playerDataList
            playerList = mergeJson(playerList);

            //don't save Hand objects in json and set scores of 0 as -1 (regular json library doesn't save integers of 0)
            Iterator<Player> iterator = playerList.iterator();
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (player != null) {
                    //don't save Players with name "Computer" (reserved for AI opponents)
                    if (Objects.equals(player.getName(), "Computer")) {
                        iterator.remove();
                        continue;
                    }
                    else {
                        //change scores of 0 to -1
                        if(player.getScore()==0)
                            player.setScore(-1);
                        player.setHand(null);
                    }
                }
                //remove null elements (required since null elements throw error when loading from json)
                else
                    iterator.remove();
            }

            //serialize the list of PlayerData to JSON
            String jsonData = json.toJson(playerList);

            //write the JSON data to the file
            Gdx.files.local("playerData.json").writeString(jsonData, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Get Player from json with specific name */
    public Player getPlayerByName(List<Player> playerList, String name) {
        for (Player player : playerList) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }
}
