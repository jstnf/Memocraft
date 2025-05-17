package work.aemnet;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sqlite.SQLiteDataSource;

import java.util.UUID;

public class MemoRegistry implements IMemoRegistry {
    private final Memocraft plugin;

    public MemoRegistry(Memocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        // Create SQLite database and table if they don't exist
        SQLiteDataSource dataSource = new SQLiteDataSource();
    }

    @Override
    public void loadAndPlaceMemos(World world) {

    }

    @Override
    public boolean hasMemo(UUID playerId) {
        return false;
    }

    @Override
    public boolean addMemo(Player player, String memo) {
        return false;
    }

    @Override
    public boolean removeMemo(UUID playerId) {
        return false;
    }
}
