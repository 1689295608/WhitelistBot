package windowx.minecraft.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class ServerListener implements Listener {
    public static ArrayList<String> login = new ArrayList<>();
    public static HashMap<String, String> lastIp = new HashMap<>();
    public static HashMap<Integer, String> codes = new HashMap<>();
    public static HashMap<Integer, String> codeIp = new HashMap<>();
    public static HashMap<String, Long> lastTime = new HashMap<>();
    public static HashMap<String, TimerTask> messages = new HashMap<>();
    public static HashMap<String, Location> loginLoc = new HashMap<>();

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();
        if (!WhitelistBot.isLoaded) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, WhitelistBot.getLanguage("plugin-not-loaded"));
            return;
        }
        String regex = WhitelistBot.active.getString("player-name");
        if (regex == null) {
            regex = "";
        }
        if (!name.matches(regex)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, WhitelistBot.getLanguage("disallow-player-name"));
            return;
        }
        if (!WhitelistBot.hasWhitelist(name)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, WhitelistBot.getLanguage("no-whitelist"));
            return;
        }
        if (ServerListener.isLogin(name)) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, WhitelistBot.getLanguagef("player-already-online", name));
            return;
        }
        String last = lastIp.get(name);
        if (last == null) {
            return;
        }
        InetAddress address = event.getAddress();
        String ip = address.getHostName();

        if (!last.equals(ip)) {
            if (!codes.containsValue(name)) {
                int code = (int) (999999 * Math.random());
                codes.put(code, name);
                codeIp.put(code, ip);
            }
            String c = "";
            for (int k : codes.keySet()) {
                if (codes.get(k).equals(name)) {
                    c = String.valueOf(k);
                }
            }
            String kickMsg = WhitelistBot.getLanguagef("unsafe-confirm", c);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        Timer message = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (WhitelistBot.isRegistered(name)) {
                    player.sendMessage(WhitelistBot.getLanguage("login-message"));
                } else {
                    player.sendMessage(WhitelistBot.getLanguage("register-message"));
                }
            }
        };
        Thread timedOut = new Thread(() -> {
            try {
                Thread.sleep(60000);
                player.kickPlayer(WhitelistBot.getLanguage("timed-out-login"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timedOut.start();
        message.schedule(task, 0, 5000);
        messages.put(name, task);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        event.setJoinMessage(WhitelistBot.getLanguagef("player-join-game", name));

        Location loc = player.getLocation();
        loginLoc.put(name, loc);
        if(WhitelistBot.loginLoc != null) {
            player.teleport(WhitelistBot.loginLoc);
        }

        // TODO: 正版玩家直接免密登录

        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return;
        }
        String host = address.getHostName();
        if (host.startsWith("192.168.") || host.startsWith("172.") || host.startsWith("10.")) {
            return;
        }
        String ip = lastIp.get(name);
        if (ip == null) {
            return;
        }
        if (!ip.equals(host)) {
            return;
        }
        if (System.currentTimeMillis() - lastTime.get(host) > 3 * 60 * 1000) {
            return;
        }
        forceLogin(player.getName());
        player.sendMessage(WhitelistBot.getLanguage("safe-login"));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        event.setQuitMessage(WhitelistBot.getLanguagef("player-quit-game", name));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        if (cmd.equals("/l") || cmd.equals("/reg")) return;
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerHitPlayer(EntityDamageByEntityEvent event) {
        Entity player = event.getEntity();
        if (player.getType() != EntityType.PLAYER) {
            Entity damager = event.getDamager();
            if (damager.getType() != EntityType.PLAYER) {
                return;
            }
            if (!isLogin(damager.getName())) {
                event.setCancelled(true);
            }
            return;
        }
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerShearEntity(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.PLAYER) {
            return;
        }
        if (!isLogin(entity.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Player player = (Player) event.getPlayer();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!isLogin(player.getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isLogin(player.getName())) {
            login.remove(player.getName());

            InetSocketAddress address = player.getAddress();
            if (address == null) {
                return;
            }
            String host = address.getHostName();
            lastIp.put(player.getName(), host);
            long time = System.currentTimeMillis();
            lastTime.put(host, time);
        }
    }

    public static boolean isLogin(String name) {
        return login.contains(name);
    }

    public static void forceLogin(String name) {
        login.add(name);
        TimerTask message = messages.get(name);
        if (message != null) {
            message.cancel();
        }
        Player player = Bukkit.getPlayer(name);
        if (player == null) return;
        Location backLoc = loginLoc.get(name);
        if (backLoc == null) return;
        player.teleport(backLoc);
    }
}
