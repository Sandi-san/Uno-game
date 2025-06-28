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
    //index of first Card in Cards array to render
    private int indexFirst;
    //index of last Card in Cards array to render
    private int indexLast;

    //texture elements for arrows used to shift first/last indexes
    private TextureRegion arrowRegionLeft;
    private TextureRegion arrowRegionRight;

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
    //copy constructor, for ai
    public Hand(Hand hand){
        this.cards = new Array<Card>(hand.getCards());
        initIndexes();
    }
    //constructor for ai
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

    /** Initializes arrow regions (textures) */
    private void initRegions(){
        positionArrowRegionLeft = new Vector2();
        positionArrowRegionRight = new Vector2();
        boundsArrowRegionLeft = new Rectangle();
        boundsArrowRegionRight = new Rectangle();
    }

    /** Sets arrow regions (sets textures) */
    public void setArrowRegions(TextureAtlas.AtlasRegion region){
        if(this.arrowRegionRight==null)
            this.arrowRegionRight = new TextureRegion(region);
        if(this.arrowRegionLeft==null) {
            this.arrowRegionLeft = new TextureRegion(region);
            this.arrowRegionLeft.flip(true, false);
        }
    }

    /** Sets positions and bounds of left arrow */
    public void setArrowRegionLeft(float x,float y,
                                   float sizeX, float sizeY){
        this.positionArrowRegionLeft.x = x;
        this.positionArrowRegionLeft.y = y;
        this.boundsArrowRegionLeft.width = sizeX;
        this.boundsArrowRegionLeft.height = sizeY;
    }

    /** Sets positions and bounds of right arrow */
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

    /** Get last index and validify indexes */
    public int getIndexLast(int maxCardsShow){
        fixIndexes(maxCardsShow);
        return this.indexLast;
    }

    /** Set default last index */
    public void setIndexLast(){this.indexLast=this.cards.size-1;}

    public int getIndexFirst(){ return indexFirst;}

    /** Set default first index */
    public void setIndexFirst(){
        this.indexFirst=0;
    }

    /** Increments both indexes */
    public void indexIncrement(int maxCardsShow){
        //first check if indexes can be changed (don't increment first index when Hand has less cards than maxCards)
        if(indexDiffValid(this.indexFirst,this.indexLast+1,maxCardsShow)) {
            //increment last index if possible
            if(this.indexLast+1<=this.cards.size-1)
                this.indexLast++;
            //increment first index (if needed)
            if(!indexDiffValid(this.indexFirst,this.indexLast,maxCardsShow)){
                this.indexFirst++;
            }
        }
        else{
            if(!indexDiffValid(this.indexFirst,this.indexLast+1,maxCardsShow)){
                this.indexFirst++;
                if(this.indexLast+1<=this.cards.size-1)
                    this.indexLast++;
            }
        }
    }

    /** Fix indexes if Hand has more cards in hand than maxCardsShow and
    player was not at max lastIndex when setting card, resulting in
    incorrect difference between first/last index (which should be maxCardsShow-1 on cards.size>maxCardsShow-1) */
    private void fixIndexes(int maxCardsShow){
        //check if indexes need to be fixed, if not, return
        if((this.indexLast-this.indexFirst==maxCardsShow-1) || (cards.size<=maxCardsShow-1))
            return;
        do {
            //increment last index until the difference between first index is same size as maxCardsShow
            this.indexLast = this.indexLast+1;
        } while(this.indexLast-this.indexFirst!=maxCardsShow-1);
    }

    /** Decrements both indexes */
    public void indexDecrement(int maxCardsShow){
        //Validify indexes first
        fixIndexes(maxCardsShow);
        //Check if index decrement can happen
        if(indexDiffValid(this.indexFirst,this.indexLast-1,maxCardsShow)) {
            this.indexLast--;
            if(!indexDiffValid(this.indexFirst,this.indexLast,maxCardsShow)){
                if (this.indexFirst - 1 >= 0)
                    this.indexFirst--;
            }
        }
        else {
            if(!indexDiffValid(this.indexFirst,this.indexLast-1,maxCardsShow)){
                if (this.indexFirst - 1 >= 0)
                    this.indexFirst--;
                this.indexLast--;
            }
        }
    }

    /** Increments last index only (when adding +2/4 Cards to Hand) */
    public void lastIndexIncrement(int num, int maxCardsShow){
        this.indexLast = this.indexLast+num;
        int diff = this.indexLast-this.indexFirst;
        //accordingly move first index when adding Cards and expanding beyond maxCards
        if(diff>=maxCardsShow) {
            this.indexFirst = this.indexLast - maxCardsShow + 1;
        }
    }

    /** Checks if changing indexes still leaves them within valid bounds */
    private boolean indexDiffValid(int first, int last, int maxCardsShow){
        //Prevents changing indexes if difference between first/last index doesn't exceed maxCardsShow
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

    /** Gets most used color within Cards of Hand */
    public String getMostUsedCardColor(){
        //Create an array for possible colors: 0-B, 1-R, 2-G, 3-Y
        Integer[] nums = new Integer[4];
        Arrays.fill(nums,0);
        //Count how often colors appear in Hand
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

        //Get most occurring color and its index
        int max=0;
        int index=-1;
        for(int i=0; i<nums.length;++i){
            if(nums[i]>max) {
                max = nums[i];
                index = i;
            }
        }

        //Return most occurring color
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

    /** Get the combined value of distinct colors within Hand */
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

    /** Get least used color that appears in Cards of Hand */
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

    /** Remove all Cards from Hand except those defined in colors array */
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

    /** Picks Card from Deck and adds it to Hand */
    public void pickCard(Deck deck){
        if(!deck.isEmpty()) {
            Card card = deck.pickCard();
            cards.add(card);
            //setIndexLast();
        }
    }

    /** Picks multiple Cards from Deck and adds them to Hand */
    public void pickCards(Deck deck, int n){
        Card card;
        for(int i=0;i<n;++i){
            if(deck.isEmpty()) break;
            card = deck.pickCard();
            cards.add(card);
        }
        //if first draw: set lastIndex
        if(indexLast==-1)
            setIndexLast();
    }

    /** Get first valid Card from Deck depending on color/value and add it to Hand */
    public void pickSpecificCard(Deck deck, Card validCard){
        if(!deck.isEmpty()) {
            Array<Card> deckCards = deck.getCards();
            //go through each Card of Deck
            for (int i = 0; i < deckCards.size; ++i) {
                Card card = deckCards.get(i);
                //check if current Card is valid (same color or value)
                if (validCard.containsColor(card) || validCard.containsSymbol(card)) {
                    //remove Card from Deck
                    deck.getCards().removeIndex(i);
                    //add Card to Hand
                    cards.add(card);
                    //stop after first valid Card
                    return;
                }
            }
        }
        //if no Card was added, that means there are no valid cards in deck, pick top Card from Deck instead
        pickCard(deck);
    }

    /** Remove Card from Hand and add it to Deck */
    public void setCard(Card card,Deck deck){
        //add to Deck
        if(deck!=null)
            deck.setCard(card);
        //remove from Hand
        int cardIndex = cards.indexOf(card,true);
        if(cardIndex != -1)
            cards.removeIndex(cardIndex);
        //if(cards.size<indexLast)
        //    setIndexLast();
    }

    /** Get Card with the highest priority within Hand */
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

    /** Get Card with the highest value within Hand */
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

    /** Get special Card with the lowest priority within Hand (return null if no special Cards) */
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

    /** Get random Card from Hand */
    public Card getRandomCard(){
        Card card = null;
        if(!cards.isEmpty()){
            Random rnd = new Random();
            int rndIndex = rnd.nextInt(cards.size);
            card = cards.get(rndIndex);
        }
        return card;
    }

    /** Get all special Cards from Hand */
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

    /** Get last Card that was added to Hand (used within phantomHand for AI) */
    public Card getLastCard(){
        if(!cards.isEmpty()){
            return cards.get(cards.size-1);
        }
        return null;
    }

    /** Get sum value of all Cards within hand */
    public int getSumCardPoints(){
        int value=0;
        for(Card card : cards){
            value+=card.getValue();
        }
        return value;
    }

    /** For each Card, set id in fetchedHand */
    public void setIdCards(Hand fetchedHand) {
        //get size of smallest hand to avoid out of bounds error
        int size = Math.min(this.cards.size, fetchedHand.getCards().size);
        for(int i=0;i<size;++i) {
            Card thisCard = this.cards.get(i);
            //local card doesn't have set id or handId, add it
            if (thisCard.getHandId() == 0 || thisCard.getId() == 0) {
                thisCard.setId(fetchedHand.getCards().get(i).getId());
                thisCard.setHandId(fetchedHand.getId());
            }
        }
    }
}
