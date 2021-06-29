package ubiquitaku.deathavoidance;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DeathAvoidance extends JavaPlugin {
    FileConfiguration config;
    Map<String,String> map = new HashMap<>();
    String prefix;
    boolean use;
    String name;
    String mate;
    List<String> lor;


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        config = getConfig();
        dataLoad();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        mapDisassembly();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("deatha")) {
            if (!sender.hasPermission("deatha.op")) {
                sender.sendMessage("§c§lあなたはこのコマンドを実行する権限を持っていません");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(prefix);
                sender.sendMessage("---------------------------------------");
                sender.sendMessage("/deatha on : プラグインの使用を可能にします");
                sender.sendMessage("/deatha off : プラグインの使用を不可能にします");
                sender.sendMessage("/deatha reload : 設定ファイルをリロードします");
                sender.sendMessage("/deatha get <個数>: プラグインで使用されるアイテムを入手します");
                sender.sendMessage("/deatha set : メインハンドに持っているアイテムの情報を取得し\n\tプラグインで使うアイテムの情報を上書きします");
                sender.sendMessage("---------------------------------------");
                return true;
            }
            if (args[0].equals("on")) {
                if (use) {
                    sender.sendMessage(prefix+"既にonになっています");
                    return true;
                }
                use = true;
                config.set("use",true);
                saveConfig();
                config = getConfig();
                sender.sendMessage(prefix+"onにしました");
                return true;
            }
            if (args[0].equals("off")) {
                if (!use) {
                    sender.sendMessage(prefix+"既にoffになっています");
                    return true;
                }
                use = false;
                config.set("use",false);
                saveConfig();
                config = getConfig();
                sender.sendMessage(prefix+"offにしました");
                return true;
            }
            if (args[0].equals("get")) {
                if (args.length != 2) {
                    sender.sendMessage(prefix+"コマンドの中身の個数が間違っています");
                    return true;
                }
                int amo = 0;
                try {
                    amo = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    sender.sendMessage(prefix+"個数に数字を入力してください");
                    return true;
                }
                ItemStack stack = new ItemStack(Material.getMaterial(mate));
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(name);
                meta.setLore(lor);
                stack.setItemMeta(meta);
                stack.setAmount(amo);
                Player p = (Player) sender;
                p.getInventory().addItem(stack);
                return true;
            }
            if (args[0].equals("set")) {
                Player p = (Player) sender;
                Material stack = p.getInventory().getItemInMainHand().getType();
                ItemMeta meta = p.getInventory().getItemInMainHand().getItemMeta();
                config.set("Item.material",stack.name());
                config.set("Item.name",meta.getDisplayName());
                config.set("Item.lore",(List<String>)meta.getLore());
                saveConfig();
                config = getConfig();
                sender.sendMessage(prefix+"保存されました");
                return true;
            }
            if (args[0].equals("reload")) {
                reloadConfig();
                config = getConfig();
                dataLoad();
                sender.sendMessage(prefix+"リロード完了");
                return true;
            }
        }
        return true;
    }

    public void dataLoad() {
        mapMake();
        use = config.getBoolean("use");
        prefix = config.getString("Item.name");
        name = config.getString("ITem.name");
        mate = config.getString("Item.material");
        lor = (List<String>) config.getList("ITem.lore");
    }

    @EventHandler
    public void death(EntityDamageEvent e) {
        if (!use) {
            return;
        }
        Player p;
        try {
            p = (Player) e.getEntity();
            if (!Bukkit.getOnlinePlayers().contains(p)) return;
        } catch (NullPointerException exception) {
            return;
        }
        if (p.getHealth()>e.getDamage()) {
            return;
        }
        if (!map.containsKey(p.getName())) {
            return;
        }
            String[] str = map.get(e.getEntity().getName()).split("/");
            e.getEntity().teleport(new Location(Bukkit.getWorld(str[0]),Integer.parseInt(str[1]),Integer.parseInt(str[2]),Integer.parseInt(str[3])));
            map.remove(e.getEntity().getName());
            p.sendMessage(prefix+"致死ダメージを受けたためストックを消費して復活しました");
    }


    @EventHandler
    public void onSet(PlayerInteractEvent e) {
        if (!use) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (!e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(config.getString("Item.name"))) {
            return;
        }
        Location loc = e.getPlayer().getLocation();
        ItemStack stack = new ItemStack(Material.getMaterial(mate));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lor);
        stack.setItemMeta(meta);
        e.getPlayer().getInventory().remove(stack);
        map.put(e.getPlayer().getName(),loc.getWorld().getName()+"/"+loc.getBlockX()+"/"+loc.getBlockY()+"/"+loc.getBlockZ());
        e.getPlayer().sendMessage(prefix+"復活地点を登録しました");
    }

    public void mapMake() {
        for(int i = 0; i < config.getList("map.name").size(); i++) {
            String s = config.getList("map.name").get(i).toString();
            map.put(s, (String) config.getList("map.location").get(i));
        }
    }

    public void mapDisassembly() {
        config.set("map.name",map.keySet());
        config.set("map.location",map.values());
        saveConfig();
    }
}
