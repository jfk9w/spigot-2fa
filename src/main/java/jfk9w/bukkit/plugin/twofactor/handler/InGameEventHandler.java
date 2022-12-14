package jfk9w.bukkit.plugin.twofactor.handler;

import com.google.common.base.Splitter;
import jfk9w.bukkit.plugin.twofactor.common.EventListeners;
import jfk9w.bukkit.plugin.twofactor.service.AuthenticationService;
import jfk9w.bukkit.plugin.twofactor.service.CredentialService;
import jfk9w.bukkit.plugin.twofactor.service.MessageService;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.EventExecutor;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class InGameEventHandler implements Listener, EventExecutor {

    private static final Set<Class<? extends Event>> WHITELIST = Set.of(
            PlayerCommandPreprocessEvent.class,
            PlayerKickEvent.class
    );

    private static final Map<Class<? extends Event>, Set<EntityEventFunction>> ENTITY_METHODS = new IdentityHashMap<>();

    @NonNull
    private final AuthenticationService authentication;
    @NonNull
    private final CredentialService credentials;
    @NonNull
    private final MessageService messages;
    @NonNull
    private final Collection<PluginCommand> commands;

    private final Random random = new Random(123L);

    @Override
    public void execute(Listener listener, Event event) throws EventException {
        if (event instanceof Cancellable cancellableEvent) {
            if (cancellableEvent.isCancelled()) {
                return;
            }

            ENTITY_METHODS.getOrDefault(event.getClass(), Set.of()).stream()
                    .map(method -> method.apply(event))
                    .filter(Objects::nonNull)
                    .filter(Player.class::isInstance)
                    .map(object -> (Player) object)
                    .findFirst()
                    .ifPresent(player -> {
                        if (!authentication.isAuthenticated(player.getUniqueId())) {
                            cancellableEvent.setCancelled(true);
                            if (random.nextInt(10000) == 1) {
                                var action = credentials.getCredential(player.getUniqueId())
                                        .map(credential -> "authenticate with \"/code <code>\"")
                                        .orElseGet(() -> "set up 2FA");

                                messages.error(player, "You must %s in order to use the server!", action);
                            }
                        }
                    });
        }
    }

    private static final Splitter COMMAND_SPLITTER = Splitter.on(' ').omitEmptyStrings().trimResults();

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        if (authentication.isAuthenticated(playerId)) {
            return;
        }

        for (var command : COMMAND_SPLITTER.split(event.getMessage())) {
            if (commands.stream()
                    .flatMap(cmd -> Stream.concat(Stream.of(cmd.getName()), cmd.getAliases().stream()))
                    .noneMatch(cmd -> ("/" + cmd).equalsIgnoreCase(command))) {
                event.setCancelled(true);
            }

            return;
        }
    }

    public static void register(EventListeners eventListeners,
                                AuthenticationService authentication,
                                CredentialService credentials,
                                MessageService messages,
                                Collection<PluginCommand> commands) {

        var eventHandler = new InGameEventHandler(authentication, credentials, messages, commands);
        var reflections = new Reflections(Scanners.SubTypes);
        for (var eventClass : reflections.getSubTypesOf(Event.class)) {
            getHandlerList(eventClass).ifPresent(handlerList -> {
                Stream.concat(Stream.of(eventClass), reflections.getSubTypesOf(eventClass).stream())
                        .filter(clazz -> !WHITELIST.contains(clazz))
                        .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                        .filter(Cancellable.class::isAssignableFrom)
                        .forEach(clazz -> {
                            ENTITY_METHODS.put(clazz, getEntityMethods(clazz));
                            eventListeners.register(handlerList, eventHandler, EventPriority.HIGHEST, true);
                        });
            });
        }

        eventListeners.register(eventHandler);
    }

    @SneakyThrows
    private static Set<EntityEventFunction> getEntityMethods(Class<? extends Event> eventClass) {
        return Arrays.stream(eventClass.getMethods())
                .filter(method -> !Modifier.isStatic(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())
                        && !Modifier.isAbstract(method.getModifiers())
                        && method.getParameterCount() == 0)
                .filter(method -> Entity.class.isAssignableFrom(method.getReturnType()))
                .map(method -> metafactory(eventClass, method))
                .collect(Collectors.toSet());
    }

    private static Optional<HandlerList> getHandlerList(Class<? extends Event> eventClass) {
        return Arrays.stream(eventClass.getMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
                .filter(method -> HandlerList.class.equals(method.getReturnType()))
                .map(InGameEventHandler::<HandlerList>invokeStatic)
                .findFirst();
    }

    @SneakyThrows
    private static EntityEventFunction metafactory(Class<?> clazz, Method method) {
        var lookup = MethodHandles.lookup();
        return (EntityEventFunction) LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(EntityEventFunction.class),
                        MethodType.methodType(Entity.class, Event.class),
                        lookup.findVirtual(clazz, method.getName(), MethodType.methodType(method.getReturnType())),
                        MethodType.methodType(method.getReturnType(), clazz))
                .getTarget()
                .invokeExact();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static <T> T invokeStatic(Method method, Object... args) {
        return (T) method.invoke(null, args);
    }

    @FunctionalInterface
    private interface EntityEventFunction {
        Entity apply(Event event);
    }
}
