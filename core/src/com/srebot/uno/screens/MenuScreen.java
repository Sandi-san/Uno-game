package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
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
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.GameData;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;

import java.util.concurrent.atomic.AtomicBoolean;

public class MenuScreen extends ScreenAdapter {

    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;
    private final GameService service;
    private Music music;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private BitmapFont font;
    private Label.LabelStyle fontSkin;
    private TextureAtlas gameplayAtlas;


    public MenuScreen(Uno game) {
        //SET GLOBAL VARS
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();

        //SET MUSIC
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
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);
        font = assetManager.get(AssetDescriptors.UI_FONT);

        //kombiniraj font in skin za font-e
        fontSkin = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        fontSkin.font = font;

        stage.addActor(createMenu());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=255/255f; //=1
        float g=190/255f;
        float b=0/255f;
        float a=0.5f; //prosojnost
        ScreenUtils.clear(r,g,b,a);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void hide(){
        dispose();
    }
    @Override
    public void dispose(){
        stage.dispose();
    }

    private Actor createMenu() {
        //TABELA
        Table table = new Table(skin);
        table.defaults().pad(20);

        //TITLE
        //kot slika
        Image titleText = new Image(gameplayAtlas.findRegion(RegionNames.textTitle));
        Container titleContainer = new Container(titleText);
        //doloci velikost
        float sizeX = GameConfig.TEXT_WIDTH;
        float sizeY = GameConfig.TEXT_HEIGHT;
        titleContainer.setSize(sizeX,sizeY);
        titleText.setScaling(Scaling.fill);
        titleText.setSize(sizeX,sizeY);
        table.add(titleContainer).width(sizeX).height(sizeY)
                .center().row();

        //kot tekst (slabo skaliranje)
        //Label titleText = new Label("UNO",skin);
        //titleText.setFontScale(4f);

        //BACKGROUND
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background3);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));

        /*
        TextButton introButton = new TextButton("Intro screen", skin);
        introButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new IntroScreen(game));
           }
        });
        */

        TextButton playSPButton = new TextButton("Singleplayer", skin);
        playSPButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameSingleplayerScreen(game));
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
        //buttonTable.add(introButton).padBottom(15).expandX().fillX().row();
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

    private void showMultiplayerDialog() {
        AtomicBoolean serverConnected = new AtomicBoolean(false);

        // Create a dialog
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                // Handle dialog result here if needed
            }
        };

        Table titleTable = new Table(skin);

        Label titleLabel = new Label("Multiplayer Games", fontSkin);
        titleTable.add(titleLabel).padLeft(40).padTop(20).expandX().center();

        // Add an exit icon to the top right of the dialog
        TextButton closeButton = new TextButton("X", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });
        titleTable.add(closeButton).padTop(20).padRight(5).right();

        // Create a Table to hold the list of games or messages
        Table contentTable = new Table(skin);
        // Set the initial height
        //contentTable.setHeight(dialog.getHeight() * 0.75f);
        // Make the table expand and fill the available space
        //contentTable.defaults().expand().fill();

        List<GameData> gamesList = new List<>(skin);

        // Fetching status label
        Label fetchingLabel = new Label("Fetching games...", fontSkin);
        contentTable.add(fetchingLabel).pad(10).colspan(2).center().row();

        // Function to fetch games
        Runnable fetchGames = () -> {
            contentTable.clearChildren(); // Clear the table
            contentTable.add(fetchingLabel).pad(10).colspan(2).center().row(); // Add fetching label

            service.fetchGames(new GameService.FetchGamesCallback() {
                @Override
                public void onSuccess(GameData[] games) {
                    Gdx.app.postRunnable(() -> {
                        contentTable.clearChildren(); // Clear the table again

                        if (games.length == 0) {
                            contentTable.add(new Label("No games found.", fontSkin)).pad(10).colspan(2).center().expandY(); // Ensure the label expands vertically
                        } else {
                            gamesList.setItems(games);

                            ScrollPane scrollPane = new ScrollPane(gamesList, skin);
                            scrollPane.setFadeScrollBars(false);
                            contentTable.add(scrollPane).width(dialog.getWidth()).height(dialog.getHeight() * 0.6f);
                        }

                        serverConnected.set(true);
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Gdx.app.postRunnable(() -> {
                        contentTable.clearChildren(); // Clear the table
                        contentTable.add(new Label("Cannot connect to database.", fontSkin)).pad(10).colspan(2).center().expandY();
                        serverConnected.set(false);
                    });
                }
            });
        };

        // Call fetchGames initially
        fetchGames.run();

        // Create Refresh button
        TextButton refreshButton = new TextButton("Refresh", skin);
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fetchGames.run(); // Refresh the games list
            }
        });

        // Create buttons
        TextButton createGameButton = new TextButton("Create Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (serverConnected.get()) {
                    showCreateGameDialog();
                    dialog.remove();
                } else {
                    Gdx.app.log("CANNOT CONNECT TO SERVER", "CANNOT CREATE GAME");
                }
            }
        });

        TextButton joinGameButton = new TextButton("Join Game", skin);
        joinGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!gamesList.getItems().isEmpty() && serverConnected.get()) {
                    GameData selectedGame = gamesList.getSelected();
                    if (selectedGame != null) {
                        Gdx.app.log("JOINING GAME", "JOINING GAME: " + selectedGame.getId());
                    }
                } else {
                    Gdx.app.log("CANNOT CONNECT TO SERVER", "CANNOT JOIN GAME");
                }
            }
        });

        Table buttonTable = new Table(skin);

        // Add the refresh button in a separate row and position it to the right
        buttonTable.add(refreshButton).expandX().right().padRight(5).row();

        // Create a new row and center the create and join buttons
        Table centeredButtonTable = new Table(skin);
        centeredButtonTable.add(createGameButton).padRight(20);
        centeredButtonTable.add(joinGameButton);

        // Add the centeredButtonTable to buttonTable and center it
        buttonTable.add(centeredButtonTable).colspan(2).center().padBottom(10);

        //add tabels to dialog box
        //add title
        dialog.getContentTable().add(titleTable).expandX().fillX().row();
        //add content (scroll pane)
        dialog.getContentTable().add(contentTable).row();
        //add buttons
        dialog.getContentTable().add(buttonTable).expandX().fillX();

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        // Show the dialog
        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH * 0.6f, GameConfig.HEIGHT * 0.6f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    private void showCreateGameDialog() {
        // Create a dialog
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                // Handle dialog result here if needed
            }
        };
        Label titleLabel = new Label("Create game", fontSkin);
        dialog.getContentTable().add(titleLabel).padTop(20).center().expand().row();

        final Table settingsTable = new Table(skin);
        settingsTable.defaults();

        //PRIPRAVI SEZNAME ZA BOX
        Integer[] numPlayerValues = new Integer[]{2,3,4};
        Integer[] deckSizeValues = new Integer[]{52,104,208};
        String[] presetValues = new String[]{"All", "Numbers only"};
        String[] orderValues = new String[]{"Clockwise", "Counter Clockwise"};

        //USTVARI WIDGETE
        //NAPOLNI SEZNAM IN NASTAVI PRIKAZAN ELEMENT
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
        Label deskSizeLabel = new Label("Deck size: ",skin);
        Label presetLabel = new Label("Card preset: ",skin);
        Label orderLabel = new Label("Turn order: ",skin);

        //STYLING
        float boxWidth = (float) Math.floor(GameConfig.WIDTH/5.3f);
        settingsTable.add(numPlayerLabel).pad(10);
        settingsTable.add(numPlayerBox).pad(10).width(boxWidth).row();
        settingsTable.add(deskSizeLabel).pad(10);
        settingsTable.add(deckSizeBox).pad(10).width(boxWidth).row();
        settingsTable.add(presetLabel).pad(10);
        settingsTable.add(presetBox).pad(10).width(boxWidth).row();
        settingsTable.add(orderLabel).pad(10);
        settingsTable.add(orderBox).pad(10).width(boxWidth).row();

        dialog.getContentTable().add(settingsTable).row();

        // Create buttons
        TextButton createGameButton = new TextButton("Create Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CREATING GAME", "CREATING GAME");
                final Array<String> args = new Array<String>();
                args.add(String.valueOf(numPlayerBox.getSelected()),String.valueOf(deckSizeBox.getSelected()),
                        presetBox.getSelected(),orderBox.getSelected());
                game.setScreen(new GameMultiplayerScreen(game,args));
                //TODO: sendi notri variable iz box-ov
            }
        });

        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CLOSING", "CLOSING");
                // Handle join game action
                showMultiplayerDialog();
                dialog.remove();
            }
        });

        // Add buttons to the dialog
        Table buttonTable = new Table(skin);
        buttonTable.add(createGameButton).left().padBottom(15);
        buttonTable.add(closeButton).right().padBottom(15);
        dialog.getContentTable().add(buttonTable);

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);
        //dialog.getContentTable().setSize(600,400);

        // Show the dialog
        dialog.show(stage);
        // Set the size of the dialog (changes when adding background image)
        dialog.setSize(GameConfig.WIDTH*0.6f, GameConfig.HEIGHT*0.6f);
        // Center the dialog
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }
}
