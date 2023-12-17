package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
import com.srebot.uno.config.GameConfig;

import java.util.ArrayList;

public class SettingsScreen extends ScreenAdapter {

    Preferences prefs = Gdx.app.getPreferences("GameSettings");

    private final Uno game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    public SettingsScreen(Uno game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show(){
        viewport = new FitViewport(GameConfig.HUD_WIDTH,GameConfig.HUD_HEIGHT);
        stage = new Stage(viewport, game.getBatch());

        skin = assetManager.get(AssetDescriptors.UI_SKIN);
        gameplayAtlas = assetManager.get(AssetDescriptors.GAMEPLAY);

        stage.addActor(createLeaderboard());
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void resize(int width, int height){
        viewport.update(width,height,true);
    }

    @Override
    public void render(float delta){
        //doloci barve ozadja
        float r=200/255f;
        float g=255/255f;
        float b=0/255f;
        float a=0.7f; //prosojnost
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

    private Actor createLeaderboard() {
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
        Image titleText = new Image(gameplayAtlas.findRegion(RegionNames.textLeaderboard));
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
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background4);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));

        /*
        introButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new IntroScreen(game));
           }
        });
        */

        //TextureRegion menuBackgroundRegion = gameplayAtlas.findRegion(RegionNames.MENU_BACKGROUND);
        //buttonTable.setBackground(new TextureRegionDrawable(menuBackgroundRegion));

        //DOBI VREDNOSTI IZ NASTAVITEV
        String namePref = prefs.getString("currentPlayer","Player 1");
        String presetPref = prefs.getString("cardPreset","All");
        String starterPref = prefs.getString("starterPlayer","Player");
        String orderPref = prefs.getString("cardOrder","Clockwise");
        boolean soundPref = prefs.getBoolean("soundEnabled", true);
        boolean musicPref = prefs.getBoolean("musicEnabled", true);

        //PRIPRAVI SEZNAME ZA BOX
        String[] presetValues = new String[]{"All", "Numbers only"};
        String[] starterValues = new String[]{"Player", "Computer"};
        String[] orderValues = new String[]{"Clockwise", "Counter Clockwise"};

        //USTVARI WIDGETE
        final TextField nameField = new TextField("",skin);
        nameField.setText(namePref);
        final SelectBox<String> presetBox = new SelectBox<String>(skin);
        //NAPOLNI SEZNAM IN NASTAVI PRIKAZAN ELEMENT
        presetBox.setItems(presetValues);
        presetBox.setSelected(presetPref);
        final SelectBox<String> starterBox = new SelectBox<String>(skin);
        starterBox.setItems(starterValues);
        starterBox.setSelected(starterPref);
        final SelectBox<String> orderBox = new SelectBox<String>(skin);
        orderBox.setItems(orderValues);
        orderBox.setSelected(orderPref);

        final CheckBox soundCheckBox = new CheckBox("Enable Sound", skin);
        soundCheckBox.setChecked(soundPref);
        final CheckBox musicCheckBox = new CheckBox("Enable Music", skin);
        musicCheckBox.setChecked(musicPref);

        Label nameLabel = new Label("Player name: ",skin);
        Label presetLabel = new Label("Card preset: ",skin);
        Label starterLabel = new Label("Starting player: ",skin);
        Label orderLabel = new Label("Turn order: ",skin);

        settingsTable.add(nameLabel).pad(10);
        settingsTable.add(nameField).pad(10).row();
        settingsTable.add(presetLabel).pad(10);
        settingsTable.add(presetBox).pad(10).width(nameField.getWidth()).row();
        settingsTable.add(starterLabel).pad(10);
        settingsTable.add(starterBox).pad(10).width(nameField.getWidth()).row();
        settingsTable.add(orderLabel).pad(10);
        settingsTable.add(orderBox).pad(10).width(nameField.getWidth()).row();
        settingsTable.add(soundCheckBox).pad(10);
        settingsTable.add(musicCheckBox).pad(10).row();

        TextButton menuButton = new TextButton("Save and return", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                prefs.putString("currentPlayer", nameField.getText());
                prefs.putString("cardPreset", presetBox.getSelected());
                prefs.putString("starterPlayer", starterBox.getSelected());
                prefs.putString("cardOrder", orderBox.getSelected());
                prefs.putBoolean("soundEnabled", soundCheckBox.isChecked());
                prefs.putBoolean("musicEnabled", musicCheckBox.isChecked());
                prefs.flush();
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
}
