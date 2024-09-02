package com.srebot.uno.classes;

public class Player {
    private int id;
    private String name;
    private int score;
    private Hand playerHand;

    //default constructor
    public Player(){
        this.name="A";
        this.score=0;
    }
    //constructor iz json
    public Player(String name, int score){
        this.name = name;
        if(score==-1) this.score=0;
        else this.score = score;
    }
    //singleplayer constructor
    public Player(String name, int score, Hand playerHand){
        this.name = name;
        this.score = score;
        this.playerHand = playerHand;
    }
    //multiplayer constructor
    public Player(int id, String name, int score, Hand playerHand){
        this.id = id;
        this.name = name;
        this.score = score;
        this.playerHand = playerHand;
    }

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

    //get ids of hand and cards from fetchedPlayer and set them to this object
    public void setIds(Player fetchedPlayer) {
        this.getHand().setId(fetchedPlayer.getHand().getId());
        this.getHand().setIdCards(fetchedPlayer.getHand());
    }
}
