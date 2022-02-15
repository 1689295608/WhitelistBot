package windowx.minecraft.plugin;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.MessageChain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;

public class BotListener implements ListenerHost {
    public static ArrayList<Long> bannedQQ = new ArrayList<>();
    public static String COMMAND_PREFIX;
    public static String REQUEST_WHITELIST;
    public static String UNBIND_WHITELIST;
    public static String CONFIRM_IP;
    public static String EXECUTE;

    @EventHandler
    public void onGroupMessage(GroupMessageEvent event) {
        Group group = event.getGroup();
        Member sender = event.getSender();
        long id = sender.getId();
        if (!WhitelistBot.isAllowedGroup(group.getId())) {
            return;
        }
        MessageChain chain = event.getMessage();
        String message = chain.serializeToMiraiCode();
        String[] cmds = message.split(" ");
        if (cmds.length < 1) {
            return;
        }
        String label = cmds[0];

        /* Debugging Logging
        System.out.printf("[%s] %s: %s\n",
                group.getName(),
                sender.getNick(),
                message
        );
        */

        if (!label.startsWith(COMMAND_PREFIX)) {
            return;
        }
        if (bannedQQ != null) {
            if (bannedQQ.contains(sender.getId())) {
                group.sendMessage(buildRespond("banned-qq", id));
                return;
            }
        }
        label = label.substring(1);
        if (label.equals(REQUEST_WHITELIST)) {
            if (cmds.length < 2) {
                group.sendMessage(buildRespond("request-whitelist-usage", id, COMMAND_PREFIX, REQUEST_WHITELIST));
                return;
            }
            if (WhitelistBot.hasQQ(sender.getId())) {
                group.sendMessage(buildRespond("already-bound", id));
                return;
            }
            if (WhitelistBot.hasWhitelist(cmds[1])) {
                group.sendMessage(buildRespond("name-already-bound", id, cmds[1]));
                return;
            }
            String regex = WhitelistBot.active.getString("player-name");
            if (regex == null) {
                regex = "";
            }
            if (!cmds[1].matches(regex)) {
                group.sendMessage(buildRespond("disallow-player-name", id));
                return;
            }
            WhitelistBot.addWhitelist(cmds[1], sender.getId());
            group.sendMessage(buildRespond("request-whitelist-success", id));
            try {
                WhitelistBot.saveData(WhitelistBot.wlfile, WhitelistBot.whitelist);
            } catch (OtherException | IOException e) {
                e.printStackTrace();
                WhitelistBot.logger.warning("§4热保存白名单数据失败!");
            }
        } else if (label.equals(UNBIND_WHITELIST)) {
            if (!WhitelistBot.hasQQ(sender.getId())) {
                group.sendMessage(buildRespond("not-bound", id));
                return;
            }
            WhitelistBot.removeWhitelist(WhitelistBot.getPlayer(sender.getId()));
            group.sendMessage(buildRespond("success-unbind", id));
        } else if (label.equals(CONFIRM_IP)) {
            if (cmds.length < 2) {
                group.sendMessage(buildRespond("confirm-ip-usage", id, COMMAND_PREFIX, CONFIRM_IP));
                return;
            }
            Integer code = null;
            try {
                code = Integer.parseInt(cmds[1]);
            } catch (NumberFormatException e) {
                group.sendMessage(buildRespond("wrong-code", id));
            }
            String player = ServerListener.codes.get(code);
            String bind = WhitelistBot.getPlayer(sender.getId());
            if (player == null) {
                group.sendMessage(buildRespond("code-not-exists", id));
                return;
            }
            if (!player.equals(bind)) {
                group.sendMessage(buildRespond("code-not-self", id));
                return;
            }
            String ip = ServerListener.codeIp.get(code);
            ServerListener.lastIp.put(player, ip);
            ServerListener.codes.remove(code);
            ServerListener.codeIp.remove(code);
            group.sendMessage(buildRespond("confirm-ip-success", id));
        } else if (label.equals(EXECUTE)) {
            if (!WhitelistBot.admins.contains(sender.getId())) {
                group.sendMessage(buildRespond("not-admin", id));
                return;
            }
            if (cmds.length < 2) {
                group.sendMessage(buildRespond("execute-usage", id, COMMAND_PREFIX, EXECUTE));
                return;
            }
            // TODO: Run Command and return Command Messages...
            // Bukkit.dispatchCommand();
        }
    }

    public static MessageChain buildRespond(String language, Object... args) {
        String message = WhitelistBot.getLanguage(language);
        return MiraiCode.deserializeMiraiCode(String.format(message, args));
    }

    @EventHandler
    public void onLeave(MemberLeaveEvent event) {
        Member member = event.getMember();
        String name = WhitelistBot.getPlayer(member.getId());
        if (name != null) {
            WhitelistBot.removeWhitelist(name);
            Player player = Bukkit.getPlayer(name);
            if (player != null) {
                player.kickPlayer(WhitelistBot.getLanguage("removed-whitelist"));
            }
        }
    }
}
