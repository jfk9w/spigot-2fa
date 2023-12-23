package jfk9w.bukkit.plugin.twofactor.util;

import lombok.NonNull;
import org.bukkit.entity.Player;

public final class Util {

    private Util() {

    }

    public static String ip(@NonNull Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    public static boolean isLAN(@NonNull Player player) {
        return player.getAddress().getAddress().isSiteLocalAddress();
    }
}
