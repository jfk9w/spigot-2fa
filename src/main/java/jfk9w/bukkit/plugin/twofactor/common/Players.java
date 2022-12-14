package jfk9w.bukkit.plugin.twofactor.common;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface Players {

    Optional<Player> getPlayer(String name);

    static Players wrap(Server server) {
        return name -> Optional.ofNullable(server.getPlayer(name));
    }
}
