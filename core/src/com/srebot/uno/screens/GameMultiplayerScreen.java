package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;
import com.srebot.uno.config.SocketManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class GameMultiplayerScreen extends ScreenAdapter implements SocketManager.GameSocketListener {

    //possible game states
    public enum State {
        Initializing, //game still setting up with backend requests
        Paused, //game initialized correctly, but still needs more players
        Running, //game is running normally
        Over, //game is over
        Choosing //game is paused except for player who is choosing card
    }

    //possible end game states (winners)
    public enum Winner {
        Player1, Player2,
        Player3, Player4,
        None
    }

    //VARIABLES FROM INHERITED CLASS (Uno)
    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;
    private final GameService service;

    //GLOBAL VARIABLES
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private OrthographicCamera backgroundCamera;
    private Viewport viewport;
    private Viewport hudViewport;
    private Viewport backgroundViewport; //viewport for background
    private Sprite background;

    private Stage stage;
    private Stage stageHud;
    private SpriteBatch batch;

    private Skin skin;
    private TextureAtlas gameplayAtlas;
    private Sound sfxPickup;
    private Sound sfxCollect;
    private BitmapFont font;

    private State state;
    private Winner winner;

    //GAME OBJECT GLOBALS
    private Deck deckDraw;
    private Deck deckDiscard;
    private Card topCard; //top card of deckDiscard

    private int playerTurn = 0; //id of current Player within backend
    private boolean playerPerformedAction; //check if current player has performed action
    private int maxPlayers = 2; //max Players allowed to join game
    private boolean clockwiseOrder = false; //turn order of game

    //PLAYERS
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;
    private List<Player> playersData;   //array of players

    //check to display arrow button for player's Hand
    private boolean showLeftArrow;
    private boolean showRightArrow;

    //array of Card objects to display when game is in Choosing State
    private Array<Card> choosingCards = new Array<Card>();

    private int localPlayerId = 0;  //id of YOUR Player
    private int currentGameId = 0;  //id of THIS Game
    private boolean isWaiting = false;  //checks if YOUR Player is waiting
    private Player playerWaiting = null;    //Player who is currently playing their turn
    private boolean connectionError = false;    //for display if server connection error occurs

    //instance of SocketManager
    private SocketManager socketManager;

    /** Get playerId of current player for dispose function */
    public int getPlayerId() {
        return localPlayerId;
    }

    /** Get gameId of current game for dispose function */
    public int getGameId() {
        return currentGameId;
    }

    /** Callback for fetching one Player */
    public interface PlayerFetchCallback {
        void onPlayerFetched(Player player);
    }

    /** Method for creating Player when starting Game (host) */
    private void fetchPlayerFromBackend(PlayerFetchCallback callback) {
        service.fetchAuthenticatedPlayer(new GameService.PlayerFetchCallback() {
            @Override
            public void onSuccess(Player player) {
                //Get Player and add to current (local) Game, draw Cards from draw Deck
                Player thisPlayer = createPlayerAndDraw(player);
                localPlayerId = player.getId();
                //check if Player is already in Players array and add if not
                if(!isPlayerAlreadyInArray(thisPlayer))
                    addPlayerToArray(thisPlayer);
                connectionError = false;    //reset value to indicate no server error
                //invoke the callback with the fetched Player
                callback.onPlayerFetched(thisPlayer);
            }
            @Override
            public void onFailure(Throwable t) {
                connectionError = true; //change variable to indicate server error
                callback.onPlayerFetched(null);
            }
        }, manager.getAccessToken());
    }

    /** Callback for creating one Game */
    public interface GameCreateCallback {
        void onGameCreated(GameData game); //get newly created game (important to get and save ids)
    }

    /** Method for creating new Game in server and fetching the result */
    private void createGame(GameData gameData, GameCreateCallback callback) {
        service.createGame(new GameService.GameCreateCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("createGame SUCCESS", "Game created with ID: " + fetchedGame.getId());
                connectionError = false;
                callback.onGameCreated(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("createGame ERROR", "Failed to create game: " + t.getMessage());
                connectionError = true;
                callback.onGameCreated(null);
            }
        }, gameData, manager.getAccessToken());
    }

    /** Callback for fetching one Game */
    public interface GameFetchCallback {
        void onGameFetched(GameData game);
    }

    /** Method for fetching one Game object data */
    private void fetchGameFromBackend(int gameId, GameFetchCallback callback) {
        service.fetchGame(gameId, new GameService.FetchGameCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("fetchGameFromBackend SUCCESS", "Fetched game: "+game.getId());
                connectionError = false;
                //get array of players from current Game from DB
                callback.onGameFetched(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("fetchGameFromBackend ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onGameFetched(null);
            }
        });
    }

    /** Callback for updating one Game */
    public interface GameUpdateCallback {
        void onGameUpdated(GameData game);
        void onFailure(Throwable t);
    }

    /** Method for updating Game and fetching the result */
    private void updateGame(GameData gameData, GameUpdateCallback callback) {
        service.updateGame(new GameService.GameUpdateCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("updateGame SUCCESS", "Game updated on backend");
                connectionError = false;
                callback.onGameUpdated(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("updateGame ERROR", "Failed to create game: " + t.getMessage());
                connectionError = true;
                callback.onFailure(t);
            }
        }, currentGameId, gameData, manager.getAccessToken());
    }

    /** Get current GameData variables and update Game on server */
    private void updateGameData() {
        //check if any Hands or draw Deck is empty and get State (check if state isn't over to prevent inf loop)
        if(state!=State.Over)
            checkGamestate();
        //create new Game data
        GameData gameData = new GameData(
                playersData, deckDraw, deckDiscard, topCard, maxPlayers,
                state.toString(), playerTurn, getOrderAsString());
        gameData.setId(currentGameId);

        updateGame(gameData, new GameUpdateCallback() {
            @Override
            public void onGameUpdated(GameData game) {
                Gdx.app.log("GameUpdate SUCCESS", "Updated game data: " + game.getId());
                /*int localPlayersSize = getPlayersSize(); //fetched new players?
                if (game.getPlayers().length != localPlayersSize) {
                    checkPlayersChanged(game.getPlayers());
                }*/
                connectionError = false;
                setGameData(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("GameUpdate ERROR", "Failed to update game: " + t.getMessage());
                connectionError = true;
            }
        });
    }

    /** Method for updating Game by connecting a Player and fetching the result */
    private void updateGameWithPlayer(Player player, int gameId, GameUpdateCallback callback) {
        service.updateGameWithPlayer(new GameService.GameUpdatePlayersCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("updateGameWithPlayer SUCCESS", "Player added to backend");
                connectionError = false;
                //get array of players from current Game from DB
                callback.onGameUpdated(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("updateGameWithPlayer ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onFailure(t);
            }
        }, gameId, player, manager.getAccessToken());
    }

    /** Method for updating Game by disconnecting a Player and fetching the result */
    private void updateGameRemovePlayer(Player player, int gameId, GameUpdateCallback callback) {
        service.updateGameRemovePlayer(new GameService.GameUpdatePlayerRemoveCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("updateGameRemovePlayer SUCCESS", "Player removed from backend");
                connectionError = false;
                callback.onGameUpdated(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("updateGameRemovePlayer ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onFailure(t);
            }
        }, gameId, player, manager.getAccessToken());
    }

    /** Method for updating Game by disconnecting a Player and changing turn and fetching the result */
    private void updateGameRemovePlayerTurn(Player player, int gameId, int turn, GameUpdateCallback callback) {
        service.updateGameRemovePlayerTurn(new GameService.GameUpdatePlayerRemoveCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("updateGameRemovePlayerTurn SUCCESS", "Player removed from backend & turn updated");
                connectionError = false;
                callback.onGameUpdated(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("updateGameRemovePlayerTurn ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onFailure(t);
            }
        }, gameId, player, turn, manager.getAccessToken());
    }

    /** Callback for fetching multiple Players */
    public interface PlayersFetchCallback {
        void onPlayersFetched(Player[] players);
    }

    /** Method for fetching Players of Game from server */
    private void checkForNewPlayers(PlayersFetchCallback callback) {
        service.fetchGamePlayers(new GameService.FetchGamePlayersCallback() {
            @Override
            public void onSuccess(Player[] players) {
                Gdx.app.log("checkForNewPlayers SUCCESS", "Players fetched: " + players.length);
                connectionError = false;
                //get array of players from current Game from DB
                callback.onPlayersFetched(players);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("checkForNewPlayers ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onPlayersFetched(null);
            }
        }, currentGameId);
    }

    /** Callback for getting turn (and discard Deck/topCard) of one Game */
    public interface TurnFetchCallback {
        void onTurnFetched(GameData fetchedGame);
    }

    /** Method for fetching turn of Game from server */
    private void fetchTurn(TurnFetchCallback callback) {
        service.fetchGameTurn(new GameService.FetchGameTurnCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("fetchTurn SUCCESS", "Fetched turn: " + fetchedGame.getCurrentTurn());
                connectionError = false;
                //get array of players from current Game from DB
                callback.onTurnFetched(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("fetchTurn ERROR", "FAILED: " + t);
                connectionError = true;
                callback.onTurnFetched(null);
            }
        }, currentGameId, manager.getAccessToken());
    }

    /** Player checker listener: checks for newly joined players */
    @Override
    public void onPlayerChangedListener() {
        Gdx.app.log("onPlayerListener", "Players data changed. Re-fetching player list.");

        //get fetched Players data from server
        checkForNewPlayers(fetchedPlayers -> {
            int localPlayersSize = getPlayersSize();
            if (fetchedPlayers != null) {
                //check if local Players data is same as fetched Players data
                if (fetchedPlayers.length != localPlayersSize) {
                    //new Player found, so add it to local array
                    Gdx.app.log("playerChecker", "Changing players");
                    connectionError = false;
                    boolean changed = checkPlayersChanged(fetchedPlayers);
                    if(changed)
                        Gdx.app.log("playerChecker", "Players have been changed");

                    //re-fetch turn when Player connects
                    //(under rare circumstances, when Player leaves and turn is updated, the listener for turnChange isn't triggered
                    //and the remaining Player doesn't have the turn changed to theirs)
                    fetchTurn(fetchedTurnData -> {
                        int fetchedTurn = fetchedTurnData.getCurrentTurn();
                        if(fetchedTurn!=playerTurn) {
                            playerTurn = fetchedTurn;
                            setWaitingPlayer(fetchedTurn);
                        }
                    });

                    //change gameState based on Player data
                    waitForPlayers();
                }
                else {
                    connectionError = false;
                    Gdx.app.log("playerChecker", "No new players");
                }
            } else {
                connectionError = true;
                Gdx.app.log("playerChecker ERROR", "Checking for players: Fetched players are null");
            }
        });
    }

    /** Checks Game State depending on amount of players */
    public boolean waitForPlayers() {
        int count = getPlayersSize();
        //1 or less players: game cannot start
        if (count <= 1) {
            if(state == State.Running || state == State.Initializing)
                state = State.Paused;
            return true;
        }
        //2 or more players: game can start
        //if (count >= 2 && state != State.Running) {
        if(state == State.Paused || state == State.Initializing)
            state = State.Running;
        return false;
    }

    /** Turn checker listener: checks for changes within turn and topCard when not currently playing */
    @Override
    public void onTurnChangedListener(int fetchedTurn, Card fetchedTopCard, String fetchedGameState) {
        Gdx.app.log("onTurnChanged","Turn changed: "+fetchedTurn);
        if (waitForTurn()) {
            //immediately change fetched states when they're Over or Paused
            if (Objects.equals(fetchedGameState, "Over")) {
                state = State.Over;
            } else if (Objects.equals(fetchedGameState, "Paused")) {
                state = State.Paused;
            }

            setWaitingPlayer(fetchedTurn);

            Gdx.app.log("turnChecker", "State: " + fetchedGameState);
            //fetched turn is valid
            if (fetchedTurn != 0) {
                topCard = fetchedTopCard;
                //fetched turn is same as index of player: it is YOUR Player's turn
                if (fetchedTurn == localPlayerId || state == State.Over) {
                    //get full data of database
                    fetchGameFromBackend(currentGameId, fetchedGame -> {
                        if (fetchedGame != null) {
                            isWaiting = false;
                            Gdx.app.log("turnChecker", "Game fetched: " + fetchedGame.getId());
                            connectionError = false;
                            //update local Game data with fetched Game data
                            setGameData(fetchedGame);
                        } else {
                            connectionError = true;
                            Gdx.app.log("turnChecker ERROR", "Failed to update game with player.");
                        }
                    });
                }
            } else {
                connectionError = true;
                Gdx.app.log("turnChecker ERROR", "Fetched turn or top card are null");
            }
        }
        isWaiting = false;
        Gdx.app.log("turnChecker", "Player: " + localPlayerId + " is not waiting.");
        //connectionError = false;
    }

    /** Checks if current turn is same as YOUR Player id */
    public boolean waitForTurn() {
        if(getPlayersSize()<2)
            return false;
        if (playerTurn != localPlayerId) {
            isWaiting = true;
            return true;
        }
        return false;
    }

    /** Get Player object which is currently performing their turn */
    private void setWaitingPlayer(int turn){
        //get Player (from current turn - id) to use as within hud display
        Gdx.app.log("turnChecker", "Player: " + localPlayerId + " is waiting for turn...");
        playerWaiting = getPlayerById(turn);
        if (playerWaiting != null)
            Gdx.app.log("turnChecker", "Waiting for player: " + playerWaiting.getName());
        else
            Gdx.app.log("turnChecker", "Player waiting is null: " + playerWaiting);
    }

    /** Constructor for Creating new Game */
    public GameMultiplayerScreen(Uno game, Array<String> args) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //set beginning state to Initializing before all necessary data is ready
        state = State.Initializing;
        winner = Winner.None;

        setMusicAndSounds();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();

        //initialize SocketManager
        socketManager = new SocketManager(this);

        //initialize Game data
        initGame(args);
    }

    /** Load music and sounds */
    private void setMusicAndSounds() {
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
    }

    /** Initialize Game objects (when creating Game) */
    private void initGame(Array<String> args) {
        //args is array of strings to determine starting game variables:
        //0 - maxPlayers, 1 - deckSize, 2 - presetBox, 3 - orderBox
        maxPlayers = Integer.parseInt(args.get(0));
        int deckSize = Integer.parseInt(args.get(1));
        String preset = args.get(2);
        clockwiseOrder = Objects.equals(args.get(3), "Clockwise");

        //CREATE DECKS
        //Create draw Deck
        deckDraw = new Deck(deckSize);
        deckDraw.generateBySize(deckSize, preset);
        //shuffle deck and set topCard as first card from drawDeck
        deckDraw.shuffleDeck();
        topCard = deckDraw.pickCard();

        //create discard Deck and add topCard
        deckDiscard = new Deck(deckSize);
        deckDiscard.setCard(topCard);

        //FILL PLAYERS ARRAY
        playersData = new ArrayList<>();
        for (int i = 0; i < maxPlayers; ++i) {
            playersData.add(null);
        }

        //Fetch (logged) Player (host) within server
        fetchPlayerFromBackend(player -> {
            if (player != null) {
                //set playerTurn as id of player that created the game (first player)
                int currentPlayerTurn = player.getId();
                if (currentPlayerTurn != 0)
                    playerTurn = currentPlayerTurn;

                //create GameData object and send to server for creating Game object
                GameData gameData = new GameData(
                        playersData, deckDraw, deckDiscard, topCard, maxPlayers,
                        state.toString(), playerTurn, getOrderAsString());

                //create game in DB and fetch id of newly created game
                createGame(gameData, fetchedGame -> {
                    if (fetchedGame != null) {
                        //set current game id locally (prevent unneeded DB fetching)
                        setGameData(fetchedGame);
                        //game is initialized, change state to Paused
                        state = State.Paused;
                        connectionError = false;

                        //connect to Game room on server
                        socketManager.connect(fetchedGame.getId());
                    }
                    else {
                        connectionError = true;
                        Gdx.app.log("initGame ERROR", "Created game is null");
                    }
                });
            } else {
                connectionError = true;
                Gdx.app.log("initGame ERROR", "Created player from backend is null");
            }
        });
    }

    /** Returns string based on boolean value */
    private String getOrderAsString() {
        if (clockwiseOrder)
            return "Clockwise";
        return "Counter Clockwise";
    }

    /** Constructor for Joining existing Game */
    public GameMultiplayerScreen(Uno game, int gameId) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //set beginning state to Initializing before all necessary data is ready
        state = State.Initializing;
        winner = Winner.None;

        setMusicAndSounds();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();
        playersData = new ArrayList<>();

        //initialize SocketManager
        socketManager = new SocketManager(this);

        //Fetch the Game from server
        fetchGameFromBackend(gameId, fetchedGame -> {
            if (fetchedGame != null) {
                Gdx.app.log("Join Game", "Game fetched: " + fetchedGame.getId());

                //set local Game variables with fetched Game data
                setGameData(fetchedGame);
                //set state to Paused so draw() and other functions don't execute until all necessary data is fetched
                state = State.Paused;

                //Create the Player within server (or fetch if already exists - important for getting Player id)
                fetchPlayerFromBackend(player -> {
                    state = State.Paused;
                    if (player != null) {
                        //Connect the new Player to the Game's player list and update the Game
                        updateGameWithPlayer(player, fetchedGame.getId(), new GameUpdateCallback() {
                            @Override
                            public void onGameUpdated(GameData updatedGame) {
                                Gdx.app.log("Join Game", "Game updated with player: " + player.getId());
                                localPlayerId = player.getId(); //save local Player's id as id of joined Player
                                setGameData(updatedGame);
                                //state = State.Paused;
                                connectionError = false;

                                //connect to Game room on server
                                socketManager.connect(updatedGame.getId());

                                setWaitingPlayer(updatedGame.getCurrentTurn());

                                waitForPlayers();
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                connectionError = true;
                                Gdx.app.log("Join Game ERROR", "Failed to update game with player.");
                            }
                        });
                    }
                    else {
                        connectionError = true;
                        Gdx.app.log("Join Game ERROR", "Created player from backend is null");
                    }
                });

            } else {
                connectionError = true;
                Gdx.app.log("Join Game ERROR", "Failed to fetch game from backend.");
            }
        });
    }

    /** Set local GameData variables from Game fetched from server */
    private void setGameData(GameData fetchedGame) {
        currentGameId = fetchedGame.getId();

        //create Deck objects
        deckDraw = fetchedGame.getDecks()[0];
        deckDiscard = fetchedGame.getDecks()[1];

        //set current topCard
        topCard = fetchedGame.getTopCard();
        //topCard = deckDiscard.getTopCard();

        maxPlayers = fetchedGame.getMaxPlayers();

        //set Players and fill Players array
        if (playersData.isEmpty()) {
            for (int i = 0; i < fetchedGame.getMaxPlayers(); ++i) {
                playersData.add(null);
            }
        }
        //if no players changed (between local & DB) copy player data from DB
        if (!checkPlayersChanged(fetchedGame.getPlayers())) {
            //get players from DB
            Player[] fetchedPlayers = fetchedGame.getPlayers();
            for (int i=0; i<fetchedPlayers.length; ++i) {
                //if newly fetched player is already in array don't re-add (important on gameCreate)
                if(!isPlayerAlreadyInArray(fetchedPlayers[i]))
                    addPlayerToArray(fetchedPlayers[i]);
                //get current Hand of player in updated DB and save locally (updates card ids!)
                else{
                    //equate Player's fetched Hand to local Hand (prevents accidentally switching Hands between Players when array order changes)
                    for(Player fetchedPlayer : fetchedPlayers){
                        Player checkedPlayer = playersData.get(i);
                        if(checkedPlayer!=null) {
                            if (checkedPlayer.getId() == fetchedPlayer.getId())
                                checkedPlayer.setHand(fetchedPlayer.getHand());
                        }
                    }
                }
            }
        }
        //else, players already changed within checkPlayersChanged function

        //set other variables
        state = State.valueOf(fetchedGame.getGameState());
        playerTurn = fetchedGame.getCurrentTurn();
        clockwiseOrder = Objects.equals(fetchedGame.getTurnOrder(), "Clockwise");
    }

    /** Checks if any changes occurred between local Players array and fetched Players */
    private boolean checkPlayersChanged(Player[] fetchedPlayers) {
        boolean changed = false;
        //if game is over, don't update Players (causes null exception on drawing game end hud)
        if(state!=State.Over) {
            //playerData is uninitialized (no non-null elements)
            if (getPlayersSize() == 0)
                return false;

            //Create a set of IDs from fetchedPlayers for quick lookup
            Set<Integer> fetchedPlayerIds = new HashSet<>();
            for (Player fetchedPlayer : fetchedPlayers) {
                fetchedPlayerIds.add(fetchedPlayer.getId());
            }

            //Remove players from playersData if they are not in fetchedPlayers
            for (int i = 0; i < playersData.size(); i++) {
                Player localPlayer = playersData.get(i);
                if (localPlayer != null && !fetchedPlayerIds.contains(localPlayer.getId())) {
                    playersData.set(i, null); //remove the player by setting the slot to null
                    removePlayerBasedIndex(i);
                    Gdx.app.log("checkPlayersChanged","Removed Player from index: "+i);
                    changed = true;
                }
            }

            //Add new players from fetchedPlayers to the first available null slot in playersData
            for (Player fetchedPlayer : fetchedPlayers) {
                boolean playerExists = false;
                for (Player localPlayer : playersData) {
                    if (localPlayer != null && localPlayer.getId() == fetchedPlayer.getId()) {
                        playerExists = true;
                        break;
                    }
                }

                //if player does not exist in playersData, add them to the first null slot
                if (!playerExists) {
                    fetchedPlayer = createPlayerAndDraw(fetchedPlayer);
                    addPlayerToArray(fetchedPlayer);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /** Sets null Player object based on index in playersData */
    private void removePlayerBasedIndex(int i) {
        switch (i) {
            case 0:
                player1 = null;
                break;
            case 1:
                player2 = null;
                break;
            case 2:
                player3 = null;
                break;
            case 3:
                player4 = null;
                break;
        }
    }

    /** Checks if given Player with id already exists in Players array */
    private boolean isPlayerAlreadyInArray(Player player){
        for(Player arrayPlayer : playersData){
            if(arrayPlayer!=null && player!=null) {
                if (arrayPlayer.getId() == player.getId())
                    return true;
            }
        }
        return false;
    }

    /** Adds Player to Players array in first available slot and initializes position */
    private void addPlayerToArray(Player player) {
        Gdx.app.log("addPlayerToArray", "Added Player: " + player.getName());
        //first Player
        if (playersData.get(0) == null) {
            playersData.set(0, player);
            player1 = player;
        }
        //second Player
        else if (playersData.get(1) == null) {
            playersData.set(1, player);
            player2 = player;
        }
        //third Player
        else if (maxPlayers == 3) {
            if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) == null) {
                playersData.set(2, player);
                player3 = player;
            }
        }
        //third/fourth Players
        else if (maxPlayers == 4) {
            //third Player
            if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) == null) {
                playersData.set(2, player);
                player3 = player;
            }
            //fourth Player
            else if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) != null && playersData.get(3) == null) {
                playersData.set(3, player);
                player4 = player;
            }
        }
    }

    /** Fetches Player from server and creates new local Player object and initializes Hand by drawing from draw Deck */
    private Player createPlayerAndDraw(Player player) {
        //Player has no initialized Hand so create new and draw
        if(player.getHand() == null) {
            Player thisPlayer = new Player(player.getId(), player.getName(), 0, new Hand());
            thisPlayer.getHand().pickCards(deckDraw, 5);
            return thisPlayer;
        }
        //Player already has initialized Hand but no cards, so draw
        else if(player.getHand().getCards().isEmpty()) {
            Player thisPlayer = new Player(player.getId(), player.getName(), 0, new Hand());
            thisPlayer.getHand().pickCards(deckDraw, 5);
        }
        //do not draw from deck again if Player has already drawed
        else {
            //player already has Hand data, but those cards must be removed from local draw Deck to prevent duplication
            deckDraw.removeCards(player.getHand().getCards());
        }
        return player;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new ExtendViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, hudCamera);
        backgroundCamera = new OrthographicCamera();
        backgroundViewport = new StretchViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, backgroundCamera);
        stage = new Stage(hudViewport, game.getBatch());
        stageHud = new Stage(hudViewport, game.getBatch());

        //nastavi pozicijo kamere
        camera.position.set(GameConfig.WORLD_WIDTH / 2f,
                GameConfig.WORLD_HEIGHT, 0);
        camera.update();

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background1);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        stage.addActor(createExitButton(State.Over));
        stageHud.addActor(createExitButton(State.Running));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);

        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.78f, 0.11f, 0.39f, 0.7f);

        //draw background
        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

        //draw stage here so user can exit during initialization or pause state
        if (state != State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stageHud.act(delta);
            stageHud.draw();
            Gdx.input.setInputProcessor(stageHud);
        }

        //Game not yet finished creating in database
        if (state == State.Initializing)
            return;
        //Game has 2 or more Players and isn't over, execute game logic
        else if (state != State.Over) {
            checkGamestate();
            //handle input only if it is current Player's turn
            if (!waitForTurn()) {
                //current Player's turn, enable Card choosing
                handleInput();
                //todo: handle input tudi ko ni tvoj turn, razn setanje/pickanje card iz deck
            }
        }

        //render in viewport dimensions
        viewport.apply();
        //setProjectionMatrix - use viewport for world view (set in WORLD UNITS)
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        draw();
        batch.end();

        //render in hudViewport dimensions
        hudViewport.apply();
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.begin();
        drawHud();
        batch.end();

        //Game is over, stop music and draw over stage
        if (state == State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
    }

    /** Get local playersData size (without null elements) and check State */
    private int getPlayersSize() {
        int count = 0;
        for (Player player : playersData) {
            if (player != null)
                count++;
        }
        if (count >= 2) {
            if(state == State.Paused)
                state = State.Running;
        }
        else {
            if (state == State.Running)
                state = State.Paused;
        }
        return count;
    }

    /** Get index in playersData of current player (used for circling through playersData array) */
    private int getIndexOfCurrentPlayer() {
        int index = -1;
        for (int i = 0; i < playersData.size(); ++i) {
            Player player = playersData.get(i);
            if (player != null) {
                if (player.getId() == localPlayerId) {
                    index = i;
                    break;
                }
            }
        }
        if (index == -1)
            Gdx.app.log("getIndexOfCurrentPlayer ERROR", "Player with id " + localPlayerId + " not found in playersData");
        return index;
    }

    /** Draw Hands and Card elements */
    private void draw() {
        //get dimensions of Card elements in v WORLD UNITS
        float sizeX, sizeY;
        if (getPlayersSize() == 2) {
            sizeX = GameConfig.CARD_WIDTH;
            sizeY = GameConfig.CARD_HEIGHT;
        }
        //small size if 3 or 4 players
        else {
            sizeX = GameConfig.CARD_WIDTH_SM;
            sizeY = GameConfig.CARD_HEIGHT_SM;
        }

        int numPlayers = getPlayersSize();

        if (state == State.Running) {
            //MIDDLE DECK
            if (topCard == null)
                topCard = deckDiscard.getTopCard();
            String topCardTexture = topCard.getTexture();
            TextureRegion topCardRegion = gameplayAtlas.findRegion(topCardTexture); //get Card texture from atlas
            //set x and y positions
            float topX = (viewport.getWorldWidth() - sizeX) / 2f;
            float topY = (viewport.getWorldHeight() - sizeY) / 2f;
            topCard.setPositionAndBounds(topX, topY, sizeX, sizeY); //set positions to topCard object
            Card.render(batch, topCardRegion, topCard); //render Card object

            //DRAW DECK
            TextureRegion drawDeckRegion = gameplayAtlas.findRegion(RegionNames.back);
            //render far right of screen
            float drawX = (viewport.getWorldWidth() - sizeX);
            //or slightly right of discard Deck but not covering left player cards
            if (numPlayers > 2)
                drawX = drawX - (topX / 2f);
            float drawY = (viewport.getWorldHeight() - sizeY) / 2f;
            deckDraw.setPositionAndBounds(drawX, drawY, sizeX, sizeY);
            Card.render(batch, drawDeckRegion, deckDraw.getPosition().x, deckDraw.getPosition().y, sizeX, sizeY);
        }
        if (state != State.Paused && state != State.Initializing) {
            //Draw Player Hands for each current Player
            int currentPlayerIndex = getIndexOfCurrentPlayer();
            //check if data from DB has not saved locally yet
            if (currentPlayerIndex == -1)
                return;
            for (int i = 0; i < playersData.size(); ++i) {
                //calculate the index in a circular manner (user always has their Hand rendered at bottom)
                int playerIndex = (currentPlayerIndex + i) % playersData.size();
                //get the Player at the calculated index
                Player player = playersData.get(playerIndex);

                if (player != null) {
                    boolean isCurrentPlayer = (player.getId() == localPlayerId);

                    Hand playerHand = player.getHand();
                    playerHand.setArrowRegions(gameplayAtlas.findRegion(RegionNames.arrow));
                    drawHand(playerHand, i,
                            sizeX, sizeY, numPlayers, isCurrentPlayer);
                }
            }

            //render color wheel if along with Hands
            if (state == State.Choosing) {
                drawColorWheel();
            }
        }
    }

    /** Draw a Player's Hand (Hand object to draw, index where on screen to draw, x size of Cards, y size of Cards, number of game players, drawing current player's Hand?) */
    private void drawHand(Hand hand, int index, float sizeX, float sizeY, int numPlayers, boolean isPlayer) {
        //SET LOCAL VARIABLES
        Array<Card> cards = hand.getCards();
        int size = cards.size;
        int maxCardsShow = getMaxCardsShow();   //max cards to show in Hand
        //get first/last index of Hand object to draw
        int firstIndex = hand.getIndexFirst();
        int lastIndex = hand.getIndexLast();
        //fix indexes if needed for player
        if(isPlayer)
            lastIndex = hand.getIndexLast(maxCardsShow);

        //Which side on screen to draw Hand based on index (0 - P1, 1 - P2, 2 - P3, 3 - P4)
        float startX = 0;   //start at bottom
        //Y-axis: where to draw cards depending on current player
        float startY = 0;   //bottom
        switch (index) {
            //right or top
            case 1:
                if (numPlayers == 2) {
                    //top
                    startY = viewport.getWorldHeight() - sizeY;
                } else {
                    //right
                    startY = sizeX;
                    startX = 1; //start at right
                }
                break;
            //top
            case 2:
                startY = viewport.getWorldHeight() - GameConfig.CARD_HEIGHT_SM; //top
                startX = 2; //start at top
                break;
            //left
            case 3:
                startY = sizeX; //left
                startX = 3; //start at left
                break;
        }

        float overlap = 0f;
        float spacing;

        //Calculate overlap between cards:
        //when drawing on left/right sides of screen
        if (startX == 1 || startX == 3) {
            for (int i = 5; i < size; ++i) {    //max cards before spacing
                if (i >= 7) break;              //max cards before no more spacing
                overlap += 0.15f;
            }
        }
        //when drawing on top/bottom sides of screen
        else {
            for (int i = maxCardsShow - 2; i < size; ++i) {
                if (i >= maxCardsShow) break;
                overlap += 0.1f;
            }
        }
        //calculate proper overlap
        overlap = sizeX * (1 - overlap);
        spacing = sizeX;

        //Rendering Hands on top/bottom (0 - bottom, 2 - top)
        if (startX == 0 || startX == 2) {
            //without spacing
            if (size <= maxCardsShow - 2) {
                hand.setIndexLast();
                lastIndex = hand.getIndexLast();
            }
            //set spacing until reaching MaxCardsToShow
            else if (size <= maxCardsShow) {
                hand.setIndexLast();
                lastIndex = hand.getIndexLast();
                spacing = overlap;
            }
            //stop spacing when Hand contains more cards than MaxCardsToShow
            else {
                spacing = overlap;
            }

            //calculate horizontal space not covered by Card elements
            float sizeLeft = viewport.getWorldWidth() - ((lastIndex+1 - firstIndex) * spacing);
            startX = sizeLeft / 2f; //divide by half for drawing (left) arrow

            //RENDERING CARDS
            Array<Integer> indexHover = new Array<Integer>();   //prepare array of hovered Card indexes to draw after drawing other cards (player only)
            //iterate from first index to last index
            for (int i = firstIndex; i < size; ++i) {
                //break when reaching lastIndex (avoids index out of range)
                if (i > lastIndex)
                    break;
                Card card = cards.get(i);
                String texture;
                TextureRegion region;
                //draw Card if not highlighted (highlighted (selected) cards are rendered later)
                if (!card.getHighlight()) {
                    //get actual texture of Card if it is player's or the game is Over
                    if (isPlayer || state == State.Over)
                        texture = card.getTexture();
                    //else draw back texture (mostly for computers)
                    else
                        texture = RegionNames.back;
                    region = gameplayAtlas.findRegion(texture);
                    //calculate positions (consider card spacing and drawing one after another)
                    float posX = (startX + (i - firstIndex) * spacing)-1f; //small padding fix
                    float posY = startY;
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                } else {
                    indexHover.add(i);  //add Card to hovered Card array
                }
            }

            //Draw hovered cards (if any) - cards are drawn on top of Hand and slightly higher
            if (!indexHover.isEmpty()) {
                for (int j : indexHover) {
                    Card card = cards.get(j);
                    String texture;
                    TextureRegion region;
                    if (isPlayer || state == State.Over)
                        texture = card.getTexture();
                    else
                        texture = RegionNames.back;
                    region = gameplayAtlas.findRegion(texture);
                    float posX = (startX + (j - firstIndex) * spacing) - 1f;
                    float posY = startY + 2f; //render slightly higher
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                }
            }

            //if (isPlayer)
            //    Gdx.app.log("PLAYER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);

            //calculate where to draw right arrow (after last drawn card)
            float sizeRight = ((lastIndex+1 - firstIndex) * spacing);
            float endX = sizeRight + (sizeLeft/2f);

            //check whether to render arrows (draw only on players and non-Over states)
            if (isPlayer) {
                //show left arrow if rendered Hand is not at minimum index
                if (firstIndex != 0 && state != State.Over)
                    showLeftArrow = true;
                else
                    showLeftArrow = false;
                //show right arrow if rendered Hand is not at maximum index
                if (lastIndex != cards.size - 1 && state != State.Over)
                    showRightArrow = true;
                else
                    showRightArrow = false;
            }

            //render left arrow
            if (size >= maxCardsShow && isPlayer && showLeftArrow) {
                float arrowX = startX - sizeX / 2 - (sizeX * 0.1f);
                float arrowY = startY + (sizeY * 0.2f);
                hand.setArrowRegionLeft(arrowX, arrowY, sizeX / 2, sizeY / 2);
                hand.renderArrowLeft(batch);
            }
            //render right arrow
            if (size >= maxCardsShow && isPlayer && showRightArrow) {
                float arrowX = endX + (sizeX * 0.1f);
                float arrowY = startY + (sizeY * 0.2f);
                hand.setArrowRegionRight(arrowX, arrowY, sizeX / 2, sizeY / 2);
                hand.renderArrowRight(batch);
            }
        }

        //Rendering Hands on right/left (1 - right, 3 - left)
        else if (startX == 1 || startX == 3) {
            //for rotating Card 90deg (far left) or -90deg (far right)
            int rotationScalar = 1;
            if (startX == 3)
                rotationScalar = -1;

            //without spacing
            if (size <= 5) {
                hand.setIndexLast();
                startY = (viewport.getWorldHeight() - size * sizeX) / 2f;
            }
            //spacing until reaching MaxCardsToShow
            else if (size <= 7) {
                hand.setIndexLast();
                spacing = overlap;
                startY = (viewport.getWorldHeight() - size * sizeX) / 2f;
            }
            //stop spacing when Hand contains more cards than MaxCardsToShow
            else {
                spacing = overlap;
                startY = (viewport.getWorldHeight() - maxCardsShow * sizeX) / 2f;
            }

            //limit bottom (start) for rendering cards (failsafe)
            if (startY < (sizeY * 0.8f))
                startY = (sizeY * 0.8f);

            //set x-axis based on hand location (left/right) (+ padding alterations)
            if (startX == 1)
                startX = viewport.getWorldWidth() - (GameConfig.CARD_WIDTH_SM + GameConfig.CARD_HEIGHT_SM * 0.1f); //slightly left
            else
                startX = GameConfig.CARD_HEIGHT_SM * 0.1f; //slightly right

            //Render cards
            for (int i = firstIndex; i < size; ++i) {
                if (i >= 7) break;  //always draw maximum of (first) 7 cards

                Card card = cards.get(i);
                String texture;
                TextureRegion region;

                //get actual texture if game is over
                texture = (isPlayer || state == State.Over) ? card.getTexture() : RegionNames.back;
                region = gameplayAtlas.findRegion(texture);

                float posX = startX; //x-axis stays constant (left or right)
                float posY = startY + (i - firstIndex) * spacing; //adjust along the y-axis

                card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                Card.renderFlipped(batch, region, card, rotationScalar);
            }
        }
    }

    /** Draw Hud text elements */
    private void drawHud(){
        //define outline offsets and outline color for big (regular size 1f) text (double outline = cleaner look)
        float bigOutlineOffset1 = 2.2f;  //offset for 1st outline (outer)
        float bigOutlineOffset2 = 1.6f;  //offset for 2nd outline (inner)

        //if connectionError variable is set, meaning there was an error connecting to server, draw error text
        if(connectionError) {
            //set text and get size to correctly draw the text in the center of the screen
            String errorText = "Server Connection Error";
            GlyphLayout errorLayout = new GlyphLayout();
            errorLayout.setText(font,errorText);
            float errorX = hudViewport.getWorldWidth()/2f - errorLayout.width/2f;
            float errorY = hudViewport.getWorldHeight()/2f + errorLayout.height/2f;

            //set outline color
            font.setColor(Color.FIREBRICK);
            //draw the text multiple times to create an outline effect (up, down, left, right)
            font.draw(batch, errorText, errorX + bigOutlineOffset1, errorY + bigOutlineOffset1);
            font.draw(batch, errorText, errorX + bigOutlineOffset2, errorY + bigOutlineOffset2);
            font.draw(batch, errorText, errorX - bigOutlineOffset1, errorY + bigOutlineOffset1);
            font.draw(batch, errorText, errorX - bigOutlineOffset2, errorY + bigOutlineOffset2);
            font.draw(batch, errorText, errorX + bigOutlineOffset1, errorY - bigOutlineOffset1);
            font.draw(batch, errorText, errorX + bigOutlineOffset2, errorY - bigOutlineOffset2);
            font.draw(batch, errorText, errorX - bigOutlineOffset1, errorY - bigOutlineOffset1);
            font.draw(batch, errorText, errorX - bigOutlineOffset2, errorY - bigOutlineOffset2);

            //set text color
            font.setColor(Color.WHITE);
            font.draw(batch, errorText, errorX,errorY);
            return;
        }

        //Draw info of players (name and number of cards)
        if(state == State.Running || state == State.Over) {
            //set the font to a smaller scale for the player's text
            font.getData().setScale(0.8f);  //scale down to 80% of the original size

            float startX = 10f; //width left + margin
            float startY = hudViewport.getWorldHeight()-10f; //top + margin
            //height of each line of font + spacing
            float lineHeight = font.getXHeight();

            //define outline offsets and outline color (double outline = cleaner look)
            float outlineOffset1 = 1.8f;  //offset for 1st outline (outer)
            float outlineOffset2 = 1.2f;  //offset for 2nd outline (inner)

            float waitingY = 0; //y-axis position where to draw waiting text
            float playerY = startY; //y-axis position where to start drawing first Player text

            //get size of all Players and index of current Player
            int numPlayers = getPlayersSize();
            int currentPlayerIndex = getIndexOfCurrentPlayer();
            if(currentPlayerIndex==-1)
                return;

            //iterate through each Player to draw info text
            for (int i = 0; i < numPlayers; ++i) {
                //get index of current player first (localId) (current player always starts at bottom)
                int playerIndex = (currentPlayerIndex + i) % playersData.size();
                //get next player at the calculated index
                Player player = playersData.get(playerIndex);
                if (player != null) {
                    //0-P1, 1-P2, 2-P3, 3-P4
                    String position = "Bottom";
                    switch (i) {
                        case 1:
                            if (numPlayers == 2) {
                                position = "Top";
                            } else {
                                position = "Right";
                            }
                            break;
                        case 2:
                            position = "Top";
                            break;
                        case 3:
                            position = "Left";
                    }
                    String playerText = position + ": " + player;   //text to display per each line
                    //get vertical start of each new player
                    if(i!=0)
                        playerY = playerY - (lineHeight*2);

                    //set the outline color
                    font.setColor(Color.BLACK);

                    //draw the text multiple times to create an outline effect (up, down, left, right)
                    font.draw(batch, playerText, startX + outlineOffset1, playerY + outlineOffset1);
                    font.draw(batch, playerText, startX + outlineOffset2, playerY + outlineOffset2);
                    font.draw(batch, playerText, startX - outlineOffset1, playerY + outlineOffset1);
                    font.draw(batch, playerText, startX - outlineOffset2, playerY + outlineOffset2);
                    font.draw(batch, playerText, startX + outlineOffset1, playerY - outlineOffset1);
                    font.draw(batch, playerText, startX + outlineOffset2, playerY - outlineOffset2);
                    font.draw(batch, playerText, startX - outlineOffset1, playerY - outlineOffset1);
                    font.draw(batch, playerText, startX - outlineOffset2, playerY - outlineOffset2);

                    //draw the original text with different color
                    font.setColor(Color.WHITE);
                    font.draw(batch, playerText, startX, playerY);

                    //set waitingY to start at position where last Player text was drawn
                    if(isWaiting)
                        waitingY = playerY;
                }
            }

            //if isWaiting variable is set, meaning Player is currently waiting for turn, and waiting for an actual Player, draw waiting text
            if(isWaiting && playerWaiting != null){
                //set y-axis position one line lower than last text (last drawn Player data)
                waitingY = waitingY - (lineHeight*2);
                font.getData().setScale(1f);    //set scale back to default (1f)
                String waitingText = "Waiting for: " + playerWaiting.getName();

                //set outline color
                font.setColor(Color.GOLDENROD);
                //draw outline effect
                font.draw(batch, waitingText, startX + outlineOffset1, waitingY + outlineOffset1);
                font.draw(batch, waitingText, startX + outlineOffset2, waitingY + outlineOffset2);
                font.draw(batch, waitingText, startX - outlineOffset1, waitingY + outlineOffset1);
                font.draw(batch, waitingText, startX - outlineOffset2, waitingY + outlineOffset2);
                font.draw(batch, waitingText, startX + outlineOffset1, waitingY - outlineOffset1);
                font.draw(batch, waitingText, startX + outlineOffset2, waitingY - outlineOffset2);
                font.draw(batch, waitingText, startX - outlineOffset1, waitingY - outlineOffset1);
                font.draw(batch, waitingText, startX - outlineOffset2, waitingY - outlineOffset2);

                //draw actual text
                font.setColor(Color.WHITE);
                font.draw(batch, waitingText, startX, waitingY);
            }

            //reset the font scale back to its original size after drawing the Players' text
            font.getData().setScale(1f, 1f);
        }
        //waiting for other Players to join Game
        else if (state == State.Paused) {
            //set text and get size to correctly draw the text in the center of the screen
            String waitText = "Waiting for players";
            GlyphLayout waitLayout = new GlyphLayout();
            waitLayout.setText(font,waitText);
            float waitX = hudViewport.getWorldWidth()/2f - waitLayout.width/2f;
            float waitY = hudViewport.getWorldHeight()/2f + waitLayout.height/2f;

            //set outline color
            font.setColor(Color.GOLDENROD);
            //draw the text multiple times to create an outline effect (up, down, left, right)
            font.draw(batch, waitText, waitX + bigOutlineOffset1, waitY + bigOutlineOffset1);
            font.draw(batch, waitText, waitX + bigOutlineOffset2, waitY + bigOutlineOffset2);
            font.draw(batch, waitText, waitX - bigOutlineOffset1, waitY + bigOutlineOffset1);
            font.draw(batch, waitText, waitX - bigOutlineOffset2, waitY + bigOutlineOffset2);
            font.draw(batch, waitText, waitX + bigOutlineOffset1, waitY - bigOutlineOffset1);
            font.draw(batch, waitText, waitX + bigOutlineOffset2, waitY - bigOutlineOffset2);
            font.draw(batch, waitText, waitX - bigOutlineOffset1, waitY - bigOutlineOffset1);
            font.draw(batch, waitText, waitX - bigOutlineOffset2, waitY - bigOutlineOffset2);

            //set text color
            font.setColor(Color.WHITE);
            font.draw(batch, waitText, waitX,waitY);
        }
        //when Game has ended, draw result and current Player's score
        if(state == State.Over){
            checkGamestate();
            //set text and get size to correctly draw the text in the center of the screen
            String wonText = "Winner is: ";
            //crate new Player instance for winning Player
            Player winningPlayer = null;
            //get winning Player and their name from array
            if(winner!=Winner.None) {
                switch (winner){
                    case Player1:
                        winningPlayer = playersData.get(0);
                        break;
                    case Player2:
                        winningPlayer = playersData.get(1);
                        break;
                    case Player3:
                        winningPlayer = playersData.get(2);
                        break;
                    case Player4:
                        winningPlayer = playersData.get(3);
                        break;
                }
                wonText = wonText+winningPlayer.getName();
            }
            else
                wonText = "No winner.";

            //create new layout to draw winner text
            GlyphLayout wonLayout = new GlyphLayout();
            wonLayout.setText(font,wonText);
            float wonX = hudViewport.getWorldWidth()/2f - wonLayout.width/2f;
            float wonY = hudViewport.getWorldHeight()/2f + wonLayout.height/2f;

            //if there is a winner and the winner is current Player, set winning text to green outline, else blue
            if(winningPlayer!=null) {
                if (Objects.equals(winningPlayer.getName(), getPlayerById(localPlayerId).getName()))
                    font.setColor(Color.FOREST);
                else
                    font.setColor(Color.BLUE);
            }
            else
                font.setColor(Color.BLUE);

            font.draw(batch, wonText, wonX + bigOutlineOffset1, wonY + bigOutlineOffset1+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX + bigOutlineOffset2, wonY + bigOutlineOffset2+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX - bigOutlineOffset1, wonY + bigOutlineOffset1+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX - bigOutlineOffset2, wonY + bigOutlineOffset2+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX + bigOutlineOffset1, wonY - bigOutlineOffset1+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX + bigOutlineOffset2, wonY - bigOutlineOffset2+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX - bigOutlineOffset1, wonY - bigOutlineOffset1+(font.getXHeight()*2));
            font.draw(batch, wonText, wonX - bigOutlineOffset2, wonY - bigOutlineOffset2+(font.getXHeight()*2));

            font.setColor(Color.WHITE);
            font.draw(batch, wonText, wonX,wonY+(font.getXHeight()*2));

            //display score of YOUR Player under winning Player
            int playerScore = getPlayerById(localPlayerId).getScore();
            String scoreText = "Your score: "+playerScore;
            wonLayout.setText(font,scoreText);

            font.setColor(Color.BLUE);
            font.draw(batch, scoreText, wonX + bigOutlineOffset1, wonY + bigOutlineOffset1);
            font.draw(batch, scoreText, wonX + bigOutlineOffset2, wonY + bigOutlineOffset2);
            font.draw(batch, scoreText, wonX - bigOutlineOffset1, wonY + bigOutlineOffset1);
            font.draw(batch, scoreText, wonX - bigOutlineOffset2, wonY + bigOutlineOffset2);
            font.draw(batch, scoreText, wonX + bigOutlineOffset1, wonY - bigOutlineOffset1);
            font.draw(batch, scoreText, wonX + bigOutlineOffset2, wonY - bigOutlineOffset2);
            font.draw(batch, scoreText, wonX - bigOutlineOffset1, wonY - bigOutlineOffset1);
            font.draw(batch, scoreText, wonX - bigOutlineOffset2, wonY - bigOutlineOffset2);

            font.setColor(Color.WHITE);
            font.draw(batch,scoreText,wonX,wonY);
        }
    }

    /** Check State of game after each move */
    private void checkGamestate() {
        //if draw Deck is empty, end game and no winner
        if (deckDraw.isEmpty()) {
            state = State.Over;
            winner = Winner.None;
        }
        //else go through each Player and check Hand size
        for (int i = 0; i < playersData.size(); ++i) {
            Player player = playersData.get(i);
            if (player != null) {
                //if Player's Hand is empty, declare winner and end game
                if (player.getHand().getCards().isEmpty()) {
                    switch (i) {
                        case 0:
                            winner = Winner.Player1;
                            state = State.Over;
                            break;
                        case 1:
                            winner = Winner.Player2;
                            state = State.Over;
                            break;
                        case 2:
                            winner = Winner.Player3;
                            state = State.Over;
                            break;
                        case 3:
                            winner = Winner.Player4;
                            state = State.Over;
                            break;
                    }
                }
            }
        }
        //if game is over, calculate players' points
        if (state == State.Over) {
            calcPoints();
        }
    }

    /** Calculate Players' points */
    void calcPoints() {
        //iterate through Players in array
        for (Player currentPlayer : playersData) {
            int sumPoints = 0;
            //go through each other Player in array
            if (currentPlayer != null) {
                for (Player otherPlayer : playersData) {
                    if (otherPlayer != null) {
                        //if other Player is not same as one currently calculating points for
                        if (!Objects.equals(otherPlayer.getName(), currentPlayer.getName())) {
                            //get sum of values of Cards from other Player's Hand and add to current Player's sum
                            sumPoints += otherPlayer.getHand().getSumCardPoints();
                        }
                    }
                }
                //set current Player's points
                currentPlayer.setScore(sumPoints);
            }
        }
    }

    /** Handles user input during gameplay */
    private void handleInput() {
        //get x and y coordinates of clicking on screen
        float touchX = Gdx.input.getX();
        float touchY = Gdx.input.getY();

        //convert screen coordinates into world coordinates (WORLD UNITS)
        Vector2 worldCoords = viewport.unproject(new Vector2(touchX, touchY));

        //allow input if game State is running
        if (state == State.Running) {
            //get current Player and Hand through playerTurn
            Player currentPlayer = getPlayerById(playerTurn);
            Hand currentHand = currentPlayer.getHand();

            //handle clicking on left/right Hand arrows (only if arrows are rendered)
            if (isClickedOnArrowButtonLeft(worldCoords.x, worldCoords.y, currentHand)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && showLeftArrow) {
                    handArrowLeftClicked(currentHand);
                }
            } else if (isClickedOnArrowButtonRight(worldCoords.x, worldCoords.y, currentHand)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && showRightArrow) {
                    handArrowRightClicked(currentHand);
                }
            }

            //check if current player hasn't performed action in current turn yet
            if (!playerPerformedAction) {
                //Gdx.app.log("Current player", currentPlayer.getName());
                //handle click on draw Deck
                if (isClickedOnDeck(worldCoords.x, worldCoords.y, deckDraw)) {
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        if (sfxPickup != null) {
                            sfxPickup.play(manager.getSoundVolumePref());
                        }
                        currentPlayer.getHand().pickCard(deckDraw); //add Card from draw Deck to Player's Hand
                        playerTurn = getNextTurn(playerTurn);   //iterate turn
                        playerPerformedAction = true;   //counts as action performed, so end Player's turn
                        handArrowRightClicked(currentPlayer.getHand()); //move hand index one to the right (because card was added)
                        updateGameData();   //call method to update server data
                    }
                }

                //handle click on Cards inside Player's Hand
                for (Card card : currentPlayer.getHand().getCards()) {
                    if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                        //set highlight on Card that's hovered over (used in drawHand method)
                        card.setHighlight(true);
                        //clicked on Card within Hand
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                            if (sfxCollect != null) {
                                sfxCollect.play(manager.getSoundVolumePref());
                            }
                            //call game logic on selected Card
                            gameControl(card, currentPlayer.getHand());
                            //topCard = deckDiscard.getTopCard();
                            break;
                        }
                    } else {
                        card.setHighlight(false);   //set other (un-hovered) Cards to false
                    }
                }

                playerPerformedAction = false;
            }
        }
        //game State is in Choosing, check click logic on Choosing Cards
        else if (state == State.Choosing) {
            //get card that player hovered over
            Card card = isClickedOnChoosingCards(worldCoords.x, worldCoords.y);
            //clicked on card and position (Card) is valid
            if (card != null) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (sfxCollect != null) {
                        sfxCollect.play(manager.getSoundVolumePref());
                    }
                    topCard = card; //change topCard
                    state = State.Running;  //reset state
                    choosingCards.clear();  //clear Choosing Cards array
                    playerTurn = getNextTurn(playerTurn);   //iterate to next turn
                    updateGameData();   //update server data
                }
            }
        }
    }

    /** Performs main game Card logic */
    private void gameControl(Card card, Hand hand) {
        //is selected Card same color or symbol as topCard
        if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
            if (card.isSpecial()) {
                //if selected card is Special, perform special actions
                specialCardAction(card, hand);
            }
            //player performed turn (State check exists so turn doesn't increment when Player is choosing cards)
            if (state == State.Running) {
                //set selected card into discard Deck
                hand.setCard(card, deckDiscard);
                //move hand index left (removed card)
                handArrowLeftClicked(hand);
                topCard = card;
                playerTurn = getNextTurn(playerTurn);
                playerPerformedAction = true;
                updateGameData();   //update server data
            }
        }
    }

    /** Performs logic of Special Cards */
    private void specialCardAction(Card card, Hand hand) {
        String special = card.getSpecial(); //get which special Card was played
        int maxCardsShow = getMaxCardsShow();
        Player nextPlayer = null;   //initialize next Player to perform +2/4 Special action on
        switch (special) {
            //Stop
            case "S":
                //iterate turn (skips next player's turn because iteration is also performed in handleInput)
                playerTurn = getNextTurn(playerTurn);
                break;
            //Reverse
            case "R":
                //change turn order and iterate turn (same player if only 2 players)
                clockwiseOrder = !clockwiseOrder;
                playerTurn = getNextTurn(playerTurn);
                break;
            //Plus 2
            case "P2":
                //get next Player object depending on their id
                nextPlayer = getPlayerById(getNextTurn(playerTurn));
                //check if Player and their Hand is valid
                if(nextPlayer!=null && nextPlayer.getHand()!=null) {
                    //draw Cards and increment lastIndex
                    nextPlayer.getHand().pickCards(deckDraw, 2);
                    nextPlayer.getHand().lastIndexIncrement(2, maxCardsShow);
                }
                break;
            //Plus 4
            case "P4":
                //get next Player object depending on their id
                nextPlayer = getPlayerById(getNextTurn(playerTurn));
                //check if Player and their Hand is valid
                if(nextPlayer!=null && nextPlayer.getHand()!=null) {
                    //draw Cards and increment lastIndex
                    nextPlayer.getHand().pickCards(deckDraw, 4);
                    nextPlayer.getHand().lastIndexIncrement(4, maxCardsShow);
                }
                break;
            //Rainbow
            default:
                //Gdx.app.log("PLAYED CHANGE COLOR", "player");
                hand.setCard(card, deckDiscard); //preemptively add Card into discard Deck because condition changes
                state = State.Choosing;
        }
    }

    /** Get id of Player (if exists) whose in the next position after the Player with id of playerId */
    private int getNextTurn(int playerId) {
        int size = playersData.size();  //get length of Players

        //find the current index of the player with the given ID
        int currentIndex = -1;
        for (int i = 0; i < size; i++) {
            Player p = playersData.get(i);
            if (p != null && p.getId() == playerId) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            throw new IllegalArgumentException("Player ID not found in playersData.");
        }

        //search for the next/previous player (depending on order), skipping nulls
        int nextIndex = currentIndex;
        do {
            nextIndex = !clockwiseOrder
                    ? (nextIndex + 1) % size
                    : (nextIndex - 1 + size) % size;
        } while (playersData.get(nextIndex) == null);

        //return next/previous Player after current Player
        return playersData.get(nextIndex).getId();
    }

    /** Find Player with id within Players array */
    private Player getPlayerById(int id){
        for(Player player : playersData){
            if(player!=null) {
                if (player.getId() == id)
                    return player;
            }
        }
        return null;
    }

    /** Get the maximum amount of Cards to render in Player's Hand during runtime */
    private int getMaxCardsShow(){
        int maxCardsShow = 7;
        if(getPlayersSize()==2){
            maxCardsShow = GameConfig.MAX_CARDS_SHOW;
        }
        //small size if 3 or 4 players
        else {
            maxCardsShow = GameConfig.MAX_CARDS_SHOW_SM;
        }
        return maxCardsShow;
    }

    /** Check if user mouse is within Deck bounds */
    private boolean isClickedOnDeck(float mouseX, float mouseY, Deck deck) {
        Vector2 position = deck.getPosition();
        Rectangle bounds = deck.getBounds();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    /** Check if user mouse is within Card bounds */
    private boolean isClickedOnCard(float mouseX, float mouseY, Card card) {
        Vector2 position = card.getPosition();
        Rectangle bounds = card.getBounds();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    /** Check if user mouse is within bounds of left arrow of Hand */
    private boolean isClickedOnArrowButtonLeft(float mouseX, float mouseY, Hand hand) {
        Vector2 position = hand.getPositionArrowRegionLeft();
        Rectangle bounds = hand.getBoundsArrowRegionLeft();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    /** Method when left arrow on Hand is clicked */
    private void handArrowLeftClicked(Hand currentHand) {
        int maxCardsShow = getMaxCardsShow();
        //decrement indexes of Hand
        currentHand.indexDecrement(maxCardsShow);
        int indexFirst = currentHand.getIndexFirst();
        int indexLast = currentHand.getIndexLast();
        Gdx.app.log("ARROW CLICK LEFT", "Index first: " + indexFirst + " | Index last: " + indexLast);
    }

    /** Check if user mouse is within bounds of right arrow of Hand */
    private boolean isClickedOnArrowButtonRight(float mouseX, float mouseY, Hand hand) {
        Vector2 position = hand.getPositionArrowRegionRight();
        Rectangle bounds = hand.getBoundsArrowRegionRight();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    /** Method when right arrow on Hand is clicked */
    private void handArrowRightClicked(Hand currentHand) {
        int maxCardsShow = getMaxCardsShow();
        //increment indexes of Hand
        currentHand.indexIncrement(maxCardsShow);
        int indexFirst = currentHand.getIndexFirst();
        int indexLast = currentHand.getIndexLast();
        Gdx.app.log("ARROW CLICK RIGHT", "Index first: " + indexFirst + " | Index last: " + indexLast);
    }

    /** Check if user mouse is within bounds of Card in choosingCards */
    private Card isClickedOnChoosingCards(float mouseX, float mouseY) {
        for (Card card : choosingCards) {
            Vector2 position = card.getPosition();
            Rectangle bounds = card.getBounds();
            if (mouseX >= position.x && mouseX <= position.x + bounds.width
                    && mouseY >= position.y && mouseY <= position.y + bounds.height)
                return card;
        }
        return null;
    }

    /** Fills choosingCards array with default colored Cards and renders them */
    private void drawColorWheel() {
        //if array is empty, create and fill
        if (choosingCards.isEmpty()) {
            //create 4 Cards in following color order: B,R,G,Y
            float sizeX = GameConfig.CARD_WIDTH;
            float sizeY = GameConfig.CARD_HEIGHT;

            //set starting positions
            float startX = (viewport.getWorldWidth() - 4 * sizeX) / 2f;
            float centerY = (viewport.getWorldHeight() - sizeY) / 2f;

            Card cardB = new Card();
            float BX = startX;
            float BY = centerY;
            cardB.setPositionAndBounds(BX, BY, sizeX, sizeY);
            cardB.setColor(RegionNames.Bdefault);
            choosingCards.add(cardB);

            Card cardR = new Card();
            //offset position of next card to the right of previous one
            float RX = startX + sizeX;
            float RY = centerY;
            cardR.setPositionAndBounds(RX, RY, sizeX, sizeY);
            cardR.setColor(RegionNames.Rdefault);
            choosingCards.add(cardR);

            Card cardG = new Card();
            float GX = startX + sizeX * 2;
            float GY = centerY;
            cardG.setPositionAndBounds(GX, GY, sizeX, sizeY);
            cardG.setColor(RegionNames.Gdefault);
            choosingCards.add(cardG);

            Card cardY = new Card();
            float YX = startX + sizeX * 3;
            float YY = centerY;
            cardY.setPositionAndBounds(YX, YY, sizeX, sizeY);
            cardY.setColor(RegionNames.Ydefault);
            choosingCards.add(cardY);
        }

        //for each Card in choosingCards array, render Card with texture and positions
        for (Card card : choosingCards) {
            String texture = card.getTexture();
            TextureRegion region = gameplayAtlas.findRegion(texture);
            Card.render(batch, region, card);
        }
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        socketManager.disconnect();
        stage.dispose();
        batch.dispose();
    }

    /** Public method to remove Player from Game and update Game turn if it is current (leaving) Player's turn */
    public void playerLeaveGame(int playerId, int gameId){
        //get Player from playersData that is leaving
        Player currentPlayer = getPlayerById(playerId);
        //if Game is over, don't add Cards back into draw Deck
        if (state != State.Over) {
            //put Player's Cards back into the draw Deck if game isn't over
            deckDraw.setCards(currentPlayer.getHand().getCards());
        }

        //Remove Player AND update turn of Game
        if(!isWaiting && playerTurn == localPlayerId) {
            //get id (turn) of next Player after the current
            playerTurn = getNextTurn(playerTurn);
            updateGameRemovePlayerTurn(currentPlayer, gameId, playerTurn, new GameUpdateCallback() {
                @Override
                public void onGameUpdated(GameData updatedGame) {
                    //connectionError = false;
                    Gdx.app.log("playerLeaveGame SUCCESS", "Removed player with id: " + playerId + " & updated turn to: "+ updatedGame.getCurrentTurn() +" from Game with id: " + updatedGame.getId());
                }
                @Override
                public void onFailure(Throwable t) {
                    //connectionError = false;
                    Gdx.app.log("playerLeaveGame ERROR", "Failed to update turn & remove player with id: " + playerId + " from game.");
                }
            });
        }

        //Only remove Player
        else {
            updateGameRemovePlayer(currentPlayer, gameId, new GameUpdateCallback() {
                @Override
                public void onGameUpdated(GameData updatedGame) {
                    //connectionError = false;
                    Gdx.app.log("playerLeaveGame SUCCESS", "Removed player with id: " + playerId + " from Game with id: " + updatedGame.getId());
                }
                @Override
                public void onFailure(Throwable t) {
                    //connectionError = true;
                    Gdx.app.log("playerLeaveGame ERROR", "Failed to remove player with id: " + playerId + " from game.");
                }
            });
        }
    }

    /** Render exit button(s) with scene2d */
    public Actor createExitButton(State state) {
        TextButton exitButton = new TextButton("Exit", skin);
        //when exit button is clicked, execute Player leaving method and set current screen to MenuScreen
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");
                playerLeaveGame(localPlayerId,currentGameId);
                game.setScreen(new MenuScreen(game));
            }
        });

        //create new table and add exit button
        Table buttonTable = new Table();
        buttonTable.defaults();
        buttonTable.add(exitButton);

        //determine position of button depending on state
        if (state == State.Over) {
            buttonTable.center().padTop(100); //center the button during game over
        }
        else {
            buttonTable.top().right().pad(2); //position button in top-right for HUD
        }

        buttonTable.setFillParent(true);

        return buttonTable;
    }
}
