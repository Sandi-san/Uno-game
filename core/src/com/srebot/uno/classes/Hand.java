package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Arrays;
import java.util.Objects;
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
        if(this.arrowRegionRight==null)
            this.arrowRegionRight = new TextureRegion(region);
        if(this.arrowRegionLeft==null) {
            this.arrowRegionLeft = new TextureRegion(region);
            this.arrowRegionLeft.flip(true, false);
        }
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
    //Get last index: if last index is less
    public int getIndexLast(int maxCardsShow){
        fixIndexes(maxCardsShow);
        return this.indexLast;
    }
    //set default (v tem classu)
    public void setIndexLast(){this.indexLast=this.cards.size-1;}

    public int getIndexFirst(){ return indexFirst;}
    //set default (v tem classu)
    public void setIndexFirst(){
        this.indexFirst=0;
    }

    //actual set
    public void indexIncrement(int maxCardsShow){
        //da ne bo inkrementiral firstIndex ko imamo manj kot MaxCards v roki
        if(indexDiffValid(this.indexFirst,this.indexLast+1,maxCardsShow)) {
            //last index
            if(this.indexLast+1<=this.cards.size-1)
                this.indexLast++;
            //first index (if needed)
            if(!indexDiffValid(this.indexFirst,this.indexLast,maxCardsShow)){
                this.indexFirst++;
            }
        }
        else{
            if(!indexDiffValid(this.indexFirst,this.indexLast+1,maxCardsShow)){
                this.indexFirst++;
                //last index
                if(this.indexLast+1<=this.cards.size-1)
                    this.indexLast++;
            }
        }
    }

    //fix indexes if Hand has more cards in hand than maxCardsShow and
    //player was not at max lastIndex when setting card, resulting in
    //incorrect diffirence between first/last index (which should be maxCardsShow-1 on cards.size>maxCardsShow-1)
    private void fixIndexes(int maxCardsShow){
        if((this.indexLast-this.indexFirst==maxCardsShow-1) || (cards.size<=maxCardsShow-1))
            return;
        do {
            this.indexLast = this.indexLast+1;
        } while(this.indexLast-this.indexFirst!=maxCardsShow-1);
    }

    public void indexDecrement(int maxCardsShow){
        //over course game se lahko lastIndex unsync-a, popravi indekse
        fixIndexes(maxCardsShow);
        if(indexDiffValid(this.indexFirst,this.indexLast-1,maxCardsShow)) {
            //last index
            this.indexLast--;

            //first index (if needed)
            if(!indexDiffValid(this.indexFirst,this.indexLast,maxCardsShow)){
                //first index
                if (this.indexFirst - 1 >= 0)
                    this.indexFirst--;
            }
        }
        else {
            if(!indexDiffValid(this.indexFirst,this.indexLast-1,maxCardsShow)){
                //first index
                if (this.indexFirst - 1 >= 0)
                    this.indexFirst--;
                //last index
                this.indexLast--;
            }
        }
    }

    public void lastIndexIncrement(int num, int maxCardsShow){
        this.indexLast = this.indexLast+num;
        int diff = this.indexLast-this.indexFirst;
        //ustrezno premikanje first/last index ko ti opponent vrze +2/+4 card
        if(diff>=maxCardsShow) {
            this.indexFirst = this.indexLast - maxCardsShow + 1;
        }
    }
    //ne spreminjaj indekse ce difference med first in last index (za show) ni vec kot st. card ki prikazujes
    private boolean indexDiffValid(int first, int last, int maxCardsShow){
        if((last+first)<=maxCardsShow-1)
            return true;
        return false;
    }

    public Array<Card> getCards(){
        return cards;
    }
    public void setCards(Array<Card> newCards){
        this.cards=newCards;
    }

    public String getMostUsedCardColor(){
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

    public String getMostValuedCardColor(){
        //0-B, 1-R, 2-G, 3-Y
        Integer[] values = new Integer[4];
        Arrays.fill(values,0);
        for(Card card : cards){
            if(card.getColor().contains("B"))
                values[0] += card.getValue();
            if(card.getColor().contains("R"))
                values[1] += card.getValue();
            if(card.getColor().contains("G"))
                values[2] += card.getValue();
            if(card.getColor().contains("Y"))
                values[3] += card.getValue();
        }
        int max=0;
        int index=-1;
        for(int i=0; i<values.length;++i){
            if(values[i]>max) {
                max = values[i];
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

    public Array<String> getLeastUsedCardColors(){
        //0-B, 1-R, 2-G, 3-Y
        Array<String> colors = new Array<>();
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
        }

        //check which colors don't appear in Hand and add them to array
        for(int i=0; i<nums.length;++i){
            if(nums[i]==0){
                switch(i){
                    case 0:
                        colors.add("B");
                        break;
                    case 1:
                        colors.add("R");
                        break;
                    case 2:
                        colors.add("G");
                        break;
                    case 3:
                        colors.add("Y");
                        break;
                }
            }
        }
        //if any colors don't appear (0 appearances) return this array
        if(!colors.isEmpty())
            return colors;

        //all colors appear, check least appeared: multiple if same number
        int min=cards.size;
        for(int i=0; i<nums.length;++i){
            if(nums[i]<=min) {
                min = nums[i];
                switch(i){
                    case 0:
                        colors.add("B");
                        break;
                    case 1:
                        colors.add("R");
                        break;
                    case 2:
                        colors.add("G");
                        break;
                    case 3:
                        colors.add("Y");
                        break;
                }
            }
        }
        return colors;
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

    //remove all cards except those defined in colors array
    public Array<Card> keepColors(Array<String> colors){
        Array<Card> parsedCards = new Array<Card>();
        //go through all cards
        for(int i=0;i<cards.size;++i){
            //go through each color
            for(int j=0;j<colors.size;++j){
                //if current card has same color as any set in colors array
                if(Objects.equals(cards.get(i).getColor(), colors.get(j))){
                    //add to parsedCards array
                    parsedCards.add(cards.get(i));
                }
            }
        }
        return parsedCards;
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
    //get first valid card from deck depending on color/value
    public void pickSpecificCard(Deck deck, Card validCard){
        if(!deck.isEmpty()) {
            Array<Card> deckCards = deck.getCards();
            //go through each card of deck
            for (int i = 0; i < deckCards.size; ++i) {
                Card card = deckCards.get(i);
                //if current card is valid
                if (validCard.containsColor(card) || validCard.containsSymbol(card)) {
                    //remove card from deck
                    deck.getCards().removeIndex(i);
                    //add card to Hand
                    cards.add(card);
                    //stop after first valid card
                    return;
                }
            }
        }
        //if no card was added, no valid cards in deck, pick top instead
        pickCard(deck);
    }
    public void setCard(Card card,Deck deck){
        if(deck!=null)
            deck.setCard(card);
        //odstrani iz hand
        int cardIndex = cards.indexOf(card,true);
        if(cardIndex != -1)
            cards.removeIndex(cardIndex);
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
    public Card getHighestValueCard(){
        int value = 0;
        Card card = null;
        if(!cards.isEmpty()) {
            for (Card c : cards) {
                if (c.getValue() > value) {
                    value = c.getValue();
                    card=c;
                }
            }
        }
        return card;
    }

    public Card getLowestPrioritySpecialCard(){
        int priority = 7;
        Card card = null;
        if(!cards.isEmpty()) {
            for (Card c : cards) {
                int cardPriority = c.getPriority();
                if (cardPriority < priority && cardPriority>1) {
                    priority = c.getPriority();
                    card=c;
                }
            }
        }
        return card;
    }

    public Card getRandomCard(){
        Card card = null;
        if(!cards.isEmpty()){
            Random rnd = new Random();
            int rndIndex = rnd.nextInt(cards.size);
            card = cards.get(rndIndex);
        }
        return card;
    }

    //get all special cards from hand
    public Array<Card> getSpecialCards() {
        Array<Card> specials = new Array<>();
        for(int i = 0; i<this.cards.size; ++i){
            Card currentCard = this.cards.get(i);
            if(currentCard.isSpecial()) {
                specials.add(currentCard);
            }
        }
        return specials;
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
    //for each card, set id in fetchedHand
    public void setIdCards(Hand fetchedHand) {
        //get size of smallest hand to avoid out of bounds error
        int size = Math.min(this.cards.size, fetchedHand.getCards().size);
        for(int i=0;i<size;++i) {
            //for(int i=0;i<fetchedHand.getCards().size;++i){
            Card thisCard = this.cards.get(i);
            //local card doesn't have set id or handId, add it
            if (thisCard.getHandId() == 0 || thisCard.getId() == 0) {
                thisCard.setId(fetchedHand.getCards().get(i).getId());
                thisCard.setHandId(fetchedHand.getId());
            }
        }
    }
}
