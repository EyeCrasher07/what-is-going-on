package dev.wigo.client;

/** Vanilla slot coordinates used by both the source pointer and the receiving panel. */
public final class MenuSlots {
    private MenuSlots() {}
    public static int[] position(String kind,int rows,int slot) {
        if (kind.equals("chest")) {
            int chestSlots=rows*9;
            if(slot<chestSlots)return new int[]{8+slot%9*18,18+slot/9*18};
            int i=slot-chestSlots;
            return new int[]{8+i%9*18,rows*18+(i<27?31+i/9*18:89)};
        }
        if(slot==0)return new int[]{154,28};
        if(slot<5)return new int[]{98+(slot-1)%2*18,18+(slot-1)/2*18};
        if(slot<9)return new int[]{8,8+(slot-5)*18};
        if(slot<36)return new int[]{8+(slot-9)%9*18,84+(slot-9)/9*18};
        if(slot<45)return new int[]{8+(slot-36)*18,142};
        return new int[]{77,62};
    }
}
