package com.srebot.uno.classes;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.srebot.uno.Uno;
import com.srebot.uno.config.GameManager;

import java.util.Objects;

public class Deck {
    private GameManager manager;

    private int size;
    private Array<Card> cards;

    //za izbiranje na ekranu
    private Vector2 position;
    private Rectangle bounds;

    public Deck(int size, Uno game){
        this.size = size;
        this.cards = new Array<Card>(size);
        manager = game.getManager();
        position = new Vector2();
        bounds = new Rectangle();
    }

    public void generateRandom(){
        for(int i=0;i<size;++i){
            Card card = Card.generateRandom();
            cards.add(card);
        }
    }
    //dobi eno karto iz vrha kupa
    public Card pickCard(){
        Card card = cards.peek();
        cards.pop();
        return card;
    }
    //polozi eno karto na vrh kupa
    public void setCard(Card card){
        cards.add(card);
    }

    public void shuffleDeck(){
        cards.shuffle();
    }

    //dobi top karto iz deka
    public Card getTopCard(){
        return cards.peek();
    }

    //poglej ce je konec deka
    public boolean isEmpty(){
        if(cards.isEmpty())
            return true;
        return false;
    }

    //ustvari karte: 1-9, reverse in stop, plus in wildcard
    public void generateByRules(int numColor,int numSpecial,int numWild){
        //2 karti od 1-9 vseh barv
        //2 +2, 2 reverse, 2 stop
        //4 WC, 4 +4
        for(int i=0;i<numColor;++i){
            cards.add(Card.generateSpecific(CardValues.B1));
            cards.add(Card.generateSpecific(CardValues.B2));
            cards.add(Card.generateSpecific(CardValues.B3));
            cards.add(Card.generateSpecific(CardValues.B4));
            cards.add(Card.generateSpecific(CardValues.B5));
            cards.add(Card.generateSpecific(CardValues.B6));
            cards.add(Card.generateSpecific(CardValues.B7));
            cards.add(Card.generateSpecific(CardValues.B8));
            cards.add(Card.generateSpecific(CardValues.B9));
            cards.add(Card.generateSpecific(CardValues.R1));
            cards.add(Card.generateSpecific(CardValues.R2));
            cards.add(Card.generateSpecific(CardValues.R3));
            cards.add(Card.generateSpecific(CardValues.R4));
            cards.add(Card.generateSpecific(CardValues.R5));
            cards.add(Card.generateSpecific(CardValues.R6));
            cards.add(Card.generateSpecific(CardValues.R7));
            cards.add(Card.generateSpecific(CardValues.R8));
            cards.add(Card.generateSpecific(CardValues.R9));
            cards.add(Card.generateSpecific(CardValues.G1));
            cards.add(Card.generateSpecific(CardValues.G2));
            cards.add(Card.generateSpecific(CardValues.G3));
            cards.add(Card.generateSpecific(CardValues.G4));
            cards.add(Card.generateSpecific(CardValues.G5));
            cards.add(Card.generateSpecific(CardValues.G6));
            cards.add(Card.generateSpecific(CardValues.G7));
            cards.add(Card.generateSpecific(CardValues.G8));
            cards.add(Card.generateSpecific(CardValues.G9));
            cards.add(Card.generateSpecific(CardValues.Y1));
            cards.add(Card.generateSpecific(CardValues.Y2));
            cards.add(Card.generateSpecific(CardValues.Y3));
            cards.add(Card.generateSpecific(CardValues.Y4));
            cards.add(Card.generateSpecific(CardValues.Y5));
            cards.add(Card.generateSpecific(CardValues.Y6));
            cards.add(Card.generateSpecific(CardValues.Y7));
            cards.add(Card.generateSpecific(CardValues.Y8));
            cards.add(Card.generateSpecific(CardValues.Y9));
        }
        if(Objects.equals(manager.getPresetPref(), "All")) {
            for (int i = 0; i < numSpecial; ++i) {
                cards.add(Card.generateSpecific(CardValues.BS));
                cards.add(Card.generateSpecific(CardValues.GS));
                cards.add(Card.generateSpecific(CardValues.RS));
                cards.add(Card.generateSpecific(CardValues.YS));
                cards.add(Card.generateSpecific(CardValues.BR));
                cards.add(Card.generateSpecific(CardValues.GR));
                cards.add(Card.generateSpecific(CardValues.RR));
                cards.add(Card.generateSpecific(CardValues.YR));
            }
            for (int i = 0; i < numWild; ++i) {
                cards.add(Card.generateSpecific(CardValues.WC));
                cards.add(Card.generateSpecific(CardValues.P4));
                cards.add(Card.generateSpecific(CardValues.P2B));
                cards.add(Card.generateSpecific(CardValues.P2R));
                cards.add(Card.generateSpecific(CardValues.P2G));
                cards.add(Card.generateSpecific(CardValues.P2Y));
                cards.add(Card.generateSpecific(CardValues.P2GY));
                cards.add(Card.generateSpecific(CardValues.P2BR));
            }
        }
    }

    public Vector2 getPosition() {
        return position;
    }
    public Rectangle getBounds() {
        return bounds;
    }
    public void setPositionAndBounds(float x,float y,
                                     float sizeX, float sizeY){
        this.position.x = x;
        this.position.y = y;
        this.bounds.width = sizeX;
        this.bounds.height = sizeY;
    }
}
