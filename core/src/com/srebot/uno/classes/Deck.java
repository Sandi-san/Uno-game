package com.srebot.uno.classes;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Deck {
    private int id;
    private int size;
    private Array<Card> cards;

    private Vector2 position;
    private Rectangle bounds;

    //constructor
    public Deck(int size){
        this.size = size;
        this.cards = new Array<Card>(true,size);
        position = new Vector2();
        bounds = new Rectangle();
    }

    //deserializer constructor (from gameService)
    public Deck(int id, int size, Array<Card> cards) {
        this.id = id;
        this.size = size;
        this.cards = cards;
        position = new Vector2();
        bounds = new Rectangle();
    }

    public Vector2 getPosition() {return position;}
    public Rectangle getBounds() {return bounds;}
    public void setPositionAndBounds(float x,float y,
                                     float sizeX, float sizeY){
        this.position.x = x;
        this.position.y = y;
        this.bounds.width = sizeX;
        this.bounds.height = sizeY;
    }

    public int getSize() {return size;}
    public Array<Card> getCards() {return cards;}

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

    /** Fill Deck with random Card Values */
    public void generateRandom(){
        for(int i=0;i<size;++i){
            Card card = Card.generateRandom();
            cards.add(card);
        }
    }

    /** Return (and remove) top Card from Deck */
    public Card pickCard(){
        Card card = cards.peek();
        cards.pop();
        return card;
    }

    /** Add one Card to top of Deck */
    public void setCard(Card card){
        cards.add(card);
    }

    /** Add multiple Cards to top of Deck */
    public void setCards(Array<Card> cardsToSet){
        for(Card card : cardsToSet){
            cards.add(card);
        }
    }

    /** Shuffle Cards in Deck */
    public void shuffleDeck(){
        cards.shuffle();
    }

    /** Get top Card from Deck (but don't remove) */
    public Card getTopCard(){
        return cards.peek();
    }

    /** Get second Card from top of Deck */
    public Card getSecondTopCard(){
        if (cards.size <= 1) throw new IllegalStateException("Array is empty.");
        return cards.get(cards.size - 2);
    }

    /** Check if Deck contains no Cards */
    public boolean isEmpty(){
        return cards.isEmpty();
    }

    /** Generate Cards (sets of 1-9 numbered, sets of reverse & stop, sets of plus & wildcard) */
    public void generateByRules(int numColor,int numSpecial,int numWild) {
        for (int i = 0; i < numColor; ++i) {
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

    /** Generate Cards: 1-9 numbered, reverse & stop, plus & wildcard (set size, preset of Cards to include) */
    public void generateBySize(int deckSize, String preset){
        int size = 2;
        switch (deckSize) {
            case 52:
                size=1;
                break;
            case 208:
                size=4;
                break;
            default:
                size=2;
        }
        //52: 1,1,1
        //104: 2,2,2
        //208: 4,4,4
        //2 Cards from 1-9 all colors
        //2 +2, 2 reverse, 2 stop
        //4 WC, 4 +4
        for(int i=0;i<size;++i){
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
        //generate Special & Wild Cards as well
        if(Objects.equals(preset, "All")) {
            for (int i = 0; i < size; ++i) {
                cards.add(Card.generateSpecific(CardValues.BS));
                cards.add(Card.generateSpecific(CardValues.GS));
                cards.add(Card.generateSpecific(CardValues.RS));
                cards.add(Card.generateSpecific(CardValues.YS));
                cards.add(Card.generateSpecific(CardValues.BR));
                cards.add(Card.generateSpecific(CardValues.GR));
                cards.add(Card.generateSpecific(CardValues.RR));
                cards.add(Card.generateSpecific(CardValues.YR));

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
        //all except Wild Cards
        else if(Objects.equals(preset, "No Wildcards")) {
            for (int i = 0; i < size; ++i) {
                cards.add(Card.generateSpecific(CardValues.BS));
                cards.add(Card.generateSpecific(CardValues.GS));
                cards.add(Card.generateSpecific(CardValues.RS));
                cards.add(Card.generateSpecific(CardValues.YS));
                cards.add(Card.generateSpecific(CardValues.BR));
                cards.add(Card.generateSpecific(CardValues.GR));
                cards.add(Card.generateSpecific(CardValues.RR));
                cards.add(Card.generateSpecific(CardValues.YR));

                cards.add(Card.generateSpecific(CardValues.P2B));
                cards.add(Card.generateSpecific(CardValues.P2R));
                cards.add(Card.generateSpecific(CardValues.P2G));
                cards.add(Card.generateSpecific(CardValues.P2Y));
                cards.add(Card.generateSpecific(CardValues.P2GY));
                cards.add(Card.generateSpecific(CardValues.P2BR));
            }
        }
        //all except Plus Cards
        else if(Objects.equals(preset, "No Plus Cards")) {
            for (int i = 0; i < size; ++i) {
                cards.add(Card.generateSpecific(CardValues.BS));
                cards.add(Card.generateSpecific(CardValues.GS));
                cards.add(Card.generateSpecific(CardValues.RS));
                cards.add(Card.generateSpecific(CardValues.YS));
                cards.add(Card.generateSpecific(CardValues.BR));
                cards.add(Card.generateSpecific(CardValues.GR));
                cards.add(Card.generateSpecific(CardValues.RR));
                cards.add(Card.generateSpecific(CardValues.YR));
                cards.add(Card.generateSpecific(CardValues.WC));
            }
        }
    }

    /** Remove specific Cards from Deck */
    public void removeCards(Array<Card> cards) {
        //create a set of IDs to remove (faster implementation)
        Set<Integer> cardsToRemove = new HashSet<>();
        for (Card card : cards) {
            cardsToRemove.add(card.getId());
        }
        int iters = 0;
        //iterate through the Deck in reverse and remove matching Cards
        for (int i = this.cards.size - 1; i >= 0; i--) {
            if (cardsToRemove.contains(this.cards.get(i).getId())) {
                this.cards.removeIndex(i);
                ++iters;
            }
            //prevent checking all Cards when necessary ones are removed
            if(iters>=cards.size)
                break;
        }
    }
}
