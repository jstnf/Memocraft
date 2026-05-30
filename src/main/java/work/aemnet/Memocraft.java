package work.aemnet;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import work.aemnet.command.AkaCommand;
import work.aemnet.command.ManagememoCommand;
import work.aemnet.command.MemoCommand;
import work.aemnet.data.DataSource;
import work.aemnet.listener.JoinQuitListener;
import work.aemnet.listener.MemoInteractListener;

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
        dataSource.onDisable();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public IMemoRegistry getMemoRegistry() {
        return memoRegistry;
    }
}
