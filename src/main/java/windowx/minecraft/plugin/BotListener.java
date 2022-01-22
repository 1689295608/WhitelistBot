package windowx.minecraft.plugin;

import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.ListenerHost;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BotListener implements ListenerHost {
    @EventHandler
    public void onGroupMessage(GroupMessageEvent event) {
        Group group = event.getGroup();
        Member sender = event.getSender();
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
        if (!label.startsWith(WhitelistBot.getConfig("command-prefix", "#"))) {
            return;
        }
        label = label.substring(1);
        if (label.equals(WhitelistBot.REQUEST_WHITELIST)) {
            if (cmds.length < 2) {
                group.sendMessage(new At(sender.getId()).plus("语法: " + WhitelistBot.REQUEST_WHITELIST + " <您的游戏昵称>"));
                return;
            }
            if (WhitelistBot.hasQQ(sender.getId())) {
                group.sendMessage(new At(sender.getId()).plus("一个 QQ 只能绑定一个账号!"));
                return;
            }
            if (WhitelistBot.hasWhitelist(cmds[1])) {
                group.sendMessage(new At(sender.getId()).plus("该账号已被绑定!"));
                return;
            }
            WhitelistBot.addWhitelist(cmds[1], sender.getId());
            group.sendMessage(new At(sender.getId()).plus("申请白名单成功!"));
        } else if (label.equals(WhitelistBot.UNBIND_WHITELIST)) {
            if (!WhitelistBot.hasQQ(sender.getId())) {
                group.sendMessage(new At(sender.getId()).plus("你还没有绑定过账号!"));
                return;
            }
            WhitelistBot.removeWhitelist(WhitelistBot.getPlayer(sender.getId()));
            group.sendMessage(new At(sender.getId()).plus("解除绑定成功!"));
        } else if (label.equals(WhitelistBot.CONFIRM_IP)) {
            if (cmds.length < 2) {
                group.sendMessage(new At(sender.getId()).plus("语法: " + WhitelistBot.CONFIRM_IP + " <验证码>"));
                return;
            }
            Integer code = null;
            try {
                code = Integer.parseInt(cmds[1]);
            } catch (NumberFormatException e) {
                group.sendMessage(new At(sender.getId()).plus("该验证码不正确!"));
            }
            String player = ServerListener.codes.get(code);
            String bind = WhitelistBot.getPlayer(sender.getId());
            if (player == null) {
                group.sendMessage(new At(sender.getId()).plus("该验证码不存在!"));
                return;
            }
            if (!player.equals(bind)) {
                group.sendMessage(new At(sender.getId()).plus("该验证码不属于你!"));
                return;
            }
            String ip = ServerListener.codeIp.get(code);
            ServerListener.lastIp.put(player, ip);
            ServerListener.codes.remove(code);
            ServerListener.codeIp.remove(code);
            group.sendMessage(new At(sender.getId()).plus("验证成功!"));
        }
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
