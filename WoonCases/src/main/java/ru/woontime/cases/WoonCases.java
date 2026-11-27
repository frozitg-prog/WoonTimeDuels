package ru.woontime.cases;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class WoonCases extends JavaPlugin implements Listener, CommandExecutor {
    private final MiniMessage mm=MiniMessage.miniMessage();
    private YamlConfiguration locations;
    private File locationsFile;
    private final Set<UUID> opening=new HashSet<>();
    @Override public void onEnable(){
        saveDefaultConfig(); locationsFile=new File(getDataFolder(),"cases.yml");
        locations=YamlConfiguration.loadConfiguration(locationsFile);
        getCommand("wooncases").setExecutor(this); Bukkit.getPluginManager().registerEvents(this,this);
    }
    private void save(){try{locations.save(locationsFile);}catch(IOException e){getLogger().severe(e.getMessage());}}
    @Override public boolean onCommand(CommandSender s,Command c,String l,String[] a){
        if(!s.hasPermission("wooncases.admin")){s.sendMessage("§cНет прав.");return true;}
        if(a.length==1&&a[0].equalsIgnoreCase("reload")){reloadConfig();locations=YamlConfiguration.loadConfiguration(locationsFile);s.sendMessage("§aПерезагружено.");return true;}
        if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;}
        if(a.length>=3&&a[0].equalsIgnoreCase("setblock")){
            String id=a[1].toLowerCase(Locale.ROOT); Material mat=Material.matchMaterial(a[2]);
            if(mat==null||!mat.isBlock()){p.sendMessage("§cНеизвестный материал блока.");return true;}
            Location loc=p.getLocation().getBlock().getLocation();
            Block b=loc.getBlock(); b.setType(mat);
            String key=id+"."+loc.getWorld().getName()+"."+loc.getBlockX()+"."+loc.getBlockY()+"."+loc.getBlockZ();
            locations.set(key+".world",loc.getWorld().getName()); locations.set(key+".x",loc.getBlockX()); locations.set(key+".y",loc.getBlockY()); locations.set(key+".z",loc.getBlockZ()); locations.set(key+".material",mat.name()); save();
            p.sendMessage("§aКейс §f"+id+" §aустановлен: "+mat.name());return true;
        }
        if(a.length==2&&a[0].equalsIgnoreCase("remove")){
            String id=a[1]; ConfigurationSection sec=locations.getConfigurationSection(id);
            if(sec!=null) locations.set(id,null); else {p.sendMessage("§cКейсы не найдены.");return true;}
            save();p.sendMessage("§aЗаписи кейса удалены.");return true;
        }
        p.sendMessage("§e/wooncases setblock <id> <MATERIAL> §7— поставить кейс под собой");
        p.sendMessage("§e/wooncases remove <id> §7— удалить записи кейса");return true;
    }
    @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEvent e){
        if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null)return;
        Block b=e.getClickedBlock(); String found=null;
        if(locations.getConfigurationSection("")==null)return;
        for(String id:locations.getKeys(false)){
            ConfigurationSection sec=locations.getConfigurationSection(id); if(sec==null)continue;
            for(String key:sec.getKeys(false)){
                ConfigurationSection entry=sec.getConfigurationSection(key); if(entry==null)continue;
                if(entry.getString("world","").equals(b.getWorld().getName())&&entry.getInt("x")==b.getX()&&entry.getInt("y")==b.getY()&&entry.getInt("z")==b.getZ()){found=id;break;}
            }
            if(found!=null)break;
        }
        if(found==null)return;
        e.setCancelled(true); Player p=e.getPlayer();
        String command=getConfig().getString("open-menu-command","dm open %menu% %player%")
                .replace("%menu%",getConfig().getString("menu",found)).replace("%player%",p.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);
        beginOpening(p,found,b.getLocation().add(.5,0,.5));
    }
    private void beginOpening(Player p,String id,Location loc){
        if(!opening.add(p.getUniqueId()))return;
        List<String> rewards=getConfig().getStringList("cases."+id+".rewards");
        if(rewards.isEmpty()){opening.remove(p.getUniqueId());p.sendMessage("§cУ кейса нет наград.");return;}
        String reward=rewards.get(new Random().nextInt(rewards.size()));
        int duration=Math.max(20,getConfig().getInt("opening.duration-ticks",60));
        String particleName=getConfig().getString("opening.particle","TOTEM_OF_UNDYING");
        Particle particle; try{particle=Particle.valueOf(particleName);}catch(IllegalArgumentException ex){particle=Particle.HAPPY_VILLAGER;}
        final Particle fx=particle;
        new BukkitRunnable(){int tick=0;
            public void run(){
                if(!p.isOnline()){opening.remove(p.getUniqueId());cancel();return;}
                Location at=loc.clone().add(0,1.1+Math.sin(tick*.25)*.35,0);
                loc.getWorld().spawnParticle(fx,at,18,.35,.35,.35,.02);
                if(tick%10==0){String sound=getConfig().getString("opening.sound","BLOCK_BEACON_POWER_SELECT");try{loc.getWorld().playSound(loc,Sound.valueOf(sound),1f,1.1f+tick/(float)duration);}catch(IllegalArgumentException ignored){}}
                if(tick>=duration){
                    loc.getWorld().spawnParticle(Particle.FIREWORK,loc.clone().add(0,1,0),45,.5,.5,.5,.08);
                    try{loc.getWorld().playSound(loc,Sound.valueOf(getConfig().getString("opening.finish-sound","UI_TOAST_CHALLENGE_COMPLETE")),1f,1f);}catch(IllegalArgumentException ignored){}
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"prefixes give "+p.getName()+" "+reward);
                    String message=getConfig().getString("opening.message","<green>Получен префикс <reward>!");
                    p.sendMessage(mm.deserialize(message.replace("<player>",p.getName()).replace("<reward>",reward)));
                    opening.remove(p.getUniqueId());cancel();
                } tick++;
            }
        }.runTaskTimer(this,0L,2L);
    }
}