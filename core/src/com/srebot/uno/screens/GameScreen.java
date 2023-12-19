package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
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
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.PlayerData;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends ScreenAdapter {

    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;

    private OrthographicCamera camera;
    private Viewport viewport;
    private Viewport hudViewport;

    private Stage stage;
    private SpriteBatch batch; //batch le en

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    private Music music;
    private Sound sfxPickup;
    private Sound sfxCollect;

    //globale za igro
    //deki za vlecenje in za opuscanje
    private int deckSize = 104; //MAX st. kart
    private Deck deckDraw;
    private Deck deckDiscard;
    //karta, ki je na vrhu discard kupa
    private Card topCard;
    //trenutni turn
    private String playerTurn;
    //playerji
    private PlayerData player;
    private PlayerData computer;
    private List<PlayerData> playerData;

    public GameScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();

        if(manager.getMusicPref()) {
            game.stopMusic();
            game.setMusic(assetManager.get(AssetDescriptors.GAME_MUSIC_1));
            game.playMusic();
        }
        else{
            game.stopMusic();
        }
        if(manager.getSoundPref()){
            sfxPickup = assetManager.get(AssetDescriptors.PICK_SOUND);
            sfxCollect = assetManager.get(AssetDescriptors.SET_SOUND);
        }
        playerTurn = manager.getOrderPref();

        batch = new SpriteBatch();
        initGame();
    }

    @Override
    public void show(){
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH,GameConfig.WORLD_HEIGHT,camera);
        hudViewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        //nastavi pozicijo kamere
        camera.position.set(GameConfig.WORLD_WIDTH/2f,
                GameConfig.WORLD_HEIGHT,0);
        camera.update();

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        //KO JE KONEC IGRE
        stage.addActor(createExitButton());
        //Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        hudViewport.update(width,height,true);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=200/255f;
        float g=30/255f;
        float b=100/255f;
        float a=0.7f; //prosojnost
        ScreenUtils.clear(r,g,b,a);

        gameControl();

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        draw();
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    public void draw(){
        //TODO HUD

        //VELIKOST kart (v WORLD UNITS)
        float sizeX = GameConfig.CARD_HEIGHT;
        float sizeY = GameConfig.CARD_WIDTH;

        //MIDDLE DECK
        String topCardTexture = topCard.getTexture();
        TextureRegion topCardRegion = gameplayAtlas.findRegion(topCardTexture);
        //POZICIJA
        float topX = (GameConfig.WORLD_WIDTH-sizeX)/2f;
        float topY = (GameConfig.WORLD_HEIGHT-sizeY)/2f;
        topCard.setPositionAndBounds(topX,topY,sizeX,sizeY);
        Card.render(batch, topCardRegion,topCard);

        //DRAW DECK
        TextureRegion drawDeckRegion = gameplayAtlas.findRegion(RegionNames.back);
        float drawX = (GameConfig.WORLD_WIDTH-sizeX);
        float drawY = (GameConfig.WORLD_HEIGHT-sizeY)/2f;
        Card.render(batch, drawDeckRegion,drawX,drawY,sizeX,sizeY);

        //DRAW PLAYER in COMPUTER HANDS
        Hand playerHand = player.getHand();
        drawHand(playerHand,0,
                sizeX, sizeY, true);
        Hand computerHand = computer.getHand();
        drawHand(computerHand,(GameConfig.WORLD_HEIGHT - sizeY),
                sizeX, sizeY, false);
    }
    private void drawHand(Hand hand, float startY, float sizeX,float sizeY, boolean isPlayer){
        Array<Card> cards = hand.getCards();
        int size = cards.size;

        float overlap = 0.2f;
        for(int i=5;i<size;++i)
            overlap+=0.2f;
        overlap = sizeX*overlap;
        float startX;
        float spacing;

        //doloci spacing ce vec kart v roki
        if(size<=5){
            spacing=sizeX;
            startX = (GameConfig.WORLD_WIDTH - size * spacing) / 2f;
        }
        else {
            spacing = overlap;
            startX = (GameConfig.WORLD_WIDTH - (size-1)*spacing)/2f;
        }
        //narisi karte
        for(int i=0;i<size;++i){
            Card card = cards.get(i);
            String texture;
            TextureRegion region;
            if(isPlayer) {
                texture = card.getTexture();
                region = gameplayAtlas.findRegion(texture);
            }
            else{
                region = gameplayAtlas.findRegion(RegionNames.back);
            }
            float posX = startX+i*spacing;
            float posY = startY;
            /*
            if (isPlayer) {
                posY = startY;
            } else {
                posY = GameConfig.WORLD_HEIGHT - sizeY - startY;
            }
             */
            card.setPositionAndBounds(posX,posY,sizeX,sizeY);
            Card.render(batch, region,card);
        }
    }

    private void gameControl(){
        handleInput();
        update(Gdx.graphics.getDeltaTime());
    }
    private void handleInput() {
        //touch == phone touchscreen?
        //if (Gdx.input.justTouched()) {
        //za mouse
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();

            //pretvori screen koordinate v world koordinate
            Vector3 worldCoords = viewport.unproject(new Vector3(touchX, touchY, 0));

            // Check if the player clicked on the exit button
            Actor hitActor = stage.hit(worldCoords.x, worldCoords.y, true);
            if (hitActor instanceof Label) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");

                manager.appendToJson(playerData);
                game.setScreen(new MenuScreen(game));
                return; // Exit the method to avoid processing card clicks
            }

            for(Card card : player.getHand().getCards()){
                if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                    // Player clicked on this card
                    playPlayerCard(card);
                    // Perform other actions as needed
                }
            }

            // Simulate computer's move
            //playComputerCard(computer.getRandomCard());

            //je kliknil na exit button?

        }
    }

    private boolean isClickedOnCard(float mouseX, float mouseY, Card card) {
        // Check if the mouse click is within the bounds of the card
        // Implement the logic based on your card rendering and positioning
        // You may need to consider the card's position, size, and orientation
        // For simplicity, assuming the cards are arranged horizontally
        Vector2 position = card.getPosition();
        Rectangle bounds = card.getBounds();
        return mouseX >= position.x && mouseX <= position.x + bounds.width
                && mouseY >= position.y && mouseY <= position.y + bounds.height;
    }

    private void playPlayerCard(Card card) {
        // Remove the card from the player's hand
        if(sfxCollect!=null){
            sfxCollect.play();
        }
        //player.getHand().removeCard(card);
        // Place the card on top of the discard deck
        //discardDeck.addCard(card);
        // Perform other actions related to playing the card
    }
/*
    private void playComputerCard(Card card) {
        // Remove the card from the computer's hand
        computer.getHand().removeCard(card);
        // Place the card on top of the discard deck
        discardDeck.addCard(card);
        // Perform other actions related to the computer playing the card
    }
    */
    private void update(float delta){}

    @Override
    public void hide(){
        dispose();
    }
    @Override
    public void dispose(){
        stage.dispose();
        /*
        batch.dispose();
        skin.dispose();
        gameplayAtlas.dispose();
        sfxPickup.dispose();
        sfxCollect.dispose();
         */
    }


    public void initGame(){
        playerData = new ArrayList<>();
        //USTVARI DECKE
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

        //USTVARI PLAYERJE
        //dobi iz jsona ce obstaja
        player = manager.getPlayerByName(manager.loadFromJson(),manager.getNamePref());
        Hand playerHand = new Hand();
        if(player==null){
            player = new PlayerData(manager.getNamePref(),0,playerHand);
        }
        else{
            player.setHand(playerHand);
        }
        player.getHand().pickCards(deckDraw,5);
        //computer
        Hand computerHand = new Hand();
        computer = new PlayerData("Computer",0,computerHand);
        computer.getHand().pickCards(deckDraw,5);

        //for each player dodaj v playerData (za json shranjevanje)
        playerData.add(player);
    }

    public Actor createExitButton(){
        Label label = new Label("Exit",skin);
        // Create a TextButton with "Exit" label and the skin
        TextButton exitButton = new TextButton("", skin);
        exitButton.add(label);
/*
        // Set a ClickListener to handle the button click event
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button Clicked", "Exit button clicked!");
                game.setScreen(new MenuScreen(game));
            }
        });
 */
        float width = 1f;
        float height = 1f;
        exitButton.setSize(width,height);

        // Add the exit button to a table for layout purposes
        Table table = new Table();
        table.setFillParent(true); // Make the table take up the whole stage
        table.left();
        table.add(exitButton);
        table.setWidth(width);
        table.setHeight(height);

        return table;
    }
}
