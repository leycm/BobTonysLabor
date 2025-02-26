package de.lobbyles.bobtonyslabor.boby;

public enum TonyMode {
    BANED_MEMBER(0),
    MEMBER(1),
    SIGMA(2),
    BOB(3);

    private int lvl;

    TonyMode (int lvl){
        this.lvl = lvl;
    }

    public TonyMode fromString(String s){
        try{
            return TonyMode.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
