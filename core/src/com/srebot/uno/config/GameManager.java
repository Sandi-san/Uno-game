package com.srebot.uno.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.srebot.uno.classes.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
    private int difficultyPref;
    private String orderPref;
    private boolean soundPref;
    private boolean musicPref;
    private float soundVPref;
    private float musicVPref;

    private Player playersData;

    public GameManager() {
        PREFS = Gdx.app.getPreferences("GameSettings");
        //DOBI VREDNOSTI IZ NASTAVITEV
        namePref = PREFS.getString("currentPlayer","Player 1");
        presetPref = PREFS.getString("cardPreset","All");
        difficultyPref = PREFS.getInteger("difficulty",2);
        orderPref = PREFS.getString("cardOrder","Clockwise");
        soundPref = PREFS.getBoolean("soundEnabled", true);
        musicPref = PREFS.getBoolean("musicEnabled", true);
        soundVPref = PREFS.getFloat("soundVolume", 1f);
        musicVPref = PREFS.getFloat("musicVolume", 0.5f);
    }
    public String getNamePref() {
        return namePref;
    }
    public String getPresetPref() {
        return presetPref;
    }
    public int getDifficultyPref() {
        return difficultyPref;
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
    public float getSoundVolumePref(){
        return soundVPref;
    }
    public float getMusicVolumePref(){
        return musicVPref;
    }

    public void setNamePref(String namePref) {
        //player name ne sme biti "Computer"
        //rezervirano za AI playerja
        if(!namePref.equals("Computer")) {
            this.namePref = namePref;
            PREFS.putString("currentPlayer", namePref);
        }
    }
    public void setPresetPref(String presetPref) {
        this.presetPref = presetPref;
        PREFS.putString("cardPreset", presetPref);
    }
    public void setDifficultyPref(int difficultyPref) {
        this.difficultyPref = difficultyPref;
        PREFS.putInteger("difficultyPref", difficultyPref);
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
    public void setSoundVolumePref(float soundVPref) {
        this.soundVPref = soundVPref;
        PREFS.putFloat("soundVolume", soundVPref);
    }
    public void setMusicVolumePref(float musicVPref) {
        this.musicVPref = musicVPref;
        PREFS.putFloat("musicVolume", musicVPref);
    }
    public void savePrefs(){
        PREFS.flush();
    }

    public List<Player> loadFromJson() {
        List<Player> playerList = new ArrayList<>();

        try {
            String jsonData = Gdx.files.local("playerData.json").readString();
            JsonValue root = new JsonReader().parse(jsonData);

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

    //zdruzi playerje iz json z playerje trenutne igre
    public List<Player>  mergeJson(List<Player> playerList) {
        List<Player> playersFromJson = loadFromJson();
        //iteriraj skozi playersFromJson in zdruzi s playerDataList
        for (Player playerJson : playersFromJson) {
            boolean playerExists = false;
            for (Player existingPlayer : playerList) {
                if (existingPlayer != null) {
                    if (Objects.equals(playerJson.getName(), existingPlayer.getName())) {
                        //posodobi player rezultat le ce je visji
                        if (playerJson.getScore() > existingPlayer.getScore()) {
                            existingPlayer.setScore(playerJson.getScore());
                        }
                        playerExists = true;
                        break;
                    }
                }
            }
            //ce player se ne obstaja v trenutni listi, dodaj
            if (!playerExists) {
                playerList.add(playerJson);
            }
        }
        return playerList;
    }

    public void saveDataToJsonFile(List<Player> playerList) {
        try {
            //zdruzi loadJson in playerDataList
            playerList = mergeJson(playerList);

            //ne shranjevat Hand in nastavi score na -1, ce je score 0
            //ker json ne zna shranit int=0
            Iterator<Player> iterator = playerList.iterator();
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (player != null) {
                    // ne shranit playerja z imenom "Computer"
                    if (Objects.equals(player.getName(), "Computer")) {
                        iterator.remove();
                        continue;
                    }
                    else {
                        //ce je score==0, nastavi na -1, da lahko shrani v json
                        if(player.getScore()==0)
                            player.setScore(-1);
                        player.setHand(null);
                    }
                }
                //odstrani null primerke (pomembno, sicer error pri load)
                else
                    iterator.remove();
            }
            // Serialize the list of PlayerData to JSON
            String jsonData = json.toJson(playerList);

            // Write the JSON data to the file
            Gdx.files.local("playerData.json").writeString(jsonData, false);
        } catch (Exception e) {
            // Handle exceptions (write error, etc.)
            e.printStackTrace();
        }
    }

    public Player getPlayerByName(List<Player> playerList, String name) {
        for (Player player : playerList) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }
}
