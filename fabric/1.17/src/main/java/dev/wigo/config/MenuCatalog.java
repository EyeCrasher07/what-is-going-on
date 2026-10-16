package dev.wigo.config;

import java.util.*;

/** Stable identifiers shared by every loader; never uses translated or obfuscated class names. */
public final class MenuCatalog {
    public record Menu(String id, String english, String russian, List<String> titles) {}
    public static final String OTHER = "wigo:other_vanilla";
    public static final String UNSUPPORTED = "wigo:unsupported";
    public static final String SETTINGS = "wigo:settings";
    public static final String ENDER_CHEST = "minecraft:ender_chest";
    private static final List<Menu> menus = new ArrayList<>();
    private static final Map<String, Menu> byId = new LinkedHashMap<>();
    private static final Map<String, String> byTitle = new HashMap<>();
    static {
        for (int rows=1; rows<=6; rows++) add("generic_9x"+rows,"Chests: "+rows+" rows","Сундуки: "+rows+" рядов");
        add("ender_chest", "Ender chests", "Эндер-сундуки");
        add("generic_3x3","Dispensers and droppers","Раздатчики и выбрасыватели");
        add("shulker_box","Shulker boxes","Шалкеровые ящики");
        add("hopper","Hoppers","Воронки");
        add("furnace","Furnaces","Печи");
        add("blast_furnace","Blast furnaces","Плавильные печи");
        add("smoker","Smokers","Коптильни");
        add("brewing_stand","Brewing stands","Зельеварки");
        add("crafting","Crafting tables and recipe books","Верстаки и их книги рецептов");
        add("crafter_3x3","Crafters","Автоматические верстаки");
        add("anvil","Anvils","Наковальни");
        add("enchantment","Enchanting tables","Чародейские столы");
        add("beacon","Beacons","Маяки");
        add("grindstone","Grindstones","Точила");
        add("cartography_table","Cartography tables","Столы картографа");
        add("loom","Looms","Ткацкие станки");
        add("smithing","Smithing tables","Кузнечные столы");
        add("legacy_smithing","Legacy smithing tables (1.19.4)","Старые кузнечные столы (1.19.4)");
        add("stonecutter","Stonecutters","Камнерезы");
        add("merchant","Villager trading","Торговля с жителями");
        add("lectern","Lecterns","Кафедры");
        add("inventory","Survival inventory, effects and recipe book","Инвентарь выживания, эффекты и книга рецептов");
        add("creative_inventory","Creative inventory and tabs","Инвентарь и вкладки креатива");
        add("horse_inventory","Horse, donkey and other mount inventories","Инвентари лошадей, ослов и других ездовых животных");
        add("chat","Chat drafts and nearby draft preview","Черновики чата и их предпросмотр", "chat_screen.title");
        add("pause","Pause menu","Меню паузы", "menu.game", "menu.paused");
        add("advancements","Advancements","Достижения", "gui.advancements");
        add("statistics","Statistics: all tabs","Статистика: все вкладки", "gui.stats");
        add("game_mode","F3+F4 game mode switcher","Переключение режима игры F3+F4");
        add("book_edit","Writing books","Редактирование книг", "book.edit.title", "book.editTitle");
        add("book_view","Reading books","Чтение книг", "book.view.title");
        add("book_sign","Signing books","Подписание книг", "book.sign.title");
        add("sign_edit","Editing signs","Редактирование табличек", "sign.edit");
        add("hanging_sign_edit","Editing hanging signs","Редактирование подвесных табличек", "hanging_sign.edit");
        add("command_block","Command blocks","Командные блоки", "advMode.setCommand");
        add("command_minecart","Command block minecarts","Вагонетки с командным блоком");
        add("structure_block","Structure blocks","Блоки-конструкторы");
        add("jigsaw_block","Jigsaw blocks","Блоки пазла");
        add("test_block","Test blocks","Тестовые блоки", "block.minecraft.test_block");
        add("test_instance_block","Test instance blocks","Блоки экземпляра теста", "block.minecraft.test_instance_block");
        add("options","Main game settings","Главное меню настроек", "options.title");
        add("video_settings","Video settings","Настройки графики", "options.videoTitle");
        add("sound_settings","Music and sound settings","Музыка и звуки", "options.sounds.title");
        add("controls","Controls","Управление", "controls.title");
        add("key_binds","Key bindings","Назначение клавиш", "controls.keybinds.title");
        add("mouse_settings","Mouse settings","Настройки мыши", "options.mouse_settings.title");
        add("skin_settings","Skin customization","Настройки внешнего вида", "options.skinCustomisation.title");
        add("language","Language selection","Выбор языка", "options.language.title");
        add("font_settings","Font settings","Настройки шрифта", "options.font.title");
        add("chat_settings","Chat settings","Настройки чата", "options.chat.title");
        add("accessibility","Accessibility settings","Специальные возможности", "options.accessibility.title");
        add("online_settings","Online settings","Сетевые настройки", "options.online.title");
        add("resource_packs","Resource pack selection","Выбор наборов ресурсов", "resourcePack.title");
        add("telemetry","Telemetry settings","Настройки телеметрии", "options.telemetry.title");
        add("credits","Credits and attribution","Титры и лицензии", "options.credits_and_attribution");
        add("world_options","World options","Настройки мира", "options.worldOptions.title");
        add("game_rules","Game rules","Правила игры", "editGamerule.title");
        add("debug_options","Debug options","Настройки отладки", "debug.options.title");
        add("social_interactions","Social interactions","Социальные взаимодействия", "gui.socialInteractions.title");
        add("friends","Friends list","Список друзей", "options.friendsList", "friends.title");
        add("report_player","Player reporting","Жалобы на игроков", "gui.abuseReport.title");
        add("report_chat","Chat reports","Жалобы на сообщения", "gui.chatReport.title");
        add("report_name","Name reports","Жалобы на никнеймы", "gui.abuseReport.name.title");
        add("report_skin","Skin reports","Жалобы на скины", "gui.abuseReport.skin.title");
        add("report_reason","Report reason selection","Выбор причины жалобы", "gui.abuseReport.reason.title");
        add("report_chat_selection","Report message selection","Выбор сообщений для жалобы", "gui.chatSelection.title");
        entry(OTHER,"Other vanilla screens and confirmation dialogs","Прочие ванильные экраны и окна подтверждения");
        entry(UNSUPPORTED,"Name of an unsupported menu (no contents)","Название неподдерживаемого меню (без содержимого)");
        entry(SETTINGS,"WIGO settings (F8)","Настройки WIGO (F8)");
    }
    private MenuCatalog() {}
    private static void add(String name,String english,String russian,String... titles) {entry("minecraft:"+name,english,russian,titles);}
    private static void entry(String id,String english,String russian,String... titles) {
        Menu menu=new Menu(id,english,russian,List.of(titles));
        if(byId.put(id,menu)!=null)throw new IllegalStateException("Duplicate menu "+id);
        menus.add(menu);
        for(String title:titles)if(byTitle.put(title,id)!=null)throw new IllegalStateException("Duplicate title "+title);
    }
    public static List<Menu> menus() {return Collections.unmodifiableList(menus);}
    public static boolean known(String id) {return byId.containsKey(id);}
    public static String forTitle(String translationKey) {return byTitle.getOrDefault(translationKey,OTHER);}
}
