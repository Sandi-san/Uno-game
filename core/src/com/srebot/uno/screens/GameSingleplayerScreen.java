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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.Card;
import com.srebot.uno.classes.Deck;
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GameSingleplayerScreen extends ScreenAdapter {
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

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private OrthographicCamera backgroundCamera;
    private Viewport viewport;
    private Viewport hudViewport;
    private Viewport backgroundViewport; //viewport for background
    private Sprite background;

    private Stage stage; //stage for when game over
    private Stage stageHud; //stage for hud
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
    private Player computer1;
    private Player computer2;
    private Player computer3;
    private List<Player> playersData;

    //player hand arrow button display
    private boolean showLeftArrow;
    private boolean showRightArrow;

    private Array<Card> choosingCards = new Array<Card>();

    public GameSingleplayerScreen(Uno game, Array<String> args) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        state = State.Running;

        setMusicAndSounds();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        batch = new SpriteBatch();
        initGame(args);
    }

    //pripravi igro (init globals)
    public void initGame(Array<String> args) {
        //0-numComputers,1-AIdiff,2-deckSize,3-presetBox,4-orderBox,
        int numComputers = Integer.parseInt(args.get(0));
        difficultyAI = Integer.parseInt(args.get(1));
        deckSize = Integer.parseInt(args.get(2));
        String preset = args.get(3);
        clockwiseOrder = Objects.equals(args.get(4), "Clockwise");

        //USTVARI DECKE
        //ustvari main deck
        deckDraw = new Deck(deckSize, game);
        //deckDraw.generateRandom();
        //deckDraw.generateByRules(2, 2, 2);
        deckDraw.generateBySize(deckSize,preset);
        deckDraw.shuffleDeck();
        //vzemi eno karto iz deka
        topCard = deckDraw.pickCard();

        //ustvari discard dek in polozi to karto nanj
        deckDiscard = new Deck(deckSize, game);
        deckDiscard.setCard(topCard);

        playersData = new ArrayList<>();
        //USTVARI PLAYERJE
        //dobi iz jsona ce obstaja
        player = manager.getPlayerByName(manager.loadFromJson(), manager.getNamePref());
        Hand playerHand = new Hand();
        if (player == null) {
            player = new Player(manager.getNamePref(), 0, playerHand);
        } else {
            player.setHand(playerHand);
        }
        player.getHand().pickCards(deckDraw, 5);
        //playerHand.initIndexes();

        //ZA VSE OSTALE PLAYERJE

        //for each player dodaj v playerData
        //Pomembni order: bottom->left->top->right
        computer1 = new Player("Computer", 0, new Hand());
        computer1.getHand().pickCards(deckDraw, 5);
        //1 AI
        if(numComputers==1) {
            playersData.add(player); //bottom
            playersData.add(null);  //right
            playersData.add(computer1); //top
            playersData.add(null);  //left
        }
        //2 AIs
        else {
            computer2 = new Player("Computer", 0, new Hand());
            computer2.getHand().pickCards(deckDraw, 5);

            playersData.add(player);
            playersData.add(computer1);
            playersData.add(computer2);
            //3 AIs
            if (numComputers == 3) {
                computer3 = new Player("Computer", 0, new Hand());
                computer3.getHand().pickCards(deckDraw, 5);
                playersData.add(computer3);
            }
            else
                playersData.add(null);
        }

        //pripravi globale
        getFirstTurn();
        playerPerformedAction = false;
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

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, hudCamera);
        backgroundCamera = new OrthographicCamera();
        backgroundViewport = new StretchViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, backgroundCamera);
        stage = new Stage(hudViewport, game.getBatch());    //buttons
        stageHud = new Stage(hudViewport, game.getBatch()); //hud

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
        //scale background
        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
        //TODO: scale for other Screens, re-position Hands (2+ player) / Deck
    }

    @Override
    public void render(float delta) {
        //doloci barve ozadja
        float r = 200 / 255f;
        float g = 30 / 255f;
        float b = 100 / 255f;
        float a = 0.7f; //prosojnost
        ScreenUtils.clear(r, g, b, a);

        //render background
        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

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

        // Apply HUD viewport
        hudViewport.apply();
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        batch.begin();
        drawHud();
        batch.end();

        if (state == State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
        else {
            stageHud.act(delta);
            stageHud.draw();
            Gdx.input.setInputProcessor(stageHud);
        }
    }

    private void draw() {
        //VELIKOST kart (v WORLD UNITS)
        float sizeX = 11.2f;
        float sizeY = 16f;
        //regular size if 2 players
        if(getPlayersSize()==2){
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
            String topCardTexture = topCard.getTexture();
            TextureRegion topCardRegion = gameplayAtlas.findRegion(topCardTexture);
            //POZICIJA
            float topX = (GameConfig.WORLD_WIDTH - sizeX) / 2f;
            float topY = (GameConfig.WORLD_HEIGHT - sizeY) / 2f;
            topCard.setPositionAndBounds(topX, topY, sizeX, sizeY);
            Card.render(batch, topCardRegion, topCard);

            //DRAW DECK
            TextureRegion drawDeckRegion = gameplayAtlas.findRegion(RegionNames.back);
            //FAR RIGHT
            float drawX = (GameConfig.WORLD_WIDTH - sizeX);
            //or slightly right of draw deck but not covering left player cards
            if(numPlayers>2)
                drawX = drawX - (topX / 2f);
            float drawY = (GameConfig.WORLD_HEIGHT - sizeY) / 2f;
            deckDraw.setPositionAndBounds(drawX, drawY, sizeX, sizeY);
            Card.render(batch, drawDeckRegion, drawX, drawY, sizeX, sizeY);
        }
        if (state == State.Running || state==State.Over) {
            //DRAW COMPUTERS
            //if 1 computer
            if(numPlayers==2) {
                Hand computerHand = computer1.getHand();
                drawHand(computerHand, 1,
                        sizeX, sizeY, numPlayers,false);
            }
            //if more than 1 computer
            else{
                for(int i=1; i<numPlayers;++i){
                    Hand computerHand = playersData.get(i).getHand();
                    drawHand(computerHand, i,
                            sizeX, sizeY, numPlayers,false);
                }
            }

            //DRAW PLAYER
            Hand playerHand = player.getHand();
            playerHand.setArrowRegions(gameplayAtlas.findRegion(RegionNames.arrow));
            drawHand(playerHand, 0,
                    sizeX, sizeY, numPlayers,true);
        }
        else if (state == State.Choosing) {
            drawColorWheel();
        }

        //DRAW EXIT BUTTON
        //drawExitButton();
    }

    private void drawHand(Hand hand, int index, float sizeX, float sizeY, int numPlayers, boolean isPlayer) {
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
        //0-P1, 1-P2, 2-P3, 3-P4
        switch (index) {
            //right or top
            case 1:
                if (numPlayers == 2) {
                    //top
                    startY = GameConfig.WORLD_HEIGHT - sizeY;
                } else {
                    //right
                    startY = sizeX;
                    startX = 1; //start at right
                }
                break;
            //top
            case 2:
                //ce maxPlayers=>3: draw P1
                startY = GameConfig.WORLD_HEIGHT - GameConfig.CARD_HEIGHT_SM; //top
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
            //draw left arrow
            float sizeLeft = GameConfig.WORLD_WIDTH - ((lastIndex+1 - firstIndex) * spacing);
            startX = sizeLeft / 2f;
            Gdx.app.log("SizeLeft:","StartX: " + startX);

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
                    float posX = (startX + (i - firstIndex) * spacing)-1f; //small padding fix
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
                    float posX = (startX + (j - firstIndex) * spacing)-1f; //small padding fix
                    float posY = startY + 2f; //slightly gor
                    card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                    Card.render(batch, region, card);
                }
            }

            if (isPlayer)
                Gdx.app.log("PLAYER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);
            //else
            //    Gdx.app.log("COMPUTER", "size: " + size + " | indexes: " + firstIndex + " , " + lastIndex);

            //calculate where to draw right arrow (after last drawn card)
            float sizeRight = ((lastIndex+1 - firstIndex) * spacing);
            float endX = sizeRight + (sizeLeft/2f);
            Gdx.app.log("SizeRight:","EndX: " + endX);

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
            //for rotating Card 90deg (far left) or -90deg (far right)
            int rotationScalar = 1;
            //if(startX==2)
            if (startX == 3)
                rotationScalar = -1;

            //brez spacing
            if (size <= 5) {
                hand.setIndexLast();
                startY = (GameConfig.WORLD_HEIGHT - size * sizeX) / 2f;
            }
            //spacing le dokler ne reachas MaxCardsToShow
            else if (size <= 7) {
                hand.setIndexLast();
                spacing = overlap;
                startY = (GameConfig.WORLD_HEIGHT - size * sizeX) / 2f;
            }
            //ne vec spacingat ko imas vec kart kot MaxCardsToShow
            else {
                spacing = overlap;
                startY = (GameConfig.WORLD_HEIGHT - maxCardsShow * sizeX) / 2f;
            }

            //LIMIT RENDER CARDS DOL
            if (startY < (sizeY * 0.8f))
                startY = (sizeY * 0.8f);

            //set x-axis based on hand location (left/right)
            if (startX == 1)
                startX = GameConfig.WORLD_WIDTH - (GameConfig.CARD_WIDTH_SM + GameConfig.CARD_HEIGHT_SM * 0.1f); //malce levo
            else
                startX = GameConfig.CARD_HEIGHT_SM * 0.1f; //malce desno

            // Render vertically
            for (int i = firstIndex; i < size; ++i) {
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
        if(state== State.Running) {
            // Set the font to a smaller scale for the player's text
            font.getData().setScale(0.8f);  // Scale down to 80% of the original size

            float startX = 10f; //width left + margin
            float startY = GameConfig.HUD_HEIGHT-10f; //top + margin

            //height of each line of font + spacing
            float lineHeight = font.getXHeight();

            // Define outline offsets and outline color
            float outlineOffset = 1.8f;  // The offset for the outline
            Color outlineColor = Color.BLACK;  // Outline color

            float playerY = startY;

            for (int i = 0; i < playersData.size(); ++i) {
                Player player = playersData.get(i);
                if (player != null) {
                    String position = "Bottom";
                    switch (i){
                        case 1:
                            position = "Right";
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

                    //TODO: malce boljse da zgleda
                    // Draw the text multiple times to create an outline effect (up, down, left, right)
                    font.draw(batch, playerText, startX + outlineOffset, playerY + outlineOffset);  // Top-right
                    font.draw(batch, playerText, startX - outlineOffset, playerY + outlineOffset);  // Top-left
                    font.draw(batch, playerText, startX + outlineOffset, playerY - outlineOffset);  // Bottom-right
                    font.draw(batch, playerText, startX - outlineOffset, playerY - outlineOffset);  // Bottom-left

                    // Draw the original text in its normal color
                    font.setColor(Color.WHITE);  // Set the font color to the main color (e.g., white)
                    //display player's data
                    font.draw(batch, playerText, startX, playerY);
                }
            }

            // Reset the font scale back to its original size after drawing the player's text
            font.getData().setScale(1f, 1f);
        }
        else if(state == State.Over){
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
            float waitX = GameConfig.HUD_WIDTH/2f - wonLayout.width/2f;
            float waitY = GameConfig.HUD_HEIGHT/2f + wonLayout.height/2f;
            font.draw(batch, wonText, waitX,waitY+(font.getXHeight()*2));

            int playerScore = playersData.get(0).getScore();
            String scoreText = "Your score: "+playerScore;
            wonLayout.setText(font,scoreText);
            font.draw(batch,scoreText,waitX,waitY);
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
    /*
    void calcPoints(PlayerData player){
        int sumPoints = 0;
        for(PlayerData p : playersData){
            if(!Objects.equals(p.getName(), player.getName())){
                sumPoints+=p.getHand().getSumCardPoints();
            }
        }
        player.setScore(sumPoints);
    }*/

    private void handleInput() {
        //za mouse
        //if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
        float touchX = Gdx.input.getX();
        float touchY = Gdx.input.getY();

        //pretvori screen koordinate v world koordinate
        Vector2 worldCoords = viewport.unproject(new Vector2(touchX, touchY));

        if (state == State.Running) {
            //arrow button click cycle
            Player currentPlayer = playersData.get(playerTurn - 1);
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

                    //kliknil na card - kateri card v roki
                    for (Card card : player.getHand().getCards()) {
                        if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                            //izbran card ima highlight
                            card.setHighlight(true);
                            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                                if (sfxCollect != null) {
                                    sfxCollect.play();
                                }
                                //TODO: izbira vec kart (isHighlighted) in posli vse v discard deck
                                gameControl(card, currentPlayer.getHand());
                                topCard = deckDiscard.getTopCard();

                                //move hand index left (removed card) - in gameControl
                                //handArrowLeftClicked(currentPlayer.getHand());
                                break;
                            }
                        } else {
                            card.setHighlight(false);
                        }
                    }
                }
                //current player je computer
                else {
                    cardAIchoose(currentPlayer);
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
            if (state == State.Running) {
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
        int maxCardsShow = getMaxCardsShow();
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
                //naslednji player pick-a 2x karti
                playersData.get(index - 1).getHand().pickCards(deckDraw, 2);
                //inkrementiraj lastIndex
                playersData.get(index - 1).getHand().lastIndexIncrement(2,maxCardsShow);
                break;
            //Plus 4
            case "P4":
                //dobi naslednjega playerja glede na turnOrder pref
                //naj vlecejo +4
                index = getNextTurn(playerTurn);
                playersData.get(index - 1).getHand().pickCards(deckDraw, 4);
                playersData.get(index - 1).getHand().lastIndexIncrement(4, maxCardsShow);
                break;
            //Rainbow
            default:
                if (isPlayer) {
                    Gdx.app.log("PLAYED CHANGE COLOR", "player");
                    state = State.Choosing;
                } else
                    AIchooseColor(hand);
        }
    }

    //COMPUTER AI
    private void cardAIchoose(Player computer) {
        //kopija roke (copy ker noces spreminjat original Hand v loopu)
        Hand phantomHand = new Hand(computer.getHand());
        Card card = null;
        while (true) {
            switch (difficultyAI) {
                case 1:
                    //10% chance da computer vlece karto iz drawDeck tudi ce ima valid Card za igrat
                    Random rnd = new Random();
                    //get random number between 1-10
                    int rndNumber = rnd.nextInt(10) + 1;
                    //clear phantomHand, simulating no cards to play
                    if(rndNumber==1)
                        phantomHand.getCards().clear();
                    //dobi random card
                    card = phantomHand.getRandomCard();
                    break;
                case 3:
                    //gleda od playerjev karte
                    //hint: less priority on symbols/colors player has
                    card = cardAIcheater(phantomHand);
                    break;
                default:
                    //dobi karto iz roke z najvecjo prioriteto
                    card = phantomHand.getHighestPriorityCard();
            }

            phantomHand.setCard(card, null);
            //karta je validna
            if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
                computer.getHand().setCard(card, deckDiscard);
                topCard = deckDiscard.getTopCard();
                if (card.isSpecial()) {
                    specialCardAction(card, computer.getHand(), false);
                }
                //move hand index left (removed card)
                handArrowLeftClicked(computer.getHand());
                playerPerformedAction = true;
                break;
            }
            //computer nima validnih kart v roki, draw new card
            if (phantomHand.getCards().isEmpty()) {
                Random rnd = new Random();
                //get random number between 1-10
                int rndNumber = rnd.nextInt(10) + 1;
                //if 10% chance activates, computer cheats by drawing valid card from deck
                if (rndNumber == 1 && difficultyAI == 3)
                    computer.getHand().pickSpecificCard(deckDraw, topCard);
                else
                    computer.getHand().pickCard(deckDraw);
                //copy ampak samo zadnji card (redundanca)
                //for the logic when the AI doesn't have any valid cards in the hand and then draws a card from the deck
                phantomHand = new Hand(computer.getHand().getLastCard());
                //ce hocemo da vlece le eno karto, nato player poteza
                playerPerformedAction = true;
                //move hand index right (draw card)
                handArrowRightClicked(computer.getHand());
                break;
            }
        }
        //computer oddal zadnjo karto in zmagal
        if (computer.getHand().getCards().isEmpty()) {
            playerPerformedAction = true;
            return;
        }
        //computer nima ustreznih kart, vleci iz deckDraw
        if (phantomHand.getCards().isEmpty()) {
            computer.getHand().pickCard(deckDraw);
            playerPerformedAction = true;
        }
        //computer odpravil potezo, next turn
        if (playerPerformedAction)
            playerTurn = getNextTurn(playerTurn);
    }

    //priority glede least cards next player
    private Card cardAIcheater(Hand hand){
        //hand naslednjega playerja
        int index = getNextTurn(playerTurn);
        Hand nextHand = playersData.get(index - 1).getHand();
        //if computer hand is bigger than next player, try playing special card (stop, reverse, +2/4)
        if(hand.getCards().size>nextHand.getCards().size){
            Array<Card> specials = hand.getSpecialCards();
            if(!specials.isEmpty()) {
                //if multiple special cards can be played, first sort by value, then by priority
                if(specials.size>1) {
                    specials.sort((o1, o2) -> Integer.compare(o1.getValue(), o2.getValue()));
                    specials.sort((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
                }
                return specials.get(0);
            }
        }

        //play non-special cards
        //get least used color from next player
        Array<String> colors = nextHand.getLeastUsedCardColors();
        if(colors.isEmpty() || colors.size==4)
            return hand.getHighestPriorityCard();
        //check if any of those colors exist in hand and check only those Cards
        Array<Card> parsedCards = hand.keepColors(colors);
        if(!parsedCards.isEmpty()){
            //create hand copy with parsed cards, because phantomHand must still contain those cards
            Hand parsedHand = new Hand(hand);
            parsedHand.setCards(parsedCards);
            Card card = parsedHand.getLowestPrioritySpecialCard();
            //card is null, meaning there is no Special Card in Hand
            if(card==null){
                //get highest valued regular card (numbered cards 1-9)
                return parsedHand.getHighestValueCard();
            }
            return card;
        }

        //no colors have been parsed, see if any special cards are in Hand and play them
        Card card = hand.getLowestPrioritySpecialCard();
        if(card==null){
            //if Hand has no special cards, play highest value regular card (values 1-9)
            return hand.getHighestValueCard();
        }
        return card;
    }

    //AI spremeni karto glede diff
    private void AIchooseColor(Hand hand) {
        switch (difficultyAI) {
            case 1:
                changeTopDeckCard(hand.getRandomColor());
                break;
            case 3:
                //hand naslednjega playerja
                int index = getNextTurn(playerTurn);
                Hand nextHand = playersData.get(index - 1).getHand();
                //get colors nextPlayer has LEAST of
                Array<String> colors = nextHand.getLeastUsedCardColors();
                //no valid colors, get computer's most used colors
                if(colors.isEmpty())
                    changeTopDeckCard(hand.getMostUsedCardColor());
                //valid color only one, choose this one
                else if(colors.size==1)
                    changeTopDeckCard(colors.get(0));
                //multiple valid colors, use one that computer has most of
                else
                    changeTopDeckCard(hand.getMostValuedCardColor());
                break;
            default:
                changeTopDeckCard(hand.getMostUsedCardColor());
        }
    }

    private void changeTopDeckCard(String color) {
        topCard = Card.switchCard(deckDiscard.getSecondTopCard(), color);
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
            float sizeX = GameConfig.CARD_WIDTH;
            float sizeY = GameConfig.CARD_HEIGHT;

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
            float RX = startX + sizeX;
            float RY = centerY;
            cardR.setPositionAndBounds(RX, RY, sizeX, sizeY);
            cardR.setDefault(RegionNames.Rdefault);
            choosingCards.add(cardR);

            Card cardG = new Card();
            //float GX = centerX;
            float GX = startX + sizeX * 2;
            float GY = centerY;
            cardG.setPositionAndBounds(GX, GY, sizeX, sizeY);
            cardG.setDefault(RegionNames.Gdefault);
            choosingCards.add(cardG);

            Card cardY = new Card();
            //float YX = centerX + sizeX;
            float YX = startX + sizeX * 3;
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

    //Get local playersData size (without null elements)
    private int getPlayersSize() {
        int count = 0;
        for (Player player : playersData) {
            if (player != null)
                count++;
        }
        return count;
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
    public Actor createExitButton(State state) {
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
