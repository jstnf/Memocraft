package work.aemnet.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import work.aemnet.Memocraft;
import work.aemnet.data.MemoPlayer;

public class JoinQuitListener implements Listener {
    private final Memocraft plugin;

    public JoinQuitListener(Memocraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        MemoPlayer memoPlayerData = plugin.getDataSource().insertOrUpdateOnJoin(player);
        if (memoPlayerData == null) {
            plugin.getLogger().warning("Failed to persist player row on join for " + player.getName());
            return;
        }
        plugin.getMemoRegistry().onPlayerJoin(player, memoPlayerData);
    }
}
