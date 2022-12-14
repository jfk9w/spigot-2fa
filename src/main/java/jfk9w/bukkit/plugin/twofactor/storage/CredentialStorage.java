package jfk9w.bukkit.plugin.twofactor.storage;

import jfk9w.bukkit.plugin.twofactor.common.Credential;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CredentialStorage {

    @NonNull
    Map<UUID, Credential> getCredentials();

    boolean saveCredential(@NonNull UUID playerId, @NonNull Credential credential);

    boolean deleteCredential(@NonNull UUID playerId);
}
