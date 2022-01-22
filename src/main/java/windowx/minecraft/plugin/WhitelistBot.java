package windowx.minecraft.plugin;

import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.utils.BotConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class WhitelistBot extends JavaPlugin {
    public static Bot bot;
    public static Location loginLoc = null;
    public static boolean isLoaded = false;
    public static List<Long> allowedGroup;
    public static FileConfiguration config;
    public static ConfigurationSection active;
    public static ConfigurationSection languages;
    public static ConfigurationSection commands;
    public static HashMap<String, Long> whitelist = new HashMap<>();
    public static HashMap<String, String> playerdata = new HashMap<>();

    public static String REQUEST_WHITELIST;
    public static String UNBIND_WHITELIST;
    public static String CONFIRM_IP;

    File wlfile;
    File datafile;
    Logger logger;
    String dir;

    /**
     * 获取该玩家是否在白名单内
     * @param name 玩家名
     * @return 该玩家是否在白名单内
     */
    public static boolean hasWhitelist(String name) {
        return whitelist.containsKey(name);
    }

    /**
     * 添加一个玩家到白名单
     * @param name 玩家名
     * @param qq
     */
    public static void addWhitelist(String name, Long qq) {
        whitelist.put(name, qq);
    }

    /**
     * 将一个玩家从白名单中移除
     * @param name 玩家名
     */
    public static void removeWhitelist(String name) {
        whitelist.remove(name);
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            player.kickPlayer(WhitelistBot.getConfig("kick-message"));
        }
    }

    /**
     * 获取该群 ID 是否是启用的
     * @param id 群 ID
     * @return 是否是启用的
     */
    public static boolean isAllowedGroup(long id) {
        return allowedGroup.contains(id);
    }

    /**
     * 通过 QQ 获取绑定的玩家名
     * @param qq QQ
     * @return 玩家名
     */
    public static String getPlayer(long qq) {
        for(String p : whitelist.keySet()) {
            if (whitelist.get(p) == qq) {
                return p;
            }
        }
        return null;
    }

    public static void setPassword(String player, String password) {
        playerdata.put(player, password);
    }

    public static String getPassword(String player) {
        return playerdata.get(player);
    }

    public static boolean isRegistered(String player) {
        return playerdata.containsKey(player);
    }

    public static boolean hasQQ(long qq) {
        return whitelist.containsValue(qq);
    }

    public static long getQQ(String name) {
        return whitelist.get(name);
    }

    public static String getConfig(String key) {
        return active.getString(key);
    }

    public static String getConfig(String key, String def) {
        return active.getString(key, def);
    }

    public static String getLanguage(String key) {
        return languages.getString(key);
    }

    public static String getLanguagef(String key, Object... args) {
        String lang = languages.getString(key);
        if (lang == null) {
            return "";
        }
        return String.format(lang, args);
    }

    public static HashMap<String, String> getAsHashMap(InputStream is) {
        HashMap<String, String> output = new HashMap<>();
        Scanner scan = new Scanner(is);
        while(scan.hasNextLine()) {
            String line = scan.nextLine();
            String trim = line.trim();
            if (trim.startsWith("#")) continue;
            if (!line.contains("=")) continue;
            String[] prop = line.split("=");
            output.put(prop[0], prop[1]);
        }
        return output;
    }

    public static void setExecutorIfNotNull(PluginCommand cmd, CommandExecutor executor) {
        if (cmd == null || executor == null) return;
        cmd.setExecutor(executor);
    }

    @Override
    public void onEnable() {
        logger = this.getLogger();
        logger.info("正在启动 WhitelistBot...");

        this.saveDefaultConfig();

        config = this.getConfig();
        dir = this.getDataFolder().getPath();
        wlfile = new File(dir, "whitelist.ini");
        active = config.getConfigurationSection("active-settings");
        if (active == null) {
            logger.warning("§4配置文件有误，请检查后再试!");
            this.setEnabled(false);
            return;
        }
        commands = active.getConfigurationSection("commands");
        languages = active.getConfigurationSection("languages");
        if (languages == null) {
            logger.warning("§4配置文件有误，请检查后再试!");
            this.setEnabled(false);
            return;
        }
        if (commands != null) {
            REQUEST_WHITELIST = commands.getString("request-whitelist");
            UNBIND_WHITELIST = commands.getString("unbind-whitelist");
            CONFIRM_IP = commands.getString("confirm-ip");
        }
        allowedGroup = active.getLongList("enabled-groups");

        try {
            ConfigurationSection loginWarp = active.getConfigurationSection("login-warp");
            if (loginWarp == null) {
                throw new OtherException("登录点配置不存在");
            }
            boolean enabled = loginWarp.getBoolean("enabled");
            if (!enabled) {
                throw new NothingException();
            }
            ConfigurationSection location = loginWarp.getConfigurationSection("location");
            if (location == null) {
                throw new OtherException("登录点不存在");
            }
            double x = location.getDouble("x");
            double y = location.getDouble("y");
            double z = location.getDouble("z");
            float yaw = (float) location.getDouble("yaw");
            float pitch = (float) location.getDouble("pitch");
            String strworld = location.getString("world");
            if (strworld == null) {
                throw new OtherException("登录点世界不存在");
            }
            World world = Bukkit.getWorld(strworld);
            loginLoc = new Location(world, x, y, z, yaw, pitch);
        } catch (OtherException e) {
            logger.warning("§4登录点加载异常，因为：" + e.getLocalizedMessage());
        } catch (NothingException ignored) {

        }

        ConfigurationSection botConfig = config.getConfigurationSection("bot-settings");
        if (botConfig == null) {
            logger.warning("§4配置文件有误，请检查后再试！");
            this.setEnabled(false);
            return;
        }

        long qq = botConfig.getLong("qq");
        String password = botConfig.getString("password");
        if (password == null || password.isEmpty()) {
            logger.warning("§4密码不能为空！");
            this.setEnabled(false);
            return;
        }
        bot = BotFactory.INSTANCE.newBot(qq, password, new BotConfiguration(){{
            noBotLog();
            noNetworkLog();
            fileBasedDeviceInfo(new File(dir, "device.json").getPath());
        }});
        logger.info("正在尝试登录机器人...");
        try {
            bot.login();
        } catch (Exception e) {
            logger.warning("§4登录失败，详细信息：" + e.getLocalizedMessage());
        }
        if (!bot.isOnline()) {
            this.setEnabled(false);
        }

        logger.info("正在注册机器人事件...");
        GlobalEventChannel.INSTANCE.registerListenerHost(new BotListener());

        logger.info("正在加载白名单数据...");
        try {
            if (!wlfile.exists()) {
                if (!wlfile.createNewFile()) {
                    throw new IOException("can not create file");
                }
            }
            FileInputStream fis = new FileInputStream(wlfile);
            HashMap<String, String> tmp = getAsHashMap(fis);
            for(String k : tmp.keySet()) {
                long q = Long.parseLong(tmp.get(k));
                whitelist.put(k, q);
            }
        } catch (IOException e) {
            logger.warning("§4载入失败，详细信息: " + e.getLocalizedMessage());
            this.setEnabled(false);
        }

        logger.info("正在加载玩家登录数据...");
        try {
            datafile = new File(dir, "data.ini");
            if (!datafile.exists()) {
                if (!datafile.createNewFile()) {
                    throw new IOException("can not create file");
                }
            }
            FileInputStream fis = new FileInputStream(datafile);
            HashMap<String, String> tmp = getAsHashMap(fis);
            for(String k : tmp.keySet()) {
                playerdata.put(k, tmp.get(k));
            }
        } catch (IOException e) {
            logger.warning("§4载入失败，详细信息: " + e.getLocalizedMessage());
            this.setEnabled(false);
        }

        logger.info("正在注册服务器事件...");
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new ServerListener(), this);

        logger.info("正在注册命令执行器...");
        setExecutorIfNotNull(this.getCommand("wb"), new WbCommand());
        setExecutorIfNotNull(this.getCommand("l"), new LCommand());
        setExecutorIfNotNull(this.getCommand("reg"), new RegCommand());

        logger.info("正在将已在线玩家加入已登录列表...");
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (Player player : players) {
            ServerListener.forceLogin(player.getName());
        }

        isLoaded = true;
        logger.info("§a成功启动 WhitelistBot!");
    }

    @Override
    public void onDisable() {
        logger.info("§e正在关闭 WhitelistBot...");

        logger.info("正在将未登录玩家踢出服务器...");
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        for (Player player : players) {
            if (!ServerListener.isLogin(player.getName())) {
                player.kickPlayer(getLanguage("reloading-plugin"));
            }
        }
        if (bot != null) {
            bot.close();
        }
        boolean suc = false;
        try {
            if (!wlfile.exists()) {
                suc = wlfile.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(wlfile);
            PrintWriter pw = new PrintWriter(fos);
            for(String s : whitelist.keySet()) {
                Long wl = whitelist.get(s);
                if (wl == null) continue;
                pw.println(s + "=" + wl);
            }
            pw.close();

            if (!datafile.exists()) {
                suc = datafile.createNewFile();
            }
            fos = new FileOutputStream(datafile);
            pw = new PrintWriter(fos);
            for(String s : playerdata.keySet()) {
                String data = playerdata.get(s);
                if (data == null) continue;
                pw.println(s + "=" + data);
            }
            pw.close();

            suc = true;
        } catch (IOException e) {
            logger.warning("§4保存白名单失败，详细信息: " + e.getLocalizedMessage());
        }
        if (!suc) {
            logger.warning("§4保存白名单数据失败!");
        }
        logger.info("§a已关闭 WhitelistBot!");
    }
}
