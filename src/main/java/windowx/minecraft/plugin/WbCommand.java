package windowx.minecraft.plugin;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.code.MiraiCode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WbCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String subcmd = "";
        if (args.length > 0) {
            subcmd = args[0];
        }
        String usage = """
                    §e# §f·----===[§a WhitelistBot §f]===----· §e#
                     §8-§f /wb whitelist list §7-§f 列出所有白名单内的玩家
                     §8-§f /wb whitelist add <player> <qq> §7-§f 添加一个玩家到白名单内
                     §8-§f /wb whitelist get <player> §7-§f 查询一个玩家绑定的 QQ
                     §8-§f /wb whitelist remove <player> §7-§f 将一个玩家移出白名单
                     §8-§f /wb debug kick <player> <messages...> §7-§f 踢出一个玩家
                     §8-§f /wb debug delay-kick <player> <delay> <messages...> §7-§f 等待一段时间后踢出一个玩家""";
        switch (subcmd) {
            case "whitelist" -> {
                if (!sender.hasPermission("whitelistbot.command.whitelist")) {
                    sender.sendMessage(WhitelistBot.getLanguage("no-permission"));
                    return true;
                }
                switch (args[1]) {
                    case "list" -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("§e# §f·----===[§a WhitelistBot §f]===----· §e#");
                        for (String player : WhitelistBot.whitelist.keySet()) {
                            sb.append("\n")
                                    .append(" §7-§f ")
                                    .append(player)
                                    .append(": ")
                                    .append(WhitelistBot.whitelist.get(player));
                        }
                        sender.sendMessage(sb.toString());
                    }
                    case "add" -> {
                        if (args.length < 4) {
                            sender.sendMessage("语法: /wb whitelist add <player> <qq>");
                            return true;
                        }
                        try {
                            String player = args[2];
                            long qq = Long.parseLong(args[3]);
                            WhitelistBot.addWhitelist(player, qq);
                            sender.sendMessage("§a成功添加白名单!");
                        } catch (NumberFormatException e) {
                            sender.sendMessage("§c" + e.getLocalizedMessage());
                            return true;
                        }
                    }
                    case "get" -> {
                        if (args.length < 3) {
                            sender.sendMessage("语法: /wb whitelist get <player>");
                            return true;
                        }
                        String player = args[2];
                        if (!WhitelistBot.hasWhitelist(player)) {
                            sender.sendMessage("§e该玩家并不在白名单内!");
                            return true;
                        }
                        sender.sendMessage("§a玩家 " + player + " 所绑定的 QQ 为: " + WhitelistBot.getQQ(player));
                    }
                    case "remove" -> {
                        if (args.length < 3) {
                            sender.sendMessage("语法: /wb whitelist get <player>");
                            return true;
                        }
                        String player = args[2];
                        if (!WhitelistBot.hasWhitelist(player)) {
                            sender.sendMessage("§e该玩家并不在白名单内!");
                            return true;
                        }
                        WhitelistBot.removeWhitelist(player);
                        sender.sendMessage("§a已将玩家 " + player + " 移出白名单!");
                    }
                    default -> {
                        sender.sendMessage(usage);
                        return true;
                    }
                }
            }
            case "send" -> {
                if (args.length < 3) {
                    sender.sendMessage("语法: /wb send <group> <messages...>");
                }
                long gid = Long.parseLong(args[1]);
                Group group = WhitelistBot.bot.getGroup(gid);
                if (group == null) {
                    sender.sendMessage("§c该群不存在!");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(args[i]);
                }
                if (sb.isEmpty()) {
                    sender.sendMessage("§c发送的消息不能为空!");
                    return true;
                }
                group.sendMessage(MiraiCode.deserializeMiraiCode(sb.toString()));
                sender.sendMessage("§a发送成功!");
            }
            case "debug" -> {
                if (!sender.hasPermission("whitelistbot.command.debug")) {
                    sender.sendMessage(WhitelistBot.getLanguage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(usage);
                    return true;
                }
                switch (args[1]) {
                    case "kick" -> {
                        if (args.length < 4) {
                            sender.sendMessage("语法: /wb debug kick <player> <messages...>");
                            return true;
                        }
                        Player player = Bukkit.getPlayer(args[2]);
                        if (player == null) {
                            sender.sendMessage("§4该玩家不在线!");
                            return true;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (int i = 3; i < args.length; i++) {
                            if (!sb.isEmpty()) sb.append(" ");
                            sb.append(args[i]);
                        }
                        player.kickPlayer(sb.toString());
                        sender.sendMessage("§a已踢出该玩家!");
                    }
                    case "delay-kick" -> {
                        if (args.length < 5) {
                            sender.sendMessage("语法: /wb debug delay-kick <player> <delay> <messages...>");
                            return true;
                        }
                        Player player = Bukkit.getPlayer(args[2]);
                        if (player == null) {
                            sender.sendMessage("§4该玩家不在线!");
                            return true;
                        }
                        long delay = Long.parseLong(args[3]);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 4; i < args.length; i++) {
                            if (!sb.isEmpty()) sb.append(" ");
                            sb.append(args[i]);
                        }
                        Bukkit.getScheduler().runTaskLater(WhitelistBot.plugin, () -> {
                            player.kickPlayer(sb.toString());
                            sender.sendMessage("§a已踢出该玩家!");
                        }, delay);
                        sender.sendMessage("§a线程已创建并执行, 等待执行结果...");
                    }
                    default -> sender.sendMessage(usage);
                }
            }
            default -> sender.sendMessage(usage);
        }
        return true;
    }
}
