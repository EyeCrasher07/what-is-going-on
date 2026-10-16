package dev.wigo.client;

import com.google.gson.*;
import net.minecraft.util.text.Style;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.IReorderingProcessor;
import java.util.*;

/** Only visual styles: no click/hover actions or resources from the sender. */
public final class GuiTextStyles {
    private static String font(Style style) {
        return style.getFont().toString();
    }
    private static Style withFont(Style style,String font) {
        return style.withFont(new ResourceLocation(font));
    }
    public static JsonArray runs(IReorderingProcessor value) {
        JsonArray runs=new JsonArray();StringBuilder text=new StringBuilder();Style[] previous={null};
        value.accept((index,style,point)->{
            if(previous[0]!=null&&!style.equals(previous[0])) {append(runs,text,previous[0]);text.setLength(0);}
            previous[0]=style;text.appendCodePoint(point);return text.length()<=2048&&runs.size()<128;
        });
        if(previous[0]!=null)append(runs,text,previous[0]);return runs;
    }
    private static void append(JsonArray runs,StringBuilder text,Style style) {
        String font=font(style);if(!font.startsWith("minecraft:"))font="minecraft:default";
        int flags=(style.isBold()?1:0)|(style.isItalic()?2:0)|(style.isUnderlined()?4:0)|(style.isStrikethrough()?8:0)|(style.isObfuscated()?16:0);
        runs.add(GuiCapture.array(text.toString(),font,style.getColor()==null?null:style.getColor().getValue(),flags));
    }
    public static void add(JsonArray command,IReorderingProcessor value) {
        JsonArray runs=runs(value);
        boolean styled=false;
        for(var valueRun:runs) {var run=valueRun.getAsJsonArray();if(!run.get(1).getAsString().equals("minecraft:default")||!run.get(2).isJsonNull()||run.get(3).getAsInt()!=0)styled=true;}
        if(styled)command.add(runs);
    }
    public static IReorderingProcessor text(JsonArray command) {
        if(command.size()<10||command.get(9).isJsonNull())return IReorderingProcessor.forward(command.get(3).getAsString(),Style.EMPTY);
        List<Run> runs=new ArrayList<>();
        for(var value:command.get(9).getAsJsonArray()) {
            var run=value.getAsJsonArray();int flags=run.get(3).getAsInt();
            Style style=withFont(Style.EMPTY,run.get(1).getAsString()).withBold((flags&1)!=0).withItalic((flags&2)!=0)
                    .withUnderlined((flags&4)!=0);
            if((flags&8)!=0)style=style.applyFormat(net.minecraft.util.text.TextFormatting.STRIKETHROUGH);
            if((flags&16)!=0)style=style.applyFormat(net.minecraft.util.text.TextFormatting.OBFUSCATED);
            if(!run.get(2).isJsonNull())style=style.withColor(net.minecraft.util.text.Color.fromRgb(run.get(2).getAsInt()));
            runs.add(new Run(run.get(0).getAsString(),style));
        }
        // The source sequence is already in visual order; don't reorder it a second time.
        return sink->{int index=0;for(Run run:runs)for(int offset=0;offset<run.text.length();) {
            int point=run.text.codePointAt(offset);if(!sink.accept(index,run.style,point))return false;
            offset+=Character.charCount(point);index+=Character.charCount(point);
        }return true;};
    }
    @com.github.bsideup.jabel.Desugar
    private record Run(String text,Style style) {}
}
