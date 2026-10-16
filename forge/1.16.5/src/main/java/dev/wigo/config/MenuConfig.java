package dev.wigo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** The generated file is a deliberately small TOML table: quoted IDs and boolean values. */
public final class MenuConfig {
    public static final String FILE_NAME="wigo-server.toml";
    private static final List<String> SHARING_KEYS=dev.wigo.compat.Java8.list("max_menu_distance","allow_player_distance",
            "count_invisible_observers","count_spectator_observers","allow_observer_counter","allow_smooth_mode");
    private final Map<String,Boolean> values;
    private final boolean allAllowed;
    private final int maxMenuDistance;
    private final boolean allowPlayerDistance;
    private final boolean countInvisibleObservers,countSpectatorObservers,allowObserverCounter,allowSmoothMode;
    private MenuConfig(Map<String,Boolean> values) {this(values,SharingDistance.DEFAULT,true,false,false,true,true);}
    private MenuConfig(Map<String,Boolean> values,int maximum,boolean personal,boolean invisible,boolean spectator,boolean counter,boolean smooth) {
        this.values=dev.wigo.compat.Java8.copyMap(values);allAllowed=values.values().stream().allMatch(Boolean::booleanValue);
        maxMenuDistance=maximum;allowPlayerDistance=personal;
        countInvisibleObservers=invisible;countSpectatorObservers=spectator;allowObserverCounter=counter;allowSmoothMode=smooth;
    }
    public int maxMenuDistance(){return maxMenuDistance;}
    public boolean allowPlayerDistance(){return allowPlayerDistance;}
    public boolean countInvisibleObservers(){return countInvisibleObservers;}
    public boolean countSpectatorObservers(){return countSpectatorObservers;}
    public boolean allowObserverCounter(){return allowObserverCounter;}
    public boolean allowSmoothMode(){return allowSmoothMode;}
    public static MenuConfig defaults() {
        Map<String,Boolean> values=new LinkedHashMap<>();
        for(var menu:MenuCatalog.menus())values.put(menu.id(),true);
        return new MenuConfig(values);
    }
    public boolean allows(String id) {return values.getOrDefault(id,false);}
    public boolean allowsAll() {return allAllowed;}
    public Set<String> blocked() {
        Set<String> out = new LinkedHashSet<>();
        for (var entry : values.entrySet()) if (!entry.getValue()) out.add(entry.getKey());
        return Collections.unmodifiableSet(out);
    }
    public static MenuConfig read(Path file) throws IOException {return parse(dev.wigo.compat.Java8.read(file,StandardCharsets.UTF_8));}
    public static MenuConfig parse(String text) {
        Map<String,Boolean> values=new LinkedHashMap<>(defaults().values);
        Set<String> seen=new HashSet<>(),tables=new HashSet<>();String table="";boolean personal=true;int number=0,maximum=SharingDistance.DEFAULT;
        boolean invisible=false,spectator=false,counter=true,smooth=true;
        for(String line:text.split("\\R",-1)) {
            number++;int comment=line.indexOf('#');
            if(comment>=0)line=line.substring(0,comment);
            line=dev.wigo.compat.Java8.strip(line);if(line.isEmpty())continue;
            if(line.equals("[menus]")||line.equals("[network]")||line.equals("[sharing]")) {
                table=line;if(!tables.add(table))throw invalid(number,"duplicate "+table+" table");continue;
            }
            if(table.isEmpty())throw invalid(number,"expected [menus], [network] or [sharing]");
            int equals=line.indexOf('=');if(equals<0)throw invalid(number,"expected ID = true or false");
            String id=dev.wigo.compat.Java8.strip(line.substring(0,equals)),value=dev.wigo.compat.Java8.strip(line.substring(equals+1));
            if(table.equals("[sharing]")) {
                if(!SHARING_KEYS.contains(id))throw invalid(number,"unknown sharing option "+id);
                if(!seen.add("sharing:"+id))throw invalid(number,"duplicate sharing option "+id);
                if(id.equals("max_menu_distance")) {
                    if(!value.matches("[0-9]{1,3}"))throw invalid(number,"expected distance 1–128");
                    maximum=SharingDistance.validate(Integer.parseInt(value));
                }else{
                    if(!value.equals("true")&&!value.equals("false"))throw invalid(number,"expected true or false");
                    boolean enabled=Boolean.parseBoolean(value);
                    switch(id) {
                        case "allow_player_distance" -> personal=enabled;
                        case "count_invisible_observers" -> invisible=enabled;
                        case "count_spectator_observers" -> spectator=enabled;
                        case "allow_observer_counter" -> counter=enabled;
                        case "allow_smooth_mode" -> smooth=enabled;
                    }
                }
                continue;
            }
            if(table.equals("[network]")) {
                if(!id.equals("compact_packets")&&!id.equals("translation_ids"))throw invalid(number,"unknown network option "+id);
                if(!seen.add("network:"+id))throw invalid(number,"duplicate network option "+id);
                if(!value.equals("true")&&!value.equals("false"))throw invalid(number,"expected true or false");
                continue;
            }
            if(id.length()>2&&id.startsWith("\"")&&id.endsWith("\""))id=id.substring(1,id.length()-1);
            else throw invalid(number,"menu IDs must be double quoted");
            if(!MenuCatalog.known(id))throw invalid(number,"unknown menu "+id);
            if(!seen.add(id))throw invalid(number,"duplicate menu "+id);
            if(!value.equals("true")&&!value.equals("false"))throw invalid(number,"expected true or false");
            values.put(id,Boolean.valueOf(value));
        }
        if(!tables.contains("[menus]"))throw invalid(number,"missing [menus] table");
        return new MenuConfig(values,maximum,personal,invisible,spectator,counter,smooth);
    }
    private static IllegalArgumentException invalid(int line,String message) {return new IllegalArgumentException("line "+line+": "+message);}
    public static String example() {
        StringBuilder out=new StringBuilder(sharingExample()).append("\n[menus]\n");
        for(var menu:MenuCatalog.menus())out.append('\n').append(menuLabel(menu)).append('"').append(menu.id()).append("\" = true\n");
        return out.toString();
    }
    private static String menuLabel(MenuCatalog.Menu menu){return "# "+menu.english()+" / "+menu.russian()+"\n";}
    private static String sharingDescription(String key) {
        return switch(key) {case "max_menu_distance" -> """
            # max_menu_distance — English
            # Maximum distance in blocks at which players can receive another player's menu.
            # Range: 1–128; default: 8. Example: 16 allows owners to choose 1–16 blocks.
            # If personal distance is disabled below, every owner uses this exact distance.
            # max_menu_distance — Русский
            # Максимальное расстояние в блоках для получения чужого меню.
            # От 1 до 128; по умолчанию 8. Например, 16 позволяет выбрать от 1 до 16 блоков.
            # Если личная настройка ниже выключена, у всех действует именно этот радиус.
            """;case "allow_player_distance" -> """
            # allow_player_distance — English
            # true: owners may reduce their menu distance in F8, up to the maximum above.
            # false: the slider is locked and all owners use max_menu_distance.
            # Example: maximum 8, true -> an owner may choose 3; false -> everyone uses 8.
            # Menu bans, player lists and allowed viewing sides still apply.
            # allow_player_distance — Русский
            # true: владелец может уменьшить радиус в F8, не превышая максимум выше.
            # false: ползунок заблокирован, у всех действует max_menu_distance.
            # Пример: максимум 8, true -> можно выбрать 3; false -> у всех радиус 8.
            # Запреты меню, списки игроков и разрешённые стороны продолжают действовать.
            """;case "count_invisible_observers" -> """
            # count_invisible_observers — English
            # false: players with the Invisibility effect receive menus but are not counted.
            # true: count them like other recipients. Default: false.
            # This does not change who may receive a menu or reveal player names.
            # count_invisible_observers — Русский
            # false: игроки с эффектом невидимости получают меню, но не учитываются в счётчике.
            # true: учитывать их как остальных получателей. По умолчанию false.
            # Настройка не меняет доступ к меню и не раскрывает имена игроков.
            """;case "count_spectator_observers" -> """
            # count_spectator_observers — English
            # false: spectators (GM3) receive menus but do not appear in the owner's count.
            # true: count them like other recipients. Default: false.
            # A player who is also invisible must be permitted by both count settings.
            # count_spectator_observers — Русский
            # false: игроки в режиме наблюдателя (GM3) получают меню, но не входят в счётчик.
            # true: учитывать их как остальных получателей. По умолчанию false.
            # Если игрок ещё и невидим, его учёт должны разрешать обе настройки счётчика.
            """;case "allow_observer_counter" -> """
            # allow_observer_counter — English
            # true: owners may enable the observer count and position its indicator in F8.
            # false: no counts are sent, the indicator is hidden and both controls are locked.
            # Menus still work. Personal preferences are restored when allowed again.
            # allow_observer_counter — Русский
            # true: владелец может включить счётчик в F8 и выбрать положение индикатора.
            # false: число не передаётся, индикатор скрыт, обе настройки заблокированы.
            # Меню продолжают работать. При разрешении личные настройки восстановятся.
            """;case "allow_smooth_mode" -> """
            # allow_smooth_mode — English
            # true: observers may choose Economical or Smooth in F8. Default: true.
            # false: everyone uses Economical; the display mode control is locked.
            # Economical sends menus only while the character is on an allowed side.
            # Smooth keeps receiving on both sides within range; the camera decides visibility.
            # Example: rear-only menu, character in front, F5 camera behind: only Smooth shows it.
            # Disabling Smooth saves hidden-side traffic. Personal preferences are preserved.
            # allow_smooth_mode — Русский
            # true: наблюдатель выбирает экономный или плавный режим в F8. По умолчанию true.
            # false: у всех экономный режим, переключатель режима заблокирован.
            # В экономном данные приходят, пока персонаж находится с разрешённой стороны.
            # В плавном данные приходят с обеих сторон в пределах радиуса, видимость решает камера.
            # Пример: меню видно сзади, персонаж спереди, камера F5 сзади — видно только в плавном.
            # Запрет плавного режима экономит трафик со скрытой стороны. Личный выбор сохраняется.
            """;default -> throw new IllegalArgumentException("Unknown sharing key "+key);};
    }
    private static String sharingDefault(String key) {
        return key.equals("max_menu_distance")?"8":key.startsWith("count_")?"false":"true";
    }
    private static String sharingExample() {
        StringBuilder out=new StringBuilder("[sharing]\n");
        for(String key:SHARING_KEYS) {
            if(out.length()>10)out.append('\n');
            out.append(sharingDescription(key)).append(key).append(" = ").append(sharingDefault(key)).append('\n');
        }
        return out.toString();
    }
    private static String describeSharing(String block) {
        StringBuilder out=new StringBuilder();Set<String> found=new HashSet<>();
        for(String line:block.split("\\R")) {
            String value=dev.wigo.compat.Java8.strip(line.split("#",2)[0]);int equals=value.indexOf('=');
            String key=equals<0?"":dev.wigo.compat.Java8.strip(value.substring(0,equals));
            if(SHARING_KEYS.contains(key)) {
                if(!out.toString().endsWith("\n\n")&&!found.isEmpty())out.append('\n');
                out.append(sharingDescription(key));found.add(key);
            }
            out.append(line).append('\n');
        }
        for(String key:SHARING_KEYS)if(!found.contains(key))
            out.append('\n').append(sharingDescription(key)).append(key).append(" = ").append(sharingDefault(key)).append('\n');
        return out.toString();
    }
    private static String labelMenus(String block) {
        Map<String,MenuCatalog.Menu> names=new HashMap<>();for(var menu:MenuCatalog.menus())names.put(menu.id(),menu);
        StringBuilder out=new StringBuilder();
        for(String line:block.split("\\R")) {
            String entry=dev.wigo.compat.Java8.strip(line);
            if(entry.startsWith("\"")) {
                int end=entry.indexOf('"',1);var menu=end>1?names.get(entry.substring(1,end)):null;
                if(menu!=null)out.append(menuLabel(menu));
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }
    private static String replaceGeneratedComments(String text) throws IOException {
        Set<String> legacy;
        try(var stream=MenuConfig.class.getResourceAsStream("legacy-config-comments.txt")) {
            if(stream==null)throw new IOException("Missing legacy WIGO config comments");
            legacy=new HashSet<>(dev.wigo.compat.Java8.lines(new String(dev.wigo.compat.Java8.readAllBytes(stream),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList)));
        }
        for(String key:SHARING_KEYS)legacy.addAll(dev.wigo.compat.Java8.lines(sharingDescription(key)).map(dev.wigo.compat.Java8::strip).collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(),java.util.Collections::unmodifiableList)));
        // Only whole generated comment lines are replaced; values, inline comments and
        // administrator notes remain intact.
        StringBuilder kept=new StringBuilder();
        for(String line:text.split("\\R",-1))if(!legacy.contains(dev.wigo.compat.Java8.strip(line))&&!dev.wigo.compat.Java8.strip(line).equals("#"))kept.append(line).append('\n');
        return kept.toString();
    }
    public static boolean createIfMissing(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        try {dev.wigo.compat.Java8.write(file,example(),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);return true;}
        catch(FileAlreadyExistsException existing) {return false;}
    }
    /** Upgrade an existing valid file without changing any administrator choices. */
    public static void addNewMenus(Path file) throws IOException {
        String original = dev.wigo.compat.Java8.read(file, StandardCharsets.UTF_8);
        String text=original;
        MenuConfig previous = parse(text);
        text=replaceGeneratedComments(text);
        String cleaned=text;
        StringBuilder added = new StringBuilder();
        for (var menu : MenuCatalog.menus()) {
            if (!menu.id().equals(MenuCatalog.SETTINGS) && !menu.id().equals(MenuCatalog.ENDER_CHEST)) continue;
            boolean present = dev.wigo.compat.Java8.lines(cleaned).map(l -> dev.wigo.compat.Java8.strip(l.split("#", 2)[0]))
                    .anyMatch(l -> l.matches("\\\"" + java.util.regex.Pattern.quote(menu.id()) + "\\\"\\s*=.*"));
            // Older releases included ender chests in the three-row chest rule.
            // Preserve that effective prohibition when introducing a separate key.
            boolean allowed = !menu.id().equals(MenuCatalog.ENDER_CHEST) || previous.allows("minecraft:generic_9x3");
            if (!present) added.append('\n').append(menuLabel(menu)).append('"').append(menu.id()).append("\" = ").append(allowed).append('\n');
        }
        // Move complete tables, including existing comments and explicit choices.
        // Accept inline comments on table headers just as parse() does.
        var headers=java.util.regex.Pattern.compile("(?m)^[ \\t]*\\[(menus|network|sharing)\\][ \\t]*(?:#[^\\r\\n]*)?$").matcher(text);
        Map<String,String> blocks=new HashMap<>();String head="",name=null;int start=0;
        while(headers.find()) {
            if(name==null)head=text.substring(0,headers.start());
            else blocks.put(name,dev.wigo.compat.Java8.stripTrailing(text.substring(start,headers.start()))+"\n");
            name=headers.group(1);start=headers.start();
        }
        if(name!=null)blocks.put(name,dev.wigo.compat.Java8.stripTrailing(text.substring(start))+"\n");
        head=dev.wigo.compat.Java8.strip(head);if(!head.isEmpty())head+="\n\n";
        String menuBlock=labelMenus(blocks.get("menus"));
        String networkNotes=dev.wigo.compat.Java8.lines(blocks.getOrDefault("network","")).map(line->{
            int comment=line.indexOf('#');return comment<0?"":dev.wigo.compat.Java8.strip(line.substring(comment));
        }).filter(line->!line.isEmpty()).collect(java.util.stream.Collectors.joining("\n"));
        if(!networkNotes.isEmpty())head+=networkNotes+"\n\n";
        String sharingBlock=blocks.get("sharing");
        if(sharingBlock==null)sharingBlock=sharingExample();else sharingBlock=describeSharing(sharingBlock);
        text=head+dev.wigo.compat.Java8.stripTrailing(sharingBlock)+"\n\n"+dev.wigo.compat.Java8.stripTrailing((dev.wigo.compat.Java8.stripTrailing(menuBlock)+added))+"\n";
        parse(text);
        if(!text.equals(original))dev.wigo.compat.Java8.write(file,text,StandardCharsets.UTF_8);
    }
}
