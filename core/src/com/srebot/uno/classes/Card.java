package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Random;

public class Card {
    private int priority; //AI
    private int value;
    private String color;
    private String texture;

    public int getPriority(){
        return priority;
    }
    public int getValue(){
        return value;
    }
    public String getColor(){
        return color;
    }
    public String getTexture(){
        return texture;
    }

    public static void render(SpriteBatch batch,TextureRegion texture,
                              float x,float y,
                              float sizeX, float sizeY){
        batch.draw(texture,x,y,
                sizeX,sizeY);
    }

    //generiraj random karto z CardValues vrednosti
    public static Card generateRandom(){
        //dobi random enum
        CardValues[] vals = CardValues.values();
        CardValues randVals = vals[new Random().nextInt(vals.length)];
        Card card = new Card();
        card.priority = randVals.getPriority();
        card.value = randVals.getValue();
        card.color = randVals.getColor();
        card.texture = randVals.getTexture();

        return card;
    }
    //generiraj random karto z CardValues vrednosti
    public static Card generateSpecific(CardValues value){
        Card card = new Card();
        card.priority = value.getPriority();
        card.value = value.getValue();
        card.color = value.getColor();
        card.texture = value.getTexture();

        return card;
    }
}
