package com.srebot.uno.classes;

public class PlayerTurn extends Player {
    private int currentTurn;

    //default constructor
    public PlayerTurn(){
        super();
        currentTurn=1;
    }

    //constructor iz json
    public PlayerTurn(String name, int score, int currentTurn){
        super(name, score);
        this.currentTurn = currentTurn;
    }
    //singleplayer constructor
    public PlayerTurn(String name, int score, Hand playerHand, int currentTurn){
        super(name, score, playerHand);
        this.currentTurn = currentTurn;
    }
    //multiplayer constructor
    public PlayerTurn(int id, String name, int score, Hand playerHand, int currentTurn){
        super(id, name, score, playerHand);
        this.currentTurn = currentTurn;
    }

    public int getCurrentTurn() {
        return currentTurn;
    }
/*
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}

    public String getName() {
        return name;
    }
    public int getScore() {
        return score;
    }
    public Hand getHand(){return playerHand;}

    public void setName(String name) {
        this.name = name;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public void setHand(Hand playerHand) {
        this.playerHand = playerHand;
    }

    @Override
    public String toString(){
        if(playerHand.getCards().size==1)
            return name+" "+playerHand.getCards().size+" card";
        return name+" "+playerHand.getCards().size+" cards";
    }
    */
}
