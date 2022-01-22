package windowx.minecraft.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!WhitelistBot.isRegistered(sender.getName())) {
            sender.sendMessage(WhitelistBot.getLanguage("not-registered"));
            return true;
        }
        if (ServerListener.isLogin(sender.getName())) {
            sender.sendMessage(WhitelistBot.getLanguage("already-login"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(WhitelistBot.getLanguage("login-usage"));
            return true;
        }
        String password = args[0];
        String md5 = WhitelistBot.getPassword(sender.getName());
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            password = new BigInteger(1, digest.digest()).toString(32);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            sender.sendMessage(WhitelistBot.getLanguage("fail-login"));
            return true;
        }
        if(!password.equals(md5)) {
            sender.sendMessage(WhitelistBot.getLanguage("wrong-password"));
            return true;
        }
        ServerListener.forceLogin(sender.getName());
        sender.sendMessage(WhitelistBot.getLanguage("succeed-login"));
        return true;
    }
}
