package dev.wigo.protocol;

public final class LegacyVarInt {
    private LegacyVarInt() {}
    public static int getByteSize(int value) {
        for(int size=1;size<5;size++)if((value&(-1<<(size*7)))==0)return size;
        return 5;
    }
}
