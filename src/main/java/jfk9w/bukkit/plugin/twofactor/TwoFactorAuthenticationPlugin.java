package jfk9w.bukkit.plugin.twofactor;

import jfk9w.bukkit.plugin.twofactor.common.EventListeners;
import jfk9w.bukkit.plugin.twofactor.common.Events;
import jfk9w.bukkit.plugin.twofactor.common.Players;
import jfk9w.bukkit.plugin.twofactor.handler.AuthenticationCommandExecutor;
import jfk9w.bukkit.plugin.twofactor.handler.AuthenticationEventHandler;
import jfk9w.bukkit.plugin.twofactor.handler.InGameEventHandler;
import jfk9w.bukkit.plugin.twofactor.service.AuthenticationService;
import jfk9w.bukkit.plugin.twofactor.service.CredentialService;
import jfk9w.bukkit.plugin.twofactor.service.MessageService;
import jfk9w.bukkit.plugin.twofactor.storage.JsonFileStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

@SuppressWarnings("unused")
public class TwoFactorAuthenticationPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        var logger = getLogger();
        var messages = new MessageService();
        var events = Events.wrap(getServer());
        var players = Players.wrap(getServer());
        var authentication = new AuthenticationService(logger);
        var credentials = CredentialService.builder()
                .storage(new JsonFileStorage(getDataFolder().toPath()))
                .build();

        var commandExecutor = AuthenticationCommandExecutor.builder()
                .events(events)
                .players(players)
                .authentication(authentication)
                .messages(messages)
                .build();
        var eventHandler = AuthenticationEventHandler.builder()
                .events(events)
                .scheduler((task, delay) -> getServer().getScheduler().scheduleSyncDelayedTask(this, task, delay))
                .log(logger)
                .credentials(credentials)
                .messages(messages)
                .authentication(authentication)
                .build();

        var commands = Stream.of("2fa", "code")
                .map(this::getCommand)
                .toList();
        commands.forEach(cmd -> cmd.setExecutor(commandExecutor));

        InGameEventHandler.register(EventListeners.wrap(this), authentication, credentials, messages, commands);
        getServer().getPluginManager().registerEvents(eventHandler, this);
    }
}
