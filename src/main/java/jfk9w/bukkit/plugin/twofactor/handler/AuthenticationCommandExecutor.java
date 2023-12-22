package jfk9w.bukkit.plugin.twofactor.handler;

import jfk9w.bukkit.plugin.twofactor.common.Events;
import jfk9w.bukkit.plugin.twofactor.common.Players;
import jfk9w.bukkit.plugin.twofactor.event.PlayerAuthCodeEvent;
import jfk9w.bukkit.plugin.twofactor.event.PlayerAuthResetEvent;
import jfk9w.bukkit.plugin.twofactor.service.AuthenticationService;
import jfk9w.bukkit.plugin.twofactor.service.CredentialService;
import jfk9w.bukkit.plugin.twofactor.service.MessageService;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Builder
@RequiredArgsConstructor
public class AuthenticationCommandExecutor implements CommandExecutor {

    @NonNull
    private final Events events;
    @NonNull
    private final Players players;
    @NonNull
    private final AuthenticationService authentication;
    @NonNull
    private final MessageService messages;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !(sender instanceof Player)) {
            return false;
        }

        if (args.length < 1) {
            return help(sender, command, args);
        }

        return switch (command.getName()) {
            case "2fa" -> reset(sender, command, args);
            case "code" -> code(sender, command, args);
            default -> help(sender, command, args);
        };
    }

    private boolean code(CommandSender sender, Command command, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.error(sender, "This method should only be called by a player");
            return false;
        }

        int code;
        try {
            code = Integer.parseInt(args[0]);
        } catch (Exception e) {
            return help(player, command, args);
        }

        events.callEvent(new PlayerAuthCodeEvent(player, code));
        return true;
    }

    private boolean reset(CommandSender sender, Command command, String[] args) {
        if (!sender.isOp() &&
                ((sender instanceof Player player) && !authentication.isAuthenticated(player.getUniqueId()))) {
            messages.error(sender, "You must be authenticated in order to call \"/2fa\"");
            return false;
        }

        var targetName = args[0];
        return players.getPlayer(targetName)
                .map(target -> {
                    events.callEvent(new PlayerAuthResetEvent(sender, target));
                    return true;
                })
                .orElseGet(() -> {
                    messages.error(sender, "Player %s must be online in order to setup 2FA", targetName);
                    return false;
                });
    }

    private boolean help(CommandSender sender, Command command, String[] args) {
        messages.info(sender, command.getUsage());
        return false;
    }
}
