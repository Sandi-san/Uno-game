package com.srebot.uno;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.srebot.uno.screens.IntroScreen;
import com.srebot.uno.screens.MenuScreen;

//PRVI CLASS KI SE ZAŽENE
public class Uno extends Game {

	private AssetManager assetManager;
	private SpriteBatch batch;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		assetManager = new AssetManager();

		//setScreen(new MenuScreen(this));
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
}
