package com.srebot.uno.screens;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;

import java.util.List;

public class GameMultiplayerScreen extends ScreenAdapter {
    //status igre
    public enum State {
        Running, Paused, Over, Choosing
    }

    //kdo je zmagovalec? (za vse player-je)
    public enum Winner {
        Player1, Player2,
        Player3, Player4,
        None
    }

    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;
    private final GameService service;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Viewport hudViewport;

    private Stage stage;
    private SpriteBatch batch; //batch le en

    private Skin skin;
    private TextureAtlas gameplayAtlas;
    private Sound sfxPickup;
    private Sound sfxCollect;
    private BitmapFont font;

    GameSingleplayerScreen.State state;
    GameSingleplayerScreen.Winner winner;

    //GLOBALE ZA IGRO
    //DECKI
    private int deckSize = 104; //MAX st. kart (daj v GameConfig?)
    private Deck deckDraw;
    private Deck deckDiscard;
    //karta, ki je na vrhu discard kupa
    private Card topCard;

    //trenutni turn
    private int playerTurn;
    //preglej ce trenutni player naredil akcijo
    private boolean playerPerformedAction;
    //vrstni red
    private boolean clockwiseOrder;
    //AI difficulty
    private int difficultyAI;

    //playerji
    private Player player;
    private Player computer;
    private List<Player> playersData;

    //player hand arrow button display
    private boolean showLeftArrow;
    private boolean showRightArrow;

    private Array<Card> choosingCards = new Array<Card>();

    public GameMultiplayerScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //state = GameMultiplayerScreen.State.Running;

        //music on?
        if (manager.getMusicPref()) {
            game.stopMusic();
            game.setMusic(assetManager.get(AssetDescriptors.GAME_MUSIC_1));
            game.playMusic();
            game.setMusicVolume(manager.getMusicVolumePref());
        } else {
            game.stopMusic();
        }
        //sounds on?
        if (manager.getSoundPref()) {
            sfxPickup = assetManager.get(AssetDescriptors.PICK_SOUND);
            sfxCollect = assetManager.get(AssetDescriptors.SET_SOUND);
        }

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();
        //initGame();

        service.createGame();
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);
        hudViewport = new FitViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT);
        stage = new Stage(hudViewport, game.getBatch());


        //nastavi pozicijo kamere
        camera.position.set(GameConfig.WORLD_WIDTH / 2f,
                GameConfig.WORLD_HEIGHT, 0);
        camera.update();

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        //stage.addActor(createExitButton());
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);
    }

    @Override
    public void render(float delta) {
        //doloci barve ozadja
        float r = 200 / 255f;
        float g = 30 / 255f;
        float b = 100 / 255f;
        float a = 0.7f; //prosojnost
        ScreenUtils.clear(r, g, b, a);

        /*
        if (state != GameSingleplayerScreen.State.Over) {
            checkGamestate();
            handleInput();
        }
        */

        viewport.apply();
        //setProjectionMatrix - uporabi viewport za prikaz sveta (WORLD UNITS)
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        //draw();
        batch.end();

        /*
        if (state == GameSingleplayerScreen.State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
         */
    }

    //PREKRITE (STATIC) METODE
    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        /*
        batch.dispose();
        skin.dispose();
        gameplayAtlas.dispose();
        sfxPickup.dispose();
        sfxCollect.dispose();
         */
    }
}
