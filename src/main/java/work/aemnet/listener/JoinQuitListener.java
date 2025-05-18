package work.aemnet.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URL;

public class JoinQuitListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        URL skinUrl = player.getPlayerProfile().getTextures().getSkin();
        if (skinUrl == null) return;
    }
}
