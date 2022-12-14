package jfk9w.bukkit.plugin.twofactor.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@AllArgsConstructor
public class Credential {

    private String key;
    @With
    private String ip;

    protected Credential() {

    }
}
