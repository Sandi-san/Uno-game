package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
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
import com.srebot.uno.classes.Hand;
import com.srebot.uno.classes.PlayerData;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameScreen extends ScreenAdapter {
    //status igre
    public enum State{
        Running, Paused, Over
    }
    //kdo je zmagovalec?
    public enum Winner{
        Player, Computer, None
    }

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
    private BitmapFont font;

    State state;
    Winner winner;

    //globale za igro
    //deki za vlecenje in za opuscanje
    private int deckSize = 104; //MAX st. kart
    private Deck deckDraw;
    private Deck deckDiscard;
    //karta, ki je na vrhu discard kupa
    private Card topCard;
    //trenutni turn
    private int playerTurn;
    //vrstni red
    private boolean clockwiseOrder;
    //playerji
    private PlayerData player;
    private PlayerData computer;
    private List<PlayerData> playersData;


    public GameScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        state = State.Running;

        //music on?
        if(manager.getMusicPref()) {
            game.stopMusic();
            game.setMusic(assetManager.get(AssetDescriptors.GAME_MUSIC_1));
            game.playMusic();
        }
        else{
            game.stopMusic();
        }
        //sounds on?
        if(manager.getSoundPref()){
            sfxPickup = assetManager.get(AssetDescriptors.PICK_SOUND);
            sfxCollect = assetManager.get(AssetDescriptors.SET_SOUND);
        }
        getFirstTurn();
        getFirstRotation();

        font = assetManager.get(AssetDescriptors.UI_FONT);
        //shapeRenderer = new ShapeRenderer();
        //exitButton = new Rectangle();
        batch = new SpriteBatch();
        initGame();
    }

    @Override
    public void show(){
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH,GameConfig.WORLD_HEIGHT,camera);
        hudViewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(hudViewport, game.getBatch());


        //nastavi pozicijo kamere
        camera.position.set(GameConfig.WORLD_WIDTH/2f,
                GameConfig.WORLD_HEIGHT,0);
        camera.update();

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        stage.addActor(createExitButton());
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

        checkGamestate();
        handleInput();

        switch (state) {
            case Running:
                viewport.apply();
                //setProjectionMatrix - uporabi viewport za prikaz sveta (WORLD UNITS)
                batch.setProjectionMatrix(viewport.getCamera().combined);
                batch.begin();
                draw();
                batch.end();
                break;
            case Over:
                stage.act(delta);
                stage.draw();
                Gdx.input.setInputProcessor(stage);
                break;
        }
    }

    public void draw(){
        //TODO HUD

        switch (state) {
            case Running:
                //VELIKOST kart (v WORLD UNITS)
                float sizeX = GameConfig.CARD_HEIGHT;
                float sizeY = GameConfig.CARD_WIDTH;

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

                //DRAW PLAYER in COMPUTER HANDS
                Hand playerHand = player.getHand();
                drawHand(playerHand, 0,
                        sizeX, sizeY, true);
                Hand computerHand = computer.getHand();
                drawHand(computerHand, (GameConfig.WORLD_HEIGHT - sizeY),
                        sizeX, sizeY, false);
                break;
            case Over:
                //DRAW EXIT BUTTON
                //drawExitButton();
                break;
        }
    }
    private void drawHand(Hand hand, float startY, float sizeX,float sizeY, boolean isPlayer){
        Array<Card> cards = hand.getCards();
        int size = cards.size;

        //TODO popravi da se ne bo vec levo izrisal ce je ze max levo (pri veliko kartih)

        float overlap = 0f;
        for(int i=5;i<size;++i)
            overlap+=0.1f;
        //overlap = sizeX*overlap;
        overlap = sizeX*(1-overlap);
        float startX;
        float spacing = sizeX;

        //doloci spacing ce vec kart v roki
        if(size<=5){
            startX = (GameConfig.WORLD_WIDTH - size * sizeX) / 2f;
        }
        else {
            spacing = overlap;
            startX = (GameConfig.WORLD_WIDTH - size * sizeX)/2f;
        }
        if(startX<0)
            startX=0;

        Array<Integer> indexHover = new Array<Integer>();
        //narisi karte (ki niso hoverane)
        for(int i=0;i<size;++i) {
            Card card = cards.get(i);
            String texture;
            TextureRegion region;
            if (!card.getHighlight()) {
                if (isPlayer) {
                    texture = card.getTexture();
                    region = gameplayAtlas.findRegion(texture);
                } else {
                    region = gameplayAtlas.findRegion(RegionNames.back);
                }
                float posX = startX + i * spacing;
                float posY = startY;
                card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                Card.render(batch, region, card);
            }
            else{
                indexHover.add(i);
            }
        }
        //narisi karto ki je hoverana
        if(!indexHover.isEmpty()) {
            for(int j : indexHover) {
                Card card = cards.get(j);
                String texture;
                TextureRegion region;
                if (isPlayer) {
                    texture = card.getTexture();
                    region = gameplayAtlas.findRegion(texture);
                } else {
                    region = gameplayAtlas.findRegion(RegionNames.back);
                }
                float posX = startX + j * spacing;
                float posY = startY + 1f; //slightly gor
                card.setPositionAndBounds(posX, posY, sizeX, sizeY);
                Card.render(batch, region, card);
            }
        }
    }
    /*
    public void drawExitButton(){
        float width=GameConfig.BUTTON_WIDTH;
        float height=GameConfig.BUTTON_HEIGHT;
        exitButton.setSize(width,height);
        exitButton.x=0;
        exitButton.y=0;
        exitButton.setPosition((GameConfig.WORLD_WIDTH - exitButton.width) / 2f,
                (GameConfig.WORLD_HEIGHT - exitButton.height) / 2f);

        //setProjectionMatirx - uporabi viewport za prikaz sveta (WORLD UNITS)
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.rect(exitButton.x,exitButton.y,exitButton.width,exitButton.height);
        shapeRenderer.end();

        //dodaj label na button
        boolean wasBatchDrawing = batch.isDrawing();
        if(wasBatchDrawing)
            batch.end();
        batch.begin();

        // Use GlyphLayout to calculate the width and height of the text
        GlyphLayout glyphLayout = new GlyphLayout();
        glyphLayout.setText(font, "Exit");

        // Scale the font to fit within the button bounds
        float textScaleX = exitButton.width / glyphLayout.width;
        float textScaleY = exitButton.height / glyphLayout.height;
        font.getData().setScale(Math.min(textScaleX, textScaleY));

        font.draw(batch,"Exit",exitButton.x,
                exitButton.y);
        // Reset the font scale to its original state
        font.getData().setScale(1f);

        if(!wasBatchDrawing)
            batch.end();
    }
     */

    private void checkGamestate(){
        if(deckDraw.isEmpty()) {
            state = State.Over;
            winner = Winner.None;
        }
        else if(player.getHand().getCards().isEmpty()) {
            state = State.Over;
            winner = Winner.Player;
        }
        else if(computer.getHand().getCards().isEmpty()) {
            state = State.Over;
            winner = Winner.Computer;
        }
        //calc pointe
    }

    private void gameControl(Card card, Hand hand){
        Gdx.app.log("Card",card.asString());
        int index;
        //iste barve ali simbola
        if(topCard.containsColor(card) || topCard.containsSymbol(card)){
            hand.setCard(deckDiscard,card);
            topCard = card;
            if(card.isSpecial()){
                String special = card.getSpecial();
                switch (special){
                    //Stop
                    case "S":
                        //dobi naslednjega playerja glede na turnOrder pref
                        //blokiraj njihov turn
                        playerTurn = getNextTurn(playerTurn);
                        break;
                    //Reverse
                    case "R":
                        //spremeni turnOrder
                        clockwiseOrder=!clockwiseOrder;
                        break;
                    //Plus 2
                    case "P2":
                        //dobi naslednjega playerja glede na turnOrder pref
                        //naj vlecejo +2
                        index = getNextTurn(playerTurn);
                        while(playersData.get(index-1)==null)
                            index = getNextTurn(index);
                        playersData.get(playerTurn-1).getHand().pickCards(deckDraw,2);
                        break;
                    //Plus 4
                    case "P4":
                        //dobi naslednjega playerja glede na turnOrder pref
                        //naj vlecejo +4
                        index = getNextTurn(playerTurn);
                        while(playersData.get(index-1)==null)
                            index = getNextTurn(index);
                        playersData.get(playerTurn-1).getHand().pickCards(deckDraw,4);
                        break;
                    //Rainbow
                    default:
                        //TODO select color screen da izberes nov color od topCard

                }
            }
        }
        index = getNextTurn(playerTurn);
        while(playersData.get(index-1)==null)
            index = getNextTurn(index);
        playerTurn = index;
    }
    private void handleInput() {
        //touch == phone touchscreen?
        //if (Gdx.input.justTouched()) {
        //za mouse
        //if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();

            //pretvori screen koordinate v world koordinate
            Vector2 worldCoords = viewport.unproject(new Vector2(touchX, touchY));

            if(state==State.Running) {
                if (isClickedOnDeck(worldCoords.x, worldCoords.y, deckDraw)) {
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        // Player clicked on this card
                        if (sfxPickup != null) {
                            sfxPickup.play();
                        }
                        player.getHand().pickCard(deckDraw);
                    }
                }
                for (Card card : player.getHand().getCards()) {
                    if (isClickedOnCard(worldCoords.x, worldCoords.y, card)) {
                       card.setHighlight(true);
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                            // Player clicked on this card
                            if(sfxCollect!=null){
                                sfxCollect.play();
                            }
                            //TODO izbira vec kart (isHighlighted) in posli vse v discard deck
                            gameControl(card,player.getHand());
                            break;
                            //state = State.Over;
                            // Perform other actions as needed
                        }
                    }
                    else{
                        card.setHighlight(false);
                    }
                }

                // Simulate computer's move
                //playComputerCard(computer.getRandomCard());
            }
            /*
            else if(state==State.Over) {
                //je kliknil na exit button?
                if (exitButton.contains(worldCoords)) {
                    manager.appendToJson(playersData);
                    game.setScreen(new MenuScreen(game));
                    return;
                }
            }
            */
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
    private boolean isClickedOnDeck(float mouseX, float mouseY, Deck deck) {
        // Check if the mouse click is within the bounds of the card
        // Implement the logic based on your card rendering and positioning
        // You may need to consider the card's position, size, and orientation
        // For simplicity, assuming the cards are arranged horizontally
        Vector2 position = deck.getPosition();
        Rectangle bounds = deck.getBounds();
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

    //s cigavim turn se igra zacne
    private void getFirstTurn(){
        /*
        1-bottom
        2-left
        3-top
        4-right
         */
        if(Objects.equals(manager.getStarterPref(), "Player"))
            playerTurn=1;
        else if(Objects.equals(manager.getStarterPref(), "Computer"))
            playerTurn=3;
    }
    //vrni naslednji turn index
    private int getNextTurn(int playerTurn){
        if(clockwiseOrder){
            if(playerTurn<4)
                return playerTurn+1;
            else
                return 1;
        }
        else {
            if(playerTurn>1)
                return playerTurn-1;
            else
                return 4;
        }
    }
    //dobi rotacijo
    private void getFirstRotation() {
        if (Objects.equals(manager.getOrderPref(), "Clockwise")) {
            clockwiseOrder=true;
        }
        else if (Objects.equals(manager.getOrderPref(), "Counter Clockwise")) {
            clockwiseOrder=false;
        }
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
        playersData = new ArrayList<>();
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

        //for each player dodaj v playerData
        //Pomembni order: bottom->left->top->right
        playersData.add(player); //bottom
        playersData.add(null);  //left
        playersData.add(computer); //top
        playersData.add(null);  //right
    }


    //z scene2d
    public Actor createExitButton(){
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
                manager.appendToJson(playersData);
                game.setScreen(new MenuScreen(game));
            }
        });

        Table buttonTable = new Table();
        buttonTable.defaults();

        //buttonTable.add(titleText).padBottom(15).row();
        //buttonTable.add(introButton).padBottom(15).expandX().fillX().row();
        buttonTable.add(exitButton).padBottom(15).expandX().fill().row();
        buttonTable.center();

        table.add(buttonTable);
        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }
}
