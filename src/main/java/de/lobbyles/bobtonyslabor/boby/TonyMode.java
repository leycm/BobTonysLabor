package de.lobbyles.bobtonyslabor.boby;

public enum TonyMode {
    HARD_TRAPPED(0),
    BANNED_MEMBER(1),
    TRAPPED(1),
    MEMBER(2),
    SIGMA(3),
    BOB(4);

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
