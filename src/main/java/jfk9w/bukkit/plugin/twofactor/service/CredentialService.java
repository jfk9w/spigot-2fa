package jfk9w.bukkit.plugin.twofactor.service;

import jfk9w.bukkit.plugin.twofactor.common.Credential;
import jfk9w.bukkit.plugin.twofactor.storage.CredentialStorage;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CredentialService {

    @NonNull
    private final CredentialStorage storage;

    @Builder
    public CredentialService(@NonNull CredentialStorage storage) {
        this.storage = storage;
    }

    public Optional<Credential> getCredential(@NonNull UUID playerId) {
        return Optional.ofNullable(getCredentials(false).get(playerId));
    }

    public boolean saveCredential(@NonNull UUID playerId, @NonNull Credential credential) {
        if (!storage.saveCredential(playerId, credential)) {
            return false;
        }

        getCredentials(false).put(playerId, credential);
        return true;
    }

    public boolean deleteCredential(@NonNull UUID playerId) {
        if (!storage.deleteCredential(playerId)) {
            return false;
        }

        getCredentials(false).remove(playerId);
        return true;
    }

    private volatile Map<UUID, Credential> credentials;

    private Map<UUID, Credential> getCredentials(boolean force) {
        if (credentials == null || force) {
            credentials = new ConcurrentHashMap<>(storage.getCredentials());
        }

        return credentials;
    }
}
