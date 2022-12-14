package jfk9w.bukkit.plugin.twofactor.common;

import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public abstract class EventListeners implements Listener {

    public abstract void register(HandlerList handlerList, EventExecutor executor, EventPriority priority, boolean ignoreCancelled);

    public abstract void register(Listener listener);

    public static EventListeners wrap(Plugin plugin) {
        return new EventListeners() {

            @Override
            public void register(HandlerList handlerList, EventExecutor executor, EventPriority priority, boolean ignoreCancelled) {
                handlerList.register(new RegisteredListener(this, executor, priority, plugin, ignoreCancelled));
            }

            @Override
            public void register(Listener listener) {
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            }
        };
    }
}
