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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameMultiplayerScreen extends ScreenAdapter {
    private ScheduledExecutorService scheduler = null;

    //status igre
    public enum State {
        Initializing, //game still setting up with backend requests
        Paused, //game initialized correctly, but still needs more players
        Running, //game is running normally
        Over, //game is over
        Choosing //game is paused except for player who is choosing card
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
    private OrthographicCamera hudCamera;
    private OrthographicCamera backgroundCamera;
    private Viewport viewport;
    private Viewport hudViewport;
    private Viewport backgroundViewport; //viewport for background
    private Sprite background;

    private Stage stage;
    private Stage stageHud;
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
    private int playerTurn = 0;
    //preglej ce trenutni player naredil akcijo
    private boolean playerPerformedAction = false;
    //max players
    private int maxPlayers = 2;
    //max st kart v decku
    private int deckSize = 104;
    //vrstni red
    private boolean clockwiseOrder = true;

    //playerji
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;
    private List<Player> playersData;

    private int localPlayerId = 0;
    private int currentGameId = 0;
    private boolean isWaiting = false;
    private Player playerWaiting = null;

    //player hand arrow button display
    private boolean showLeftArrow;
    private boolean showRightArrow;

    private Array<Card> choosingCards = new Array<Card>();

    /**
     * Public ID functions
     */
    //get playerId of current player for dispose function
    public int getPlayerId() {
        return localPlayerId;
    }

    //get gameId of current game for dispose function
    public int getGameId() {
        return currentGameId;
    }

    /**
     * Callback functions: get backend response before continuing executing code
     */
    //get one Player
    public interface PlayerFetchCallback {
        void onPlayerFetched(Player player);
    }

    //create one Game
    public interface GameCreateCallback {
        void onGameFetched(GameData game); //get newly created game (important to get and save ids)
    }

    //get one Game
    public interface GameFetchCallback {
        void onGameFetched(GameData game);
    }

    //update one Game
    public interface GameUpdateCallback {
        void onGameFetched(GameData game);
        void onFailure(Throwable t);
    }

    //get multiple Players
    public interface PlayersFetchCallback {
        void onPlayersFetched(Player[] players);
    }

    //get turn of Game
    public interface TurnFetchCallback {
        void onTurnFetched(GameData fetchedGame);
    }

    /**
     * Scheduler methods
     */
    private void startScheduler() {
        // If scheduler is already running, return
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        // Create a new scheduler
        scheduler = Executors.newScheduledThreadPool(1);

        //run players checker and turn checker
        playerChecker();
        turnChecker();
    }

    public void stopScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Optionally wait for the scheduler to shut down completely
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //Player checker scheduler: check for new joined players
    private void playerChecker() {
        if (scheduler == null || scheduler.isShutdown()) {
            startScheduler();
        }
        scheduler.scheduleAtFixedRate(() -> {
            if (waitForPlayers()) {
                checkForNewPlayers(fetchedPlayers -> {
                    int localPlayersSize = getPlayersSize();
                    if (fetchedPlayers != null && scheduler!=null) {
                        if (fetchedPlayers.length != localPlayersSize) {
                            Gdx.app.log("PLAYERCHECKER", "FOUND NEW PLAYER");
                            checkPlayersChanged(fetchedPlayers);
                        } else
                            Gdx.app.log("PLAYERCHECKER", "NO NEW PLAYERS");
                    } else
                        Gdx.app.log("ERROR", "Checking for players: Fetched players are null");
                });
            }
        }, 0, 7, TimeUnit.SECONDS); // Check every 5-7 seconds (completes within 7s)
    }

    //Turn checker scheduler: check DB for turn change if not currently playing
    private void turnChecker() {
        if (scheduler == null || scheduler.isShutdown()) {
            startScheduler();
        }
        scheduler.scheduleAtFixedRate(() -> {
            if (waitForTurn()) {
                checkForTurnChange(fetchedTurnAndDeck -> {
                    if(fetchedTurnAndDeck!=null && scheduler!=null) {  //TODO: isto za vse ko returnas null on BE failure //mogoce check if players>=2
                        int fetchedTurn = fetchedTurnAndDeck.getCurrentTurn();
                        Card fetchedTopCard = fetchedTurnAndDeck.getTopCard();
                        String fetchedGameState = fetchedTurnAndDeck.getGameState();
                        if (Objects.equals(fetchedGameState, "Over"))
                            state = State.Over;
                        else if (Objects.equals(fetchedGameState, "Paused")) {
                            state = State.Paused;
                        }
                        Gdx.app.log("TURN FETCH", "Player: " + localPlayerId + " is waiting for turn...");
                        playerWaiting = getPlayerById(fetchedTurn);
                        if (playerWaiting != null)
                            Gdx.app.log("TURN FETCH", "Waiting for player: " + playerWaiting.getName());
                        else
                            Gdx.app.log("TURN FETCH", "Player waiting is null: " + playerWaiting);

                        Gdx.app.log("TURN FETCH", "State: " + fetchedGameState);
                        //fetched turn is valid
                        if (fetchedTurn != 0 && fetchedTopCard != null) {
                            topCard = fetchedTopCard;
                            //fetched turn is same as index of player
                            if (fetchedTurn == localPlayerId || state == State.Over) {
                                //get full data of database
                                fetchGameFromBackend(currentGameId, fetchedGame -> {
                                    if (fetchedGame != null) {
                                        isWaiting = false;
                                        Gdx.app.log("GAME", "Game fetched: " + fetchedGame.getId());
                                        setGameData(fetchedGame);
                                        //TODO: check null on each BE return, if returned null, display Server Connection error text
                                    } else {
                                        Gdx.app.log("ERROR", "Failed to update game with player.");
                                    }
                                });
                            }
                        } else
                            Gdx.app.log("ERROR", "Fetched turn or top card are null");
                    }
                });
            }
            isWaiting = false;
            Gdx.app.log("TURN FETCH", "Player: " + localPlayerId + " is not waiting.");
        }, 0, 5, TimeUnit.SECONDS); // Check every 3-5 seconds (completes within 5s)
    }

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

    /**
     * Class constructors
     */
    //CREATE GAME
    public GameMultiplayerScreen(Uno game, Array<String> args) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //Paused dokler ni 2 playerju
        state = State.Initializing;
        winner = Winner.None;

        setMusicAndSounds();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();
        initGame(args);
    }

    //JOIN GAME
    public GameMultiplayerScreen(Uno game, int gameId, String playerName) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //Paused dokler ni 2 playerju
        state = State.Initializing;
        winner = Winner.None;

        setMusicAndSounds();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();
        playersData = new ArrayList<>();

        //Player joined successfully, join game in DB
        // Step 1: Fetch the game from the backend
        fetchGameFromBackend(gameId, fetchedGame -> {
            if (fetchedGame != null) {
                Gdx.app.log("GAME", "Game fetched: " + fetchedGame.getId());

                //create game variables, set decks & managers
                setGameData(fetchedGame);
                //state paused, da ne bo runnal draw in handle funkcij predenj ne pridobi vse podatke iz db
                state = State.Paused;

                // Step 2: Create the player and update the game
                createPlayerFromBackend(playerName, player -> {
                    state = State.Paused;
                    if (player != null) {
                        // Step 3: Add the player to the fetched game's player list and update the game in the backend
                        updateGameWithPlayer(player, fetchedGame.getId(), new GameUpdateCallback() {
                            @Override
                            public void onGameFetched(GameData updatedGame) {
                                Gdx.app.log("GAME", "Game updated with player: " + player.getId());
                                localPlayerId = player.getId();
                                setGameData(updatedGame);
                                //state = State.Paused;
                                //when fetching updated game, check if any players have to be added
                                if (!checkPlayersChanged(updatedGame.getPlayers()))
                                    startScheduler();
                                //update game State before fetching from DB
                                //TODO: check current turn (if its you) and THEN run wait for turn checker
                                waitForPlayers();
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                Gdx.app.log("ERROR", "Failed to update game with player.");
                            }
                        });
                    } else
                        Gdx.app.log("ERROR", "Created player from backend is null");
                });

            } else {
                Gdx.app.log("ERROR", "Failed to fetch game from backend.");
            }
        });
    }

    /**
     * Set current GameData variables from fetchedGame
     */
    private void setGameData(GameData fetchedGame) {
        currentGameId = fetchedGame.getId();

        //USTVARI DECKE in settaj game manager
        //ustvari main deck
        deckDraw = fetchedGame.getDecks()[0];
        //ustvari discard deck
        deckDiscard = fetchedGame.getDecks()[1];

        //ustvari current top card
        topCard = fetchedGame.getTopCard();
        //topCard = deckDiscard.getTopCard();

        maxPlayers = fetchedGame.getMaxPlayers();

        //USTVARI PLAYERJE IN NAPOLNI ARRAY
        if (playersData.isEmpty()) {
            for (int i = 0; i < fetchedGame.getMaxPlayers(); ++i) {
                playersData.add(null);
            }
        }
        //if no players changed (between local & DB) copy player data from DB
        if (!checkPlayersChanged(fetchedGame.getPlayers())) {
            //get players from DB
            Player[] fetchedPlayers = fetchedGame.getPlayers();
            for (int i=0;i<fetchedPlayers.length;++i) {
                //if newly fetched player is already in array don't re-add (important on gameCreate)
                if(!isPlayerAlreadyInArray(fetchedPlayers[i]))
                    addPlayerToArray(fetchedPlayers[i]);
                //get current Hand of player in updated DB and save locally (updates card ids!)
                else
                    playersData.get(i).setHand(fetchedPlayers[i].getHand());
            }
        }
        //else, players already changed in checkPlayersChanged function

        state = State.valueOf(fetchedGame.getGameState());
        playerTurn = fetchedGame.getCurrentTurn();
        clockwiseOrder = Objects.equals(fetchedGame.getTurnOrder(), "Clockwise");
    }

    /**
     * Get current GameData variables and update Game on backend
     */
    private void updateGameData() {
        //check if any hands or draw deck is empty and get State (check if state isnt over to prevent inf loop)
        if(state!=State.Over)
            checkGamestate();
        // Create and save game data
        GameData gameData = new GameData(
                playersData, deckDraw, deckDiscard, topCard, maxPlayers,
                state.toString(), playerTurn, getOrderAsString());
        gameData.setId(currentGameId);

        updateGame(gameData, new GameUpdateCallback() {
            @Override
            public void onGameFetched(GameData game) {
                Gdx.app.log("SUCCESS", "Updated game data: " + game.getId());
                /*int localPlayersSize = getPlayersSize(); //fetched new players?
                if (game.getPlayers().length != localPlayersSize) {
                    checkPlayersChanged(game.getPlayers());
                }*/
                setGameData(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("ERROR", "Failed to update game: " + t.getMessage());
            }
        });
    }

    //pripravi igro (init globals)
    private void initGame(Array<String> args) {
        //0-numPlayers, 1-deckSize, 2-presetBox, 3-orderBox
        maxPlayers = Integer.parseInt(args.get(0));
        deckSize = Integer.parseInt(args.get(1));
        String preset = args.get(2);
        clockwiseOrder = Objects.equals(args.get(3), "Clockwise");

        playersData = new ArrayList<>();
        //USTVARI DECKE
        //ustvari main deck
        deckDraw = new Deck(deckSize);
        deckDraw.generateBySize(deckSize, preset);
        deckDraw.shuffleDeck();

        //vzemi eno karto iz deka
        topCard = deckDraw.pickCard();

        //ustvari discard dek in polozi to karto nanj
        deckDiscard = new Deck(deckSize);
        deckDiscard.setCard(topCard);

        //USTVARI PLAYERJE IN NAPOLNI ARRAY
        for (int i = 0; i < maxPlayers; ++i) {
            playersData.add(null);
        }
        //PRIPRAVI FIRST PLAYER (HOST)
        createPlayerFromBackend(manager.getNamePref(), player -> {
            if (player != null) {
                //set playerTurn as id of player that created the game (first player)
                int currentPlayerTurn = player.getId();
                if (currentPlayerTurn != 0)
                    playerTurn = currentPlayerTurn;

                // Create and save game data
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
                        //run scheduler that checks for new players
                        startScheduler();
                    } else
                        Gdx.app.log("ERROR", "Created game is null");
                });
            } else
                Gdx.app.log("ERROR", "Created player from backend is null");
        });
    }

    private void fetchGameFromBackend(int gameId, GameFetchCallback callback) {
        service.fetchGame(gameId, new GameService.FetchGameCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("SUCCESS", "Player added to backend");
                //get array of players from current Game from DB
                callback.onGameFetched(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("GAME", "FAILED: " + t);
                callback.onGameFetched(null);
            }
        });
    }

    /** Update Game and add a new player */
    private void updateGameWithPlayer(Player player, int gameId, GameUpdateCallback callback) {
        service.updateGameWithPlayer(new GameService.GameUpdatePlayersCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("SUCCESS", "Player added to backend");
                //get array of players from current Game from DB
                callback.onGameFetched(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("GAME", "FAILED: " + t);
                callback.onFailure(t);
            }
        }, gameId, player);
    }

    /** Update Game and remove a player from it */
    private void updateGameRemovePlayer(Player player, int gameId, GameUpdateCallback callback) {
        service.updateGameRemovePlayer(new GameService.GameUpdatePlayerRemoveCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("SUCCESS", "Player removed from backend");
                callback.onGameFetched(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("GAME", "FAILED: " + t);
                callback.onFailure(t);
            }
        }, gameId, player);
    }

    /** Update Game Turn and remove a player (if current player leaves) */
    private void updateGameRemovePlayerTurn(Player player, int gameId, int turn, GameUpdateCallback callback) {
        service.updateGameRemovePlayerTurn(new GameService.GameUpdatePlayerRemoveCallback() {
            @Override
            public void onSuccess(GameData game) {
                Gdx.app.log("SUCCESS", "Player removed from backend & turn updated");
                callback.onGameFetched(game);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("GAME", "FAILED: " + t);
                callback.onFailure(t);
            }
        }, gameId, player, turn);
    }

    /**
     * Call create Game function in GameService and fetch response
     */
    private void createGame(GameData gameData, GameCreateCallback callback) {
        service.createGame(new GameService.GameCreateCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("SUCCESS", "Game created with ID: " + fetchedGame.getId());
                callback.onGameFetched(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("ERROR", "Failed to create game: " + t.getMessage());
            }
        }, gameData);
    }

    /**
     * Call update Game function in GameService and fetch response
     */
    private void updateGame(GameData gameData, GameUpdateCallback callback) {
        service.updateGame(new GameService.GameUpdateCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("SUCCESS", "Game updated on backend");
                callback.onGameFetched(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("ERROR", "Failed to create game: " + t.getMessage());
                callback.onFailure(t);
            }
        }, currentGameId, gameData);
    }

    private void checkForNewPlayers(PlayersFetchCallback callback) {
        service.fetchGamePlayers(new GameService.FetchGamePlayersCallback() {
            @Override
            public void onSuccess(Player[] players) {
                Gdx.app.log("PLAYERS", "Players fetched: " + players.length);
                //get array of players from current Game from DB
                callback.onPlayersFetched(players);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("PLAYERS", "FAILED: " + t);
                callback.onPlayersFetched(null);
            }
        }, currentGameId);
    }

    private void checkForTurnChange(TurnFetchCallback callback) {
        service.fetchGameTurn(new GameService.FetchGameTurnCallback() {
            @Override
            public void onSuccess(GameData fetchedGame) {
                Gdx.app.log("TURN FETCH", "Fetched turn: " + fetchedGame.getCurrentTurn());
                //get array of players from current Game from DB
                callback.onTurnFetched(fetchedGame);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("TURN FETCH", "FAILED: " + t);
                callback.onTurnFetched(null);
            }
        }, currentGameId);
    }

    //Get local playersData size (without null elements)
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

    //Get index in playersData of current player (used for circling through playersData array)
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
            Gdx.app.log("ERROR", "Player with id " + localPlayerId + " not found in playersData");
        return index;
    }

    //get the maximum amount of cards to render in player's hand during game
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

    private boolean isPlayerAlreadyInArray(Player player){
        for(Player arrayPlayer : playersData){
            if(arrayPlayer!=null && player!=null) {
                if (arrayPlayer.getId() == player.getId())
                    return true;
            }
        }
        return false;
    }

    //any players changed?
    private boolean checkPlayersChanged(Player[] fetchedPlayers) {
        boolean changed = false;
        //playerData is uninitialized (no non-null elements)
        if (getPlayersSize() == 0)
            return false;

        // Step 1: Create a set of IDs from fetchedPlayers for quick lookup
        Set<Integer> fetchedPlayerIds = new HashSet<>();
        for (Player fetchedPlayer : fetchedPlayers) {
            fetchedPlayerIds.add(fetchedPlayer.getId());
        }

        // Step 2: Remove players from playersData if they are not in fetchedPlayers
        for (int i = 0; i < playersData.size(); i++) {
            Player localPlayer = playersData.get(i);
            if (localPlayer != null && !fetchedPlayerIds.contains(localPlayer.getId())) {
                playersData.set(i, null); // Remove the player by setting the slot to null
                removePlayerBasedIndex(i);
                changed = true;
            }
        }

        // Step 3: Add new players from fetchedPlayers to the first available null slot in playersData
        for (Player fetchedPlayer : fetchedPlayers) {
            boolean playerExists = false;
            for (Player localPlayer : playersData) {
                if (localPlayer != null && localPlayer.getId() == fetchedPlayer.getId()) {
                    playerExists = true;
                    break;
                }
            }

            // If player does not exist in playersData, add them to the first null slot
            if (!playerExists) {
                fetchedPlayer = createPlayerAndDraw(fetchedPlayer);
                addPlayerToArray(fetchedPlayer);
                changed = true;
            }
        }
        return changed;
    }

    //create player when starting game (host)
    private void createPlayerFromBackend(String playerName, PlayerFetchCallback callback) {
        service.fetchPlayerByName(new GameService.PlayerFetchCallback() {
            @Override
            public void onSuccess(Player player) {
                // Handle the successful response
                Gdx.app.log("PLAYER", "Player fetched: " + player.getName());
                //GET PLAYER IN ADD V CURRENT GAME
                Player thisPlayer = createPlayerAndDraw(player);
                localPlayerId = player.getId();
                if(!isPlayerAlreadyInArray(thisPlayer))
                    addPlayerToArray(thisPlayer);

                // Invoke the callback with the fetched player
                callback.onPlayerFetched(thisPlayer);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("ERROR", "Failed to fetch player: " + t.getMessage());
                //CREATE NEW PLAYER AND ADD TO DB
                Player player = new Player(playerName, 0, new Hand());
                player.getHand().pickCards(deckDraw, 5);
                Gdx.app.log("PLAYER", "CREATING NEW PLAYER INSTEAD: " + player.toString());

                service.createPlayer(new GameService.PlayerCreateCallback() {
                    @Override
                    public void onSuccess(int playerId) {
                        Gdx.app.log("SUCCESS", "Player created with ID: " + playerId);

                        player.setId(playerId);
                        localPlayerId = playerId;
                        if(!isPlayerAlreadyInArray(player))
                            addPlayerToArray(player);
                        callback.onPlayerFetched(player);
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        Gdx.app.log("ERROR", "Failed to create player: " + t.getMessage());
                    }
                }, player);

                // Invoke the callback with the newly created player
                callback.onPlayerFetched(player);
            }
        }, playerName);
    }

    //fetch player form DB and create new local player object and draw from deck
    private Player createPlayerAndDraw(Player player) {
        //note, do not draw from deck again if player has already drawed
        if(player.getHand() == null) {
            Player thisPlayer = new Player(player.getId(), player.getName(), 0, new Hand());
            thisPlayer.getHand().pickCards(deckDraw, 5);
            return thisPlayer;
        }
        else if(player.getHand().getCards().isEmpty()) {
            Player thisPlayer = new Player(player.getId(), player.getName(), 0, new Hand());
            thisPlayer.getHand().pickCards(deckDraw, 5);
        }
        else {
            //player already has Hand data, but those cards must be removed from local Deck to prevent duplication
            deckDraw.removeCards(player.getHand().getCards());
        }
        return player;
    }

    //set null Player object based on index in playersData
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

    private void addPlayerToArray(Player player) {
        Gdx.app.log("PLAYER ADD", "ADDED PLAYER: " + player.getName());
        //first player
        if (playersData.get(0) == null) {
            playersData.set(0, player);
            player1 = player;
        } else if (playersData.get(1) == null) {
            playersData.set(1, player);
            player2 = player;
        } else if (maxPlayers == 3) {
            if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) == null) {
                playersData.set(2, player);
                player3 = player;
            }
        } else if (maxPlayers == 4) {
            if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) == null) {
                playersData.set(2, player);
                player3 = player;
            } else if (playersData.get(0) != null && playersData.get(1) != null &&
                    playersData.get(2) != null && playersData.get(3) == null) {
                playersData.set(3, player);
                player4 = player;
            }
        }
    }

    /*
    private int getNextTurn(int index) {
        do {
            if (clockwiseOrder) {
                if (index < maxPlayers)
                    index += 1;
                else
                    index = 1;
            } else {
                if (index > 1)
                    index -= 1;
                else
                    index = maxPlayers;
            }
        } while (playersData.get(index - 1) == null);
        return index;
    }
    */

    //vrni player Id naslednjega playerja, ce obstaja
    private int getNextTurn(int playerId) {
        int size = playersData.size();

        // Find the current index of the player with the given ID
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

        // Search for the next/previous player, skipping nulls
        int nextIndex = currentIndex;
        do {
            nextIndex = clockwiseOrder
                    ? (nextIndex + 1) % size
                    : (nextIndex - 1 + size) % size;
        } while (playersData.get(nextIndex) == null);

        return playersData.get(nextIndex).getId();
    }


    private Player getPlayerById(int id){
        for(Player player : playersData){
            if(player!=null) {
                if (player.getId() == id)
                    return player;
            }
        }
        return null;
    }

    private String getOrderAsString() {
        if (clockwiseOrder)
            return "Clockwise";
        return "Counter Clockwise";
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

    //check scheduler for retrieving players
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

    //check scheduler for retrieving turn
    public boolean waitForTurn() {
        if (playerTurn != localPlayerId) {
            isWaiting = true;
            return true;
        }
        return false;
    }

    @Override
    public void render(float delta) {
        //doloci barve ozadja
        float r = 200 / 255f;
        float g = 30 / 255f;
        float b = 100 / 255f;
        float a = 0.7f; //prosojnost
        ScreenUtils.clear(r, g, b, a);

        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

        //stage here so can exit during initialization or pause state
        if (state != State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stageHud.act(delta);
            stageHud.draw();
            Gdx.input.setInputProcessor(stageHud);
        }

        //Game not yet finished creating in DB
        if (state == State.Initializing)
            return;

        else if (state == State.Paused) {
            //Gdx.app.log("PAUSED", "Still waiting for players");
            startScheduler();
        }
        else if (state != State.Over) {
            checkGamestate();

            //handle input only if it is current player's turn
            if (!waitForTurn()) {
                //current player's turn, disable DB fetching
                stopScheduler();
                handleInput();
            } else {
                //not current player's turn, fetch DB
                startScheduler();
            }
        }

        viewport.apply();
        //setProjectionMatrix - uporabi viewport za prikaz sveta (WORLD UNITS)
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        draw();
        batch.end();

        // Apply HUD viewport
        hudViewport.apply();
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.begin();
        drawHud();
        batch.end();

        if (state == State.Over) {
            stopScheduler();
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
    }

    private void draw() {
        //VELIKOST kart (v WORLD UNITS)
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
            TextureRegion topCardRegion = gameplayAtlas.findRegion(topCardTexture);
            //POZICIJA - MIDDLE
            float topX = (viewport.getWorldWidth() - sizeX) / 2f;
            float topY = (viewport.getWorldHeight() - sizeY) / 2f;
            topCard.setPositionAndBounds(topX, topY, sizeX, sizeY);
            Card.render(batch, topCardRegion, topCard);

            //DRAW DECK
            TextureRegion drawDeckRegion = gameplayAtlas.findRegion(RegionNames.back);
            //FAR RIGHT
            float drawX = (viewport.getWorldWidth() - sizeX);
            //or slightly right of draw deck but not covering left player cards
            if (numPlayers > 2)
                drawX = drawX - (topX / 2f);
            float drawY = (viewport.getWorldHeight() - sizeY) / 2f;
            deckDraw.setPositionAndBounds(drawX, drawY, sizeX, sizeY);
            Card.render(batch, drawDeckRegion, deckDraw.getPosition().x, deckDraw.getPosition().y, sizeX, sizeY);
        }
        //TODO: when Paused: also draw cards but not decks
        if (state != State.Paused && state != State.Initializing) {
            //DRAW PLAYER HANDS for each current player
            int currentPlayerIndex = getIndexOfCurrentPlayer();
            //if returned==-1 -> data iz db se se ni shranil localno
            if (currentPlayerIndex == -1)
                return;
            for (int i = 0; i < playersData.size(); ++i) {
                // Calculate the index in a circular manner
                int playerIndex = (currentPlayerIndex + i) % playersData.size();
                // Get the player at the calculated index
                Player player = playersData.get(playerIndex);

                if (player != null) {
                    boolean isCurrentPlayer = (player.getId() == localPlayerId);

                    Hand playerHand = player.getHand();
                    playerHand.setArrowRegions(gameplayAtlas.findRegion(RegionNames.arrow));
                    drawHand(playerHand, i,
                            sizeX, sizeY, isCurrentPlayer);
                }
            }

            if (state == State.Choosing) {
                drawColorWheel();
            }
        }
    }

    private void drawHand(Hand hand, int index, float sizeX, float sizeY, boolean isPlayer) {
        Array<Card> cards = hand.getCards();
        int size = cards.size;
        int maxCardsShow = getMaxCardsShow();
        //hand.setIndexLast();
        int firstIndex = hand.getIndexFirst();
        int lastIndex = hand.getIndexLast();
        //fix indexes if needed for player
        if(isPlayer)
            lastIndex = hand.getIndexLast(maxCardsShow);

        float startX = 0; //start at bottom

        //Y-axis: where to draw cards depending on current player
        //bottom
        float startY = 0;
        //startY = sizeX;
        int numPlayers = getPlayersSize();
        //0-P1, 1-P2, 2-P3, 3-P4
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
                //ce maxPlayers=>3: draw P1
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
        float spacing = 0f;
        //drawing left/right sides of screen
        if (startX == 1 || startX == 3) {
            for (int i = 5; i < size; ++i) {    //max cards before spacing
                if (i >= 7) break;              //max cards before no more spacing
                overlap += 0.15f;
            }
        }
        //drawing top/bottom sides of screen
        else {
            for (int i = maxCardsShow - 2; i < size; ++i) {
                if (i >= maxCardsShow) break;
                overlap += 0.1f;
            }
        }
        overlap = sizeX * (1 - overlap);
        spacing = sizeX;

        //0-bottom, 2-top
        if (startX == 0 || startX == 2) {
            //if (startX == 1 || startX == 3) {
            //brez spacing
            if (size <= maxCardsShow - 2) {
                hand.setIndexLast();
                lastIndex = hand.getIndexLast();
            }
            //spacing le dokler ne reachas MaxCardsToShow
            else if (size <= maxCardsShow) {
                hand.setIndexLast();
                lastIndex = hand.getIndexLast();
                spacing = overlap;
            }
            //ne vec spacingat ko imas vec kart kot MaxCardsToShow
            else {
                spacing = overlap;
            }

            //koliko prostora ostane na horizontali ki ni pokrita z karti
            //deli polovicno za risanje arrow-jev
            float sizeLeft = viewport.getWorldWidth() - ((lastIndex+1 - firstIndex) * spacing);
            startX = sizeLeft / 2f;

            //IZRISI CARDE
            Array<Integer> indexHover = new Array<Integer>();
            for (int i = firstIndex; i < size; ++i) {
                //==i<=lastIndex razen ko settamo Card (izogni index izven array)
                if (i > lastIndex)
                    break;
                Card card = cards.get(i);
                String texture;
                TextureRegion region;
                if (!card.getHighlight()) {
                    if (isPlayer || state == State.Over)
                        texture = card.getTexture();
                    else
                        texture = RegionNames.back;
                    region = gameplayAtlas.findRegion(texture);
                    float posX = (startX + (i - firstIndex) * spacing) - 1f;
                    float posY = startY;
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                } else {
                    indexHover.add(i);
                }
            }

            //IZRISI CARDE KI SO HOVERANE
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
                    float posY = startY + 2f; //slightly gor
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                }
            }

        /*
        if (isPlayer)
            Gdx.app.log("PLAYER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);
        */
            float sizeRight = ((lastIndex+1 - firstIndex) * spacing);
            float endX = sizeRight + (sizeLeft/2f);

            //ali pa risi v normalnem MAX_CARDS_SHOW ko sta samo 2 playerja
            //preveri ce so arrowi prikazani
            if (isPlayer) {
                if (firstIndex != 0 && state != State.Over)
                    showLeftArrow = true;
                else
                    showLeftArrow = false;
                if (lastIndex != cards.size - 1 && state != State.Over)
                    showRightArrow = true;
                else
                    showRightArrow = false;
            }

            //render button left
            if (size >= maxCardsShow && isPlayer && showLeftArrow) {
                float arrowX = startX - sizeX / 2 - (sizeX * 0.1f);
                //float arrowY = startY + arrowRegion.getRegionHeight() / 2;
                float arrowY = startY + (sizeY * 0.2f);
                hand.setArrowRegionLeft(arrowX, arrowY, sizeX / 2, sizeY / 2);
                hand.renderArrowLeft(batch);
            }
            //render button right
            if (size >= maxCardsShow && isPlayer && showRightArrow) {
                float arrowX = endX + (sizeX * 0.1f);
                //float arrowY = startY + arrowRegion.getRegionHeight() / 2;
                float arrowY = startY + (sizeY * 0.2f);
                //batch.draw(arrowRegion, arrowX, arrowY);
                hand.setArrowRegionRight(arrowX, arrowY, sizeX / 2, sizeY / 2);
                hand.renderArrowRight(batch);
            }
        }
        else if (startX == 1 || startX == 3) {
            //else if (startX == 0 || startX == 2) {
            //for rotating Card 90deg (far left) or -90deg (far right)
            int rotationScalar = 1;
            //if(startX==2)
            if (startX == 3)
                rotationScalar = -1;

            //brez spacing
            if (size <= 5) {
                hand.setIndexLast();
                startY = (viewport.getWorldHeight() - size * sizeX) / 2f;
            }
            //spacing le dokler ne reachas MaxCardsToShow
            else if (size <= 7) {
                hand.setIndexLast();
                spacing = overlap;
                startY = (viewport.getWorldHeight() - size * sizeX) / 2f;
            }
            //ne vec spacingat ko imas vec kart kot MaxCardsToShow
            else {
                spacing = overlap;
                startY = (viewport.getWorldHeight() - maxCardsShow * sizeX) / 2f;
            }

            //LIMIT RENDER CARDS DOL
            if (startY < (sizeY * 0.8f))
                startY = (sizeY * 0.8f);

            //set x-axis based on hand location (left/right)
            if (startX == 1)
                startX = viewport.getWorldWidth() - (GameConfig.CARD_WIDTH_SM + GameConfig.CARD_HEIGHT_SM * 0.1f); //malce levo
            else
                startX = GameConfig.CARD_HEIGHT_SM * 0.1f; //malce desno

            // Render vertically
            for (int i = firstIndex; i < size; ++i) {
                //show 7 cards max
                if (i >= 7) break;

                Card card = cards.get(i);
                String texture;
                TextureRegion region;

                texture = (isPlayer || state == State.Over) ? card.getTexture() : RegionNames.back;
                region = gameplayAtlas.findRegion(texture);

                float posX = startX; // X stays constant (left or right)
                float posY = startY + (i - firstIndex) * spacing; // Adjust along the y-axis

                card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                Card.renderFlipped(batch, region, card, rotationScalar);
            }
        }
    }

    private void drawHud(){
        if(state == State.Running || state == State.Over) {
            // Set the font to a smaller scale for the player's text
            font.getData().setScale(0.8f);  // Scale down to 80% of the original size

            float startX = 10f; //width left + margin
            float startY = hudViewport.getWorldHeight()-10f; //top + margin

            //height of each line of font + spacing
            float lineHeight = font.getXHeight();

            // Define outline offsets and outline color
            float outlineOffset1 = 1.8f;  //offset for 1st outline (outer)
            float outlineOffset2 = 1.2f;  //offset for 2nd outline (inner)
            Color outlineColor = Color.BLACK;  // Outline color

            float waitingY = 0;
            float playerY = startY;

            int numPlayers = getPlayersSize();
            int currentPlayerIndex = getIndexOfCurrentPlayer();
            if(currentPlayerIndex==-1)
                return;
            for (int i = 0; i < numPlayers; ++i) {
                //get index of current player first (localId) (current player always starts at bottom)
                int playerIndex = (currentPlayerIndex + i) % playersData.size();
                //get next player at the calculated index
                Player player = playersData.get(playerIndex);
                if (player != null) {
                    String position = "Bottom";
                    //0-P1, 1-P2, 2-P3, 3-P4
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
                    String playerText = position + ": " + player;
                    //get vertical start of each new player
                    if(i!=0)
                        playerY = playerY - (lineHeight*2);

                    // Set the outline color
                    font.setColor(outlineColor);

                    // Draw the text multiple times to create an outline effect (up, down, left, right)
                    font.draw(batch, playerText, startX + outlineOffset1, playerY + outlineOffset1);
                    font.draw(batch, playerText, startX + outlineOffset2, playerY + outlineOffset2);
                    font.draw(batch, playerText, startX - outlineOffset1, playerY + outlineOffset1);
                    font.draw(batch, playerText, startX - outlineOffset2, playerY + outlineOffset2);
                    font.draw(batch, playerText, startX + outlineOffset1, playerY - outlineOffset1);
                    font.draw(batch, playerText, startX + outlineOffset2, playerY - outlineOffset2);
                    font.draw(batch, playerText, startX - outlineOffset1, playerY - outlineOffset1);
                    font.draw(batch, playerText, startX - outlineOffset2, playerY - outlineOffset2);

                    // Draw the original text in its normal color
                    font.setColor(Color.WHITE);  // Set the font color to the main color (e.g., white)
                    //display player's data
                    font.draw(batch, playerText, startX, playerY);

                    if(isWaiting)
                        waitingY = playerY;
                }
            }

            //draw waiting for player if
            if(isWaiting && playerWaiting!=null){
                waitingY = waitingY - (lineHeight*2);
                font.getData().setScale(1f);
                outlineColor = Color.GOLDENROD;
                String waitingText = "Waiting for: " + playerWaiting.getName();

                font.setColor(outlineColor);

                //outline effect
                font.draw(batch, waitingText, startX + outlineOffset1, waitingY + outlineOffset1);
                font.draw(batch, waitingText, startX + outlineOffset2, waitingY + outlineOffset2);
                font.draw(batch, waitingText, startX - outlineOffset1, waitingY + outlineOffset1);
                font.draw(batch, waitingText, startX - outlineOffset2, waitingY + outlineOffset2);
                font.draw(batch, waitingText, startX + outlineOffset1, waitingY - outlineOffset1);
                font.draw(batch, waitingText, startX + outlineOffset2, waitingY - outlineOffset2);
                font.draw(batch, waitingText, startX - outlineOffset1, waitingY - outlineOffset1);
                font.draw(batch, waitingText, startX - outlineOffset2, waitingY - outlineOffset2);

                //text
                font.setColor(Color.WHITE);
                font.draw(batch, waitingText, startX, waitingY);
            }

            // Reset the font scale back to its original size after drawing the player's text
            font.getData().setScale(1f, 1f);
        }
        else if (state == State.Paused) {
            //set text and get size to correctly draw the text in the center of the screen
            String waitText = "Waiting for players";
            GlyphLayout waitLayout = new GlyphLayout();
            waitLayout.setText(font,waitText);
            float waitX = hudViewport.getWorldWidth()/2f - waitLayout.width/2f;
            float waitY = hudViewport.getWorldHeight()/2f + waitLayout.height/2f;
            font.draw(batch, waitText, waitX,waitY);
        }
        if(state == State.Over){
            checkGamestate();
            //set text and get size to correctly draw the text in the center of the screen
            String wonText = "Winner is: ";
            if(winner!=Winner.None) {
                switch (winner){
                    case Player1:
                        wonText = wonText+playersData.get(0).getName();
                        break;
                    case Player2:
                        wonText = wonText+playersData.get(1).getName();
                        break;
                    case Player3:
                        wonText = wonText+playersData.get(2).getName();
                        break;
                    case Player4:
                        wonText = wonText+playersData.get(3).getName();
                        break;
                }
            }
            else
                wonText = "No winner.";
            GlyphLayout wonLayout = new GlyphLayout();
            wonLayout.setText(font,wonText);
            float waitX = hudViewport.getWorldWidth()/2f - wonLayout.width/2f;
            float waitY = hudViewport.getWorldHeight()/2f + wonLayout.height/2f;
            font.draw(batch, wonText, waitX,waitY+(font.getXHeight()*2));

            int playerScore = getPlayerById(localPlayerId).getScore();
            String scoreText = "Your score: "+playerScore;
            wonLayout.setText(font,scoreText);
            font.draw(batch,scoreText,waitX,waitY);
        }
    }

    private void handleInput() {
        //za mouse
        float touchX = Gdx.input.getX();
        float touchY = Gdx.input.getY();

        //pretvori screen koordinate v world koordinate
        Vector2 worldCoords = viewport.unproject(new Vector2(touchX, touchY));

        if (state == State.Running) {
            //arrow button click cycle
            //Player currentPlayer = playersData.get(playerTurn - 1);
            Player currentPlayer = getPlayerById(playerTurn);
            Hand currentHand = currentPlayer.getHand();
            if (isClickedOnArrowButtonLeft(worldCoords.x, worldCoords.y, currentHand)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && showLeftArrow) {
                    handArrowLeftClicked(currentHand);
                }
            } else if (isClickedOnArrowButtonRight(worldCoords.x, worldCoords.y, currentHand)) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && showRightArrow) {
                    handArrowRightClicked(currentHand);
                }
            }

            //player se ni imel poteze ta turn
            if (!playerPerformedAction) {
                //player trenutnega turna
                //Gdx.app.log("Current player", currentPlayer.getName());
                //kliknil na deck
                if (isClickedOnDeck(worldCoords.x, worldCoords.y, deckDraw)) {
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        if (sfxPickup != null) {
                            sfxPickup.play(manager.getSoundVolumePref());
                        }
                        currentPlayer.getHand().pickCard(deckDraw);
                        //ce hocemo da konec trenutnega player turna, ko vlece karto iz decka
                        playerTurn = getNextTurn(playerTurn);
                        playerPerformedAction = true;
                        //move hand index right (draw card)
                        handArrowRightClicked(currentPlayer.getHand());
                        updateGameData();
                    }
                }

                //kliknil na card - kateri card v roki
                for (Card card : currentPlayer.getHand().getCards()) {
                    if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                        //izbran card ima highlight
                        card.setHighlight(true);
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                            if (sfxCollect != null) {
                                sfxCollect.play(manager.getSoundVolumePref());
                            }
                            gameControl(card, currentPlayer.getHand());
                            break;
                        }
                    } else {
                        card.setHighlight(false);
                    }
                }

                playerPerformedAction = false;
            }
        } else if (state == State.Choosing) {
            Card card = isClickedOnChoosingCards(worldCoords.x, worldCoords.y);
            if (card != null) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (sfxCollect != null) {
                        sfxCollect.play(manager.getSoundVolumePref());
                    }
                    topCard = card;
                    state = State.Running;
                    choosingCards.clear();
                    playerTurn = getNextTurn(playerTurn);
                    updateGameData();
                }
            }
        }
    }

    private void gameControl(Card card, Hand hand) {
        //Gdx.app.log("Card",card.asString());
        //iste barve ali simbola
        if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
            if (card.isSpecial()) {
                specialCardAction(card, hand);
            }
            //player opravil turn (if: ne increment turna če še čaka da bo izbral new color)
            if (state == State.Running) {
                hand.setCard(card, deckDiscard);
                topCard = card;
                playerTurn = getNextTurn(playerTurn);
                playerPerformedAction = true;
                updateGameData();
            }
            //move hand index left (removed card)
            handArrowLeftClicked(hand);
        }
    }

    private void specialCardAction(Card card, Hand hand) {
        int index;
        String special = card.getSpecial();
        int maxCardsShow = getMaxCardsShow();
        Player nextPlayer = null;
        switch (special) {
            //Stop
            case "S":
                //dobi naslednjega playerja glede na turnOrder pref
                //skip njihov turn
                playerTurn = getNextTurn(playerTurn);
                break;
            //Reverse
            case "R":
                //spremeni turnOrder in current player ponovno potezo
                clockwiseOrder = !clockwiseOrder;
                playerTurn = getNextTurn(playerTurn);
                break;
            //Plus 2
            case "P2":
                //dobi naslednjega playerja glede na turnOrder pref
                //naj vlecejo +2
                index = getNextTurn(playerTurn);
                nextPlayer = getPlayerById(index);
                //naslednji player picka 2x karti
                if(nextPlayer!=null && nextPlayer.getHand()!=null) {
                    nextPlayer.getHand().pickCards(deckDraw, 2);
                    //inkrementiraj lastIndex
                    nextPlayer.getHand().lastIndexIncrement(2, maxCardsShow);
                }
                break;
            //Plus 4
            case "P4":
                //dobi naslednjega playerja glede na turnOrder pref
                //naj vlecejo +4
                index = getNextTurn(playerTurn);nextPlayer = getPlayerById(index);
                //naslednji player picka 2x karti
                if(nextPlayer!=null && nextPlayer.getHand()!=null) {
                    nextPlayer.getHand().pickCards(deckDraw, 4);
                    //inkrementiraj lastIndex
                    nextPlayer.getHand().lastIndexIncrement(4, maxCardsShow);
                }
                break;
            //Rainbow
            default:
                Gdx.app.log("PLAYED CHANGE COLOR", "player");
                hand.setCard(card, deckDiscard); //predhodno add card v Deck ker se pogoj spremeni
                state = State.Choosing;
        }
    }

    //SPREMINJANJE INDEXOV CARD ELEMENTOV KI SE PRIKAZEJO V PLAYER HAND-U
    private void handArrowLeftClicked(Hand currentHand) {
        int maxCardsShow = getMaxCardsShow();
        currentHand.indexDecrement(maxCardsShow);
        int indexFirst = currentHand.getIndexFirst();
        int indexLast = currentHand.getIndexLast();
        Gdx.app.log("ARROW CLICK LEFT", "Index first: " + indexFirst + " | Index last: " + indexLast);
    }

    private void handArrowRightClicked(Hand currentHand) {
        int maxCardsShow = getMaxCardsShow();
        currentHand.indexIncrement(maxCardsShow);
        int indexFirst = currentHand.getIndexFirst();
        int indexLast = currentHand.getIndexLast();
        Gdx.app.log("ARROW CLICK RIGHT", "Index first: " + indexFirst + " | Index last: " + indexLast);
    }

    //METODE ZA LOGIKO CE JE MOUSE NAD CLICKABLE ELEMENTI IGRE
    //glej ce je mouse click na karti
    private boolean isClickedOnCard(float mouseX, float mouseY, Card card) {
        Vector2 position = card.getPosition();
        Rectangle bounds = card.getBounds();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    //glej ce je mouse click na decku
    private boolean isClickedOnDeck(float mouseX, float mouseY, Deck deck) {
        Vector2 position = deck.getPosition();
        Rectangle bounds = deck.getBounds();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    //glej ce je click na choosingCards
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

    //mouse nad levi arrow button
    private boolean isClickedOnArrowButtonLeft(float mouseX, float mouseY, Hand hand) {
        Vector2 position = hand.getPositionArrowRegionLeft();
        Rectangle bounds = hand.getBoundsArrowRegionLeft();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    //mouse nad desni arrow button
    private boolean isClickedOnArrowButtonRight(float mouseX, float mouseY, Hand hand) {
        Vector2 position = hand.getPositionArrowRegionRight();
        Rectangle bounds = hand.getBoundsArrowRegionRight();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    private void drawColorWheel() {
        if (choosingCards.isEmpty()) {
            //B,R,G,Y
            float sizeX = GameConfig.CARD_WIDTH_SM;
            float sizeY = GameConfig.CARD_HEIGHT_SM;

            float startX = (viewport.getWorldWidth() - 4 * sizeX) / 2f;
            float centerY = (viewport.getWorldHeight() - sizeY) / 2f;

            Card cardB = new Card();
            //float BX = centerX - sizeX*2;
            float BX = startX;
            float BY = centerY;
            cardB.setPositionAndBounds(BX, BY, sizeX, sizeY);
            cardB.setColor(RegionNames.Bdefault);
            choosingCards.add(cardB);

            Card cardR = new Card();
            //float RX = centerX - sizeX;
            float RX = startX + sizeX;
            float RY = centerY;
            cardR.setPositionAndBounds(RX, RY, sizeX, sizeY);
            cardR.setColor(RegionNames.Rdefault);
            choosingCards.add(cardR);

            Card cardG = new Card();
            //float GX = centerX;
            float GX = startX + sizeX * 2;
            float GY = centerY;
            cardG.setPositionAndBounds(GX, GY, sizeX, sizeY);
            cardG.setColor(RegionNames.Gdefault);
            choosingCards.add(cardG);

            Card cardY = new Card();
            //float YX = centerX + sizeX;
            float YX = startX + sizeX * 3;
            float YY = centerY;
            cardY.setPositionAndBounds(YX, YY, sizeX, sizeY);
            cardY.setColor(RegionNames.Ydefault);
            choosingCards.add(cardY);
        }

        for (Card card : choosingCards) {
            String texture = card.getTexture();
            TextureRegion region = gameplayAtlas.findRegion(texture);
            Card.render(batch, region, card);
        }
    }

    private void checkGamestate() {
        if (deckDraw.isEmpty()) {
            stopScheduler();
            state = State.Over;
            winner = Winner.None;
        }
        for (int i = 0; i < playersData.size(); ++i) {
            Player player = playersData.get(i);
            if (player != null) {
                if (player.getHand().getCards().isEmpty()) {
                    switch (i) {
                        case 0:
                            winner = Winner.Player1;
                            state = State.Over;
                            //calcPoints(player);
                            break;
                        case 1:
                            winner = Winner.Player2;
                            state = State.Over;
                            //calcPoints(player);
                            break;
                        case 2:
                            winner = Winner.Player3;
                            state = State.Over;
                            //calcPoints(player);
                            break;
                        case 3:
                            winner = Winner.Player4;
                            state = State.Over;
                            //calcPoints(player);
                            break;
                    }
                }
            }
        }
        if (state == State.Over) {
            stopScheduler();
            calcPoints();
        }
    }

    void calcPoints() {
        for (Player currentPlayer : playersData) {
            int sumPoints = 0;
            if (currentPlayer != null) {
                for (Player otherPlayer : playersData) {
                    if (otherPlayer != null) {
                        if (!Objects.equals(otherPlayer.getName(), currentPlayer.getName())) {
                            sumPoints += otherPlayer.getHand().getSumCardPoints();
                        }
                    }
                }
                currentPlayer.setScore(sumPoints);
            }
        }
    }

    //PREKRITE (STATIC) METODE
    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
    }

    public void playerLeaveGame(int playerId, int gameId){
        //get player from playersData that is leaving (based on localId)
        Player currentPlayer = getPlayerById(playerId);
        //if game is over, dont add cards back into drawDeck
        if (state != State.Over) {
            //put their cards back into the drawDeck
            deckDraw.setCards(currentPlayer.getHand().getCards());
        }
        stopScheduler();

        //update turn AND remove player
        if(!isWaiting && playerTurn == localPlayerId) {
            playerTurn = getNextTurn(playerTurn);
            updateGameRemovePlayerTurn(currentPlayer, gameId, playerTurn, new GameUpdateCallback() {
                @Override
                public void onGameFetched(GameData updatedGame) {
                    Gdx.app.log("GAME", "Removed player with id: " + localPlayerId + " & updated turn to: "+ updatedGame.getCurrentTurn() +" from Game with id: " + updatedGame.getId());
                }
                @Override
                public void onFailure(Throwable t) {
                    Gdx.app.log("ERROR", "Failed to remove player from game.");
                }
            });
        }
        //only remove player
        else {
            updateGameRemovePlayer(currentPlayer, gameId, new GameUpdateCallback() {
                @Override
                public void onGameFetched(GameData updatedGame) {
                    Gdx.app.log("GAME", "Removed player with id: " + localPlayerId + " from Game with id: " + updatedGame.getId());
                }
                @Override
                public void onFailure(Throwable t) {
                    Gdx.app.log("ERROR", "Failed to remove player from game.");
                }
            });
        }
    }

    //z scene2d
    public Actor createExitButton(State state) {
        TextButton exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");
                playerLeaveGame(localPlayerId,currentGameId);
                game.setScreen(new MenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.defaults();

        buttonTable.add(exitButton);

        if (state == State.Over) {
            buttonTable.center().padTop(100); // Center the button during game over
        } else {
            buttonTable.top().right().pad(2); // Position it in the top-right for HUD
        }

        buttonTable.setFillParent(true);

        return buttonTable;
    }
}
