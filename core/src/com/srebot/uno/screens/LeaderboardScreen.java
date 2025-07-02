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
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.classes.Player;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;
import com.srebot.uno.config.GameService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LeaderboardScreen extends ScreenAdapter {
    private final Uno game;
    private final AssetManager assetManager;
    private GameManager manager;
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

    public LeaderboardScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        service = game.getService();

        if(manager.getMusicPref()) {
            game.playMusic();
        }
        else{
            game.stopMusic();
        }
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
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background2);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        //combine font & skin to create font style
        fontSkin = new Label.LabelStyle(skin.get(Label.LabelStyle.class));
        fontSkin.font = font;

        //create leaderboard table and add to stage
        stage.addActor(createLeaderboard());
        //set focus for user input
        Gdx.input.setInputProcessor(stage);
    }


    /** Callback for fetching multiple Players scores data */
    public interface FetchPlayersScoresCallback {
        void onPlayersFetched(Player[] players);
    }

    /** Method for fetching multiple Players scores data */
    private void getMultiplayerLeaderboard(FetchPlayersScoresCallback callback) {
        service.fetchPlayersScores(new GameService.FetchPlayersScoresCallback() {
            @Override
            public void onSuccess(Player[] players) {
                Gdx.app.log("PLAYERS", "Players fetched: " + players.length);
                callback.onPlayersFetched(players);
            }

            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("PLAYERS", "FAILED: " + t);
                callback.onPlayersFetched(null);
            }
        });
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        stage.getViewport().update(width,height,true);
        //scale background
        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta){
        //default background
        ScreenUtils.clear(0.78f,1,0,0.7f);

        //draw background
        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

        //draw table
        viewport.apply();
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

    /** Method for populating ScrollPane display with Players data */
    private void updateListTable(List<Player> players, Table listTable, Label.LabelStyle fontSkin) {
        listTable.clearChildren();
        listTable.clear();
        listTable.add(new Label("Player", fontSkin)).pad(5).padLeft(10);
        listTable.add(new Label("Score", fontSkin)).pad(5).padRight(10);
        listTable.row();

        if (players.isEmpty()) {
            listTable.add(new Label("No data found", fontSkin)).pad(5).colspan(2).center();
            listTable.row();
        } else {
            for (Player player : players) {
                listTable.add(new Label(player.getName(), fontSkin)).pad(5).padLeft(10);
                listTable.add(new Label(String.valueOf(player.getScore()), fontSkin)).pad(5).padRight(10);
                listTable.row();
            }
        }
    }

    private Actor createLeaderboard() {
        //prepare tables
        final Table table = new Table();
        table.defaults().pad(20);

        final Table titleTable = new Table();
        titleTable.defaults();

        final Table scrollTable = new Table();
        scrollTable.defaults();

        final Table buttonGroupTable = new Table();
        buttonGroupTable.defaults();

        final Table buttonTable = new Table();
        buttonTable.defaults();

        //main title
        Image titleText = new Image(gameplayAtlas.findRegion(RegionNames.textLeaderboard));
        Container titleContainer = new Container(titleText);
        //set size
        float sizeX = GameConfig.TEXT_WIDTH*0.5f;
        float sizeY = GameConfig.TEXT_HEIGHT*0.5f;

        titleContainer.setSize(sizeX,sizeY);
        titleText.setScaling(Scaling.fill);
        titleText.setSize(sizeX,sizeY);
        titleTable.add(titleContainer).width(sizeX).height(sizeY)
                .center().padBottom(15).row();

        titleTable.center();

        TextButton menuButton = new TextButton("Back", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        //table for display within ScrollPane
        Table listTable = new Table(skin);
        listTable.setWidth(GameConfig.WIDTH/3f);
        listTable.setHeight(
                Math.min(GameConfig.HEIGHT/3f,150));

        final ScrollPane scrollPane = new ScrollPane(listTable,skin);
        scrollPane.setFadeScrollBars(false);

        //add background to ScrollPane
        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable paneBackground = new NinePatchDrawable(patch);
        scrollTable.setBackground(paneBackground);

        Container container = new Container(scrollPane);
        container.fillX();

        //Toggle buttons for switching between SP/MP data
        //get the default TextButtonStyle from skin
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle.class));

        //set custom "checked" appearance as default hover style that the skin uses
        buttonStyle.checked = buttonStyle.over;
        buttonStyle.checkedFontColor = buttonStyle.overFontColor != null
                ? buttonStyle.overFontColor
                : buttonStyle.fontColor;

        //create buttons using this style
        TextButton singleplayerButton = new TextButton("Singleplayer", buttonStyle);
        TextButton multiplayerButton = new TextButton("Multiplayer", buttonStyle);

        //create ButtonGroup for mutual exclusivity
        ButtonGroup<TextButton> toggleGroup = new ButtonGroup<>(singleplayerButton, multiplayerButton);
        toggleGroup.setMaxCheckCount(1);
        toggleGroup.setMinCheckCount(1);
        toggleGroup.setUncheckLast(true); //prevent deselecting the last button

        //select singleplayer by default
        singleplayerButton.setChecked(true);

        //add to layout
        buttonGroupTable.add(singleplayerButton).padRight(10);
        buttonGroupTable.add(multiplayerButton);

        //method for fetching local json data and populating list table
        Runnable loadSingleplayerData = () -> {
            List<Player> localData = manager.loadFromJson();
            Collections.sort(localData, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
            updateListTable(localData, listTable, fontSkin);
        };

        //button listener for fetching SP data
        singleplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button listener", "Displaying local data");
                loadSingleplayerData.run();
            }
        });

        //method for fetching database Players data and populating list table
        Runnable loadMultiplayerData = () -> {
            getMultiplayerLeaderboard(fetchedPlayers -> {
                if (fetchedPlayers != null) {
                    Gdx.app.log("LEADERBOARD DATA", "Fetch successful");
                    List<Player> multiplayerData = Arrays.asList(fetchedPlayers);
                    Collections.sort(multiplayerData, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
                    updateListTable(multiplayerData, listTable, fontSkin);
                } else {
                    Gdx.app.log("LEADERBOARD DATA", "Fetched data is null");
                    listTable.clearChildren();
                    listTable.add(new Label("Cannot connect to database.", fontSkin)).pad(10).colspan(2).center().expandY();
                }
            });
        };

        //button listener for fetching SP data
        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Button listener", "Displaying database data");
                loadMultiplayerData.run();
            }
        });

        //call singleplayer method automatically when actor loads
        loadSingleplayerData.run();

        scrollTable.add(container).expandX().fillX().row();
        scrollTable.center();

        buttonTable.add(buttonGroupTable).row();

        buttonTable.add(menuButton).row();
        buttonTable.center();

        table.add(titleTable).row();
        table.add(scrollTable).row();
        table.add(buttonTable);

        table.center();
        table.setFillParent(true);
        //table.pack();

        return table;
    }
}
