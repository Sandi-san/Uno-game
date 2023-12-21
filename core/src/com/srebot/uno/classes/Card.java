package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class Card {
    private int priority; //AI
    private int value;
    private String color;
    private String texture;

    //za izbiranje na ekranu
    private Vector2 position;
    private Rectangle bounds;
    private boolean isSelected;

    public Card(){
        position = new Vector2();
        bounds = new Rectangle();
        isSelected = false;
    }

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
    public Vector2 getPosition() {
        return position;
    }
    public Rectangle getBounds() {
        return bounds;
    }
    public boolean getSelection() {return isSelected;}
    public void setSelection(boolean value) {this.isSelected=value;}
    public void setPosition(Vector2 position) {
        this.position = position;
    }
    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }
    public void setPositionAndBounds(float x,float y,
                                     float sizeX, float sizeY){
        this.position.x = x;
        this.position.y = y;
        this.bounds.width = sizeX;
        this.bounds.height = sizeY;
    }

    public static void render(SpriteBatch batch, TextureRegion texture,
                              Card card){
        batch.draw(texture,card.getPosition().x, card.getPosition().y,
                card.getBounds().width, card.getBounds().height);
    }
    public static void render(SpriteBatch batch, TextureRegion texture,
                              float x, float y,
                              float sizeX, float sizeY){
        batch.draw(texture,x,y,
                sizeX,sizeY);
    }

    public boolean containsColor(String color){
        //eden od card je anyColor
        if(color.equals("-") || this.color.equals("-"))
            return true;
        //vsaj en vsebuje color
        if(color.contains(this.color) || this.color.contains(color))
            return true;
        return false;
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
