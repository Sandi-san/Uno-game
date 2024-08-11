package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.utils.viewport.FitViewport;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameMultiplayerScreen extends ScreenAdapter {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean newPlayerJoined = false;

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

    private int localPlayerId;
    private int currentGameId;

    //player hand arrow button display
    private boolean showLeftArrow;
    private boolean showRightArrow;

    private Array<Card> choosingCards = new Array<Card>();

    //check if fetching from backend before continuing
    public interface PlayerFetchCallback {
        void onPlayerFetched(Player player);
    }
    public interface GameFetchCallback {
        void onGameIdFetched(int gameId);
    }

    public GameMultiplayerScreen(Uno game, Array<String> args) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();
        //Paused dokler ni 2 playerju
        state = State.Initializing;

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
    //Player checker scheduler
    private void playerChecker() {
        scheduler.scheduleAtFixedRate(() -> {
            // Check the database for new players
            checkForNewPlayers();
            /*
            boolean playerJoined = checkForNewPlayers();
            if (playerJoined) {
                newPlayerJoined = true;
                // Update player data safely
                addPlayerToArray();
            }
            */
        }, 0, 5, TimeUnit.SECONDS); // Check every 5 seconds
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

            //create game in DB and fetch id of newly created game
            createGame(gameData, gameId -> {
                //set current game id locally (prevent unneeded DB fetching)
                currentGameId = gameId;
                //game is initialized, change state
                state = State.Paused;
                //run scheduler that checks for new players

                //GAME CIKLA KO RUNNAS SCHEDULER?
                playerChecker();
            });
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

    private void createGame(GameData gameData, GameFetchCallback callback){
        service.createGame(new GameService.GameCreateCallback() {
            @Override
            public void onSuccess(int gameId) {
                Gdx.app.log("SUCCESS", "Game created with ID: " + gameId);
                callback.onGameIdFetched(gameId);
            }

            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("ERROR", "Failed to create game: " + t.getMessage());
            }
        }, gameData);
    }

    private void checkForNewPlayers(){
        service.fetchGamePlayers(new GameService.FetchGamePlayersCallback() {
            @Override
            public void onSuccess(Player[] players) {
                Gdx.app.log("PLAYERS", "Players fetched: " + players.length);
                //check if current players equals amount of players from db
                //if more: add player, if less: remove player
                compareLocalAndFetchedPlayers(players);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        }, currentGameId);
    }

    //compare local List<Player> players and fetched Player[] from Backend
    private void compareLocalAndFetchedPlayers(Player[] players){
        // Create a set of player IDs for easy lookup
        Set<Integer> fetchedPlayerIds = new HashSet<>();
        for (Player player : players) {
            fetchedPlayerIds.add(player.getId()); // Assuming Player has a getId() method
        }

        // Iterate through the local playersData to find and remove players that are not in the fetched array
        Iterator<Player> iterator = playersData.iterator();
        while (iterator.hasNext()) {
            Player localPlayer = iterator.next();
            if (localPlayer != null && !fetchedPlayerIds.contains(localPlayer.getId())) {
                iterator.remove(); // Remove the player if not in fetched array
                Gdx.app.log("PLAYERS", "Player removed: " + localPlayer.getName());
            }
        }

        // Add new players from the fetched array to playersData if they don't already exist
        for (Player fetchedPlayer : players) {
            boolean playerExists = false;
            for (Player localPlayer : playersData) {
                if (localPlayer != null && localPlayer.getId() == fetchedPlayer.getId()) {
                    playerExists = true;
                    break;
                }
            }
            if (!playerExists) {
                playersData.add(fetchedPlayer);
                Gdx.app.log("PLAYERS", "Player added: " + fetchedPlayer.getName());
            }
        }

        // Ensure that playersData has a maximum of 4 players
        while (playersData.size() > 4) {
            playersData.remove(playersData.size() - 1);
        }
    }

    //create player when starting game (host) //TODO: odstrani in mergaj z zgoraj?
    private void createPlayerFromBackend(String playerName, PlayerFetchCallback callback){
        service.fetchPlayerByName(new GameService.PlayerFetchCallback() {
            @Override
            public void onSuccess(Player player, Hand hand) {
                // Handle the successful response
                Gdx.app.log("PLAYER", "Player fetched: " + player.getName());
                //GET PLAYER IN ADD V CURRENT GAME
                Hand playerHand = new Hand();
                Player thisPlayer = new Player(player.getId(), player.getName(), 0, playerHand);
                thisPlayer.getHand().pickCards(deckDraw, 5);
                localPlayerId = player.getId();
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
                Gdx.app.log("PLAYER", "CREATING NEW PLAYER INSTEAD: " + player.toString());

                service.createPlayer(new GameService.PlayerCreateCallback() {
                    @Override
                    public void onSuccess(int playerId) {
                        Gdx.app.log("SUCCESS", "Player created with ID: " + playerId);

                        player.setId(playerId);
                        localPlayerId = playerId;
                        addPlayerToArray(player);
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
        if (maxPlayers>3) {
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

        stage.addActor(createExitButton());
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

        if(state == State.Initializing)
            return;

        else if(state==State.Paused){
            //TODO: draw still waiting for players text
        }

        if (state != State.Over) {
            checkGamestate();
            handleInput();
        }

        viewport.apply();
        //setProjectionMatrix - uporabi viewport za prikaz sveta (WORLD UNITS)
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        draw();
        batch.end();

        if (state == State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
    }

    public void draw() {
        //TODO HUD

        //VELIKOST kart (v WORLD UNITS)
        float sizeX = GameConfig.CARD_HEIGHT;
        float sizeY = GameConfig.CARD_WIDTH;

        if (state == State.Running) {
            //MIDDLE DECK
            String topCardTexture = topCard.getTexture();
            TextureRegion topCardRegion = gameplayAtlas.findRegion(topCardTexture);
            //POZICIJA
            float topX = (GameConfig.WORLD_WIDTH - sizeX) / 2f;
            float topY = (GameConfig.WORLD_HEIGHT - sizeY) / 2f;
            topCard.setPositionAndBounds(topX, topY, sizeX, sizeY);
            Card.render(batch, topCardRegion, topCard);

            //DRAW DECK
            TextureRegion drawDeckRegion = gameplayAtlas.findRegion(RegionNames.back);
            float drawX = (GameConfig.WORLD_WIDTH - sizeX);
            float drawY = (GameConfig.WORLD_HEIGHT - sizeY) / 2f;
            deckDraw.setPositionAndBounds(drawX, drawY, sizeX, sizeY);
            Card.render(batch, drawDeckRegion, drawX, drawY, sizeX, sizeY);
        } else if (state == State.Choosing) {
            drawColorWheel();
        }
        //pocakaj da se izvede backend fetch, sele nato dostopaj do objecta
        if(state != State.Initializing) {
            //DRAW PLAYER in COMPUTER HANDS
            Hand playerHand = player1.getHand();
            playerHand.setArrowRegions(gameplayAtlas.findRegion(RegionNames.arrow));
            drawHand(playerHand, 0,
                    sizeX, sizeY, true);
        }
        /*
        Hand computerHand = computer.getHand();
        drawHand(computerHand, (GameConfig.WORLD_HEIGHT - sizeY),
                sizeX, sizeY, false);
        */
        //DRAW EXIT BUTTON
        //drawExitButton();
    }

    private void drawHand(Hand hand, float startY, float sizeX, float sizeY, boolean isPlayer) {
        Array<Card> cards = hand.getCards();
        int size = cards.size;
        //hand.setIndexLast();
        int firstIndex = hand.getIndexFirst();
        int lastIndex = hand.getIndexLast();

        //OVERLAP CARD KO IMAS VEC KOT 5
        float overlap = 0f;
        for (int i = 5; i < size; ++i) {
            if (i >= GameConfig.MAX_CARDS_SHOW) break;
            overlap += 0.1f;
        }
        //overlap = sizeX*overlap;
        overlap = sizeX * (1 - overlap);
        float startX;
        float spacing = sizeX;

        //brez spacing
        if (size <= 5) {
            hand.setIndexLast();
            lastIndex = hand.getIndexLast();
            startX = (GameConfig.WORLD_WIDTH - size * sizeX) / 2f;
        }
        //spacing le dokler ne reachas MaxCardsToShow
        else if (size <= GameConfig.MAX_CARDS_SHOW) {
            hand.setIndexLast();
            lastIndex = hand.getIndexLast();
            spacing = overlap;
            startX = (GameConfig.WORLD_WIDTH - size * sizeX) / 2f;
        }
        //ne vec spacingat ko je imas card kot MaxCardsToShow
        else {
            spacing = overlap;
            startX = (GameConfig.WORLD_WIDTH - GameConfig.MAX_CARDS_SHOW * sizeX) / 2f;
        }

        //LIMIT RENDER CARDS LEVO (plac vmes je 70% card width)
        if (startX < (sizeX * 0.7f))
            startX = (sizeX * 0.7f);

        //IZRISI CARDE
        Array<Integer> indexHover = new Array<Integer>();
        if (isPlayer) {
            for (int i = firstIndex; i < size; ++i) {
                //==i<=lastIndex razen ko settamo Card (izogni index izven array)
                if (i > lastIndex)
                    break;
                Card card = cards.get(i);
                String texture;
                TextureRegion region;
                if (!card.getHighlight()) {
                    texture = card.getTexture();
                    region = gameplayAtlas.findRegion(texture);
                    float posX = startX + (i - firstIndex) * spacing;
                    float posY = startY;
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                } else {
                    indexHover.add(i);
                }
            }
        }

        //IZRISI CARDE KI SO HOVERANE
        if (!indexHover.isEmpty()) {
            for (int j : indexHover) {
                Card card = cards.get(j);
                String texture;
                TextureRegion region;
                if (isPlayer || state == State.Over) {
                    texture = card.getTexture();
                    region = gameplayAtlas.findRegion(texture);
                } else {
                    region = gameplayAtlas.findRegion(RegionNames.back);
                }
                float posX = startX + (j - firstIndex) * spacing;
                float posY = startY + 2f; //slightly gor
                card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                Card.render(batch, region, card);
            }
        }

        //kakuliraj kje se bo koncala zadnja karta
        //float endX = GameConfig.MAX_CARDS_SHOW*sizeX-overlap;
        float endX = 0;
        if (!hand.getCards().isEmpty())
            endX = (GameConfig.WORLD_WIDTH - (sizeX * 0.7f));
        //endX = (hand.getCards().get(lastIndex).getPosition().x+sizeX);

        if (isPlayer)
            Gdx.app.log("PLAYER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);
        else
            Gdx.app.log("COMPUTER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);

        //LIMIT RENDER CARDS DESNO (plac vmes je 70% card width)
        if (endX > GameConfig.WORLD_WIDTH - (sizeX * 0.7f))
            endX = (GameConfig.WORLD_WIDTH - (sizeX * 0.7f));

        //preveri ce so arrowi prikazani
        if (isPlayer) {
            if (firstIndex != 0)
                showLeftArrow = true;
            else
                showLeftArrow = false;
            if (lastIndex != cards.size - 1)
                showRightArrow = true;
            else
                showRightArrow = false;
        }

        //render button left
        if (size >= GameConfig.MAX_CARDS_SHOW && isPlayer && showLeftArrow) {
            float arrowX = startX - sizeX / 2 - (sizeX * 0.1f);
            //float arrowY = startY + arrowRegion.getRegionHeight() / 2;
            float arrowY = startY + (sizeY * 0.2f);
            hand.setArrowRegionLeft(arrowX, arrowY, sizeX / 2, sizeY / 2);
            hand.renderArrowLeft(batch);
        }
        //render button right
        if (size >= GameConfig.MAX_CARDS_SHOW && isPlayer && showRightArrow) {
            float arrowX = endX + (sizeX * 0.1f);
            //float arrowY = startY + arrowRegion.getRegionHeight() / 2;
            float arrowY = startY + (sizeY * 0.2f);
            //batch.draw(arrowRegion, arrowX, arrowY);
            hand.setArrowRegionRight(arrowX, arrowY, sizeX / 2, sizeY / 2);
            hand.renderArrowRight(batch);
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
            Player currentPlayer = playersData.get(playerTurn - 1);
            //TODO: get actual player turn-a
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

            //PlayerTurn: player se ni imel poteze ta turn
            if (!playerPerformedAction) {
                //player trenutnega turna
                //Gdx.app.log("Current player",currentPlayer.getName());
                //current player je human
                if (!Objects.equals(currentPlayer.getName(), "Computer")) {
                    //kliknil na deck
                    if (isClickedOnDeck(worldCoords.x, worldCoords.y, deckDraw)) {
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                            if (sfxPickup != null) {
                                sfxPickup.play();
                            }
                            currentPlayer.getHand().pickCard(deckDraw);
                            //ce hocemo da konec tren player turna, ko vlece karto iz decka
                            playerTurn = getNextTurn(playerTurn);
                            playerPerformedAction = true;
                            //move hand index right (draw card)
                            handArrowRightClicked(currentPlayer.getHand());
                        }
                    }
                    //TODO: get actual current player
                    //kliknil na card - kateri card v roki
                    for (Card card : player1.getHand().getCards()) {
                        if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                            //izbran card ima highlight
                            card.setHighlight(true);
                            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                                if (sfxCollect != null) {
                                    sfxCollect.play();
                                }
                                gameControl(card, currentPlayer.getHand());
                                topCard = deckDiscard.getTopCard();

                                //move hand index left (removed card)
                                //handArrowLeftClicked(currentPlayer.getHand());
                                break;
                            }
                        } else {
                            card.setHighlight(false);
                        }
                    }
                }
                playerPerformedAction = false;
            }
        } else if (state == State.Choosing) {
            Card card = isClickedOnChoosingCards(worldCoords.x, worldCoords.y);
            if (card != null) {
                if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                    if (sfxCollect != null) {
                        sfxCollect.play();
                    }
                    changeTopDeckCard(card.getColor());
                    state = State.Running;
                    choosingCards.clear();
                    playerTurn = getNextTurn(playerTurn);
                }
            }
        }
    }

    private void gameControl(Card card, Hand hand) {
        //Gdx.app.log("Card",card.asString());
        //iste barve ali simbola
        if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
            hand.setCard(card, deckDiscard);
            if (card.isSpecial()) {
                specialCardAction(card, hand, true);
            }
            //player odpravil turn (if - ne increment turna ce se caka da bo izbral new color)
            if(state == State.Running) {
                playerPerformedAction = true;
                playerTurn = getNextTurn(playerTurn);
            }
            //move hand index left (removed card)
            handArrowLeftClicked(hand);
        }
    }

    private void specialCardAction(Card card, Hand hand, boolean isPlayer) {
        int index;
        String special = card.getSpecial();
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
                //naslednji player picka 2x karti
                playersData.get(index - 1).getHand().pickCards(deckDraw, 2);
                //inkrementiraj lastIndex
                playersData.get(index - 1).getHand().lastIndexIncrement(2);
                break;
            //Plus 4
            case "P4":
                //dobi naslednjega playerja glede na turnOrder pref
                //naj vlecejo +4
                index = getNextTurn(playerTurn);
                playersData.get(index - 1).getHand().pickCards(deckDraw, 4);
                playersData.get(index - 1).getHand().lastIndexIncrement(4);
                break;
            //Rainbow
            default:
                if (isPlayer) {
                    Gdx.app.log("PLAYED CHANGE COLOR", "player");
                    state = State.Choosing;
                }
        }
    }

    private void changeTopDeckCard(String color) {
        topCard = Card.switchCard(deckDiscard.getSecondTopCard(), color);
    }

    //TODO: computer card num v Hand ko hoveras
    //SPREMINJANJE INDEXOV CARD ELEMENTOV KI SE PRIKAZEJO V PLAYER HAND-U
    private void handArrowLeftClicked(Hand currentHand) {
        currentHand.firstIndexDecrement();
        currentHand.lastIndexDecrement();
        int indexFirst = currentHand.getIndexFirst();
        int indexLast = currentHand.getIndexLast();
        Gdx.app.log("ARROW CLICK LEFT", "Index first: " + indexFirst + " | Index last: " + indexLast);
    }

    private void handArrowRightClicked(Hand currentHand) {
        currentHand.firstIndexIncrement();
        currentHand.lastIndexIncrement();
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
            if(mouseX >= position.x && mouseX <= position.x + bounds.width
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
        if(choosingCards.isEmpty()) {
            //B,R,G,Y
            float sizeX = GameConfig.CARD_HEIGHT;
            float sizeY = GameConfig.CARD_WIDTH;

            float startX = (GameConfig.WORLD_WIDTH - 4 * sizeX) / 2f;
            //float centerX = (GameConfig.WORLD_WIDTH - sizeX) / 2f;
            float centerY = (GameConfig.WORLD_HEIGHT - sizeY) / 2f;

            Card cardB = new Card();
            //float BX = centerX - sizeX*2;
            float BX = startX;
            float BY = centerY;
            cardB.setPositionAndBounds(BX, BY, sizeX, sizeY);
            cardB.setDefault(RegionNames.Bdefault);
            choosingCards.add(cardB);

            Card cardR = new Card();
            //float RX = centerX - sizeX;
            float RX = startX+sizeX;
            float RY = centerY;
            cardR.setPositionAndBounds(RX, RY, sizeX, sizeY);
            cardR.setDefault(RegionNames.Rdefault);
            choosingCards.add(cardR);

            Card cardG = new Card();
            //float GX = centerX;
            float GX = startX+sizeX*2;
            float GY = centerY;
            cardG.setPositionAndBounds(GX, GY, sizeX, sizeY);
            cardG.setDefault(RegionNames.Gdefault);
            choosingCards.add(cardG);

            Card cardY = new Card();
            //float YX = centerX + sizeX;
            float YX = startX+sizeX*3;
            float YY = centerY;
            cardY.setPositionAndBounds(YX, YY, sizeX, sizeY);
            cardY.setDefault(RegionNames.Ydefault);
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
            state = State.Over;
            winner = Winner.None;
        }
        for (int i = 0; i < playersData.size(); ++i) {
            if (state == State.Over) break;
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
        /*
        batch.dispose();
        skin.dispose();
        gameplayAtlas.dispose();
        sfxPickup.dispose();
        sfxCollect.dispose();
         */
    }

    //z scene2d
    public Actor createExitButton() {
        Table table = new Table();
        table.defaults().pad(20);
        /*
        //BACKGROUND
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background3);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));
        */

        TextButton exitButton = new TextButton("Exit", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");
                //shrani player podatke v json
                manager.saveDataToJsonFile(playersData);
                game.setScreen(new MenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.defaults();

        buttonTable.add(exitButton).padBottom(15).expandX().fill().row();
        buttonTable.center();

        table.add(buttonTable);
        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }
}
