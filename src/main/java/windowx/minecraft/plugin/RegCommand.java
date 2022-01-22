package windowx.minecraft.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (WhitelistBot.isRegistered(sender.getName())) {
            sender.sendMessage(WhitelistBot.getLanguage("already-registered"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(WhitelistBot.getLanguage("register-usage"));
            return true;
        }
        String password = args[0];
        String repassword = args[1];
        if (!password.equals(repassword)) {
            sender.sendMessage(WhitelistBot.getLanguage("password-not-same"));
            return true;
        }
        String md5;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(password.getBytes(StandardCharsets.UTF_8));
            md5 = new BigInteger(1, digest.digest()).toString(32);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            sender.sendMessage(WhitelistBot.getLanguage("fail-register"));
            return true;
        }
        WhitelistBot.setPassword(sender.getName(), md5);
        ServerListener.forceLogin(sender.getName());
        sender.sendMessage(WhitelistBot.getLanguage("succeed-register"));
        return true;
    }
}
