package ru.woontime.prefixes;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.*;

public final class WoonPrefixes extends JavaPlugin implements CommandExecutor, Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private YamlConfiguration data;
    private File dataFile;
    private final Map<UUID,String> selectedCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "players.yml");
        if (!dataFile.exists()) try { getDataFolder().mkdirs(); dataFile.createNewFile(); } catch(IOException e) { getLogger().severe(e.getMessage()); }
        data = YamlConfiguration.loadConfiguration(dataFile);
        Objects.requireNonNull(getCommand("prefixes")).setExecutor(this);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new PrefixExpansion(this).register();
    }
    public String prefix(String id) { return getConfig().getString("prefixes."+id+".display", ""); }
    public boolean owns(UUID uuid, String id) { return data.getStringList(uuid+".owned").contains(id); }
    public void grant(UUID uuid, String id) {
        if (prefix(id).isEmpty()) return;
        List<String> owned = data.getStringList(uuid+".owned");
        if (!owned.contains(id)) owned.add(id);
        data.set(uuid+".owned", owned); saveData();
    }
    private void saveData() { try { data.save(dataFile); } catch(IOException e) { getLogger().severe("Не удалось сохранить players.yml: "+e.getMessage()); } }
    private String selected(UUID uuid) { return selectedCache.getOrDefault(uuid, ""); }\n    @EventHandler public void onChat(AsyncChatEvent event) {\n        String id=selected(event.getPlayer().getUniqueId()); if(id.isEmpty() || prefix(id).isEmpty()) return;\n        event.renderer((source, displayName, message, viewer) -> mm.deserialize(prefix(id)).append(Component.space()).append(displayName).append(Component.text(": ")).append(message));\n    }

    @Override public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length==0) { s.sendMessage("§d/prefixes list | select <id> | give <player> <id> | remove <player> <id>"); return true; }
        if (a[0].equalsIgnoreCase("list")) {
            if (getConfig().getConfigurationSection("prefixes") == null) return true;
            for(String id:getConfig().getConfigurationSection("prefixes").getKeys(false))
                s.sendMessage("§7- §f"+id+" §8("+PlainTextComponentSerializer.plainText().serialize(mm.deserialize(prefix(id)))+"§8)");
            return true;
        }
        if (a[0].equalsIgnoreCase("select") && s instanceof Player p && a.length>=2) {
            String id=a[1].toLowerCase(Locale.ROOT);
            if (!owns(p.getUniqueId(),id)) { p.sendMessage("§cУ тебя нет этого префикса."); return true; }
            data.set(p.getUniqueId()+".selected",id); selectedCache.put(p.getUniqueId(),id); saveData(); p.sendMessage(mm.deserialize("<green>Выбран префикс: "+prefix(id))); return true;
        }
        if (a[0].equalsIgnoreCase("give") && s.hasPermission("woomprefixes.admin") && a.length>=3) {
            Player target=Bukkit.getPlayerExact(a[1]); String id=a[2].toLowerCase(Locale.ROOT);
            if(target==null || prefix(id).isEmpty()) { s.sendMessage("§cИгрок должен быть онлайн, а ID префикса — существовать."); return true; }
            grant(target.getUniqueId(),id); target.sendMessage(mm.deserialize("<green>Тебе выдан префикс "+prefix(id))); s.sendMessage("§aВыдано."); return true;
        }
        if (a[0].equalsIgnoreCase("remove") && s.hasPermission("woomprefixes.admin") && a.length>=3) {
            Player target=Bukkit.getPlayerExact(a[1]); if(target==null) { s.sendMessage("§cИгрок не в сети."); return true; }
            List<String> owned=data.getStringList(target.getUniqueId()+".owned"); owned.remove(a[2]); data.set(target.getUniqueId()+".owned",owned);
            if(data.getString(target.getUniqueId()+".selected","").equals(a[2])) data.set(target.getUniqueId()+".selected",""); selectedCache.remove(target.getUniqueId());
            saveData(); s.sendMessage("§aУдалено."); return true;
        }
        s.sendMessage("§cНедостаточно прав или неверные аргументы."); return true;
    }
    private final class PrefixExpansion extends PlaceholderExpansion {
        private final WoonPrefixes plugin; PrefixExpansion(WoonPrefixes p){plugin=p;}
        @Override public String getIdentifier(){return "woonprefixes";}
        @Override public String getAuthor(){return "frozitg-prog";}
        @Override public String getVersion(){return plugin.getDescription().getVersion();}
        @Override public boolean persist(){return true;}
        @Override public String onPlaceholderRequest(Player p,String params){
            if(p==null)return "";
            String id=selected(p.getUniqueId());
            if(params.equalsIgnoreCase("selected")) return id.isEmpty()?"":PlainTextComponentSerializer.plainText().serialize(mm.deserialize(prefix(id)));
            if(params.equalsIgnoreCase("selected_mini")) return id.isEmpty()?"":prefix(id);
            if(params.startsWith("has_")) return owns(p.getUniqueId(),params.substring(4))?"true":"false";
            return "";
        }
    }
}