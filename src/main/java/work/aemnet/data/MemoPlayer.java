package work.aemnet.data;

import java.util.UUID;

public class MemoPlayer {
    private int id;
    private UUID playerId;
    private String name;
    private String skinTexture;
    private String skinSignature;
    private String akaName;

    public MemoPlayer(int id, UUID playerId, String name, String skinTexture, String skinSignature, String akaName) {
        this.id = id;
        this.playerId = playerId;
        this.name = name;
        this.skinTexture = skinTexture;
        this.skinSignature = skinSignature;
        this.akaName = akaName;
    }
}
