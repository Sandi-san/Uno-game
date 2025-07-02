package com.srebot.uno.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.config.GameConfig;

public class IntroScreen extends ScreenAdapter {
    //duration of intro in seconds
    public static final float INTRO_DURATION = 4f;
    //size of Cards used in intro in pixels
    public static final float CARD_SIZE = 200f;

    private final Uno game;
    private final AssetManager assetManager;
    private TextureAtlas gameplayAtlas;
    private Sprite background;
    private Music music;

    private Viewport viewport;
    private Stage stage;
    private OrthographicCamera backgroundCamera;
    private StretchViewport backgroundViewport;
    private SpriteBatch batch;

    private float duration=0f;

    public IntroScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        music = game.getMusic();
        batch = new SpriteBatch();
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        backgroundCamera = new OrthographicCamera();
        backgroundViewport = new StretchViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, backgroundCamera);
        stage = new Stage(viewport,game.getBatch());

        //Load following assets with asset manager
        assetManager.load(AssetDescriptors.UI_FONT);
        assetManager.load(AssetDescriptors.UI_SKIN);
        assetManager.load(AssetDescriptors.GAMEPLAY);
        assetManager.load(AssetDescriptors.SET_SOUND);
        assetManager.load(AssetDescriptors.PICK_SOUND);
        assetManager.load(AssetDescriptors.MAIN_MUSIC);
        assetManager.load(AssetDescriptors.GAME_MUSIC_1);
        assetManager.load(AssetDescriptors.GAME_MUSIC_2);
        assetManager.finishLoading();

        game.setMusic(assetManager.get(AssetDescriptors.MAIN_MUSIC));
        //game.setMusicVolume(0.5f);

        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
        //create background image
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background1);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        //add animation for each separate Card
        stage.addActor(createAnimation1());
        stage.addActor(createAnimation2());
        stage.addActor(createAnimation3());
        stage.addActor(createAnimation4());
        stage.addActor(createFinalCard());
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        stage.getViewport().update(width,height,true);
        //scalable background
        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta){
        //default background
        ScreenUtils.clear(0,1,0.78f,0.8f);

        //timer
        duration += delta;

        //check if intro is set to be played
        if(game.getManager().getPlayIntroPref()) {
            //when intro ends, switch to Menu screen
            if (duration > INTRO_DURATION) {
                game.setScreen(new MenuScreen(game));
            }
        }
        //skip intro
        else {
            game.setScreen(new MenuScreen(game));
        }

        //draw background
        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

        //draw stage for intro
        viewport.apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide(){
        dispose();
    }
    @Override
    public void dispose(){
        stage.dispose();
    }

    private Actor createAnimation1(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.B1));
        //size of object, convert to WORLD UNITS
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH_RATIO);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT_RATIO);
        //set starting position
        card.setPosition(0,0);
        //origin of transformation
        card.setOrigin(Align.center);

        //define center
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        //sequence = executes in order
        //parallel = executes all at once

        card.addAction(
                //run actions in order
                Actions.sequence(
                        //play multiple actions simultaneously
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(viewport.getWorldWidth()-card.getWidth(),
                                        viewport.getWorldHeight()-card.getHeight(),1f)
                        ),
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(0,0,1f)
                        ),
                        Actions.moveTo(centerX-card.getWidth()/2f,
                                centerY-card.getHeight()/2f,1f),
                        Actions.removeActor()
                )
        );

        return card;
    }
    private Actor createAnimation2(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.G2));
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH_RATIO);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT_RATIO);
        card.setPosition(0,viewport.getWorldHeight()-card.getHeight());
        card.setOrigin(Align.center);

        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        card.addAction(
                Actions.sequence(
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(viewport.getWorldWidth()-card.getWidth(),
                                        0,1f)
                        ),
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(0,viewport.getWorldHeight()-card.getHeight(),1f)
                        ),
                        Actions.moveTo(centerX-card.getWidth()/2f,
                                centerY-card.getHeight()/2f,1f),
                        Actions.removeActor()
                )
        );

        return card;
    }
    private Actor createAnimation3(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.R3));
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH_RATIO);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT_RATIO);
        card.setPosition(viewport.getWorldWidth()-card.getWidth(),0);
        card.setOrigin(Align.center);

        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        card.addAction(
                Actions.sequence(
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(0,viewport.getWorldHeight()-card.getHeight(),1f)
                        ),
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(viewport.getWorldWidth()-card.getWidth(),0,1f)
                        ),
                        Actions.moveTo(centerX-card.getWidth()/2f,
                                centerY-card.getHeight()/2f,1f),
                        Actions.removeActor()
                )
        );

        return card;
    }
    private Actor createAnimation4(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.Y4));
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH_RATIO);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT_RATIO);
        card.setPosition(viewport.getWorldWidth()-card.getWidth(),
                viewport.getWorldHeight()-card.getHeight());
        card.setOrigin(Align.center);

        final float centerX = viewport.getWorldWidth() / 2f;
        final float centerY = viewport.getWorldHeight() / 2f;

        card.addAction(
                Actions.sequence(
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(0,0,1f)
                        ),
                        Actions.parallel(
                                Actions.rotateBy(360,1f),
                                Actions.moveTo(viewport.getWorldWidth()-card.getWidth(),
                                        viewport.getWorldHeight()-card.getHeight(),1f)
                        ),
                        Actions.moveTo(centerX-card.getWidth()/2f,
                                centerY-card.getHeight()/2f,1f),
                        Actions.fadeOut(1f),
                        Actions.removeActor()
                )
        );

        return card;
    }

    private Actor createFinalCard(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.rainbow));

        final float centerX = viewport.getWorldWidth() / 2f;
        final float centerY = viewport.getWorldHeight() / 2f;

        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH_RATIO);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT_RATIO);

        //set alpha to 0
        card.getColor().a = 0f;

        card.setPosition(centerX-card.getWidth()/2f,
                centerY-card.getHeight()/2f);
        card.setOrigin(Align.center);

        card.addAction(
                Actions.sequence(
                        //wait 3 seconds until the animation starts
                        Actions.delay(3f),
                        Actions.parallel(
                                Actions.fadeIn(1f)
                                //Actions.scaleTo(CARD_SIZE*GameConfig.CARD_WIDTH,CARD_SIZE*GameConfig.CARD_HEIGHT,1f)
                        ),
                        Actions.removeActor()
                )
        );

        return card;
    }
}
