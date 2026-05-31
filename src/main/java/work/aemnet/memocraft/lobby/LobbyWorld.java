package work.aemnet.memocraft.lobby;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import work.aemnet.memocraft.Memocraft;

public final class LobbyWorld {
    private static final String VOID_GENERATOR_SETTINGS =
            "{\"layers\":[],\"biome\":\"minecraft:the_void\",\"structures\":{\"structures\":{}}}";

    private LobbyWorld() {}

    public static World loadOrCreate(Memocraft plugin) {
        String name = plugin.getConfig().getString("lobby.world-name", "memo-lobby");

        WorldCreator creator = new WorldCreator(name)
                .type(WorldType.FLAT)
                .generatorSettings(VOID_GENERATOR_SETTINGS)
                .environment(World.Environment.NORMAL);

        World world = creator.createWorld();
        if (world == null) {
            plugin.getLogger().severe("WorldCreator returned null for lobby world '" + name + "'.");
            return null;
        }

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);

        plugin.getLogger().info("Loaded lobby world '" + name + "'.");
        return world;
    }

    public static void startVoidGuard(Memocraft plugin, World world) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Location spawn = world.getSpawnLocation();
            for (Player p : world.getPlayers()) {
                if (p.getLocation().getY() < -64.0) {
                    p.teleport(spawn);
                }
            }
        }, 20L, 20L);
    }
}
