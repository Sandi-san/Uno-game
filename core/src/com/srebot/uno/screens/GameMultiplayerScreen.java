package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
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
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private State state;
    private Winner winner;

    //GLOBALE ZA IGRO
    //DECKI
    private Deck deckDraw;
    private Deck deckDiscard;
    //karta, ki je na vrhu discard kupa
    private Card topCard;

    //trenutni turn
    private int playerTurn;
    //preglej ce trenutni player naredil akcijo
    private boolean playerPerformedAction;
    //max players
    private int maxPlayers=2;
    //max st kart v decku
    private int deckSize=104;
    //vrstni red
    private boolean clockwiseOrder=true;

    //playerji
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;
    private List<Player> playersData;

    //player hand arrow button display
    private boolean showLeftArrow;
    private boolean showRightArrow;

    private Array<Card> choosingCards = new Array<Card>();

    //check if fetching from backend before continuing
    public interface PlayerFetchCallback {
        void onPlayerFetched(Player player);
    }

    public GameMultiplayerScreen(Uno game, Array<String> args) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        state = State.Running;

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
        initGame(args);
    }

    //pripravi igro (init globals)
    public void initGame(Array<String> args) {
        //0-numPlayers,1-deckSize,2-presetBox,3-orderBox
        maxPlayers = Integer.parseInt(args.get(0));
        deckSize = Integer.parseInt(args.get(1));
        String preset = args.get(2);
        clockwiseOrder = Objects.equals(args.get(3), "Clockwise");

        playersData = new ArrayList<>();
        //USTVARI DECKE
        //ustvari main deck
        deckDraw = new Deck(deckSize, game);
        deckDraw.generateBySize(deckSize,preset);
        deckDraw.shuffleDeck();

        //vzemi eno karto iz deka
        topCard = deckDraw.pickCard();

        //ustvari discard dek in polozi to karto nanj
        deckDiscard = new Deck(deckSize, game);
        deckDiscard.setCard(topCard);

        //USTVARI PLAYERJE IN NAPOLNI ARRAY
        for(int i=0;i<maxPlayers;++i){
            playersData.add(null);
        }
        //PRIPRAVI FIRST PLAYER (HOST)
        createPlayerFromBackend(manager.getNamePref(),player -> {
            // Code that should run after the player is fetched/created

            // Get first turn, set up game, etc.
            getFirstTurn();
            playerPerformedAction = false;

            // Create and save game data
            GameData gameData = new GameData(playersData, deckDraw, deckDiscard, maxPlayers, topCard);
            service.createGame(gameData);
        });

        //for each player dodaj v playerData
        //Pomembni order: bottom->left->top->right
        /*
        playersData.add(player); //bottom
        playersData.add(null);  //left
        playersData.add(computer); //top
        playersData.add(null);  //right
        */
    }

    private void createPlayerFromBackend(String playerName, PlayerFetchCallback callback){
        service.fetchPlayerByName(new GameService.PlayerFetchCallback() {
            @Override
            public void onSuccess(Player player, Hand hand) {
                // Handle the successful response
                Gdx.app.log("PLAYER", "Player fetched: " + player.getName());
                //GET PLAYER IN ADD V CURRENT GAME
                Hand playerHand = new Hand();
                Player thisPlayer = new Player(player.getName(), 0, playerHand);
                thisPlayer.getHand().pickCards(deckDraw, 5);
                addPlayerToArray(thisPlayer);

                // Invoke the callback with the fetched player
                callback.onPlayerFetched(thisPlayer);
            }

            @Override
            public void onFailure(Throwable t) {
                // Handle the error response
                Gdx.app.log("ERROR", "Failed to fetch player: " + t.getMessage());
                //CREATE NEW PLAYER AND ADD TO DB
                Hand playerHand = new Hand();
                Player player = new Player(manager.getNamePref(), 0, playerHand);
                player.getHand().pickCards(deckDraw, 5);
                Gdx.app.log("PLAYER", "CREATING NEW PLAYER INSTEAD: " + player.getName());
                service.createPlayer(player);
                addPlayerToArray(player);

                // Invoke the callback with the newly created player
                callback.onPlayerFetched(player);
            }
        }, playerName);
    }

    private void addPlayerToArray(Player player){
        //first player
        if(playersData.get(0)==null){
            playersData.set(0, player);
            player1 = player;
        }
        else if(playersData.get(1)==null){
            playersData.set(1, player);
            player2 = player;
        }
        if(maxPlayers>2) {
         if (playersData.get(2) == null) {
                playersData.set(2, player);
                player3 = player;
            }
        }
        else if (maxPlayers>3) {
            if (playersData.get(3) == null) {
                playersData.set(3, player);
                player4 = player;
            }
        }
    }

    //s cigavim turn se igra zacne
    private void getFirstTurn() {
        /*
        1-bottom
        2-left
        3-top
        4-right
         */
        playerTurn = 1;
        /*
        if(Objects.equals(manager.getStarterPref(), "Player"))
            playerTurn=1;
        else if(Objects.equals(manager.getStarterPref(), "Computer"))
            playerTurn=3;
         */
    }

    //vrni turn index naslednjega playerja, ce obstaja
    private int getNextTurn(int index) {
        do {
            if (clockwiseOrder) {
                if (index < 4)
                    index += 1;
                else
                    index = 1;
            } else {
                if (index > 1)
                    index -= 1;
                else
                    index = 4;
            }
        } while (playersData.get(index - 1) == null);
        return index;
    }


    //TODO: render? -> check players od game, dodaj player-ja ce se je join-al

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
