package com.srebot.uno;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;
import com.srebot.uno.screens.GameMultiplayerScreen;
import com.srebot.uno.screens.GameSingleplayerScreen;
import com.srebot.uno.screens.IntroScreen;

//PRVI CLASS KI SE ZAŽENE
public class Uno extends Game {
    private AssetManager assetManager;
    private SpriteBatch batch;
    private GameManager manager;
    private GameService service;
    private Music music;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assetManager = new AssetManager();
        manager = new GameManager();
        service = new GameService();
        music = null;

        setScreen(new IntroScreen(this));

        // Adding the LifecycleListener here
        Gdx.app.addLifecycleListener(new LifecycleListener() {
            @Override
            public void pause() {
                // Handle pause
            }

            @Override
            public void resume() {
                // Handle resume
            }

            @Override
            public void dispose() {
                Gdx.app.log("DISPOSE", "CLOSING APPLICATION FROM LIFECYCLE LISTENER");
                // Clean up resources
                if(screen instanceof GameMultiplayerScreen) {
                    Gdx.app.log("DISPOSE", "Calling from GameMultiplayerScreen");
                    GameMultiplayerScreen mpScreen = (GameMultiplayerScreen) screen;
                    //close scheduler
                    mpScreen.stopScheduler();
                    int playerId = mpScreen.getPlayerId();
                    int gameId = mpScreen.getGameId();
                    if (playerId != 0) {
                        Gdx.app.log("DISPOSE", "MultiplayerScreen closed with localPlayerId: " + playerId);
                        mpScreen.playerLeaveGame(playerId,gameId);
                        //TODO: CALL DELETE GAME IN BACKEND
                        //BE: delete player's GameId from Game, if Game then has no players, delete game
                    }
                }
            }
        });
    }

    @Override
    public void dispose() {
        batch.dispose();
        assetManager.dispose();
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public GameManager getManager() {
        return manager;
    }

    public GameService getService() {
        return service;
    }

    public Music getMusic() {
        return music;
    }

    public void setMusic(Music music) {
        this.music = music;
    }

    public void playMusic() {
        if (!this.music.isPlaying()) {
            stopMusic();
            music.setLooping(true);
            music.play();
        }
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
        }
    }

    public void setMusicVolume(float volume) {
        music.setVolume(volume);
    }

    //TODO
    public void setSoundVolume(float volume) {
        //sound.setVolume(volume);
    }
}
