package work.aemnet;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import work.aemnet.data.Memo;
import work.aemnet.data.MemoPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MemoRegistry implements IMemoRegistry {
    private final Memocraft plugin;
    private final Map<Integer, MemoPlayer> playerData;
    private final Map<Integer, Memo> memoData;

    public MemoRegistry(Memocraft plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.memoData = new HashMap<>();
    }

    @Override
    public void loadAndPlaceMemos(World world) {

    }

    public void despawnAll() {
    }

    @Override
    public boolean hasMemo(UUID playerId) {
        return false;
    }

    @Override
    public boolean addMemo(Player player, String memo) {
        World world = player.getWorld();
        Location playerLocation = player.getEyeLocation().setRotation(0, 0).add(0, 2, 0);
        world.spawn(playerLocation, TextDisplay.class, textDisplay -> {
            textDisplay.text(Component.text(memo));
            textDisplay.setPersistent(false);
            textDisplay.setBillboard(Display.Billboard.VERTICAL);
        });

        return false;
    }

    @Override
    public boolean removeMemo(UUID playerId) {
        return false;
    }

    @Override
    public void setAkaUsername(Player player, String akaUsername) {

    }

    @Override
    public void onPlayerJoin(Player player, MemoPlayer memoPlayerData) {

    }
}
