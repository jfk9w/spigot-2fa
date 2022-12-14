package jfk9w.bukkit.plugin.twofactor.service;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import static jfk9w.bukkit.plugin.twofactor.util.Util.ip;

@RequiredArgsConstructor
public class AuthenticationService {

    private final Logger log;

    private final Set<UUID> authenticatedPlayers = new ConcurrentSkipListSet<>();

    public boolean isAuthenticated(UUID playerId) {
        return authenticatedPlayers.contains(playerId);
    }

    public void authenticate(Player player) {
        authenticatedPlayers.add(player.getUniqueId());
        log.info(String.format("Successfully authenticated %s (%s) from %s", player.getName(), player.getUniqueId(), ip(player)));
    }

    public void revoke(Player player) {
        authenticatedPlayers.remove(player.getUniqueId());
        log.info(String.format("Revoked authentication for %s (%s)", player.getName(), player.getUniqueId()));
    }
}
