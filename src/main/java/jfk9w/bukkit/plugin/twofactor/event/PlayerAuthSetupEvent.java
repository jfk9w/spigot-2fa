package jfk9w.bukkit.plugin.twofactor.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerAuthSetupEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PlayerAuthSetupEvent(Player who) {
        super(who);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
