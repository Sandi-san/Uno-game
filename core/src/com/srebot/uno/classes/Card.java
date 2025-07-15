package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

public class Card {
    private int id;
    private int handId;
    private int priority; //for AI
    private int value;
    private String color;
    private String texture;

    //for determining positions on screen
    private Vector2 position;
    private Rectangle bounds;
    private boolean isHighlighted;

    //default constructor
    public Card() {
        id = 0;
        handId = 0;
        priority = 0;
        value = 0;
        color = "";
        texture = "";
        position = new Vector2();
        bounds = new Rectangle();
        isHighlighted = false;
    }

    //copy constructor
    public Card(Card card) {
        position = new Vector2(card.position.x, card.position.y);
        bounds = new Rectangle(card.bounds.x, card.bounds.y,
                card.bounds.width, card.bounds.height);
        isHighlighted = false;
        priority = card.getPriority();
        value = card.getValue();
        color = card.getColor();
        texture = card.getTexture();
    }
    //deserializer constructor
    public Card(int id, int priority, int value, String color, String texture) {
        this.id = id;
        this.priority = priority;
        this.value = value;
        this.color = color;
        this.texture = texture;
        position = new Vector2();
        bounds = new Rectangle();
        isHighlighted = false;
    }


    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getHandId() {
        return handId;
    }
    public void setHandId(int handId) {
        this.handId = handId;
    }

    public int getPriority() {
        return priority;
    }

    public int getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    public String getTexture() {
        return texture;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean getHighlight() {
        return isHighlighted;
    }
    public void setHighlight(boolean value) {
        this.isHighlighted = value;
    }

    public void setPositionAndBounds(float x, float y,
                                     float sizeX, float sizeY) {
        this.position.x = x;
        this.position.y = y;
        this.bounds.width = sizeX;
        this.bounds.height = sizeY;
    }

    /** Sets color based on texture */
    public void setColor(String texture) {
        this.texture = texture;
        if (texture.contains("blue")) {
            this.color = "B";
        } else if (texture.contains("red")) {
            this.color = "R";
        } else if (texture.contains("green")) {
            this.color = "G";
        } else if (texture.contains("yellow")) {
            this.color = "Y";
        }
    }

    /** Render Card object with batch */
    public static void render(SpriteBatch batch, TextureRegion texture,
                              Card card) {
        batch.draw(texture, card.getPosition().x, card.getPosition().y,
                card.getBounds().width, card.getBounds().height);
    }
    /** Render Card with batch */
    public static void render(SpriteBatch batch, TextureRegion texture,
                              float x, float y,
                              float sizeX, float sizeY) {
        batch.draw(texture, x, y,
                sizeX, sizeY);
    }
    /** Render Card object with batch and rotation */
    public static void renderFlipped(SpriteBatch batch, TextureRegion texture,
                              Card card, int rotationScalar) {
            batch.draw(texture, card.getPosition().x, card.getPosition().y,
                    card.getBounds().width/2f,card.getBounds().height/2f,
                    card.getBounds().width, card.getBounds().height,1f,1f,rotationScalar*90f);
    }

    /** Check if two Cards contain the same (or valid) color */
    public boolean containsColor(Card card) {
        if(card!=null) {
            String color = card.getColor();
            //one of the Cards is any color
            if (color.equals("-") || this.color.equals("-"))
                return true;
            //Cards contain the same color
            if (color.contains(this.color) || this.color.contains(color))
                return true;
        }
        return false;
    }

    /** Check if two Cards contain the symbol color */
    public boolean containsSymbol(Card card) {
        if(card!=null) {
            int value = card.getValue();
            //check if both Cards are normal cards and contain same value
            if (value < 10 && value == this.value)
                return true;

            String texture = card.getTexture();
            //check if both Cards are Stop Cards
            if (texture.contains("stop") && this.texture.contains("stop"))
                return true;
            //check if both Cards are Reverse Cards
            if (texture.contains("Reverse") && this.texture.contains("Reverse"))
                return true;
            //check if both Cards are (colored) Plus 2 Cards
            if (texture.contains("plus2") && this.texture.contains("plus2"))
                return true;
        }
        return false;
    }

    public boolean isSpecial() {
        if (this.value > 9)
            return true;
        return false;
    }

    /** Return simplified type of Special Card */
    public String getSpecial() {
        if (this.texture.contains("stop"))
            return "S";
        if (this.texture.contains("Reverse"))
            return "R";
        if (this.texture.contains("plus2"))
            return "P2";
        if (this.texture.contains("plus4"))
            return "P4";
        return "-"; //Card is Rainbow Card
    }

    /** Generate random Card from CardValues values */
    public static Card generateRandom() {
        //get random enum
        CardValues[] vals = CardValues.values();
        CardValues randVals = vals[new Random().nextInt(vals.length)];
        Card card = new Card();
        card.priority = randVals.getPriority();
        card.value = randVals.getValue();
        card.color = randVals.getColor();
        card.texture = randVals.getTexture();

        return card;
    }

    /** Generate Card with specific CardValues value */
    public static Card generateSpecific(CardValues value) {
        Card card = new Card();
        card.priority = value.getPriority();
        card.value = value.getValue();
        card.color = value.getColor();
        card.texture = value.getTexture();

        return card;
    }

    /** Get Card with same value but different color */
    public static Card switchCard(Card oldCard, String color) {
        Card newCard = new Card(oldCard);
        newCard.color = color;
        newCard.texture = CardValues.switchTexture(oldCard, newCard);
        return newCard;
    }

    /** Return values of Card as string (debug only) */
    @Override
    public String toString() {
        String priority = String.valueOf(this.getPriority());
        String value = String.valueOf(this.getValue());
        String color = this.getColor();
        String texture = this.getTexture();
        String position = String.valueOf(this.getPosition());
        String bounds = String.valueOf(this.getBounds());
        String isHighlighted = String.valueOf(this.getHighlight());
        return "\"" + texture + "\"" + " (" + color + ", " + value + ") "
                + "Priority: " + priority
                + " Position: " + position + " Bounds: " + bounds
                + " Hightlight? " + isHighlighted;
    }
}
