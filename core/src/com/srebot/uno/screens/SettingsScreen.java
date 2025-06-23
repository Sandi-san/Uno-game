package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragScrollListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.srebot.uno.Uno;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.config.GameConfig;
import com.srebot.uno.config.GameManager;

import java.util.Random;

public class SettingsScreen extends ScreenAdapter {
    private final Uno game;
    private final AssetManager assetManager;
    private final GameManager manager;

    private Viewport viewport;
    private Stage stage;
    private OrthographicCamera backgroundCamera;
    private StretchViewport backgroundViewport;
    private SpriteBatch batch;

    private Skin skin;
    private TextureAtlas gameplayAtlas;
    private Sprite background;
    private Sound sfxPickup;
    private Sound sfxCollect;

    public SettingsScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
        manager = game.getManager();
        game.setMusicVolume(manager.getMusicVolumePref());
        if (manager.getMusicPref()) {
            game.playMusic();
        } else {
            game.stopMusic();
        }
        sfxPickup = assetManager.get(AssetDescriptors.PICK_SOUND);
        sfxCollect = assetManager.get(AssetDescriptors.SET_SOUND);
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

        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background2);
        background = new Sprite(backgroundRegion);
        background.setSize(viewport.getWorldWidth(), viewport.getWorldHeight());
        background.setPosition(0, 0);

        stage.addActor(createTable());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
        stage.getViewport().update(width,height,true);

        backgroundViewport.update(width, height, true);
        background.setSize(backgroundViewport.getWorldWidth(), backgroundViewport.getWorldHeight());
        background.setPosition(0, 0);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=200/255f;
        float g=255/255f;
        float b=0/255f;
        float a=0.7f; //prosojnost
        ScreenUtils.clear(r,g,b,a);

        backgroundViewport.apply();
        batch.setProjectionMatrix(backgroundCamera.combined);
        batch.begin();
        background.draw(batch);
        batch.end();

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

    private Actor createTable() {
        //TABELA
        final Table table = new Table();
        table.defaults().pad(20);

        final Table titleTable = new Table();
        titleTable.defaults();

        final Table settingsTable = new Table();
        settingsTable.defaults();

        final Table buttonTable = new Table();
        buttonTable.defaults();

        //TITLE
        //kot slika
        Image titleText = new Image(gameplayAtlas.findRegion(RegionNames.textSettings));
        Container titleContainer = new Container(titleText);
        //doloci velikost
        float sizeX = GameConfig.TEXT_WIDTH*0.5f;
        float sizeY = GameConfig.TEXT_HEIGHT*0.5f;
        titleContainer.setSize(sizeX,sizeY);
        titleText.setScaling(Scaling.fill);
        titleText.setSize(sizeX,sizeY);
        titleTable.add(titleContainer).width(sizeX).height(sizeY)
                .center().padBottom(15).row();

        //kot tekst (slabo skaliranje)
        /*
        Label titleText = new Label("LEADERBOARD",skin);
        titleText.setFontScale(4f);
        titleTable.add(titleText).padBottom(15).row();
        */
        titleTable.center();

        //BACKGROUND
        //TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background4);
        //table.setBackground(new TextureRegionDrawable(backgroundRegion));

        //DOBI VREDNOSTI IZ NASTAVITEV
        String namePref = manager.getNamePref();
        boolean soundPref = manager.getSoundPref();
        boolean musicPref = manager.getMusicPref();
        float soundVPref = manager.getSoundVolumePref();
        float musicVPref = manager.getMusicVolumePref();
        boolean introPref = manager.getPlayIntroPref();

        //USTVARI WIDGETE
        final TextField nameField = new TextField("",skin);
        nameField.setText(namePref);

        final CheckBox soundCheckBox = new CheckBox("Enable Sound", skin);
        soundCheckBox.setChecked(soundPref);
        final CheckBox musicCheckBox = new CheckBox("Enable Music", skin);
        musicCheckBox.setChecked(musicPref);
        final CheckBox introCheckBox = new CheckBox("Enable Intro", skin);
        introCheckBox.setChecked(introPref);

        final Slider musicVolumeSlider = new Slider(0f,1f,0.01f, false,skin);
        musicVolumeSlider.setValue(musicVPref);
        final Slider soundVolumeSlider = new Slider(0f,1f,0.01f, false,skin);
        soundVolumeSlider.setValue(soundVPref);

        Label nameLabel = new Label("Player name: ",skin);
        Label musicVolumeLabel = new Label("Music volume: ",skin);
        Label soundVolumeLabel = new Label("SFX volume: ",skin);

        //STYLING
        settingsTable.add(nameLabel).pad(10);
        settingsTable.add(nameField).pad(10).row();
        settingsTable.add(soundCheckBox).pad(10);
        settingsTable.add(musicCheckBox).pad(10).row();
        settingsTable.add(musicVolumeLabel).pad(10);
        settingsTable.add(musicVolumeSlider).pad(10).row();
        settingsTable.add(soundVolumeLabel).pad(10);
        settingsTable.add(soundVolumeSlider).pad(10).row();
        settingsTable.add(introCheckBox).colspan(2).center().pad(10).row();

        //dynamic spreminjanje music play glede na slider
        musicCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean value = musicCheckBox.isChecked();
                if(value){
                    game.playMusic();
                }
                else{
                    game.stopMusic();
                }
            }
        });

        //dynamic spreminjanje music volume glede na slider
        musicVolumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = musicVolumeSlider.getValue();
                game.setMusicVolume(value);
            }
        });

        //dynamic igraj sound effect ko user interakta z sliderjem
        soundVolumeSlider.addListener(new DragListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Return true to indicate we want to handle touchUp
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                float value = soundVolumeSlider.getValue();
                if(soundCheckBox.isChecked()){
                    Random rnd = new Random();
                    //get random number 0 or 1
                    int rndNumber = rnd.nextInt(2);
                    if(rndNumber==0)
                        sfxPickup.play(value);
                    else
                        sfxCollect.play(value);
                }
            }
        });

        final TextButton menuButton = new TextButton("Save and return", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.setNamePref(nameField.getText());
                manager.setSoundPref(soundCheckBox.isChecked());
                manager.setMusicPref(musicCheckBox.isChecked());
                manager.setMusicVolumePref(musicVolumeSlider.getValue());
                manager.setSoundVolumePref(soundVolumeSlider.getValue());
                manager.setPlayIntroPref(introCheckBox.isChecked());
                manager.savePrefs();
                game.setScreen(new MenuScreen(game));
            }
        });
        buttonTable.add(menuButton).row();

        table.add(titleTable).row();
        table.add(settingsTable).row();
        table.add(buttonTable);

        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }

    //TODO: add skipIntro?, remove card preset, AI diff, turn order
}
