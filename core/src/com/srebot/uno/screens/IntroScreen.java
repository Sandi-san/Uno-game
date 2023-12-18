package com.srebot.uno.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.config.GameConfig;

public class IntroScreen extends ScreenAdapter {

    //DOLZINA INTRO V SEKUNDIH
    public static final float INTRO_DURATION = 0f; //4f

    public static final float CARD_SIZE = 200f;

    private final Uno game;
    private final AssetManager assetManager;
    private final Music music;

    private Viewport viewport;
    private TextureAtlas gameplayAtlas;

    private float duration=0f;
    private Stage stage;

    private Image backgroundImage;

    public IntroScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        music = game.getMusic();
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport,game.getBatch());

        //NALOŽI VIRE
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

        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
        TextureRegion backgroundText = gameplayAtlas.findRegion(RegionNames.background1);
        backgroundImage = new Image(backgroundText);

        backgroundImage.setSize(stage.getWidth(),stage.getHeight());
        stage.addActor(backgroundImage);

        stage.addActor(createAnimation1());
        stage.addActor(createAnimation2());
        stage.addActor(createAnimation3());
        stage.addActor(createAnimation4());

        stage.addActor(createFinalCard());
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        backgroundImage.setSize(width,height);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=1/255f; //=1
        float g=255/255f;
        float b=200/255f;
        float a=0.8f; //prosojnost
        ScreenUtils.clear(r,g,b,a);

        duration += delta;

        //KO INTRO KONEC, POJDI NA MENU SCREEN
        if(duration>INTRO_DURATION){
            game.setScreen(new MenuScreen(game));
        }

        //TextureRegion background = gameplayAtlas.findRegion(RegionNames.background1);

        stage.act(delta);
        //stage.getBatch().begin();
        //stage.getBatch().draw(background,0,0,GameConfig.WIDTH,GameConfig.HEIGHT);
        //stage.getBatch().end();
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
        //VELIKOST OBJEKTA V WU
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT);
        //POZICIONIRAJ OBJEKT
        card.setPosition(0,0);
        //ORIGIN TRANSFORMACIJE ACTORJA
        card.setOrigin(Align.center);

        //definiraj center
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        //sequence - potek po vrstnem redu
        //parallel - izvede vse naenkrat

        card.addAction(
                Actions.sequence(
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
        //VELIKOST OBJEKTA V WU
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT);
        //POZICIONIRAJ OBJEKT
        card.setPosition(0,viewport.getWorldHeight()-card.getHeight());
        //ORIGIN TRANSFORMACIJE ACTORJA
        card.setOrigin(Align.center);

        //definiraj center
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        //sequence - potek po vrstnem redu
        //parallel - izvede vse naenkrat
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
        //VELIKOST OBJEKTA V WU
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT);
        //POZICIONIRAJ OBJEKT
        card.setPosition(viewport.getWorldWidth()-card.getWidth(),0);
        //ORIGIN TRANSFORMACIJE ACTORJA
        card.setOrigin(Align.center);

        //definiraj center
        float centerX = viewport.getWorldWidth() / 2f;
        float centerY = viewport.getWorldHeight() / 2f;

        //sequence - potek po vrstnem redu
        //parallel - izvede vse naenkrat

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
        //VELIKOST OBJEKTA V WU
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT);
        //POZICIONIRAJ OBJEKT
        card.setPosition(viewport.getWorldWidth()-card.getWidth(),
                viewport.getWorldHeight()-card.getHeight());
        //ORIGIN TRANSFORMACIJE ACTORJA
        card.setOrigin(Align.center);

        //definiraj center
        final float centerX = viewport.getWorldWidth() / 2f;
        final float centerY = viewport.getWorldHeight() / 2f;

        //sequence - potek po vrstnem redu
        //parallel - izvede vse naenkrat
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
                        /*
                        Actions.delay(0.01f),
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                // Create and add a new actor at the old actor's position
                                stage.addActor(createFinalCard());
                            }
                        }),
                        */
                        Actions.removeActor()
                )
        );

        return card;
    }

    private Actor createFinalCard(){
        Image card = new Image(gameplayAtlas.findRegion(RegionNames.rainbow));

        //definiraj center
        final float centerX = viewport.getWorldWidth() / 2f;
        final float centerY = viewport.getWorldHeight() / 2f;

        //VELIKOST OBJEKTA V WU
        card.setWidth(CARD_SIZE*GameConfig.CARD_WIDTH);
        card.setHeight(CARD_SIZE*GameConfig.CARD_HEIGHT);
        //card.setWidth(0f);
        //card.setHeight(0f);

        //NASTAVI PROSOJNOST NA 0
        card.getColor().a = 0f;

        //POZICIONIRAJ OBJEKT
        card.setPosition(centerX-card.getWidth()/2f,
                centerY-card.getHeight()/2f);
        //ORIGIN TRANSFORMACIJE ACTORJA
        card.setOrigin(Align.center);

        card.addAction(
                Actions.sequence(
                        //pocakaj 3s predenj se animacija zacne
                        Actions.delay(3f),
                        Actions.parallel(
                                Actions.fadeIn(1f)
                                //Actions.scaleTo(CARD_SIZE*GameConfig.CARD_WIDTH,
                                //        CARD_SIZE*GameConfig.CARD_HEIGHT,1f)
                        ),
                        Actions.removeActor()
                )
        );

        return card;
    }
}
