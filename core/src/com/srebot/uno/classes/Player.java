package com.srebot.uno.classes;

public class Player {
    private String name;
    private int score;
    private Hand playerHand;

    public Player(){
        this.name="A";
        this.score=0;
    }

    public Player(String name, int score){
        this.name = name;
        if(score==-1) this.score=0;
        else this.score = score;
    }
    public Player(String name, int score, Hand playerHand){
        this.name = name;
        this.score = score;
        this.playerHand = playerHand;
    }
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
}
