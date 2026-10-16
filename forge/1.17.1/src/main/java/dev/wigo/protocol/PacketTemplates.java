package dev.wigo.protocol;

import com.google.gson.*;
import java.util.*;

/** Fixed wire schemas for existing instructions. Reconstruction preserves fields and order. */
final class PacketTemplates {
    static final List<String> DRAW=List.of("r","t","e","c","b","s","a","i","o","k","v","h","n","m","l","z","q");
    static final int[] LENGTHS={9,9,9,9,13,13,14,7,7,8,8,8,6,5,5,4,4};
    record ObjectSchema(String op,List<String> keys) {}
    static final List<ObjectSchema> OBJECTS;
    static {
        var schemas=new ArrayList<ObjectSchema>();
        scene(schemas,"ui_begin","op session revision count title full rect");
        scene(schemas,"ui_patch","op session revision changes");
        scene(schemas,"ui_patch","op session revision changes copies");
        scene(schemas,"ui_commit","op session revision carried");
        scene(schemas,"ui_update","op session revision count title full rect changes carried");
        scene(schemas,"ui_update","op session revision count title full rect changes carried copies");
        add(schemas,"cursor","op actor session x y hover");
        add(schemas,"patch","op actor session revision slots");
        add(schemas,"commit","op actor session revision carried");
        add(schemas,"commit","op actor session revision");
        add(schemas,"update","op actor session revision slots");
        add(schemas,"update","op actor session revision slots carried");
        add(schemas,"close","op actor session");
        add(schemas,null,"id count components");add(schemas,null,"id components");add(schemas,null,"id count");add(schemas,null,"id");
        add(schemas,null,"version kind session window x y hover share receive");
        add(schemas,"batch","op states");add(schemas,"batch","op activity gui");
        OBJECTS=List.copyOf(schemas);
    }
    private static void scene(List<ObjectSchema> schemas,String op,String fields){add(schemas,op,fields);add(schemas,op,fields+" actor");}
    private static void add(List<ObjectSchema> schemas,String op,String fields){schemas.add(new ObjectSchema(op,List.of(fields.split(" "))));}
    static int object(JsonObject object) {
        var keys=new ArrayList<>(LegacyJson.keys(object));
        for(int index=0;index<OBJECTS.size();index++) {
            var schema=OBJECTS.get(index);if(!keys.equals(schema.keys()))continue;
            if(schema.op()==null||object.get("op").isJsonPrimitive()&&object.get("op").getAsJsonPrimitive().isString()&&object.get("op").getAsString().equals(schema.op()))return index;
        }
        return -1;
    }
    static int draw(JsonArray array) {
        if(array.size()==0||!array.get(0).isJsonPrimitive()||!array.get(0).getAsJsonPrimitive().isString())return -1;
        int id=DRAW.indexOf(array.get(0).getAsString());if(id<0)return -1;
        return array.size()==LENGTHS[id]||id==1&&(array.size()==10||array.size()==11)?id:-1;
    }
    static boolean styledLiteral(JsonArray array) {
        if(array.size()<10||!array.get(3).isJsonPrimitive()||!array.get(3).getAsJsonPrimitive().isString()||!array.get(9).isJsonArray())return false;
        var text=new StringBuilder();
        for(var value:array.get(9).getAsJsonArray()) {
            if(!value.isJsonArray())return false;var run=value.getAsJsonArray();
            if(run.size()!=4||!run.get(0).isJsonPrimitive()||!run.get(0).getAsJsonPrimitive().isString())return false;
            text.append(run.get(0).getAsString());
        }
        return text.toString().equals(array.get(3).getAsString());
    }
}
