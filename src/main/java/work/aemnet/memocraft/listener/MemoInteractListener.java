package work.aemnet.memocraft.listener;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import work.aemnet.memocraft.npc.IMemoNPC;

public class MemoInteractListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        IMemoNPC npc = asMemoNpc(e.getRightClicked());
        if (npc == null) return;
        e.setCancelled(true);
        npc.showMemoTo(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeftClick(PrePlayerAttackEntityEvent e) {
        IMemoNPC npc = asMemoNpc(e.getAttacked());
        if (npc == null) return;
        e.setCancelled(true);
        npc.showMemoTo(e.getPlayer());
    }

    private static @Nullable IMemoNPC asMemoNpc(Entity entity) {
        if (!(entity instanceof Player bukkitPlayer)) return null;
        var handle = ((CraftPlayer) bukkitPlayer).getHandle();
        return handle instanceof IMemoNPC memo ? memo : null;
    }
}
