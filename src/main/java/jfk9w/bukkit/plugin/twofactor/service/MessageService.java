package jfk9w.bukkit.plugin.twofactor.service;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class MessageService {

    private static final String PREFIX = "[2FA] ";

    public void url(Player player, String url, String pattern, String... args) {
        var component = new TextComponent(PREFIX + String.format(pattern, (Object[]) args));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        player.spigot().sendMessage(component);
    }

    public void info(CommandSender sender, String pattern, Object... args) {
        send(sender, null, pattern, args);
    }

    public void success(CommandSender sender, String pattern, Object... args) {
        send(sender, ChatColor.GREEN, pattern, args);
    }

    public void warn(CommandSender sender, String pattern, Object... args) {
        send(sender, ChatColor.YELLOW, pattern, args);
    }

    public void error(CommandSender sender, String pattern, Object... args) {
        send(sender, ChatColor.RED, pattern, args);
    }

    private void send(CommandSender sender, ChatColor color, String pattern, Object[] args) {
        var colorStr = Optional.ofNullable(color).map(ChatColor::toString).orElse("");
        sender.sendMessage(colorStr + PREFIX + String.format(pattern, args));
    }
}
