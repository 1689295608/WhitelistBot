package windowx.minecraft.plugin;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.code.MiraiCode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
                     §8-§f /wb whitelist remove <player> §7-§f 将一个玩家移出白名单""";
        if (subcmd.equals("whitelist")) {
            if (!sender.hasPermission("whitelistbot.command.whitelist")) {
                sender.sendMessage(WhitelistBot.getLanguage("no-permission"));
                return true;
            }
            switch(args[1]) {
                case "list" -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("§e# §f·----===[§a WhitelistBot §f]===----· §e#");
                    for(String player : WhitelistBot.whitelist.keySet()) {
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
        } else if (subcmd.equals("send")) {
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
            for (int i = 2; i < args.length; i ++) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(args[i]);
            }
            if (sb.isEmpty()) {
                sender.sendMessage("§c发送的消息不能为空!");
                return true;
            }
            group.sendMessage(MiraiCode.deserializeMiraiCode(sb.toString()));
            sender.sendMessage("§a发送成功!");
        } else {
            sender.sendMessage(usage);
        }
        return true;
    }
}
