package dev.wigo.config;

/** Stable local choices. Only the numeric ID travels with the owner's pointer. */
public enum CursorStyle {
    ARROW(0,"arrow",null,10,16),
    FOOD(1,"food","hud/food_full.png",9,9),
    AIR(2,"air","hud/air_empty.png",9,9),
    SEARCH(3,"search","icon/search.png",12,12),
    WITHERED(4,"withered","hud/heart/withered_hardcore_full.png",9,9),
    HEART(5,"heart","hud/heart/full.png",9,9),
    BUG(6,"bug","pause_menu/bug.png",13,13);

    public final int id,width,height;
    public final String key,texture;
    CursorStyle(int id,String key,String texture,int width,int height) {
        this.id=id;this.key=key;this.texture=texture;this.width=width;this.height=height;
    }
    public String label() {return "wigo.cursor."+key;}
    public static CursorStyle fromId(int id) {
        for(var style:values())if(style.id==id)return style;
        return ARROW;
    }
    public static CursorStyle fromKey(String key) {
        for(var style:values())if(style.key.equals(key))return style;
        return ARROW;
    }
}
