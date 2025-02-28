package de.lobbyles.bobtonyslabor.boby;

public enum TonyMode {
    BANNED_MEMBER(0),
    MEMBER(1),
    SIGMA(2),
    BOB(3);

    private int lvl;

    TonyMode (int lvl){
        this.lvl = lvl;
    }

    public int lvl(){
        return this.lvl;
    }

    public static TonyMode fromString(String s){
        try{
            return TonyMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
