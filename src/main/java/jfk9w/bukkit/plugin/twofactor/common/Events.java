package jfk9w.bukkit.plugin.twofactor.common;

import org.bukkit.Server;
import org.bukkit.event.Event;

public interface Events {

    void callEvent(Event event);

    static Events wrap(Server server) {
        return server.getPluginManager()::callEvent;
    }
}
