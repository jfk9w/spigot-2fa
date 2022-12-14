package jfk9w.bukkit.plugin.twofactor.handler;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import jfk9w.bukkit.plugin.twofactor.common.Credential;
import jfk9w.bukkit.plugin.twofactor.common.Events;
import jfk9w.bukkit.plugin.twofactor.common.Scheduler;
import jfk9w.bukkit.plugin.twofactor.event.PlayerAuthCodeEvent;
import jfk9w.bukkit.plugin.twofactor.event.PlayerAuthResetEvent;
import jfk9w.bukkit.plugin.twofactor.event.PlayerAuthSetupEvent;
import jfk9w.bukkit.plugin.twofactor.service.AuthenticationService;
import jfk9w.bukkit.plugin.twofactor.service.CredentialService;
import jfk9w.bukkit.plugin.twofactor.service.MessageService;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static jfk9w.bukkit.plugin.twofactor.util.Util.ip;

@Builder
@RequiredArgsConstructor
public class AuthenticationEventHandler implements Listener {

    @NonNull
    private final Scheduler scheduler;
    @NonNull
    private final Events events;
    @NonNull
    private final Logger log;
    @NonNull
    private final CredentialService credentials;
    @NonNull
    private final AuthenticationService authentication;
    @NonNull
    private final MessageService messages;

    private final GoogleAuthenticator authenticator = new GoogleAuthenticator();
    private final Map<UUID, PendingSetup> pendingSetups = new ConcurrentHashMap<>();

    @EventHandler
    @SuppressWarnings("unused")
    void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        scheduler.scheduleSyncDelayedTask(() -> {
            if (!authentication.isAuthenticated(player.getUniqueId()) && player.isOnline()) {
                log.info(String.format(
                        "%s (%s) from %s is being kicked due to failing authentication in 3 minutes since joining",
                        player.getName(), player.getUniqueId(), ip(player)));

                player.kickPlayer("[2FA] You need to authenticate with \"/code <code>\"");
            }
        }, 3 * 60 * 20);

        credentials.getCredential(player.getUniqueId()).ifPresentOrElse(
                credential -> join(player, credential),
                () -> events.callEvent(new PlayerAuthSetupEvent(player)));
    }

    @EventHandler
    @SuppressWarnings("unused")
    void onPlayerQuit(PlayerQuitEvent event) {
        authentication.revoke(event.getPlayer());
    }

    @EventHandler
    @SuppressWarnings("unused")
    void onPlayerAuthSetup(PlayerAuthSetupEvent event) {
        var player = event.getPlayer();
        credentials.getCredential(player.getUniqueId()).ifPresentOrElse(
                credential -> messages.warn(player, "You have already set up authentication"),
                () -> setup(player));
    }

    @EventHandler
    @SuppressWarnings("unused")
    void onPlayerAuthCode(PlayerAuthCodeEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        Stream.concat(
                        Optional.ofNullable(pendingSetups.get(playerId))
                                .map(PendingSetup::key)
                                .filter(StringUtils::isNotEmpty)
                                .map(key -> Credential.builder().key(key).build())
                                .stream(),
                        credentials.getCredential(playerId).stream())
                .findFirst()
                .ifPresentOrElse(
                        credential -> authenticate(player, credential, event.getCode()),
                        () -> events.callEvent(new PlayerAuthSetupEvent(player)));
    }

    @EventHandler
    @SuppressWarnings("unused")
    void onPlayerAuthReset(PlayerAuthResetEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        if (!credentials.deleteCredential(playerId)) {
            messages.error(event.getCaller(), "Failed to delete 2FA credentials due to internal error");
            return;
        }

        authentication.revoke(player);
        pendSetup(playerId, null);
        events.callEvent(new PlayerAuthSetupEvent(player));
    }

    private void join(Player player, Credential credential) {
        if (StringUtils.equalsIgnoreCase(ip(player), credential.getIp())) {
            messages.success(player, "You were authenticated automatically");
            authentication.authenticate(player);
            return;
        }

        messages.warn(player, "Please authenticate by entering \"/code <code>\"");
    }

    private void authenticate(Player player, Credential credential, int code) {
        var playerId = player.getUniqueId();
        if (authenticator.authorize(credential.getKey(), code)) {
            if (!credentials.saveCredential(playerId, credential.withIp(ip(player)))) {
                messages.error(player, "Failed to save 2FA credentials due to internal error");
                return;
            }

            pendingSetups.remove(playerId);
            authentication.authenticate(player);
            messages.success(player, "Authenticated successfully");
            return;
        }

        messages.error(player, "Invalid authentication code");
    }

    private void setup(Player player) {
        var playerId = player.getUniqueId();
        if (!pendingSetups.containsKey(playerId) && !(player.isOp() && credentials.getCredential(playerId).isPresent())) {
            messages.warn(player, "You are required to setup 2FA");
            messages.warn(player, "Please ask an authenticated player to call \"/2fa <your_name>\" in order to setup authentication");
            return;
        }

        var key = authenticator.createCredentials().getKey();
        pendSetup(playerId, key);

        var qrCodeUrl = String.format(
                "https://www.google.com/chart?chs=512x512&cht=qr&chl=otpauth://totp/%s?secret=%s&issuer=%s",
                player.getName(), key, Bukkit.getServer().getMotd());

        messages.info(player, "Your key is %s", key);
        messages.url(player, qrCodeUrl, "Click this message in order to generate QR code for Google Authenticator");
        messages.warn(player, "Please use \"/code <code>\" in order to complete authentication setup");
    }

    private void pendSetup(UUID playerId, String key) {
        var setupId = UUID.randomUUID();
        pendingSetups.put(playerId, new PendingSetup(setupId, key));
        scheduler.scheduleSyncDelayedTask(
                () -> pendingSetups.computeIfPresent(playerId, (k, v) -> setupId.equals(v.id()) ? null : v),
                3 * 60 * 20);
    }

    private record PendingSetup(@NonNull UUID id, String key) {

    }
}
