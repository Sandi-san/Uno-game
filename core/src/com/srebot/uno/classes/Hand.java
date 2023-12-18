package com.srebot.uno.classes;

import com.badlogic.gdx.utils.Array;

public class Hand {
    private Array<Card> cards;

    public Hand(){
        this.cards = new Array<Card>();
    }

    public void pickCard(Deck deck){
        Card card = deck.pickCard();
        cards.add(card);
    }
    public void pickCards(Deck deck, int n){
        Card card;
        for(int i=0;i<n;++i){
            card = deck.pickCard();
            cards.add(card);
        }
    }
    public void setCard(Deck deck, Card card){
        deck.setCard(card);
        //odstrani iz hand
        int cardIndex = cards.indexOf(card,true);
        if(cardIndex != -1){
            cards.removeIndex(cardIndex);
        }
    }
    public Array<Card> getCards(){
        return cards;
    }
}
