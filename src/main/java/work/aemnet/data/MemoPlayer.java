package work.aemnet.data;

import java.util.UUID;

public record MemoPlayer(int id, UUID playerId, String name,
                         String skinTexture, String skinSignature, String akaUsername) {
}
