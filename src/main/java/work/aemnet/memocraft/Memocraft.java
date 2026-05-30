package work.aemnet.memocraft;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import work.aemnet.memocraft.command.AkaCommand;
import work.aemnet.memocraft.command.ManagememoCommand;
import work.aemnet.memocraft.command.MemoCommand;
import work.aemnet.memocraft.data.DataSource;
import work.aemnet.memocraft.listener.JoinQuitListener;
import work.aemnet.memocraft.listener.MemoInteractListener;

public class Memocraft extends JavaPlugin {
    private DataSource dataSource;
    private IMemoRegistry memoRegistry;

    @Override
    public void onLoad() {
        dataSource = new DataSource(this);
        try {
            dataSource.initialize();
        } catch (Exception e) {
            getLogger().severe("Failed to initialize the database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        memoRegistry = new MemoRegistry(this);
    }

    @Override
    public void onEnable() {
        getCommand("aka").setExecutor(new AkaCommand(this));
        getCommand("memo").setExecutor(new MemoCommand(this));
        getCommand("managememo").setExecutor(new ManagememoCommand(this));

        // Place memos on first server tick
        getServer().getScheduler().runTaskLater(this, () -> {
            World mainWorld = getServer().getWorlds().getFirst();
            if (mainWorld == null) return;
            memoRegistry.loadAndPlaceMemos(mainWorld);
        }, 1L);

        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MemoInteractListener(), this);
    }

    @Override
    public void onDisable() {
        if (memoRegistry != null) ((MemoRegistry) memoRegistry).despawnAll();
        if (dataSource != null) dataSource.onDisable();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public IMemoRegistry getMemoRegistry() {
        return memoRegistry;
    }
}
