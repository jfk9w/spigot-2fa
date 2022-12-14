package jfk9w.bukkit.plugin.twofactor.storage;

import com.google.gson.Gson;
import jfk9w.bukkit.plugin.twofactor.common.Credential;
import lombok.Builder;
import lombok.NonNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Builder
public class JsonFileStorage implements CredentialStorage {

    private static final Log log = LogFactory.getLog(JsonFileStorage.class);

    private static final String FILE_NAME_SUFFIX = ".json";

    @NonNull
    private final Path dirPath;

    private final Gson gson = new Gson();

    public JsonFileStorage(Path dirPath) {
        this.dirPath = dirPath.resolve("players");
        try {
            Files.createDirectories(this.dirPath);
        } catch (IOException e) {
            log.error("Failed to create directory for 2FA credentials", e);
        }
    }

    @Override
    public @NonNull Map<UUID, Credential> getCredentials() {
        var credentials = new HashMap<UUID, Credential>();
        try (var filePaths = Files.list(dirPath)) {
            for (var filePath : (Iterable<Path>) filePaths::iterator) {
                if (!filePath.toString().endsWith(FILE_NAME_SUFFIX)) {
                    continue;
                }

                try (var fileReader = new FileReader(filePath.toFile())) {
                    var credential = gson.fromJson(fileReader, Credential.class);
                    var playerId = UUID.fromString(filePath.getFileName().toString().replace(FILE_NAME_SUFFIX, ""));
                    credentials.put(playerId, credential);
                } catch (FileNotFoundException e) {
                    log.warn(String.format("File %s disappeared while reading 2FA credentials", filePath), e);
                } catch (IOException e) {
                    log.error(String.format("Failed to read 2FA credentials from %s", filePath), e);
                }
            }
        } catch (IOException e) {
            log.error(String.format("Failed to read credentials from %s", dirPath), e);
        }

        return Collections.unmodifiableMap(credentials);
    }

    @Override
    public boolean saveCredential(@NonNull UUID playerId, @NonNull Credential credential) {
        var filePath = dirPath.resolve(playerId + FILE_NAME_SUFFIX);
        try (var fileWriter = new FileWriter(filePath.toFile())) {
            gson.toJson(credential, fileWriter);
        } catch (IOException e) {
            log.error(String.format("Failed to save 2FA credentials for %s", playerId), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean deleteCredential(@NonNull UUID playerId) {
        var filePath = dirPath.resolve(playerId + FILE_NAME_SUFFIX);
        try {
            Files.delete(filePath);
        } catch (NoSuchFileException e) {
            return true;
        } catch (IOException e) {
            log.error(String.format("Failed to delete 2FA credentials for %s", playerId), e);
            return false;
        }

        return true;
    }
}
