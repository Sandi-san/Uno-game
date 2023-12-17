package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

public class GameScreen extends ScreenAdapter {

    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;

    private Viewport viewport;
    private Stage stage;
    private SpriteBatch batch; //batch le en

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private Music music;

    //globale za igro
    //deki za vlecenje in za opuscanje
    private int deckSize = 104; //MAX st. kart
    private Deck deckDraw;
    private Deck deckDiscard;
    //karta, ki je na vrhu discard kupa
    private Card topCard;
    //trenutni turn
    private int playerTurn = 1;

    public GameScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();

        if(manager.getMusicPref()) {
            game.stopMusic();
            game.setMusic(assetManager.get(AssetDescriptors.GAME_MUSIC_1));
            game.playMusic();
        }
        batch = new SpriteBatch();
        initGame();
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        //KO JE KONEC IGRE
        //stage.addActor(createExitButton());
        //Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=200/255f;
        float g=30/255f;
        float b=100/255f;
        float a=0.7f; //prosojnost
        ScreenUtils.clear(r,g,b,a);

        //TODO POGLEJ KDAJ SE KLICE RENDER

        batch.begin();
        draw();
        batch.end();

        //stage.act(delta);
        //stage.draw();
    }

    public void draw(){
        String textureName = topCard.getTexture();
        TextureRegion region = gameplayAtlas.findRegion(textureName);
        float posX = GameConfig.WIDTH;
        float posY = GameConfig.HEIGHT;
        batch.draw(region,posX,posY);
    }

    @Override
    public void hide(){
        dispose();
    }
    @Override
    public void dispose(){
        stage.dispose();
    }


    public void initGame(){
        //ustvari main deck
        deckDraw = new Deck(deckSize,game);
        //deckDraw.generateRandom();
        deckDraw.generateByRules(2,2,2);
        deckDraw.shuffleDeck();
        //vzemi eno karto iz deka
        topCard = deckDraw.pickCard();
        //ustvari discard dek in polozi to karto nanj
        deckDiscard = new Deck(deckSize,game);
        deckDiscard.setCard(topCard);
    }
}
