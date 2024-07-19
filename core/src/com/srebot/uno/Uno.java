package com.srebot.uno;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.screens.IntroScreen;
import com.srebot.uno.screens.MenuScreen;

//PRVI CLASS KI SE ZAŽENE
public class Uno extends Game {

	private AssetManager assetManager;
	private SpriteBatch batch;
	private GameManager manager;
	private Music music;

	@Override
	public void create () {
		batch = new SpriteBatch();
		assetManager = new AssetManager();
		manager = new GameManager();
		music = null;

		setScreen(new IntroScreen(this));
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		assetManager.dispose();
	}

	public AssetManager getAssetManager(){
		return assetManager;
	}
	public SpriteBatch getBatch(){
		return batch;
	}
	public GameManager getManager(){
		return manager;
	}
	public Music getMusic() {
		return music;
	}
	public void setMusic(Music music) {
		this.music = music;
	}
	public void playMusic() {
		if(!this.music.isPlaying()) {
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
	public void setMusicVolume(float volume){
		music.setVolume(volume);
	}
	//TODO
	public void setSoundVolume(float volume){
		//sound.setVolume(volume);
	}
}
