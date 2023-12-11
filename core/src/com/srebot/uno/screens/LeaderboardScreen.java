package com.srebot.uno.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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
import com.srebot.uno.config.GameConfig;

import java.util.ArrayList;

public class LeaderboardScreen extends ScreenAdapter {

    private final Uno game;
    private final AssetManager assetManager;

    private Viewport viewport;
    private Stage stage;

    private Skin skin;
    private TextureAtlas gameplayAtlas;

    public LeaderboardScreen(Uno game) {
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

        final Table scrollTable = new Table();
        scrollTable.defaults();

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
        TextureRegion backgroundRegion = gameplayAtlas.findRegion(RegionNames.background2);
        table.setBackground(new TextureRegionDrawable(backgroundRegion));

        /*
        introButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new IntroScreen(game));
           }
        });
        */

        TextButton menuButton = new TextButton("Back", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });

        //TextureRegion menuBackgroundRegion = gameplayAtlas.findRegion(RegionNames.MENU_BACKGROUND);
        //buttonTable.setBackground(new TextureRegionDrawable(menuBackgroundRegion));

        Table listTable = new Table(skin);

        //SEZNAM PODATKOV
        int n=15;
        for(int i=1;i<=n;i++){
            listTable.add(new Label("Player"+i,skin)).pad(5);
            listTable.add(new Label(String.valueOf(n*100-100*i),skin)).pad(5);
            listTable.row();
        }
        listTable.setWidth(GameConfig.WIDTH/3f);
        listTable.setHeight(
                Math.min(GameConfig.HEIGHT/3f,n*(10)));

        final ScrollPane scrollPane = new ScrollPane(listTable,skin);
        scrollPane.setFadeScrollBars(false);

        /*
        TextureRegion paneBackground = gameplayAtlas.findRegion(RegionNames.background1);
        scrollTable.setBackground(new TextureRegionDrawable(paneBackground));
        */
        NinePatch patch = new NinePatch(gameplayAtlas.findRegion(RegionNames.backgroundPane1));
        NinePatchDrawable paneBackground = new NinePatchDrawable(patch);
        scrollTable.setBackground(paneBackground);

        Container container = new Container(scrollPane);
        container.fillX();


        scrollTable.add(container).expandX().fillX().row();
        scrollTable.center();

        buttonTable.add(menuButton).row();
        buttonTable.center();


        //buttonTable.add(introButton).padBottom(15).expandX().fillX().row();
        //buttonTable.add(playButton).padBottom(15).expandX().fill().row();
        //buttonTable.add(leaderboardButton).padBottom(15).fillX().row();

        table.add(titleTable).row();
        table.add(scrollTable).expand().fill().row();
        table.add(buttonTable);

        /*
        table.add(titleTable).row();
        table.add(scrollTable).row();
        table.add(buttonTable).row();
        */
        table.center();
        table.setFillParent(true);
        table.pack();

        return table;
    }
}
