package work.aemnet.npc;

import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * The external surface of a memo NPC: everything plugin code outside the
 * {@code npc} package needs to interact with one. Implementations are
 * server-side entities (currently {@link MemoNPC} subclassing
 * {@code ServerPlayer}), but callers only need this interface — they don't
 * need to touch NMS or know about packet broadcasts, display passengers,
 * or wander loops.
 *
 * <p>Used as the value type of {@code MemoRegistry}'s lookup map, the return
 * type of interaction-listener helpers, and the filter predicate in
 * {@code instanceof} checks ("is this entity one of our NPCs?").
 */
public interface IMemoNPC {
    int getMemoId();

    Vec3 getOrigin();

    String getMemoBody();

    String getMemoDisplayName();

    /** Place the NPC in the world and finish its visibility setup. */
    void spawn(Location bukkitLocation);

    /** Remove the NPC and its passenger displays from the world. */
    void despawn();

    /** Briefly reveal the memo body above the NPC for everyone in tracking range. */
    void showMemoTo(Player viewer);

    /** Update the alias displayed on the NPC's name tag. */
    void setAka(String newDisplayName);

    /**
     * Replace the NPC's skin texture and broadcast the respawn-for-trackers
     * cycle so clients re-render with the new skin. Caller is responsible for
     * deciding when to invoke this (typically on the owning player's login
     * with a changed texture).
     */
    void refreshSkin(String texture, String signature);

    /**
     * The currently-rendered skin texture (the base64 textures-property value),
     * or {@code null} if the NPC has no skin assigned. Callers compare this
     * against a fresh value from the DB to decide whether {@link #refreshSkin}
     * is needed.
     */
    String getCurrentSkinTexture();
}
