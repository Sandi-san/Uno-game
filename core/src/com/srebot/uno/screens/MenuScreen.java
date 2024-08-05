package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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

import java.util.Arrays;

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
        Table table = new Table();
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

        Table buttonTable = new Table();
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
        // Create a dialog
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                // Handle dialog result here if needed
            }
        };

        Label titleLabel = new Label("Multiplayer Games", fontSkin);
        //titleLabel.setFontScale(1); // Increase the title font size
        dialog.getTitleTable().add(titleLabel).padTop(12).padRight(32).left();
        //dialog.getTitleTable().add(titleLabel).left();

        // Add an exit icon to the top right of the dialog
        TextButton closeButton = new TextButton("x", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.hide();
            }
        });
        //dialog.getTitleTable().add(closeButton).right();
        dialog.getTitleTable().add(closeButton).padTop(12).padLeft(12).right();

        // Create a Table to hold either the list of games or a "No games found." message
        Table contentTable = new Table(skin);
        List<GameData> gamesList = new List<>(skin);

        //contentTable.defaults().pad(10);

        // Fetch games from backend
        service.fetchGames(new GameService.GameFetchCallback() {
            @Override
            public void onSuccess(GameData[] games) {
                Gdx.app.postRunnable(() -> {
                    if (games.length == 0) {
                        // Display "No games found." if the list is empty
                        contentTable.add(new Label("No games found.", fontSkin)).pad(10).colspan(2).center();
                        contentTable.row();
                    } else {
                        // Update the list with the fetched games
                        gamesList.setItems(games);

                        // Add the list to a ScrollPane
                        ScrollPane scrollPane = new ScrollPane(gamesList, skin);
                        scrollPane.setFadeScrollBars(false);

                        // Add the ScrollPane to the contentTable
                        contentTable.add(scrollPane).width(480).height(280).padTop(30).row(); // Adjusted size
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Gdx.app.postRunnable(() -> {
                    contentTable.add(new Label("Cannot connect to database.", fontSkin)).pad(10).colspan(2).center();
                    contentTable.row();
                });
            }
        });

        //med Title in ScrollPane
        dialog.getContentTable().add(contentTable).row();

        // Create buttons
        TextButton createGameButton = new TextButton("Create Game", skin);
        createGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("CREATING GAME", "CREATING GAME");
                // Handle create game action
                //createGame();
                //game.setScreen(new GameMultiplayerScreen(game));
            }
        });

        TextButton joinGameButton = new TextButton("Join Game", skin);
        joinGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("JOINING GAME", "JOINING GAME");
                // Handle join game action
                if(!gamesList.getItems().isEmpty()) {
                    GameData selectedGame = gamesList.getSelected();
                    if (selectedGame != null) {
                        Gdx.app.log("JOINING GAME", "JOINING GAME: "+selectedGame.getId());
                        //joinGame(selectedGame);
                        //game.setScreen(new GameMultiplayerScreen(game));
                    }
                }
            }
        });

        // Add buttons to the dialog
        dialog.button(createGameButton);
        dialog.button(joinGameButton);

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
