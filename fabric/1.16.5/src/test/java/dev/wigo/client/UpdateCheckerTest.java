package dev.wigo.client;

import com.google.gson.*;

/** Published tags, numeric prereleases and per-loader/game assets; no HTTP in this test. */
public final class UpdateCheckerTest {
    private static int checks;
    public static void run(){
        state("[]",UpdateChecker.State.NO_RELEASE,"");
        state("[{tag_name:'v0.1.0-alpha.8',draft:false}]",UpdateChecker.State.CURRENT,"0.1.0-alpha.8");
        state("[{tag_name:'v0.1.0-alpha.9',prerelease:true}]",UpdateChecker.State.AVAILABLE,"0.1.0-alpha.9");
        state("[{tag_name:'0.1.0-alpha.9'},{tag_name:'0.1.0-alpha.10'}]",UpdateChecker.State.AVAILABLE,"0.1.0-alpha.10");
        state("[{tag_name:'0.1.0'},{tag_name:'0.1.0-rc.9'}]",UpdateChecker.State.AVAILABLE,"0.1.0");
        state("[{tag_name:'1.0.0',draft:true},{tag_name:'0.1.0-alpha.7'}]",UpdateChecker.State.CURRENT,"0.1.0-alpha.7");
        state("[null,12,{tag_name:'not-a-version'},{}]",UpdateChecker.State.NO_RELEASE,"");
        state("[{tag_name:'v9.0.0',assets:[{name:'wigo-forge-26.3-9.0.0.jar'}]}]",UpdateChecker.State.NO_RELEASE,"");
        state("[{tag_name:'v9.0.0',assets:[{name:'wigo-fabric-1.16.5-9.0.0.jar'}]}]",UpdateChecker.State.NO_RELEASE,"");
        state("[{tag_name:'v9.0.0',assets:[{name:'wigo-fabric-26.3-0.1.0-alpha.8.jar'},{name:'wigo-forge-26.3-9.0.0.jar'}]}]",UpdateChecker.State.CURRENT,"0.1.0-alpha.8");
        state("[{tag_name:'v9.0.0',assets:[{name:'wigo-fabric-26.3-0.1.0-alpha.10.jar'},{name:'wigo-fabric-26.3-0.1.0-alpha.9.jar'}]}]",UpdateChecker.State.AVAILABLE,"0.1.0-alpha.10");
        state("[{tag_name:'v0.2.0',assets:[{name:'source.zip'}]}]",UpdateChecker.State.AVAILABLE,"0.2.0");
        state("[{tag_name:'0.1.0-alpha.8+build.99'}]",UpdateChecker.State.CURRENT,"0.1.0-alpha.8+build.99");
        rejects(()->UpdateChecker.select(new JsonArray(),"unknown","fabric","26.3"));
        System.out.println("WIGO update checker: "+checks+" checks passed; no HTTP requests");
    }
    private static void state(String json,UpdateChecker.State expected,String version){
        var result=UpdateChecker.select(new com.google.gson.JsonParser().parse(json).getAsJsonArray(),"0.1.0-alpha.8","fabric","26.3");
        require(result.state()==expected&&result.version().equals(version),"Wrong release selection: "+json+" -> "+result);
    }
    private static void rejects(Runnable task){try{task.run();throw new AssertionError("Invalid version accepted");}catch(IllegalArgumentException expected){checks++;}}
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
