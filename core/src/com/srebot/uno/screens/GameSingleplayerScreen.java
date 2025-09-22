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
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GameSingleplayerScreen extends ScreenAdapter {
    //possible game states
    public enum State {
        Running, Over, Choosing
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

    //GLOBAL VARIABLES
    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private OrthographicCamera backgroundCamera;
    private Viewport viewport;
    private Viewport hudViewport;
    private Viewport backgroundViewport; //viewport for background
    private Sprite background;  //image for background

    private Stage stage; //stage for game
    private Stage stageHud; //stage for hud
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

    private int playerTurn; //index of current turn based on Hand locations on screen
    private boolean playerPerformedAction; //check if current player has performed action
    private boolean clockwiseOrder = false; //turn order of game
    private int difficultyAI;

    //PLAYERS
    private Player player;
    private Player computer1;
    private Player computer2;
    private Player computer3;
    private List<Player> playersData; //array of players

    //check to display arrow button for player's Hand
    private boolean showLeftArrow;
    private boolean showRightArrow;

    //array of Card objects to display when game is in Choosing State
    private Array<Card> choosingCards = new Array<Card>();

    //Constructor
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

    /** Load music and sounds **/
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

    /** Init game globals (args passed from MenuScreen) */
    public void initGame(Array<String> args) {
        //args is array of strings to determine starting game variables:
        //0 - numComputers, 1 - AIdiff, 2 - deckSize, 3 - presetBox, 4 - orderBox,
        //5,6,7 - "Custom" Deck variables
        int numComputers = Integer.parseInt(args.get(0));
        difficultyAI = Integer.parseInt(args.get(1));
        int deckSize = Integer.parseInt(args.get(2));
        String preset = args.get(3);
        clockwiseOrder = Objects.equals(args.get(4), "Clockwise");

        playerTurn = 1;
        playerPerformedAction = false;

        //CREATE DECKS
        //Create draw Deck
        deckDraw = new Deck(deckSize);
        //if preset is custom, use args variables for color, special and wild card sizes
        if(preset.equals("Custom"))
            deckDraw.generateByRules(Integer.parseInt(args.get(5)), Integer.parseInt(args.get(6)), Integer.parseInt(args.get(7)));
        //else create a deck of set size
        else
            deckDraw.generateBySize(deckSize,preset);
        //shuffle deck and set topCard as first card from drawDeck
        deckDraw.shuffleDeck();
        topCard = deckDraw.pickCard();

        //create discard Deck and add topCard
        deckDiscard = new Deck(deckSize);
        deckDiscard.setCard(topCard);

        //FILL PLAYERS ARRAY
        playersData = new ArrayList<>();
        //get player from local json if possible and create new Hand object
        player = manager.getPlayerByName(manager.loadFromJson(), manager.getNamePref());
        Hand playerHand = new Hand();
        if (player == null) {
            //else create new player
            player = new Player(manager.getNamePref(), 0, playerHand);
        } else {
            player.setHand(playerHand);
        }
        //pick cards from draw Deck and set to Hand
        player.getHand().pickCards(deckDraw, 5);

        //Players set order: bottom->left->top->right
        //create and add first computer (AI)
        computer1 = new Player("Computer", 0, new Hand());
        computer1.getHand().pickCards(deckDraw, 5);
        //players array order for only 1 AI
        if(numComputers==1) {
            playersData.add(player); //bottom
            playersData.add(null);  //right
            playersData.add(computer1); //top
            playersData.add(null);  //left
        }
        //order for 2 AIs
        else {
            //create and add second AI
            computer2 = new Player("Computer", 0, new Hand());
            computer2.getHand().pickCards(deckDraw, 5);

            playersData.add(player);
            playersData.add(computer1);
            playersData.add(computer2);

            //order for 3  AIs
            if (numComputers == 3) {
                //create and add third ai
                computer3 = new Player("Computer", 0, new Hand());
                computer3.getHand().pickCards(deckDraw, 5);
                playersData.add(computer3);
            }
            else
                playersData.add(null);
        }
    }

    @Override
    public void show() {
        //Set viewports, cameras and stages
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT, camera);
        hudCamera = new OrthographicCamera();
        hudViewport = new ExtendViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, hudCamera);
        backgroundCamera = new OrthographicCamera();
        backgroundViewport = new StretchViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, backgroundCamera);
        stage = new Stage(hudViewport, game.getBatch());    //buttons
        stageHud = new Stage(hudViewport, game.getBatch()); //hud

        //set camera positions
        camera.position.set(GameConfig.WORLD_WIDTH / 2f,
                GameConfig.WORLD_HEIGHT, 0);
        camera.update();
        hudCamera.position.set(GameConfig.WORLD_WIDTH / 2f,
                GameConfig.WORLD_HEIGHT, 0);
        hudCamera.update();
        backgroundCamera.position.set(GameConfig.WORLD_WIDTH / 2f,
                GameConfig.WORLD_HEIGHT, 0);
        backgroundCamera.update();

        //set skins, atlas and background image
        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background1);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        //set stage button actors
        stage.addActor(createExitButton(State.Over));
        stageHud.addActor(createExitButton(State.Running));
    }

    @Override
    public void resize(int width, int height) {
        //resize viewports
        viewport.update(width, height, true);
        hudViewport.update(width, height, true);
        //scale background
        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta) {
        //default background color
        ScreenUtils.clear(0.78f, 0.11f, 0.39f, 0.7f);

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

        //if game is over, draw stage (exit button)
        if (state == State.Over) {
            if (manager.getMusicPref())
                game.stopMusic();
            stage.act(delta);
            stage.draw();
            Gdx.input.setInputProcessor(stage);
        }
        //else draw hud stage
        else {
            stageHud.act(delta);
            stageHud.draw();
            Gdx.input.setInputProcessor(stageHud);
        }
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

        //DRAW COMPUTERS
        //if 1 computer (2 players in game)
        if (numPlayers == 2) {
            //get computer player hand and call drawHand method
            Hand computerHand = computer1.getHand();
            drawHand(computerHand, 1,
                    sizeX, sizeY, numPlayers, false);
        }
        //if more than 1 computer
        else {
            //draw each computer player individually
            for (int i = 1; i < numPlayers; ++i) {
                Hand computerHand = playersData.get(i).getHand();
                drawHand(computerHand, i,
                        sizeX, sizeY, numPlayers, false);
            }
        }

        //DRAW PLAYER
        Hand playerHand = player.getHand();
        playerHand.setArrowRegions(gameplayAtlas.findRegion(RegionNames.arrow));    //set arrow textures
        drawHand(playerHand, 0,
                sizeX, sizeY, numPlayers, true);

        //draw color wheel if State is set to Choosing
        if (state == State.Choosing) {
            drawColorWheel();
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
            //Gdx.app.log("SizeLeft:","StartX: " + startX);

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
                    float posX = (startX + (j - firstIndex) * spacing)-1f;
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
        //Draw info of players (name and number of cards)
        if(state == State.Running || state == State.Over || state == State.Choosing) {
            //set the font to a smaller scale for the player's text
            font.getData().setScale(0.8f);  //scale down to 80% of the original size

            float startX = 10f; //width left + margin
            float startY = hudViewport.getWorldHeight()-10f; //top + margin
            //height of each line of font + spacing
            float lineHeight = font.getXHeight();

            //define outline offsets and outline color (double outline = cleaner look)
            float outlineOffset1 = 1.8f;  //offset for 1st outline (outer)
            float outlineOffset2 = 1.2f;  //offset for 2nd outline (inner)

            //y-axis position where to start drawing first Player text
            float playerY = startY;

            //iterate through each non-null Player to draw info text
            for (int i = 0; i < playersData.size(); ++i) {
                Player player = playersData.get(i);
                if (player != null) {
                    //get position based on index
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
                    String playerText = position + ": " + player;   //text to display per each line
                    //get vertical start of each new player
                    if(i!=0)
                        playerY = playerY - (lineHeight*2);

                    //set the outline color
                    font.setColor(Color.BLACK);

                    //draw the text multiple times to create an outline effect (up, down, left, right)
                    font.draw(batch, playerText, startX + outlineOffset1, playerY + outlineOffset1);  // Top-right
                    font.draw(batch, playerText, startX + outlineOffset2, playerY + outlineOffset2);  // Top-right
                    font.draw(batch, playerText, startX - outlineOffset1, playerY + outlineOffset1);  // Top-left
                    font.draw(batch, playerText, startX - outlineOffset2, playerY + outlineOffset2);  // Top-left
                    font.draw(batch, playerText, startX + outlineOffset1, playerY - outlineOffset1);  // Bottom-right
                    font.draw(batch, playerText, startX + outlineOffset2, playerY - outlineOffset2);  // Bottom-right
                    font.draw(batch, playerText, startX - outlineOffset1, playerY - outlineOffset1);  // Bottom-left
                    font.draw(batch, playerText, startX - outlineOffset2, playerY - outlineOffset2);  // Bottom-left

                    //draw the original text with different color
                    font.setColor(Color.WHITE);
                    font.draw(batch, playerText, startX, playerY);
                }
            }

            //reset the font scale back to its original size after drawing the players' text
            font.getData().setScale(1f, 1f);
        }
        //draw result text in middle
        if(state == State.Over){
            //set text and get size to correctly draw the text in the center of the screen
            String wonText = "Winner is: ";
            //get winner and their name
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

            //define outline offsets and outline color for big (regular size 1f) text (double outline = cleaner look)
            float bigOutlineOffset1 = 2.2f;  //offset for 1st outline (outer)
            float bigOutlineOffset2 = 1.6f;  //offset for 2nd outline (inner)

            //create new layout to draw winner text
            GlyphLayout wonLayout = new GlyphLayout();
            wonLayout.setText(font,wonText);
            float wonX = hudViewport.getWorldWidth()/2f - wonLayout.width/2f;
            float wonY = hudViewport.getWorldHeight()/2f + wonLayout.height/2f;

            //if there is a winner and the winner isn't a AI, set winning text to green outline, else blue
            if(winner!=Winner.None && !wonText.contains("Computer"))
                font.setColor(Color.FOREST);
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

            //display score of (main) player under winner
            int playerScore = playersData.get(0).getScore();
            String scoreText = "Your score: "+playerScore;
            wonLayout.setText(font,scoreText);
            font.draw(batch,scoreText,wonX,wonY);

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
            if (state == State.Over) break;
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
    private void calcPoints() {
        //iterate through Players in array
        for (Player currentPlayer : playersData) {
            if (currentPlayer != null) {
                int sumPoints = 0;
                //go through each other Player in array
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
            Player currentPlayer = playersData.get(playerTurn - 1);
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
                //current player isn't computer
                if (!Objects.equals(currentPlayer.getName(), "Computer")) {
                    //handle click on draw Deck
                    if (isClickedOnDeck(worldCoords.x, worldCoords.y, deckDraw)) {
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                            //play pickup sound
                            if (sfxPickup != null) {
                                sfxPickup.play(manager.getSoundVolumePref());
                            }
                            currentPlayer.getHand().pickCard(deckDraw); //add Card from draw Deck to Player's Hand
                            playerTurn = getNextTurn(playerTurn);   //iterate turn
                            playerPerformedAction = true;   //counts as action performed, so end Player's turn
                            handArrowRightClicked(currentPlayer.getHand()); //move hand index one to the right (because card was added)
                        }
                    }

                    //handle click on Cards inside Player's Hand
                    for (Card card : player.getHand().getCards()) {
                        if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                            //set highlight on Card that's hovered over (used in drawHand method)
                            card.setHighlight(true);
                            //clicked on Card within Hand
                            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                                if (sfxCollect != null) {
                                    sfxCollect.play(manager.getSoundVolumePref());
                                }
                                //call game logic on selected Card
                                //TODO: select multiple (same) cards (isHighlighted) and send all to discard Deck
                                gameControl(card, currentPlayer.getHand());
                                topCard = deckDiscard.getTopCard();

                                //move hand index left (removed card) - in gameControl
                                //handArrowLeftClicked(currentPlayer.getHand());
                                break;
                            }
                        } else {
                            card.setHighlight(false);   //set other (un-hovered) Cards to false
                        }
                    }
                }
                //current player is computer, call separate AI game logic
                else {
                    cardAIchoose(currentPlayer);
                }
                playerPerformedAction = false;  //reset global performed action to false (for next player)
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
                    changeTopDeckCard(card.getColor()); //switch texture of topCard
                    state = State.Running;  //reset state
                    choosingCards.clear();  //clear Choosing Cards array
                    playerTurn = getNextTurn(playerTurn);   //iterate to next turn
                }
            }
        }
    }

    /** Performs main game Card logic */
    private void gameControl(Card card, Hand hand) {
        //is selected Card same color or symbol as topCard
        if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
            //set selected card into discard Deck
            hand.setCard(card, deckDiscard);
            //if selected card is Special, perform special actions
            if (card.isSpecial()) {
                specialCardAction(card, hand, true);
            }
            //player performed turn (State check exists so turn doesn't increment when Player is choosing cards)
            if (state == State.Running) {
                playerPerformedAction = true;
                playerTurn = getNextTurn(playerTurn);
            }
            //move hand index left (removed card)
            handArrowLeftClicked(hand);
        }
    }

    /** Performs logic of Special Cards */
    private void specialCardAction(Card card, Hand hand, boolean isPlayer) {
        int index;
        String special = card.getSpecial(); //get which special Card was played
        int maxCardsShow = getMaxCardsShow();
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
                //if only 2 players, get next turn (same player plays twice)
                if(getPlayersSize()==2)
                    playerTurn = getNextTurn(playerTurn);
                break;
            //Plus 2
            case "P2":
                //get index of next turn Player
                index = getNextTurn(playerTurn);
                //Player draws 2 cards from draw Deck
                playersData.get(index - 1).getHand().pickCards(deckDraw, 2);
                //increment Player's lastIndex
                playersData.get(index - 1).getHand().lastIndexIncrement(2,maxCardsShow);
                break;
            //Plus 4
            case "P4":
                index = getNextTurn(playerTurn);
                playersData.get(index - 1).getHand().pickCards(deckDraw, 4);
                playersData.get(index - 1).getHand().lastIndexIncrement(4, maxCardsShow);
                break;
            //Rainbow
            default:
                //if Player played Card, set State to choosing
                if (isPlayer) {
                    //Gdx.app.log("PLAYED CHANGE COLOR", "player");
                    state = State.Choosing;
                }
                //else call AI choose color method
                else
                    AIchooseColor(hand);
        }
    }

    /** Computer AI main Card choosing logic */
    private void cardAIchoose(Player computer) {
        //create copy of AI Hand (so original Hand is not changed in case no valid Card is found)
        Hand phantomHand = new Hand(computer.getHand());
        Card card = null;
        while (true) {
            //depending on AI difficulty, choose different algorithms for how AI chooses Card
            switch (difficultyAI) {
                //AI difficulty 1
                case 1:
                    //10% chance for AI to draw from draw Deck instead of placing a Card into discard Deck
                    Random rnd = new Random();
                    //get random number between 1-10
                    int rndNumber = rnd.nextInt(10) + 1;
                    //if check succeeds: clear phantomHand, simulating no cards to play
                    if(rndNumber==1)
                        phantomHand.getCards().clear();
                    //else get random Card from hand
                    card = phantomHand.getRandomCard();
                    break;
                case 3:
                    //knows Player's Hand (hint: less priority on symbols/colors player has)
                    card = cardAIcheater(phantomHand);
                    break;
                default:
                    //get Card from Hand with the highest priority
                    card = phantomHand.getHighestPriorityCard();
            }

            //remove selected Card from copied Hand
            phantomHand.setCard(card, null);
            //check if selected Card is valid
            if (topCard.containsColor(card) || topCard.containsSymbol(card)) {
                //Card is valid, so set it onto discard Deck, update topCard and check if Card is special
                computer.getHand().setCard(card, deckDiscard);
                topCard = deckDiscard.getTopCard();
                if (card.isSpecial()) {
                    specialCardAction(card, computer.getHand(), false);
                }
                //move hand index left (removed card)
                handArrowLeftClicked(computer.getHand());
                playerPerformedAction = true;   //end current turn
                break;
            }
            //AI doesn't have valid Cards in Hand, draw new Card from draw Deck
            if (phantomHand.getCards().isEmpty()) {
                Random rnd = new Random();
                //get random number between 1-10
                int rndNumber = rnd.nextInt(10) + 1;
                //if 10% chance activates, difficulty 3 AI cheats by drawing valid Card from draw Deck
                if (rndNumber == 1 && difficultyAI == 3)
                    computer.getHand().pickSpecificCard(deckDraw, topCard);
                //if no valid Card exists in draw Deck, draw first Card
                else
                    computer.getHand().pickCard(deckDraw);
                //copy but only last Card (redundancy)
                //when the AI doesn't have any valid cards in the hand and then draws a card from the deck
                phantomHand = new Hand(computer.getHand().getLastCard());
                playerPerformedAction = true;   //counts as action performed, so AI doesn't try to play Card that was just drawn
                //move hand index right (draw card)
                handArrowRightClicked(computer.getHand());
                break;
            }
        }
        //AI set their last Card in Hand and won
        if (computer.getHand().getCards().isEmpty()) {
            playerPerformedAction = true;
            return;
        }
        //AI doesn't have valid Cards in copy Hand, so draw from draw Deck (and end turn)
        if (phantomHand.getCards().isEmpty()) {
            computer.getHand().pickCard(deckDraw);
            playerPerformedAction = true;
        }
        //if AI performed action (valid Card was set to discard Deck) iterate to next turn
        if (playerPerformedAction)
            playerTurn = getNextTurn(playerTurn);
    }

    /** AI difficulty 3 only: chooses Card to play based on least appearing Cards in next player's Hand */
    private Card cardAIcheater(Hand hand){
        //get Hand of next player
        int index = getNextTurn(playerTurn);
        Hand nextHand = playersData.get(index - 1).getHand();
        //If AI's Hand contains more Cards than next player, try playing special card (stop, reverse, +2/4)
        if(hand.getCards().size>nextHand.getCards().size){
            //save all special Cards into array
            Array<Card> specials = hand.getSpecialCards();
            if(!specials.isEmpty()) {
                //if multiple special cards can be played, first sort by value, then by priority
                if(specials.size>1) {
                    specials.sort((o1, o2) -> Integer.compare(o1.getValue(), o2.getValue()));
                    specials.sort((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
                }
                return specials.get(0); //select first special Card
            }
        }

        //Play non-special cards
        Array<String> colors = nextHand.getLeastUsedCardColors();   //get least used color from next player
        //if next player's Hand contains all possible colors, then skip color selection and choose Card by priority instead
        if(colors.isEmpty() || colors.size==4)
            return hand.getHighestPriorityCard();
        //check if any of those colors exist in AI's Hand and check only those Cards
        Array<Card> parsedCards = hand.keepColors(colors);
        if(!parsedCards.isEmpty()){
            //create Hand copy with parsed cards, because phantomHand must still contain those cards
            Hand parsedHand = new Hand(hand);
            parsedHand.setCards(parsedCards);
            //get special Card with lowest priority that has a color which next player's Hand doesn't contain
            Card card = parsedHand.getLowestPrioritySpecialCard();
            //card is null, meaning there is no Special Card in Hand, so choose regular Card instead
            if(card==null){
                //get highest valued regular card (numbered cards 1-9)
                return parsedHand.getHighestValueCard();
            }
            return card;
        }

        //no colors have been parsed, see if any special Cards are in AI's Hand and play them
        Card card = hand.getLowestPrioritySpecialCard();
        if(card==null){
            //if Hand has no special cards, play highest value regular card (values 1-9)
            return hand.getHighestValueCard();
        }
        return card;    //return lowest priority special Card
    }

    /** AI chooses new color of topCard */
    private void AIchooseColor(Hand hand) {
        //AI chooses based on difficulty
        switch (difficultyAI) {
            //difficulty 1: chooses random Card color
            case 1:
                changeTopDeckCard(hand.getRandomColor());
                break;
            //difficulty 3: chooses color which next player has least of
            case 3:
                //get Hand of next player
                int index = getNextTurn(playerTurn);
                Hand nextHand = playersData.get(index - 1).getHand();
                //get colors from next player's Hand has LEAST of
                Array<String> colors = nextHand.getLeastUsedCardColors();
                //no valid colors, get AI's most used colors instead
                if (colors.isEmpty()) {
                    changeTopDeckCard(hand.getMostUsedCardColor());
                }
                //only one valid color, choose this one
                else if (colors.size == 1) {
                    changeTopDeckCard(colors.get(0));
                }
                //multiple valid colors, use one that AI has most of
                else {
                    changeTopDeckCard(hand.getMostValuedCardColor());
                }
                break;
            //difficulty 2: chooses color which AI has most of
            default:
                changeTopDeckCard(hand.getMostUsedCardColor());
        }
    }

    /** Change texture of topCard so that it is same symbol but the color of newly selected color */
    private void changeTopDeckCard(String color) {
        topCard = Card.switchCard(deckDiscard.getSecondTopCard(), color);
    }

    /** Return turn index of next player */
    private int getNextTurn(int index) {
        do {
            if (!clockwiseOrder) {
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

    /** Get local playersData size (without null elements) */
    private int getPlayersSize() {
        int count = 0;
        for (Player player : playersData) {
            if (player != null)
                count++;
        }
        return count;
    }

    /** Get the maximum amount of cards to render in player's hand during game */
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
        stage.dispose();
        batch.dispose();
    }

    /** Render exit button(s) with scene2d */
    public Actor createExitButton(State state) {
        TextButton exitButton = new TextButton("Exit", skin);
        //when exit button is clicked, save current Players data (update Player score) and set current screen to MenuScreen
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");
                manager.saveDataToJsonFile(playersData);
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
