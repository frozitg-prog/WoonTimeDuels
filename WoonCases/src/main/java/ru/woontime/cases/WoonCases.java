package ru.woontime.cases;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
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
    private NamespacedKey hologramKey;

    @Override public void onEnable(){
        saveDefaultConfig();
        locationsFile=new File(getDataFolder(),"cases.yml");
        locations=YamlConfiguration.loadConfiguration(locationsFile);
        hologramKey=new NamespacedKey(this,"case_hologram");
        Objects.requireNonNull(getCommand("wooncases")).setExecutor(this);
        Objects.requireNonNull(getCommand("case")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this,this);
        refreshHolograms();
    }

    private void save(){try{locations.save(locationsFile);}catch(IOException e){getLogger().severe(e.getMessage());}}

    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
        if(c.getName().equalsIgnoreCase("case")) return handleOpenCommand(s,a);
        if(!s.hasPermission("wooncases.admin")){s.sendMessage("§cНет прав.");return true;}
        if(a.length==1&&a[0].equalsIgnoreCase("reload")){
            reloadConfig(); locations=YamlConfiguration.loadConfiguration(locationsFile);
            refreshHolograms(); s.sendMessage("§aWoonCases перезагружен."); return true;
        }
        if(!(s instanceof Player p)){s.sendMessage("Только игрок.");return true;}
        if(a.length>=3&&a[0].equalsIgnoreCase("setblock")){
            String id=a[1].toLowerCase(Locale.ROOT); Material mat=Material.matchMaterial(a[2]);
            if(mat==null||!mat.isBlock()){p.sendMessage("§cНеизвестный материал блока.");return true;}
            Location loc=p.getLocation().getBlock().getLocation().subtract(0,1,0);
            loc.getBlock().setType(mat);
            List<String> entries=locations.getStringList(id);
            String entry=loc.getWorld().getName()+";"+loc.getBlockX()+";"+loc.getBlockY()+";"+loc.getBlockZ();
            if(!entries.contains(entry))entries.add(entry);
            locations.set(id,entries); save(); refreshHolograms();
            p.sendMessage("§aКейс §f"+id+" §aустановлен: "+mat.name());return true;
        }
        if(a.length==2&&a[0].equalsIgnoreCase("remove")){
            if(!locations.contains(a[1])){p.sendMessage("§cКейсы не найдены.");return true;}
            locations.set(a[1],null);save();refreshHolograms();
            p.sendMessage("§aЗаписи кейса удалены.");return true;
        }
        p.sendMessage("§e/wooncases setblock <id> <MATERIAL>");
        p.sendMessage("§e/wooncases remove <id> §7— удалить кейс");
        p.sendMessage("§e/wooncases reload");return true;
    }

    private boolean handleOpenCommand(CommandSender sender,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("§cКоманда доступна только игроку.");return true;}
        if(args.length!=2||!args[0].equalsIgnoreCase("open")){
            p.sendMessage("§eИспользование: /case open <id>");return true;
        }
        String id=args[1].toLowerCase(Locale.ROOT);
        Location caseLoc=findNearbyCase(p,id);
        if(caseLoc==null){
            p.sendMessage(mm.deserialize("<red>Ты должен находиться не дальше <gold>3 блоков</gold> от кейса <white>"+id+"</white>, чтобы открыть его.</red>"));
            return true;
        }
        beginOpening(p,id,caseLoc.clone().add(.5,0,.5));
        return true;
    }

    private Location findNearbyCase(Player player,String id){
        for(String entry:locations.getStringList(id)){
            String[] v=entry.split(";");
            if(v.length!=4||!v[0].equals(player.getWorld().getName()))continue;
            try{
                Location loc=new Location(player.getWorld(),Integer.parseInt(v[1])+.5,Integer.parseInt(v[2]),Integer.parseInt(v[3])+.5);
                if(player.getLocation().distanceSquared(loc)<=9.0)return loc;
            }catch(NumberFormatException ignored){}
        }
        return null;
    }

    @EventHandler(ignoreCancelled=true) public void interact(PlayerInteractEvent e){
        if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null)return;
        Block b=e.getClickedBlock(); String found=null;
        for(String id:locations.getKeys(false)){
            for(String entry:locations.getStringList(id)){
                String[] v=entry.split(";");
                if(v.length==4&&v[0].equals(b.getWorld().getName())){
                    try{
                        if(Integer.parseInt(v[1])==b.getX()&&Integer.parseInt(v[2])==b.getY()&&Integer.parseInt(v[3])==b.getZ()){found=id;break;}
                    }catch(NumberFormatException ignored){}
                }
            }
            if(found!=null)break;
        }
        if(found==null)return;
        e.setCancelled(true);
        Player p=e.getPlayer();
        if(opening.contains(p.getUniqueId())){p.sendMessage("§cТы уже открываешь кейс.");return;}
        String command=getConfig().getString("open-menu-command","dm open %menu% %player%")
                .replace("%menu%",getConfig().getString("cases."+found+".menu",getConfig().getString("menu",found)))
                .replace("%player%",p.getName());
        if(Bukkit.getPluginManager().getPlugin("DeluxeMenus")!=null) Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);
        else p.sendMessage("§cДля выбора кейса установи DeluxeMenus.");
    }

    private void refreshHolograms(){
        for(World world:Bukkit.getWorlds()){
            for(org.bukkit.entity.Entity entity:world.getEntities()){
                if(entity.getPersistentDataContainer().has(hologramKey,PersistentDataType.BYTE))entity.remove();
            }
        }
        for(String id:locations.getKeys(false)){
            String title=getConfig().getString("cases."+id+".title","<gradient:#ff69b4:#ff2020>Кейс</gradient>");
            for(String entry:locations.getStringList(id)){
                String[] v=entry.split(";");
                if(v.length!=4)continue;
                World world=Bukkit.getWorld(v[0]); if(world==null)continue;
                try{
                    Location loc=new Location(world,Integer.parseInt(v[1])+.5,Integer.parseInt(v[2])+2.35,Integer.parseInt(v[3])+.5);
                    TextDisplay display=world.spawn(loc,TextDisplay.class);
                    display.text(mm.deserialize("<font:woontime:fantasy>"+title+"</font>"));
                    display.setBillboard(Display.Billboard.CENTER);
                    display.setAlignment(TextDisplay.TextAlignment.CENTER);
                    display.setSeeThrough(true);
                    display.setShadowed(true);
                    display.setDefaultBackground(false);
                    display.setLineWidth(300);
                    display.setPersistent(true);
                    display.getPersistentDataContainer().set(hologramKey,PersistentDataType.BYTE,(byte)1);
                }catch(NumberFormatException ignored){}
            }
        }
    }

    private ItemDisplay spawnDye(Location location,Material material){
        ItemDisplay display=location.getWorld().spawn(location,ItemDisplay.class);
        display.setItemStack(new ItemStack(material));
        display.setBillboard(Display.Billboard.CENTER);
        display.setPersistent(false);
        display.setInvulnerable(true);
        return display;
    }

    private void playOpeningSound(Location loc,float pitch){
        try{
            Sound sound=Sound.valueOf(getConfig().getString("opening.sound","BLOCK_BEACON_POWER_SELECT"));
            loc.getWorld().playSound(loc,sound,1f,pitch);
        }catch(IllegalArgumentException ignored){}
    }

    private void sendWinMessage(Player p,String reward){
        String display=getConfig().getString("reward-display."+reward,reward);
        Component message=mm.deserialize(
                "<dark_purple>╔══════════════════════════════╗</dark_purple>\n"+
                "<gradient:#ff69b4:#ff2020><bold>       ПОЗДРАВЛЯЕМ!</bold></gradient>\n"+
                "<white>Ты выиграл префикс:</white>\n"+
                "<font:woontime:fantasy>"+display+"</font>\n"+
                "<green>Тебе выдан префикс!</green>\n"+
                "<dark_purple>╚══════════════════════════════╝</dark_purple>\n"+
                "<light_purple><bold>             WOONDUELS</bold></light_purple>");
        p.sendMessage(message);
    }

    private void beginOpening(Player p,String id,Location loc){
        if(!opening.add(p.getUniqueId()))return;
        List<String> rewards=getConfig().getStringList("cases."+id+".rewards");
        if(rewards.isEmpty()){opening.remove(p.getUniqueId());p.sendMessage("§cУ кейса нет наград.");return;}
        String reward=rewards.get(new Random().nextInt(rewards.size()));
        int duration=Math.max(20,getConfig().getInt("opening.duration-ticks",60));
        Particle particle;
        try{particle=Particle.valueOf(getConfig().getString("opening.particle","TOTEM_OF_UNDYING"));}
        catch(IllegalArgumentException ex){particle=Particle.HAPPY_VILLAGER;}
        final Particle effectParticle=particle;
        final List<ItemDisplay> dyeDisplays=new ArrayList<>();
        final Material[] dyeMaterials={Material.RED_DYE,Material.BLUE_DYE,Material.PURPLE_DYE};
        new BukkitRunnable(){int tick=0;
            public void run(){
                if(!p.isOnline()){
                    dyeDisplays.forEach(ItemDisplay::remove); opening.remove(p.getUniqueId());cancel();return;
                }
                for(int i=0;i<dyeMaterials.length;i++){
                    if(tick==i*8){
                        dyeDisplays.add(spawnDye(loc.clone().add(0,.75+i*.48,0),dyeMaterials[i]));
                        playOpeningSound(loc,.85f+i*.18f);
                    }
                }
                for(int i=0;i<dyeDisplays.size();i++){
                    ItemDisplay display=dyeDisplays.get(i);
                    if(display.isValid())display.teleport(loc.clone().add(0,.75+i*.48+Math.sin(tick*.18+i)*.06,0));
                }
                Location at=loc.clone().add(0,1.1+Math.sin(tick*.25)*.35,0);
                loc.getWorld().spawnParticle(effectParticle,at,18,.35,.35,.35,.02);
                if(tick%10==0)playOpeningSound(loc,1.1f+tick/(float)duration);
                if(tick>=duration){
                    loc.getWorld().spawnParticle(Particle.FIREWORK,loc.clone().add(0,1,0),45,.5,.5,.5,.08);
                    try{loc.getWorld().playSound(loc,Sound.valueOf(getConfig().getString("opening.finish-sound","UI_TOAST_CHALLENGE_COMPLETE")),1f,1f);}
                    catch(IllegalArgumentException ignored){}
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"prefixes give "+p.getName()+" "+reward);
                    sendWinMessage(p,reward);
                    dyeDisplays.forEach(ItemDisplay::remove);
                    opening.remove(p.getUniqueId());cancel();
                }
                tick++;
            }
        }.runTaskTimer(this,0L,2L);
    }
}