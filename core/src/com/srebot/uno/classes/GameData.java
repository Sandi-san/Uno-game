package com.srebot.uno.classes;
import java.util.Date;
import java.util.List;

public class GameData {
    //same variables as in database
    private int id;
    private Deck[] decks;
    private Player[] players;
    private Date createdAt;
    private int maxPlayers;
    private Card topCard;

    private String gameState;
    private int currentTurn; //id of Player from DB
    private String turnOrder;

    public GameData(
            List<Player> playersList, Deck deckDraw, Deck deckDiscard, Card topCard, int maxPlayers,
            String gameState, int currentTurn, String turnOrder){
        this.players = new Player[playersList.size()];
        for(int i=0; i<this.players.length;++i){
            this.players[i]=playersList.get(i);
        }
        this.decks = new Deck[2];
        this.decks[0] = deckDraw;
        this.decks[1] = deckDiscard;
        this.maxPlayers = maxPlayers;
        //this.topCard = deckDiscard.getTopCard();
        this.topCard = topCard;
        this.gameState = gameState;
        this.currentTurn = currentTurn;
        this.turnOrder = turnOrder;
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

    public String getGameState() {
        return gameState;
    }
    public void setGameState(String gameState) {
        this.gameState = gameState;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }
    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getTurnOrder() {
        return turnOrder;
    }
    public void setTurnOrder(String turnOrder) {
        this.turnOrder = turnOrder;
    }

    /** Get count of non-null Players */
    public int getActualPlayersSize(){
        int count = 0;
        for(Player player : players){
            if(player != null)
                count++;
        }
        return count;
    }

    @Override
    public String toString() {
        return "Game: "+id+" | Players: "+getActualPlayersSize()+"/"+maxPlayers+" | Date: "+ createdAt.toLocaleString();
    }
}
