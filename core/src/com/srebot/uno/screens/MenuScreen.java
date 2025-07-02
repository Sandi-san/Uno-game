package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class MenuScreen extends ScreenAdapter {
    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;
    private final GameService service;

    private Viewport viewport;
    private Stage stage;
    private OrthographicCamera backgroundCamera;
    private StretchViewport backgroundViewport;
    private SpriteBatch batch;

    private Skin skin;
    private BitmapFont font;
    private Label.LabelStyle fontSkin;
    private TextureAtlas gameplayAtlas;
    private Sprite background;

    public MenuScreen(Uno game) {
        //set global vars
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();

        //set music
        if(manager.getMusicPref()) {
            game.setMusic(assetManager.get(AssetDescriptors.MAIN_MUSIC));
            game.playMusic();
            if(game.getMusic().isPlaying()){
                game.setMusicVolume(manager.getMusicVolumePref());
            }
        }
        else{
            game.stopMusic();
        }
        //create batch for scalable background image
        batch = new SpriteBatch();
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        backgroundCamera = new OrthographicCamera();
        backgroundViewport = new StretchViewport(GameConfig.HUD_WIDTH, GameConfig.HUD_HEIGHT, backgroundCamera);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
        font = assetManager.get(AssetDescriptors.UI_FONT);

        //create background image
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background3);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        //combine font & skin for font style
        fontSkin = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        fontSkin.font = font;

        stage.addActor(createMenu());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        stage.getViewport().update(width,height,true);
        //scalable background
        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta){
        ScreenUtils.clear(1,0.74f,0,0.5f);

        //draw background
        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

        //draw stage
        viewport.apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide(){dispose();}
    @Override
    public void dispose(){stage.dispose();}

    private Actor createMenu() {
        Table table = new Table(skin);
        table.defaults().pad(20);

        //title as image
        Image titleText = new Image(gameplayAtlas.findRegion(RegionNames.textTitle));
        Container titleContainer = new Container(titleText);
        //set size
        float sizeX = GameConfig.TEXT_WIDTH;
        float sizeY = GameConfig.TEXT_HEIGHT;
        titleContainer.setSize(sizeX,sizeY);
        titleText.setScaling(Scaling.fill);
        titleText.setSize(sizeX,sizeY);
        table.add(titleContainer).width(sizeX).height(sizeY)
                .center().row();

        //as tekst (bad scaling)
        //Label titleText = new Label("UNO",skin);
        //titleText.setFontScale(4f);

        //set buttons and listeners for methods
        TextButton playSPButton = new TextButton("Singleplayer", skin);
        playSPButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showCreateGameSingleplayerDialog();
            }
        });

        TextButton playMPButton = new TextButton("Multiplayer", skin);
        playMPButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMultiplayerDialog();
            }
        });

        TextButton leaderboardButton = new TextButton("Leaderboard", skin);
        leaderboardButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LeaderboardScreen(game));
            }
        });

        TextButton settingsButton = new TextButton("Settings", skin);
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SettingsScreen(game));
            }
        });

        TextButton quitButton = new TextButton("Quit", skin);
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.defaults();

        //buttonTable.add(titleText).padBottom(15).row();
        buttonTable.add(playSPButton).padBottom(15).expandX().fill().row();
        buttonTable.add(playMPButton).padBottom(15).expandX().fill().row();
        buttonTable.add(leaderboardButton).padBottom(15).fillX().row();
        buttonTable.add(settingsButton).padBottom(15).fillX().row();
        buttonTable.add(quitButton).fillX();
        buttonTable.center();

        table.add(buttonTable);
        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }

    /** Opens and displays dialog for Multiplayer games */
    private void showMultiplayerDialog() {
        //dynamic bool value for checking server connection availability
        AtomicBoolean serverConnected = new AtomicBoolean(false);

        //create the dialog
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
            }
        };

        Table titleTable = new Table(skin);

        Label titleLabel = new Label("Multiplayer Games", fontSkin);
        titleTable.add(titleLabel).padLeft(40).padTop(20).expandX().center();

        //add a close button to the top right of the dialog
        TextButton closeButton = new TextButton("X", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });
        titleTable.add(closeButton).padTop(20).padRight(5).right();

        //create table to hold the list of games
        Table contentTable = new Table(skin);

        List<GameData> gamesList = new List<>(skin);

        //fetching/loading status label
        Label fetchingLabel = new Label("Fetching games...", fontSkin);
        contentTable.add(fetchingLabel).pad(10).colspan(2).center().row();

        //Function to fetch games
        Runnable fetchGames = () -> {
            contentTable.clearChildren(); //clear the table
            contentTable.add(fetchingLabel).pad(10).colspan(2).center().row(); //add fetching label (loading)

            service.fetchGames(new GameService.FetchGamesCallback() {
                @Override
                public void onSuccess(GameData[] games) {
                    Gdx.app.postRunnable(() -> {
                        contentTable.clearChildren();

                        if (games.length == 0) {
                            contentTable.add(new Label("No games found.", fontSkin)).pad(10).colspan(2).center().expandY(); //ensure the label expands vertically
                        }
                        else {
                            gamesList.setItems(games);

                            ScrollPane scrollPane = new ScrollPane(gamesList, skin);
                            scrollPane.setFadeScrollBars(false);
                            contentTable.add(scrollPane).width(dialog.getWidth()).height(dialog.getHeight() * 0.6f); //60% of dialog height
                        }

                        serverConnected.set(true);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        contentTable.clearChildren();
                        contentTable.add(new Label("Cannot connect to database.", fontSkin)).pad(10).colspan(2).center().expandY();
                        serverConnected.set(false);
                    });
                }
            });
        };

        //automatically call fetchGames function (when dialog loads)
        fetchGames.run();

        //create refresh button
        TextButton refreshButton = new TextButton("Refresh", skin);
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fetchGames.run(); //re-fetch the games
            }
        });

        //Create button
        TextButton createGameButton = new TextButton("Create Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //open new game settings dialog if server connection is successful
                if (serverConnected.get()) {
                    showCreateGameMultiplayerDialog();
                    dialog.remove();
                } else {
                    Gdx.app.log("CANNOT CONNECT TO SERVER", "CANNOT CREATE GAME");
                    showMessageDialog("Cannot connect to server!");
                }
            }
        });

        //Join button
        TextButton joinGameButton = new TextButton("Join Game", skin);
        joinGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!gamesList.getItems().isEmpty() && serverConnected.get()) {
                    //get selected game within the list
                    GameData selectedGame = gamesList.getSelected();
                    if (selectedGame != null) {
                        //check if player can join (maximum number of players has not been filled)
                        Player[] gamePlayers = selectedGame.getPlayers();
                        if(gamePlayers.length>=selectedGame.getMaxPlayers()){
                            Gdx.app.log("ERROR", "CANNOT JOIN GAME: " + selectedGame.getId()
                            +". PLAYER SLOTS ARE FULL.");
                            showMessageDialog("Player slots are full.");
                            return;
                        }
                        //check if current player has same name as player already within the game
                        for(Player player : gamePlayers){
                            if(Objects.equals(player.getName(), manager.getNamePref())) {
                                Gdx.app.log("ERROR", "CANNOT JOIN GAME: " + selectedGame.getId()
                                        + ". PLAYER WITH SAME NAME IS ALREADY PLAYING.");
                                showMessageDialog("Player with same name is already playing.");
                                return;
                            }
                        }

                        //open MP game screen if successful
                        Gdx.app.log("JOINING GAME", "JOINING GAME: " + selectedGame.getId());
                        game.setScreen(new GameMultiplayerScreen(game,selectedGame.getId(),manager.getNamePref()));
                    }
                } else {
                    Gdx.app.log("CANNOT JOIN GAME", "NO GAME SELECTED");
                    showMessageDialog("No game selected.");
                }
            }
        });

        Table buttonTable = new Table(skin);

        //add refresh button in a separate row and position it to the right
        buttonTable.add(refreshButton).expandX().right().padRight(5).row();

        //create new row and center the create and join buttons
        Table centeredButtonTable = new Table(skin);
        centeredButtonTable.add(createGameButton).padRight(20);
        centeredButtonTable.add(joinGameButton);

        //add the centeredButtonTable to buttonTable and center it
        buttonTable.add(centeredButtonTable).colspan(2).center().padBottom(10);

        //add tables to dialog box
        //add title
        dialog.getContentTable().add(titleTable).expandX().fillX().row();
        //add content (scroll pane)
        dialog.getContentTable().add(contentTable).row();
        //add buttons
        dialog.getContentTable().add(buttonTable).expandX().fillX();

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        //show the dialog
        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH * 0.6f, GameConfig.HEIGHT * 0.6f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    /** Displays dialog for creating a new Multiplayer Game with settings */
    private void showCreateGameMultiplayerDialog() {
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
            }
        };
        Label titleLabel = new Label("Create game", fontSkin);
        dialog.getContentTable().add(titleLabel).padTop(20).center().expand().row();

        final Table settingsTable = new Table(skin);
        settingsTable.defaults();

        //prepare hardcoded values for boxes
        Integer[] numPlayerValues = new Integer[]{2,3,4};
        Integer[] deckSizeValues = new Integer[]{52,104,208};
        String[] presetValues = new String[]{"All", "Numbers only", "No Wildcards", "No Plus Cards"};
        String[] orderValues = new String[]{"Clockwise", "Counter Clockwise"};

        //create widgets
        //fill list and display selected element
        final SelectBox<Integer> numPlayerBox = new SelectBox<Integer>(skin);
        numPlayerBox.setItems(numPlayerValues);
        numPlayerBox.setSelected(numPlayerValues[0]);
        final SelectBox<Integer> deckSizeBox = new SelectBox<Integer>(skin);
        deckSizeBox.setItems(deckSizeValues);
        deckSizeBox.setSelected(deckSizeValues[1]);
        final SelectBox<String> presetBox = new SelectBox<String>(skin);
        presetBox.setItems(presetValues);
        final SelectBox<String> orderBox = new SelectBox<String>(skin);
        orderBox.setItems(orderValues);
        orderBox.setSelected(orderValues[0]);

        Label numPlayerLabel = new Label("Maximum players: ",skin);
        Label deckSizeLabel = new Label("Deck size: ",skin);
        Label presetLabel = new Label("Card preset: ",skin);
        Label orderLabel = new Label("Turn order: ",skin);

        //set styling
        float boxWidth = (float) Math.floor(GameConfig.WIDTH/5.3f);
        settingsTable.add(numPlayerLabel).pad(10);
        settingsTable.add(numPlayerBox).pad(10).width(boxWidth).row();
        settingsTable.add(deckSizeLabel).pad(10);
        settingsTable.add(deckSizeBox).pad(10).width(boxWidth).row();
        settingsTable.add(presetLabel).pad(10);
        settingsTable.add(presetBox).pad(10).width(boxWidth).row();
        settingsTable.add(orderLabel).pad(10);
        settingsTable.add(orderBox).pad(10).width(boxWidth).row();

        dialog.getContentTable().add(settingsTable).row();

        TextButton createGameButton = new TextButton("Create Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CREATING GAME", "CREATING GAME");
                //save selected settings into array and send into class
                final Array<String> args = new Array<String>();
                args.add(String.valueOf(numPlayerBox.getSelected()),String.valueOf(deckSizeBox.getSelected()),
                        presetBox.getSelected(),orderBox.getSelected());
                game.setScreen(new GameMultiplayerScreen(game,args));
            }
        });

        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CLOSING", "CLOSING");
                showMultiplayerDialog();
                dialog.remove();
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.add(createGameButton).left().padBottom(15);
        buttonTable.add(closeButton).right().padBottom(15);
        dialog.getContentTable().add(buttonTable);

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);
        //dialog.getContentTable().setSize(600,400);

        dialog.show(stage);
        //set the size of the dialog (changes when adding background image)
        dialog.setSize(GameConfig.WIDTH*0.6f, GameConfig.HEIGHT*0.6f);
        //center the dialog
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    /** Displays dialog for creating a new Singleplayer Game with settings */
    private void showCreateGameSingleplayerDialog() {
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
            }
        };
        Label titleLabel = new Label("Game Settings", fontSkin);
        dialog.getContentTable().add(titleLabel).padTop(20).center().expand().row();

        final Table settingsTable = new Table(skin);
        settingsTable.defaults();

        Integer[] numComputerValues = new Integer[]{1,2,3};
        Integer[] deckSizeValues = new Integer[]{52,104,208};
        String[] presetValues = new String[]{"All", "Numbers only", "No Wildcards", "No Plus Cards", "Custom"};
        String[] orderValues = new String[]{"Clockwise", "Counter Clockwise"};
        Integer[] AIDiffValues = new Integer[]{1,2,3};

        final SelectBox<Integer> numComputerBox = new SelectBox<Integer>(skin);
        numComputerBox.setItems(numComputerValues);
        numComputerBox.setSelected(numComputerValues[0]);
        final SelectBox<Integer> deckSizeBox = new SelectBox<Integer>(skin);
        deckSizeBox.setItems(deckSizeValues);
        deckSizeBox.setSelected(deckSizeValues[1]);
        final SelectBox<String> presetBox = new SelectBox<String>(skin);
        presetBox.setItems(presetValues);
        final SelectBox<String> orderBox = new SelectBox<String>(skin);
        orderBox.setItems(orderValues);
        orderBox.setSelected(orderValues[0]);
        final SelectBox<Integer> AIDiffBox = new SelectBox<Integer>(skin);
        AIDiffBox.setItems(AIDiffValues);
        AIDiffBox.setSelected(AIDiffValues[1]);

        Label numComputerLabel = new Label("Number of AIs: ",skin);
        Label AIDiffLabel = new Label("AI difficulty: ",skin);
        Label deckSizeLabel = new Label("Deck size: ",skin);
        Label presetLabel = new Label("Card preset: ",skin);
        Label orderLabel = new Label("Turn order: ",skin);

        Integer[] rulesValues = new Integer[]{1,1,1};

        //set button listener for opening dialog for custom deck
        TextButton customRulesButton = new TextButton("Custom Deck", skin);
        customRulesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cardRulesDialog(rulesValues);
                //dialog.remove();
            }
        });

        float boxWidth = (float) Math.floor(GameConfig.WIDTH/5.3f);
        settingsTable.add(numComputerLabel);
        settingsTable.add(numComputerBox).width(boxWidth).row();
        settingsTable.add(AIDiffLabel);
        settingsTable.add(AIDiffBox).width(boxWidth).row();
        settingsTable.add(deckSizeLabel);
        settingsTable.add(deckSizeBox).width(boxWidth).row();
        settingsTable.add(presetLabel);
        settingsTable.add(presetBox).width(boxWidth).row();
        settingsTable.add(orderLabel);
        settingsTable.add(orderBox).width(boxWidth).row();
        settingsTable.add(customRulesButton).colspan(2).center().width(boxWidth).row();

        dialog.getContentTable().add(settingsTable).row();

        TextButton createGameButton = new TextButton("Start Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("STARTING GAME", "STARTING GAME");
                final Array<String> args = new Array<String>();
                args.add(String.valueOf(numComputerBox.getSelected()),String.valueOf(AIDiffBox.getSelected()));
                args.add(String.valueOf(deckSizeBox.getSelected()),presetBox.getSelected(),orderBox.getSelected());
                args.add(String.valueOf(rulesValues[0]),String.valueOf(rulesValues[1]),String.valueOf(rulesValues[2]));
                game.setScreen(new GameSingleplayerScreen(game,args));
            }
        });

        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CLOSING", "CLOSING");
                dialog.remove();
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.add(createGameButton).left().padBottom(15);
        buttonTable.add(closeButton).right().padBottom(15);
        dialog.getContentTable().add(buttonTable);

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH*0.6f, GameConfig.HEIGHT*0.6f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    /** Displays dialog for generating custom Deck with specific card types */
    private void cardRulesDialog(Integer[] rulesValues){
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
            }
        };
        Label titleLabel = new Label("Use Custom Deck (values 0-9)", fontSkin);
        dialog.getContentTable().add(titleLabel).padTop(20).center().expand().row();

        final Table settingsTable = new Table(skin);
        settingsTable.defaults();

        //create widgets
        final TextField numColorField = new TextField("",skin);
        numColorField.setText("1");
        final TextField numSpecialField = new TextField("",skin);
        numSpecialField.setText("1");
        final TextField numWildField = new TextField("",skin);
        numWildField.setText("1");

        Label numColorLabel = new Label("Number of Normal cards: ",skin);
        Label numSpecialLabel = new Label("Number of Special cards: ",skin);
        Label numWildLabel = new Label("Number of Wild cards: ",skin);

        //styling
        float boxWidth = (float) Math.floor(GameConfig.WIDTH/5.3f);
        settingsTable.add(numColorLabel).pad(10);
        settingsTable.add(numColorField).pad(10).width(boxWidth).row();
        settingsTable.add(numSpecialLabel).pad(10);
        settingsTable.add(numSpecialField).pad(10).width(boxWidth).row();
        settingsTable.add(numWildLabel).pad(10);
        settingsTable.add(numWildField).pad(10).width(boxWidth).row();

        dialog.getContentTable().add(settingsTable).row();

        TextButton closeButton = new TextButton("OK", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //check validity of text box values before allowing user to progress
                try {
                    int colorValue = Integer.parseInt(numColorField.getText());
                    int specialValue = Integer.parseInt(numSpecialField.getText());
                    int wildValue = Integer.parseInt(numWildField.getText());
                    rulesValues[0] = colorValue;
                    rulesValues[1] = specialValue;
                    rulesValues[2] = wildValue;
                    for(int i=0;i<=2;++i) {
                        if (rulesValues[i] > 9)
                            rulesValues[i] = 9;
                        else if(rulesValues[i]<0)
                            rulesValues[i]=0;
                    }
                    if(rulesValues[0]==0){
                        numColorField.setText("1");
                        throw new IllegalArgumentException("Number of normal cards cannot be 0");
                    }
                    dialog.remove();
                }
                catch (NumberFormatException e){
                    Gdx.app.log("NumberFormatException",e.getMessage());
                    showMessageDialog("Cannot convert string to integer.");
                }
                catch (IllegalArgumentException e){
                    Gdx.app.log("ERROR",e.getMessage());
                    showMessageDialog(e.getMessage());
                }
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.add(closeButton).center().padBottom(15);
        dialog.getContentTable().add(buttonTable);

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH*0.5f, GameConfig.HEIGHT*0.4f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    /** Displays text dialog for displaying message for user */
    private void showMessageDialog(String messageText){
        Dialog dialog = new Dialog("",skin);
        Label labelText = new Label(messageText,fontSkin);
        dialog.getContentTable().add(labelText).padTop(20).center().expand().row();
        //background
        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);
        //button
        dialog.button("OK");
        dialog.show(stage);
        //size & center
        dialog.setSize(labelText.getWidth()+20f, GameConfig.HEIGHT*0.2f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }
}
