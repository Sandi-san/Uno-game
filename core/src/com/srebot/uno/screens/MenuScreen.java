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

import java.util.Date;
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

    //for displaying expiration time of valid access_token
    private Date tokenExpirationDate = null;
    //for saving Player object based on access_token
    private Player loggedPlayer = null;

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

    /** Gets access_token from local preferences and checks validity */
    private boolean accessTokenValid(){
        String token = manager.getAccessToken();
        tokenExpirationDate = new Date(manager.getTokenExpiration());
        Gdx.app.log("CHECK TOKEN", "Access token: " + token);
        Gdx.app.log("CHECK TOKEN", "Time: " + tokenExpirationDate);
        if(!Objects.equals(token, "")){
            //setFetchedPlayer();
            return true;
        }
        return false;
    }

    /** Save access_token and expiration to local preferences and set expiration Date in this class */
    private void saveAccessToken(String access_token){
        //when receiving access token, save it to preferences alongside expiration
        manager.setAccessToken(access_token);
        manager.setTokenExpiration();
        manager.savePrefs();
        //also set expiration Date in this class for display
        tokenExpirationDate = new Date(manager.getTokenExpiration());
        Gdx.app.log("TOKENS", "Access token: " + manager.getAccessToken());
        Gdx.app.log("TOKENS", "Time: " + tokenExpirationDate);
        //update loggedPlayer object by fetching Player from server
        if(!Objects.equals(access_token, ""))
            setFetchedPlayer();
    }

    /** Callback for fetching one Player */
    public interface PlayerFetchCallback {
        void onPlayerFetched(Player player);
    }

    /** Method for fetching Player object from server by authentication token */
    private void fetchPlayerFromBackend(PlayerFetchCallback callback) {
        service.fetchAuthenticatedPlayer(new GameService.PlayerFetchCallback() {
            @Override
            public void onSuccess(Player player) {
                Gdx.app.log("fetchPlayerFromBackend", "Player fetched: " + player.getName());
                //invoke the callback with the fetched Player
                callback.onPlayerFetched(player);
            }
            @Override
            public void onFailure(Throwable t) {
                Gdx.app.log("createPlayerFromBackend ERROR", "Failed to fetch player: " + t.getMessage());
                callback.onPlayerFetched(null);
            }
        }, manager.getAccessToken());
    }

    /** Fetch Player from server based on access_token and save into local Player object  */
    private void setFetchedPlayer() {
        fetchPlayerFromBackend(fetchedPlayer -> {
            if(fetchedPlayer != null) {
                Gdx.app.log("FETCHED PLAYER", fetchedPlayer.getName());
                loggedPlayer = fetchedPlayer;
            }
        });
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

        TextButton registerButton = new TextButton("Register", skin);
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //open register dialog
                showAuthDialog("Register",null);
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

        buttonTable.add(playSPButton).padBottom(10).expandX().fill().row();
        buttonTable.add(playMPButton).padBottom(10).expandX().fill().row();
        buttonTable.add(registerButton).padBottom(10).fillX().row();
        buttonTable.add(leaderboardButton).padBottom(10).fillX().row();
        buttonTable.add(settingsButton).padBottom(10).fillX().row();
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
                            contentTable.add(scrollPane).width(dialog.getWidth()).height(dialog.getHeight() * 0.5f); //60% of dialog height
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
                if (serverConnected.get() && accessTokenValid()) {
                    showCreateGameMultiplayerDialog();
                    dialog.remove();
                }
                else if(!accessTokenValid()) {
                    Gdx.app.log("CANNOT CREATE GAME", "MUST LOGIN FIRST");
                    showMessageDialog("You must login first!");
                }
                else if(!serverConnected.get()) {
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
                if (!gamesList.getItems().isEmpty() && serverConnected.get() && accessTokenValid()) {
                    //get selected game within the list
                    GameData selectedGame = gamesList.getSelected();
                    if (selectedGame != null) {
                        //check if player can join (maximum number of players has not been filled)
                        Player[] gamePlayers = selectedGame.getPlayers();
                        if (gamePlayers.length >= selectedGame.getMaxPlayers()) {
                            Gdx.app.log("ERROR", "CANNOT JOIN GAME: " + selectedGame.getId()
                                    + ". PLAYER SLOTS ARE FULL.");
                            showMessageDialog("Player slots are full.");
                            return;
                        }

                        //check if player has been logged
                        if(loggedPlayer!=null) {
                            //fetch Player from server based on authentication token
                            for (Player player : gamePlayers) {
                                //Gdx.app.log("GAME PLAYER", player.getName());
                                //check if current player has same name as player already within the game
                                if (Objects.equals(player.getName(), loggedPlayer.getName())) {
                                    Gdx.app.log("ERROR", "CANNOT JOIN GAME: " + selectedGame.getId()
                                            + ". PLAYER WITH SAME NAME IS ALREADY PLAYING.");
                                    showMessageDialog("Player with same name is already playing.");
                                    return;
                                }
                            }

                            //open MP game screen if successful
                            Gdx.app.log("JOINING GAME", "JOINING GAME: " + selectedGame.getId());
                            game.setScreen(new GameMultiplayerScreen(game, selectedGame.getId(), manager.getNamePref()));
                        }
                    }
                    else {
                        Gdx.app.log("CANNOT JOIN GAME", "NO GAME SELECTED");
                        showMessageDialog("No game selected.");
                    }
                }
                else if(!accessTokenValid()) {
                    Gdx.app.log("CANNOT CREATE GAME", "MUST LOGIN FIRST");
                    showMessageDialog("You must login first!");
                }
                else if(!serverConnected.get()) {
                    Gdx.app.log("CANNOT CONNECT TO SERVER", "CANNOT CREATE GAME");
                    showMessageDialog("Cannot connect to server!");
                }
                else {
                    Gdx.app.log("CANNOT JOIN GAME", "NO GAME SELECTED");
                    showMessageDialog("No game selected.");
                }
            }
        });

        Table buttonTable = new Table(skin);

        //add refresh button to separate row on right side
        buttonTable.add(refreshButton).expandX().right().padRight(5).row();

        //create new row and center the create and join buttons
        Table centeredButtonTable = new Table(skin);
        centeredButtonTable.add(createGameButton).padRight(20);
        centeredButtonTable.add(joinGameButton);

        //add the centeredButtonTable to buttonTable and center it
        buttonTable.add(centeredButtonTable).colspan(2).center().padBottom(5).row();

        //add tables to dialog box
        //add title
        dialog.getContentTable().add(titleTable).expandX().fillX().row();
        //add content (scroll pane)
        dialog.getContentTable().add(contentTable).row();
        //add buttons
        dialog.getContentTable().add(buttonTable).expandX().fillX().row();


        //create new table for login
        Table loginTable = new Table(skin);

        //Function to display time until expiration if token is already valid, else render button to login
        //set as a safe reference to allow self-reference (function calls itself inside it)
        final Runnable[] setLoginDisplay = new Runnable[1];

        //Function to display time until expiration if token is already valid, else render button to login
        setLoginDisplay[0] = () -> {
                loginTable.clearChildren(); //clear the table

                if(accessTokenValid()) {
                    //calculate difference between expiration time and current time
                    long differenceTime = manager.getTokenExpiration() - new Date().getTime();
                    if (differenceTime < 0) {
                        //if expiration is before current time, set access token to empty value (edge case)
                        saveAccessToken("");
                    }
                    Date differenceDate = new Date(differenceTime);
                    //if loggedPlayer object has not been set (when first opening the dialog), fetch from server and save locally
                    if (loggedPlayer == null) {
                        fetchPlayerFromBackend(fetchedPlayer -> {
                            //fetchedPlayer can be null if response from server is invalid
                            if(fetchedPlayer!=null) {
                                loggedPlayer = fetchedPlayer;
                                String expirationText = "Logged in as \"" + loggedPlayer.getName() + "\" for: ";
                                //set text display depending on if minutes or seconds are left on expiration timer
                                if (differenceDate.getMinutes() < 1)
                                    expirationText = expirationText + differenceDate.getSeconds() + " seconds.";
                                else {
                                    if (differenceDate.getHours() > 1)
                                        expirationText = expirationText + (differenceDate.getMinutes() + 60) + " minutes.";
                                    else
                                        expirationText = expirationText + differenceDate.getMinutes() + " minutes.";
                                }
                                //set expiration label and scale font to a smaller size
                                Label expirationLabel = new Label(expirationText, fontSkin);
                                expirationLabel.setFontScale(0.7f);
                                loginTable.add(expirationLabel).center();

                                //create logout button
                                TextButton logoutButton = new TextButton("Logout", skin);
                                logoutButton.addListener(new ClickListener() {
                                    @Override
                                    public void clicked(InputEvent event, float x, float y) {
                                        saveAccessToken("");
                                        setLoginDisplay[0].run();
                                    }
                                });
                                loginTable.add(logoutButton).right();
                            }
                        });
                    }
                    //loggedPlayer has already been set so no need for re-fetching
                    else {
                        String expirationText = "Logged in as \"" + loggedPlayer.getName() + "\" for: ";
                        //set text display depending on if minutes or seconds are left on expiration timer
                        if (differenceDate.getMinutes() < 1)
                            expirationText = expirationText + differenceDate.getSeconds() + " seconds.";
                        else {
                            if (differenceDate.getHours() > 1)
                                expirationText = expirationText + (differenceDate.getMinutes() + 60) + " minutes.";
                            else
                                expirationText = expirationText + differenceDate.getMinutes() + " minutes.";
                        }
                        //set expiration label and scale font to a smaller size
                        Label expirationLabel = new Label(expirationText, fontSkin);
                        expirationLabel.setFontScale(0.7f);
                        loginTable.add(expirationLabel).center();

                        //create logout button
                        TextButton logoutButton = new TextButton("Logout", skin);
                        logoutButton.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                saveAccessToken("");
                                setLoginDisplay[0].run();
                            }
                        });
                        loginTable.add(logoutButton).right();
                    }
                }
                else {
                    //create login button
                    TextButton loginButton = new TextButton("Login", skin);
                    loginButton.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (serverConnected.get()) {
                                //open login dialog
                                showAuthDialog("Login", setLoginDisplay[0]);
                            } else {
                                Gdx.app.log("CANNOT CONNECT TO SERVER", "CANNOT CREATE GAME");
                                showMessageDialog("Cannot connect to server!");
                            }
                        }
                    });

                    loginTable.add(loginButton).expandX().center();
                }
        };

        setLoginDisplay[0].run();
        dialog.getContentTable().add(loginTable).expandX().fillX();

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        //show the dialog
        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH * 0.6f, GameConfig.HEIGHT * 0.6f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) / 2, (stage.getHeight() - dialog.getHeight()) / 2);
    }

    /** Displays dialog for user authentication for register/login a Player for Multiplayer Game
     * @param state "Register" or "Login"
     * @param onClose Runnable method which is called when the dialog is closed. Can be null */
    private void showAuthDialog(String state, Runnable onClose){
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
            }
        };

        Label titleLabel = new Label("", fontSkin);
        //set main label depending on state (register or login)
        if(Objects.equals(state, "Login"))
            titleLabel.setText("Login with Account");
        else if(Objects.equals(state, "Register"))
            titleLabel.setText("Register Account");
        dialog.getContentTable().add(titleLabel).padTop(20).center().expand().row();

        final Table dataTable = new Table(skin);
        dataTable.defaults();

        //create widgets
        final TextField nameField = new TextField("",skin);
        final TextField passwordField = new TextField("",skin);
        //set password mode and character to password field, else characters will not be hidden
        passwordField.setPasswordCharacter('*');
        passwordField.setPasswordMode(true);

        Label nameLabel = new Label("Name: ",skin);
        Label passwordLabel = new Label("Password: ",skin);

        //styling
        float boxWidth = (float) Math.floor(GameConfig.WIDTH/5.3f);
        dataTable.add(nameLabel).pad(10);
        dataTable.add(nameField).pad(10).width(boxWidth).row();
        dataTable.add(passwordLabel).pad(10);
        dataTable.add(passwordField).pad(10).width(boxWidth).row();

        dialog.getContentTable().add(dataTable).row();

        TextButton confirmButton = new TextButton("Confirm", skin);
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //check validity of text box values before allowing user to progress
                if(passwordField.getText().length()<6) {
                    showMessageDialog("Password must be 6 or more characters.");
                    return;
                }
                if(nameField.getText().length()<1) {
                    showMessageDialog("Name must not be blank.");
                    return;
                }
                else {
                    //call Register route
                    if(Objects.equals(state, "Register")){
                        service.registerUser(nameField.getText(), passwordField.getText(), new GameService.AuthCallback() {
                            @Override
                            public void onSuccess(String accessToken) {
                                saveAccessToken(accessToken);
                                dialog.remove();
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                Gdx.app.log("REGISTER", "Register failed: " + t.getMessage());
                                //show error in UI
                                showMessageDialog(t.getMessage());
                            }
                        });
                    }
                    //call Login route
                    else if(Objects.equals(state, "Login")){
                        service.loginUser(nameField.getText(), passwordField.getText(), new GameService.AuthCallback() {
                            @Override
                            public void onSuccess(String accessToken) {
                                saveAccessToken(accessToken);
                                //close this dialog and run Runnable method (refreshes loginTable in showMultiplayerDialog)
                                dialog.remove();
                                if (onClose != null) onClose.run();
                            }
                            @Override
                            public void onFailure(Throwable t) {
                                Gdx.app.log("LOGIN", "Login failed: " + t.getMessage());
                                showMessageDialog(t.getMessage());
                            }
                        });
                    }
                }
            }
        });

        TextButton closeButton = new TextButton("Close", skin);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dialog.remove();
            }
        });

        Table buttonTable = new Table(skin);
        buttonTable.add(confirmButton).center().padBottom(15);
        buttonTable.add(closeButton).center().padBottom(15);
        dialog.getContentTable().add(buttonTable);

        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable dialogBackground = new NinePatchDrawable(patch);
        dialog.setBackground(dialogBackground);

        dialog.show(stage);
        dialog.setSize(GameConfig.WIDTH*0.5f, GameConfig.HEIGHT*0.4f);
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
        String[] orderValues = new String[]{"Counter Clockwise", "Clockwise"};

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
        String[] orderValues = new String[]{"Counter Clockwise", "Clockwise"};
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
                showCardRulesDialog(rulesValues);
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
    private void showCardRulesDialog(Integer[] rulesValues){
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
