package dev.wigo.protocol;

import java.util.*;
import java.util.zip.*;
import java.io.*;

/** A bounded strip of Minecraft map colour IDs, never a captured image. */
public final class MapPalette {
    public static String encode(byte[] colors,int offset) {
        Deflater encoder=new Deflater(Deflater.BEST_SPEED);
        try {
            ByteArrayOutputStream buffer=new ByteArrayOutputStream(4200);
            DeflaterOutputStream stream=new DeflaterOutputStream(buffer,encoder);
            stream.write(colors,offset,4096);stream.finish();
            if (buffer.size()>=4096) return "r"+Base64.getEncoder().encodeToString(Arrays.copyOfRange(colors,offset,offset+4096));
            return "z"+Base64.getEncoder().encodeToString(buffer.toByteArray());
        } catch (IOException e) {throw new IllegalArgumentException("Map palette encoding",e);
        } finally {encoder.end();}
    }
    public static byte[] decode(String encoded) {
        if (encoded.length()>5500||encoded.isEmpty()) throw new IllegalArgumentException("Map palette budget");
        byte[] bytes=Base64.getDecoder().decode(encoded.substring(1));
        if (encoded.charAt(0)=='r') {if (bytes.length!=4096) throw new IllegalArgumentException("Map palette size");return bytes;}
        if (encoded.charAt(0)!='z') throw new IllegalArgumentException("Map palette encoding");
        Inflater decoder=new Inflater();
        try {
            decoder.setInput(bytes);byte[] colors=new byte[4097];int size=0;
            while (!decoder.finished()&&size<colors.length) {int n=decoder.inflate(colors,size,colors.length-size);if (n==0) break;size+=n;}
            if (size!=4096||!decoder.finished()||decoder.getRemaining()!=0) throw new IllegalArgumentException("Map palette size");
            return Arrays.copyOf(colors,4096);
        } catch (DataFormatException e) {throw new IllegalArgumentException("Map palette data",e);}
        finally {decoder.end();}
    }
}
