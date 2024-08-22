package com.srebot.uno.classes;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.srebot.uno.assets.RegionNames;
import com.srebot.uno.config.GameConfig;

import java.util.Arrays;
import java.util.Random;

public class Hand {
    private int id;
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
        this.id=0;
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

    //deserializer constructor
    public Hand(int id, int indexFirst, int indexLast, Array<Card> cards) {
        this.id = id;
        this.indexFirst=indexFirst;
        this.indexLast=indexLast;
        this.cards=cards;
        initRegions();
    }

    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

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

    public Vector2 getPositionArrowRegionLeft() {
        return positionArrowRegionLeft;
    }
    public Vector2 getPositionArrowRegionRight() {
        return positionArrowRegionRight;
    }
    public Rectangle getBoundsArrowRegionLeft() {
        return boundsArrowRegionLeft;
    }
    public Rectangle getBoundsArrowRegionRight() {
        return boundsArrowRegionRight;
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
    //set default (v tem classu)
    public void setIndexLast(){this.indexLast=this.cards.size-1;}

    public int getIndexFirst(){ return indexFirst;}
    //set default (v tem classu)
    public void setIndexFirst(){
        this.indexFirst=0;
    }

    //actual set
    public void firstIndexIncrement(){
        //da ne bo inkrementiral firstIndex ko imamo manj kot MaxCards v roki
        if(indexDiffValid())
            this.indexFirst++;
    }
    public void firstIndexDecrement(){
        if(this.indexFirst-1>=0)
            this.indexFirst--;
    }
    //actual set
    public void lastIndexIncrement(){
        if(this.indexLast+1<=this.cards.size-1)
            this.indexLast++;
    }
    public void lastIndexDecrement(){
        this.indexLast--;
        if(!indexDiffValid())
            this.indexLast++;
    }

    public void lastIndexIncrement(int num){
        int newLast = this.indexLast+num;
        this.indexLast=newLast;
        //ustrezno premikanje first/last index ko ti opponent vrze +2/+4 card
        if(newLast>=GameConfig.MAX_CARDS_SHOW) {
            this.indexFirst = indexLast - (GameConfig.MAX_CARDS_SHOW-1);
        }
    }
    //ne spreminjaj indekse ce difference med first in last index (za show) ni vec kot st. card ki prikazujes
    private boolean indexDiffValid(){
        if((this.indexLast+this.indexFirst)>=GameConfig.MAX_CARDS_SHOW-1)
            return true;
        return false;
    }

    public Array<Card> getCards(){
        return cards;
    }

    public String getHighestUsedCardColor(){
        //0-B, 1-R, 2-G, 3-Y
        Integer[] nums = new Integer[4];
        Arrays.fill(nums,0);
        for(Card card : cards){
            if(card.getColor().contains("B"))
                ++nums[0];
            if(card.getColor().contains("R"))
                ++nums[1];
            if(card.getColor().contains("G"))
                ++nums[2];
            if(card.getColor().contains("Y"))
                ++nums[3];
            /*
            if(card.getColor().equals("-")) {
                ++nums[0];
                ++nums[1];
                ++nums[2];
                ++nums[3];
            }*/
        }
        if(nums.length==0 || nums==null){
            throw new IllegalArgumentException("Array is null or empty");
        }
        int max=0;
        int index=-1;
        for(int i=0; i<nums.length;++i){
            if(nums[i]>max) {
                max = nums[i];
                index = i;
            }
        }
        switch(index){
            case 0:
                return "B";
            case 1:
                return "R";
            case 2:
                return "G";
            case 3:
                return "Y";
            default:
                return getRandomColor();
        }
    }

    public String getRandomColor(){
        String[] colors = new String[4];
        colors[0] = "B";
        colors[1] = "R";
        colors[2] = "G";
        colors[3] = "Y";
        Random rnd = new Random();
        int rndIndex = rnd.nextInt(colors.length);
        return colors[rndIndex];
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
        //first draw?: set lastIndex
        if(indexLast==-1)
            setIndexLast();
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
