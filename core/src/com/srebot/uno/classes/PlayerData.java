package com.srebot.uno.classes;

public class PlayerData {
    private String name;
    private int score;
    private Hand playerHand;

    public PlayerData(){

    }

    public PlayerData(String name, int score){
        this.name = name;
        this.score = score;
    }
    public PlayerData(String name, int score,Hand playerHand){
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
