package jfk9w.bukkit.plugin.twofactor.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerAuthResetEvent extends PlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter
    private final Player caller;

    public PlayerAuthResetEvent(Player caller, Player who) {
        super(who);
        this.caller = caller;
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
