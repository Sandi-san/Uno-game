package com.srebot.uno.classes;

public class GameData {
    private int id;
    private Deck[] decks;
    private Player[] players;
    private String timeCreated;

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

    @Override
    public String toString() {
        return "Game: "+id;
    }
}
