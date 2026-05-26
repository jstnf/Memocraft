package work.aemnet;

import org.bukkit.World;
import org.bukkit.entity.Player;
import work.aemnet.data.MemoPlayer;

import java.util.UUID;

public interface IMemoRegistry {
    void loadAndPlaceMemos(World world);

    /**
     * Check if a player has a memo
     * @param playerId the player UUID
     * @return true if the player has a memo, false otherwise
     */
    boolean hasMemo(UUID playerId);

    /**
     * Create a memo for a player
     * @param player the player
     * @param memo a memo message
     * @return true if the player already had a memo, false otherwise
     */
    boolean addMemo(Player player, String memo);

    /**
     * Remove a memo for a player
     * @param playerId the player UUID
     * @return true if a memo was removed, false otherwise
     */
    boolean removeMemo(UUID playerId);

    void setAkaUsername(Player player, String akaUsername);

    void onPlayerJoin(Player player, MemoPlayer memoPlayerData);
}
