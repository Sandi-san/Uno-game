package com.srebot.uno.classes;

import com.badlogic.gdx.utils.Array;

public class Hand {
    private Array<Card> cards;

    public Hand(){
        this.cards = new Array<Card>();
    }
    //copy constructor
    public Hand(Hand hand){
        this.cards = new Array<Card>(hand.getCards());
    }

    public Hand(Card lastCard) {
        Array<Card> cards = new Array<Card>();
        cards.add(lastCard);
        this.cards = cards;
    }

    public Array<Card> getCards(){
        return cards;
    }

    public void pickCard(Deck deck){
        if(!deck.isEmpty()) {
            Card card = deck.pickCard();
            cards.add(card);
        }
    }
    public void pickCards(Deck deck, int n){
        Card card;
        for(int i=0;i<n;++i){
            if(deck.isEmpty()) break;
            card = deck.pickCard();
            cards.add(card);
        }
    }
    public void setCard(Card card,Deck deck){
        if(deck!=null)
            deck.setCard(card);
        //odstrani iz hand
        int cardIndex = cards.indexOf(card,true);
        if(cardIndex != -1){
            cards.removeIndex(cardIndex);
        }
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
