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
    private boolean isHighlighted;

    public Card(){
        position = new Vector2();
        bounds = new Rectangle();
        isHighlighted = false;
    }
    //copy
    public Card(Card card){
        position = new Vector2(card.position.x,card.position.y);
        bounds = new Rectangle(card.bounds.x,card.bounds.y,
                card.bounds.width,card.bounds.height);
        isHighlighted = false;
        priority = card.getPriority();
        value = card.getValue();
        color = card.getColor();
        texture = card.getTexture();
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
    public boolean getHighlight() {return isHighlighted;}
    public void setHighlight(boolean value) {this.isHighlighted =value;}
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

    public boolean containsColor(Card card){
        String color = card.getColor();
        //eden od card je anyColor
        if(color.equals("-") || this.color.equals("-"))
            return true;
        //vsaj en vsebuje color
        if(color.contains(this.color) || this.color.contains(color))
            return true;
        return false;
    }
    public boolean containsSymbol(Card card){
        //preveri karte z stevilkami
        int value = card.getValue();
        if(value<10 && value==this.value)
            return true;

        String texture = card.getTexture();
        //preveri stop karte
        if(texture.contains("stop") && this.texture.contains("stop"))
            return true;
        //preveri reverse karte
        if(texture.contains("Reverse") && this.texture.contains("Reverse"))
            return true;
        //preveri plus2 karte
        if(texture.contains("plus2") && this.texture.contains("plus2"))
            return true;
        return false;
    }
    //karta je posebna (ni regular stevilka)
    public boolean isSpecial(){
        if(this.value>9)
            return true;
        return false;
    }
    //ce je karta special, vrni katera je
    public String getSpecial(){
        //preveri stop karte
        if(this.texture.contains("stop"))
            return "S";
        //preveri reverse karte
        if(this.texture.contains("Reverse"))
            return "R";
        //preveri plus2 karte
        if(this.texture.contains("plus2"))
            return "P2";
        //preveri plus4 karte
        if(this.texture.contains("plus4"))
            return "P4";
        //preveri rainbow karte
        //if(this.texture.contains("rainbow"))
        //    return "-";
        return "-";
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
    //dobi karto z isto vrednostjo ampak drugacno barvo
    public static Card switchCard(Card oldCard, String color){
        Card newCard = new Card(oldCard);
        newCard.color = color;
        newCard.texture = CardValues.switchTexture(oldCard, newCard);
        return newCard;
    }

    //izpisi vrednosti karte kot string (debug only)
    public String asString(){
        String priority = String.valueOf(this.getPriority());
        String value = String.valueOf(this.getValue());
        String color = this.getColor();
        String texture = this.getTexture();
        String position = String.valueOf(this.getPosition());
        String bounds = String.valueOf(this.getBounds());
        String isHighlighted = String.valueOf(this.getHighlight());
        return "\""+texture+"\"" + " ("+color+", "+value+") "
                + "Priority: "+priority
                + " Position: "+position + " Bounds: "+bounds
                +" Hightlight? "+isHighlighted;
    }
}
