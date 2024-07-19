package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.srebot.uno.assets.RegionNames;

public class Hand {
    private Array<Card> cards;
    //index prve karte v cards array za izris
    private int indexFirst;
    //index zadnje karte v cards array za izris
    private int indexLast;

    //texture elementi za preklapljanje med Cardi v drawHands
    private TextureRegion arrowRegionLeft;
    private TextureRegion arrowRegionRight;
    //za izbiranje na ekranu
    private Vector2 positionArrowRegionLeft;
    private Rectangle boundsArrowRegionLeft;
    private Vector2 positionArrowRegionRight;
    private Rectangle boundsArrowRegionRight;

    public Hand(){
        this.cards = new Array<Card>();
        initIndexes();
        initRegions();
    }
    //copy constructor, ai
    public Hand(Hand hand){
        this.cards = new Array<Card>(hand.getCards());
        initIndexes();
    }
    //ai
    public Hand(Card lastCard) {
        Array<Card> cards = new Array<Card>();
        cards.add(lastCard);
        this.cards = cards;
        initIndexes();
    }

    private void initIndexes(){
        this.indexFirst=0;
        this.indexLast=this.cards.size-1;
    }
    private void initRegions(){
        positionArrowRegionLeft = new Vector2();
        positionArrowRegionRight = new Vector2();
        boundsArrowRegionLeft = new Rectangle();
        boundsArrowRegionRight = new Rectangle();
    }

    public void setArrowRegions(TextureAtlas.AtlasRegion region){
        this.arrowRegionRight = new TextureRegion(region);
        this.arrowRegionLeft = new TextureRegion(region);
        this.arrowRegionLeft.flip(true,false);
    }
    public void setArrowRegionLeft(float x,float y,
                                   float sizeX, float sizeY){
        this.positionArrowRegionLeft.x = x;
        this.positionArrowRegionLeft.y = y;
        this.boundsArrowRegionLeft.width = sizeX;
        this.boundsArrowRegionLeft.height = sizeY;
    }
    public void setArrowRegionRight(float x,float y,
                                     float sizeX, float sizeY){
        this.positionArrowRegionRight.x = x;
        this.positionArrowRegionRight.y = y;
        this.boundsArrowRegionRight.width = sizeX;
        this.boundsArrowRegionRight.height = sizeY;
    }
    public void renderArrowLeft(SpriteBatch batch){
        if(arrowRegionLeft!=null) {
            batch.draw(arrowRegionLeft,positionArrowRegionLeft.x,positionArrowRegionLeft.y,
                    boundsArrowRegionLeft.width,boundsArrowRegionLeft.height);
        }
    }
    public void renderArrowRight(SpriteBatch batch){
        if(arrowRegionRight!=null) {
            batch.draw(arrowRegionRight,positionArrowRegionRight.x,positionArrowRegionRight.y,
                    boundsArrowRegionRight.width,boundsArrowRegionRight.height);
        }
    }

    public int getIndexLast(){return this.indexLast;}
    //actual set
    public void setIndexLast(int index){this.indexLast=index;}
    //set default (v tem classu)
    public void setIndexLast(){
        this.indexLast=this.cards.size-1;
    }

    public int getIndexFirst(){return indexFirst;}
    //actual set
    public void setIndexFirst(int index){this.indexFirst=index;}
    //set default (v tem classu)
    public void setIndexFirst(){
        this.indexFirst=0;
    }

    public Array<Card> getCards(){
        return cards;
    }

    public void pickCard(Deck deck){
        if(!deck.isEmpty()) {
            Card card = deck.pickCard();
            cards.add(card);
            //setIndexLast();
        }
    }
    public void pickCards(Deck deck, int n){
        Card card;
        for(int i=0;i<n;++i){
            if(deck.isEmpty()) break;
            card = deck.pickCard();
            cards.add(card);
        }
        //setIndexLast();
    }
    public void setCard(Card card,Deck deck){
        if(deck!=null)
            deck.setCard(card);
        //odstrani iz hand
        int cardIndex = cards.indexOf(card,true);
        if(cardIndex != -1){
            cards.removeIndex(cardIndex);
        }
        //if(cards.size<indexLast)
        //    setIndexLast();
    }

    public Card getHighestPriorityCard(){
        int priority = 0;
        Card card = null;
        if(!cards.isEmpty()) {
            for (Card c : cards) {
                if (c.getPriority() > priority) {
                    priority = c.getPriority();
                    card=c;
                }
            }
        }
        return card;
    }
    public Card getLastCard(){
        if(!cards.isEmpty()){
            return cards.get(cards.size-1);
        }
        return null;
    }
    //dobi vsoto vseh tock kart v roki
    public int getSumCardPoints(){
        int value=0;
        for(Card card : cards){
            value+=card.getValue();
        }
        return value;
    }
}
