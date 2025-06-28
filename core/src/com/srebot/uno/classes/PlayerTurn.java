package com.srebot.uno.classes;

/** Used for serializing Player + currentTurn and passing data through backend */
public class PlayerTurn extends Player {
    private int currentTurn;

    //default constructor
    public PlayerTurn(){
        super();
        currentTurn=1;
    }

    //constructor for json
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
}
