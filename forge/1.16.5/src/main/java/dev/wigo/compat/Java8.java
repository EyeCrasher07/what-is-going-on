package dev.wigo.compat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.*;

/** Small Java 8 equivalents of the newer APIs used by WIGO. */
public final class Java8 {
    private Java8() {}

    @SafeVarargs public static <T> List<T> list(T... values) {
        List<T> result = new ArrayList<>(values.length);
        for (T value : values) result.add(Objects.requireNonNull(value));
        return Collections.unmodifiableList(result);
    }
    @SafeVarargs public static <T> Set<T> set(T... values) {
        Set<T> result = new LinkedHashSet<>();
        for (T value : values) {
            if (!result.add(Objects.requireNonNull(value))) throw new IllegalArgumentException("Duplicate value");
        }
        return Collections.unmodifiableSet(result);
    }
    public static <T> List<T> copyList(Collection<? extends T> source) {
        List<T> result = new ArrayList<>(source.size());
        for (T value : source) result.add(Objects.requireNonNull(value));
        return Collections.unmodifiableList(result);
    }
    public static <T> Set<T> copySet(Collection<? extends T> source) {
        Set<T> result=new LinkedHashSet<>();for(T value:source)result.add(Objects.requireNonNull(value));return Collections.unmodifiableSet(result);
    }
    public static <K,V> Map<K,V> copyMap(Map<? extends K,? extends V> source) {
        Map<K,V> result = new LinkedHashMap<>();
        source.forEach((key,value) -> result.put(Objects.requireNonNull(key),Objects.requireNonNull(value)));
        return Collections.unmodifiableMap(result);
    }
    public static <T> T nonNullElse(T value,T fallback) {
        return value != null ? value : Objects.requireNonNull(fallback);
    }
    public static String read(Path path) throws IOException { return read(path,StandardCharsets.UTF_8); }
    public static String read(Path path,Charset charset) throws IOException {
        return new String(Files.readAllBytes(path),charset);
    }
    public static Path write(Path path,String text,OpenOption... options) throws IOException {
        return write(path,text,StandardCharsets.UTF_8,options);
    }
    public static Path write(Path path,String text,Charset charset,OpenOption... options) throws IOException {
        return Files.write(path,text.getBytes(charset),options);
    }
    public static String strip(String text) {
        int start=0,end=text.length();
        while (start<end && Character.isWhitespace(text.codePointAt(start))) start+=Character.charCount(text.codePointAt(start));
        while (start<end && Character.isWhitespace(text.codePointBefore(end))) end-=Character.charCount(text.codePointBefore(end));
        return text.substring(start,end);
    }
    public static boolean isBlank(String text) {
        for (int i=0;i<text.length();) {
            int point=text.codePointAt(i);
            if (!Character.isWhitespace(point)) return false;
            i+=Character.charCount(point);
        }
        return true;
    }
    public static String stripTrailing(String text) {
        int end=text.length();while(end>0&&Character.isWhitespace(text.codePointBefore(end)))end-=Character.charCount(text.codePointBefore(end));return text.substring(0,end);
    }
    public static java.util.stream.Stream<String> lines(String text) {
        if(text.isEmpty())return java.util.stream.Stream.empty();String[] lines=text.split("\\r\\n|\\r|\\n",-1);
        return Arrays.stream(lines,0,lines.length-(lines[lines.length-1].isEmpty()?1:0));
    }
    public static java.util.stream.Stream<String> lines(java.io.BufferedReader reader){return reader.lines();}
    public static byte[] readAllBytes(java.io.InputStream input) throws IOException {return readNBytes(input,Integer.MAX_VALUE);}
    public static byte[] readNBytes(java.io.InputStream input,int maximum) throws IOException {
        if(maximum<0)throw new IllegalArgumentException("Negative byte count");java.io.ByteArrayOutputStream result=new java.io.ByteArrayOutputStream();
        byte[] buffer=new byte[Math.min(8192,maximum)];int remaining=maximum;
        while(remaining>0){int count=input.read(buffer,0,Math.min(remaining,buffer.length));if(count<0)break;result.write(buffer,0,count);remaining-=count;}
        return result.toByteArray();
    }
    public static String hex(byte[] bytes) {
        char[] result=new char[bytes.length*2],digits="0123456789abcdef".toCharArray();
        for(int i=0;i<bytes.length;i++){result[i*2]=digits[(bytes[i]&255)>>>4];result[i*2+1]=digits[bytes[i]&15];}return new String(result);
    }
}
