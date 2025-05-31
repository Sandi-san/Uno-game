package com.srebot.uno.classes;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.srebot.uno.assets.AssetDescriptors;
import com.srebot.uno.assets.RegionNames;

import java.util.Objects;


public enum CardValues {
    B1(1,1,"B", RegionNames.B1),
    B2(1,2,"B", RegionNames.B2),
    B3(1,3,"B", RegionNames.B3),
    B4(1,4,"B", RegionNames.B4),
    B5(1,5,"B", RegionNames.B5),
    B6(1,6,"B", RegionNames.B6),
    B7(1,7,"B", RegionNames.B7),
    B8(1,8,"B", RegionNames.B8),
    B9(1,9,"B", RegionNames.B9),
    R1(1,1,"R", RegionNames.R1),
    R2(1,2,"R", RegionNames.R2),
    R3(1,3,"R", RegionNames.R3),
    R4(1,4,"R", RegionNames.R4),
    R5(1,5,"R", RegionNames.R5),
    R6(1,6,"R", RegionNames.R6),
    R7(1,7,"R", RegionNames.R7),
    R8(1,8,"R", RegionNames.R8),
    R9(1,9,"R", RegionNames.R9),
    G1(1,1,"G", RegionNames.G1),
    G2(1,2,"G", RegionNames.G2),
    G3(1,3,"G", RegionNames.G3),
    G4(1,4,"G", RegionNames.G4),
    G5(1,5,"G", RegionNames.G5),
    G6(1,6,"G", RegionNames.G6),
    G7(1,7,"G", RegionNames.G7),
    G8(1,8,"G", RegionNames.G8),
    G9(1,9,"G", RegionNames.G9),
    Y1(1,1,"Y", RegionNames.Y1),
    Y2(1,2,"Y", RegionNames.Y2),
    Y3(1,3,"Y", RegionNames.Y3),
    Y4(1,4,"Y", RegionNames.Y4),
    Y5(1,5,"Y", RegionNames.Y5),
    Y6(1,6,"Y", RegionNames.Y6),
    Y7(1,7,"Y", RegionNames.Y7),
    Y8(1,8,"Y", RegionNames.Y8),
    Y9(1,9,"Y", RegionNames.Y9),
    BS(2,20,"B", RegionNames.Bstop),
    RS(2,20,"R", RegionNames.Rstop),
    GS(2,20,"G", RegionNames.Gstop),
    YS(2,20,"Y", RegionNames.Ystop),
    BR(3,20,"B", RegionNames.Breverse),
    RR(3,20,"R", RegionNames.Rreverse),
    GR(3,20,"G", RegionNames.Greverse),
    YR(3,20,"Y", RegionNames.Yreverse),
    P2BR(4,20,"B-R", RegionNames.plus2blueRed),
    P2GY(4,20,"G-Y", RegionNames.plus2greenYellow),
    P2B(5,20,"B", RegionNames.plus2blue),
    P2R(5,20,"R", RegionNames.plus2red),
    P2G(5,20,"G", RegionNames.plus2green),
    P2Y(5,20,"Y", RegionNames.plus2yellow),
    P4(6,50,"-", RegionNames.plus4),
    WC(6,50,"-", RegionNames.rainbow);


    //prioriteta, da bo AI igral karto, če jo ima
    private final int priority;
    private final int value;
    private final String color;
    private final String textureName;

    CardValues(int priority, int value, String color, String textureName){
        this.priority=priority;
        this.value=value;
        this.color = color;
        this.textureName = textureName;
    }

    public int getPriority(){
        return priority;
    }
    public int getValue(){
        return value;
    }
    public String getColor(){
        return color;
    }
    public String getTexture(){
        return textureName;
    }

    //TODO: spremeni v switch card
    public static String switchTexture(Card oldCard, Card newCard){
        int oldValue = oldCard.getValue();
        String newColor = newCard.getColor();
        //old color is all ("-"), or dual color: no alt color exists, so return default new color card
        if(oldCard.getColor().equals("-") || oldCard.getPriority()==4){
            switch (newColor) {
                case "B":
                    return RegionNames.Bdefault;
                case "R":
                    return RegionNames.Rdefault;
                case "G":
                    return RegionNames.Gdefault;
                case "Y":
                    return RegionNames.Ydefault;
            }
        }
        switch (oldValue){
            case 1:
                switch (newColor) {
                    case "B":
                        return B1.textureName;
                    case "R":
                        return R1.textureName;
                    case "G":
                        return G1.textureName;
                    case "Y":
                        return Y1.textureName;
                }
                break;
            case 2:
                switch (newColor) {
                    case "B":
                        return B2.textureName;
                    case "R":
                        return R2.textureName;
                    case "G":
                        return G2.textureName;
                    case "Y":
                        return Y2.textureName;
                }
                break;
            case 3:
                switch (newColor) {
                    case "B":
                        return B3.textureName;
                    case "R":
                        return R3.textureName;
                    case "G":
                        return G3.textureName;
                    case "Y":
                        return Y3.textureName;
                }
                break;
            case 4:
                switch (newColor) {
                    case "B":
                        return B4.textureName;
                    case "R":
                        return R4.textureName;
                    case "G":
                        return G4.textureName;
                    case "Y":
                        return Y4.textureName;
                }
                break;
            case 5:
                switch (newColor) {
                    case "B":
                        return B5.textureName;
                    case "R":
                        return R5.textureName;
                    case "G":
                        return G5.textureName;
                    case "Y":
                        return Y5.textureName;
                }
                break;
            case 6:
                switch (newColor) {
                    case "B":
                        return B6.textureName;
                    case "R":
                        return R6.textureName;
                    case "G":
                        return G6.textureName;
                    case "Y":
                        return Y6.textureName;
                }
                break;
            case 7:
                switch (newColor) {
                    case "B":
                        return B7.textureName;
                    case "R":
                        return R7.textureName;
                    case "G":
                        return G7.textureName;
                    case "Y":
                        return Y7.textureName;
                }
                break;
            case 8:
                switch (newColor) {
                    case "B":
                        return B8.textureName;
                    case "R":
                        return R8.textureName;
                    case "G":
                        return G8.textureName;
                    case "Y":
                        return Y8.textureName;
                }
                break;
            case 9:
                switch (newColor) {
                    case "B":
                        return B9.textureName;
                    case "R":
                        return R9.textureName;
                    case "G":
                        return G9.textureName;
                    case "Y":
                        return Y9.textureName;
                }
                break;
            case 20:
                int priority = oldCard.getPriority();
                //stop
                if(priority==2){
                    switch (newColor) {
                        case "B":
                            return BS.textureName;
                        case "R":
                            return RS.textureName;
                        case "G":
                            return GS.textureName;
                        case "Y":
                            return YS.textureName;
                    }
                }
                //reverse
                else if(priority==3){
                    switch (newColor) {
                        case "B":
                            return BR.textureName;
                        case "R":
                            return RR.textureName;
                        case "G":
                            return GR.textureName;
                        case "Y":
                            return YR.textureName;
                    }
                }
                //plus2 - single color
                else if(priority==5){
                    switch (newColor) {
                        case "B":
                            return P2B.textureName;
                        case "R":
                            return P2R.textureName;
                        case "G":
                            return P2G.textureName;
                        case "Y":
                            return P2Y.textureName;
                    }
                }
                break;
         }
        return oldCard.getTexture();
    }
}
