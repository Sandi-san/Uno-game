package com.srebot.uno.classes;
import java.util.Date;
import java.util.List;

public class GameData {
    private int id;
    private Deck[] decks;
    private Player[] players;
    private Date createdAt;
    private int maxPlayers;
    private Card topCard;

    //PLACEHOLDER
    public GameData(){
        this.id=10;
        this.decks = new Deck[1];
        this.decks[0] = new Deck();
        this.decks[0].setCard(new Card());
        this.players = new Player[1];
        this.players[0] = new Player();
        this.players[0].setHand(new Hand());
        this.players[0].getHand().pickCard(decks[0]);
    }
    public GameData(List<Player> playersList, Deck deckDraw, Deck deckDiscard, int maxPlayers, Card topCard){
        this.players = new Player[playersList.size()];
        for(int i=0; i<this.players.length;++i){
            this.players[i]=playersList.get(i);
        }
        this.decks = new Deck[2];
        this.decks[0] = deckDraw;
        this.decks[1]=deckDiscard;
        this.maxPlayers=maxPlayers;
        this.topCard=topCard;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Deck[] getDecks() {
        return decks;
    }

    public void setDecks(Deck[] decks) {
        this.decks = decks;
    }

    public Player[] getPlayers() {
        return players;
    }

    public void setPlayers(Player[] players) {
        this.players = players;
    }


    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Card getTopCard() {
        return topCard;
    }

    public void setTopCard(Card topCard) {
        this.topCard = topCard;
    }

    @Override
    public String toString() {
        return "Game: "+id+" | Max Players: "+maxPlayers+" | Date: "+ createdAt.toLocaleString();
    }
}
