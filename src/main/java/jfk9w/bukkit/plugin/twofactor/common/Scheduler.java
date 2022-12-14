package jfk9w.bukkit.plugin.twofactor.common;

public interface Scheduler {

    int scheduleSyncDelayedTask(Runnable task, long delay);
}
