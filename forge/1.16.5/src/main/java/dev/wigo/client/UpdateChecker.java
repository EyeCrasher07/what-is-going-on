package dev.wigo.client;

import com.google.gson.*;
import dev.wigo.Wigo;
import net.minecraft.util.SharedConstants;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/** One bounded, asynchronous public GitHub release request per client launch. */
public final class UpdateChecker {
    public enum State { CHECKING, AVAILABLE, CURRENT, NO_RELEASE, FAILED }
    @com.github.bsideup.jabel.Desugar
    public record Result(State state,String version) {}
    private static final URI API=URI.create("https://api.github.com/repos/EyeCrasher07/what-is-going-on/releases?per_page=100");
    private static volatile Result result=new Result(State.CHECKING,"");
    private static boolean started;
    private UpdateChecker(){}
    public static Result result(){return result;}
    public static synchronized void start(){
        if(started)return;started=true;
        String current=BuildInfo.version(),loader=BuildInfo.loader(),game=SharedConstants.getCurrentVersion().getName();
        Thread worker=new Thread(()->{
            try{result=select(fetch(),current,loader,game);}
            catch(IOException|RuntimeException e){result=new Result(State.FAILED,"");Wigo.LOG.debug("Cannot check WIGO releases: {}",e.toString());}
        },"WIGO update check");
        worker.setDaemon(true);worker.start();
    }
    private static JsonArray fetch() throws IOException {
        HttpURLConnection connection=(HttpURLConnection)API.toURL().openConnection();
        connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent","WIGO/"+BuildInfo.version());
        connection.setRequestProperty("Accept","application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version","2026-03-10");
        try {
            if(connection.getResponseCode()!=200)throw new IOException("GitHub HTTP "+connection.getResponseCode());
            try(var input=connection.getInputStream()) {
                byte[] data=dev.wigo.compat.Java8.readNBytes(input,2*1024*1024+1);
                if(data.length>2*1024*1024)throw new IOException("GitHub response exceeds limit");
                return new com.google.gson.JsonParser().parse(new String(data,StandardCharsets.UTF_8)).getAsJsonArray();
            }
        }finally{connection.disconnect();}
    }
    /** Source-only/tagged releases work too; named WIGO JARs must match game and loader. */
    public static Result select(JsonArray releases,String current,String loader,String game){
        Version installed=Version.parse(current);if(installed==null)throw new IllegalArgumentException("Invalid installed version");
        Version latest=null;String display="";String prefix="wigo-"+loader+"-"+game+"-";
        for(int i=0;i<Math.min(releases.size(),100);i++) {
            if(!releases.get(i).isJsonObject())continue;
            JsonObject release=releases.get(i).getAsJsonObject();
            if(release.has("draft")&&release.get("draft").getAsBoolean())continue;
            Version candidate=null;String candidateName="";boolean namedJars=false;
            if(release.has("assets")&&release.get("assets").isJsonArray()) {
                var assets=release.getAsJsonArray("assets");
                for(int j=0;j<Math.min(assets.size(),1000);j++) {
                    if(!assets.get(j).isJsonObject())continue;
                    String name=string(assets.get(j).getAsJsonObject(),"name");
                    if(name.startsWith("wigo-")&&name.endsWith(".jar"))namedJars=true;
                    if(name.startsWith(prefix)&&name.endsWith(".jar")) {
                        String version=name.substring(prefix.length(),name.length()-4);Version parsed=Version.parse(version);
                        if(parsed!=null&&(candidate==null||parsed.compareTo(candidate)>0)){candidate=parsed;candidateName=version;}
                    }
                }
            }
            if(!namedJars){candidateName=string(release,"tag_name");candidate=Version.parse(candidateName);if(candidateName.startsWith("v"))candidateName=candidateName.substring(1);}
            if(candidate!=null&&(latest==null||candidate.compareTo(latest)>0)){latest=candidate;display=candidateName;}
        }
        return latest==null?new Result(State.NO_RELEASE,""):new Result(latest.compareTo(installed)>0?State.AVAILABLE:State.CURRENT,display);
    }
    private static String string(JsonObject object,String key){
        JsonElement value=object.get(key);return value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString()?value.getAsString():"";
    }
    @com.github.bsideup.jabel.Desugar
    private record Version(BigInteger major,BigInteger minor,BigInteger patch,List<String> prerelease) implements Comparable<Version> {
        private static final Pattern FORMAT=Pattern.compile("^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$");
        static Version parse(String text){
            if(text.length()>128)return null;var match=FORMAT.matcher(text);if(!match.matches())return null;
            List<String> parts=match.group(4)==null?dev.wigo.compat.Java8.list():dev.wigo.compat.Java8.list(match.group(4).split("\\.",-1));
            if(parts.stream().anyMatch(String::isEmpty))return null;
            return new Version(new BigInteger(match.group(1)),new BigInteger(match.group(2)),new BigInteger(match.group(3)),parts);
        }
        @Override public int compareTo(Version other){
            for(int c:new int[]{major.compareTo(other.major),minor.compareTo(other.minor),patch.compareTo(other.patch)})if(c!=0)return c;
            if(prerelease.isEmpty()||other.prerelease.isEmpty())return prerelease.isEmpty()?(other.prerelease.isEmpty()?0:1):-1;
            for(int i=0;i<Math.min(prerelease.size(),other.prerelease.size());i++) {
                String a=prerelease.get(i),b=other.prerelease.get(i);boolean an=a.matches("[0-9]+"),bn=b.matches("[0-9]+");
                int c=an&&bn?new BigInteger(a).compareTo(new BigInteger(b)):an!=bn?(an?-1:1):a.compareTo(b);
                if(c!=0)return c;
            }
            return Integer.compare(prerelease.size(),other.prerelease.size());
        }
    }
}
