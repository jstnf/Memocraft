package work.aemnet.memocraft;

import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import work.aemnet.memocraft.data.Memo;
import work.aemnet.memocraft.data.MemoPlayer;
import work.aemnet.memocraft.npc.IMemoNPC;
import work.aemnet.memocraft.npc.MemoNPC;
import work.aemnet.memocraft.npc.MemoProfileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MemoRegistry implements IMemoRegistry {
    private final Memocraft plugin;
    private final Map<Integer, IMemoNPC> npcByMemoId = new HashMap<>();
    private final Map<UUID, Integer> memoIdByPlayer = new HashMap<>();

    public MemoRegistry(Memocraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public void loadAndPlaceMemos(World world) {
        var ds = plugin.getDataSource();
        var nmsLevel = ((CraftWorld) world).getHandle();
        var server = ((CraftServer) plugin.getServer()).getServer();
        var team = ensureNpcTeam();

        var seenPlayerRowIds = new HashSet<Integer>();
        var toDelete = new ArrayList<Integer>();
        int spawned = 0;

        for (var row : ds.loadAllMemos()) {
            var memo = row.memo();
            var memoPlayer = row.player();
            if (!seenPlayerRowIds.add(memoPlayer.id())) {
                // De-duping, keep the first seen (most recent due to ORDER BY DESC), drop this one
                plugin.getLogger().warning("Duplicate memo for player " + memoPlayer.name()
                        + " (memoId " + memo.id() + "); dropping older row.");
                toDelete.add(memo.id());
                continue;
            }

            var displayName = memoPlayer.akaUsername() != null ? memoPlayer.akaUsername() : memoPlayer.name();
            var profile = MemoProfileManager.profileFor(
                    memo.id(), memoPlayer.name(),
                    memoPlayer.skinTexture(), memoPlayer.skinSignature());

            var origin = new Vec3(memo.x(), memo.y(), memo.z());
            var bukkitLocation = new Location(world, memo.x(), memo.y(), memo.z(),
                    memo.yaw(), memo.pitch());

            var npc = new MemoNPC(server, nmsLevel, profile,
                    memo.id(), origin, memo.message(), displayName);
            npc.spawn(bukkitLocation);
            team.addEntry(profile.name());

            npcByMemoId.put(memo.id(), npc);
            memoIdByPlayer.put(memoPlayer.playerId(), memo.id());
            spawned++;
        }

        if (!toDelete.isEmpty()) ds.deleteMemos(toDelete);
        plugin.getLogger().info("Loaded " + spawned + " memos.");
    }

    public void despawnAll() {
        for (var npc : npcByMemoId.values()) npc.despawn();
        npcByMemoId.clear();
        memoIdByPlayer.clear();
    }

    @Override
    public boolean hasMemo(UUID playerId) {
        return memoIdByPlayer.containsKey(playerId);
    }

    @Override
    public boolean addMemo(Player player, String memoBody) {
        var ds = plugin.getDataSource();
        int playerRowId = ds.getPlayerRowId(player.getUniqueId());
        if (playerRowId < 0) {
            plugin.getLogger().warning("addMemo: no players row for " + player.getName() + " — join event missed?");
            return false;
        }

        Integer oldMemoId = memoIdByPlayer.get(player.getUniqueId());

        Memo memo = ds.createMemo(playerRowId, memoBody, player.getLocation());
        if (memo == null) return false;

        // Spawn the NPC at the player's current location.
        var bukkitWorld = player.getWorld();
        var nmsLevel = ((CraftWorld) bukkitWorld).getHandle();
        var server = ((CraftServer) plugin.getServer()).getServer();

        var profile = MemoProfileManager.profileFor(
                memo.id(),
                player.getName(),
                getTextureFor(player),
                getSignatureFor(player));

        var origin = new Vec3(
                player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());

        var npc = new MemoNPC(server, nmsLevel, profile,
                memo.id(), origin, memoBody, player.getName());
        npc.spawn(player.getLocation());
        ensureNpcTeam().addEntry(profile.name());

        npcByMemoId.put(memo.id(), npc);
        memoIdByPlayer.put(player.getUniqueId(), memo.id());

        boolean replaced = false;
        if (oldMemoId != null) {
            var oldNpc = npcByMemoId.remove(oldMemoId);
            if (oldNpc != null) oldNpc.despawn();
            ds.deleteMemo(oldMemoId);
            replaced = true;
        }
        return replaced;
    }

    private String getTextureFor(Player player) {
        return player.getPlayerProfile().getProperties().stream()
                .filter(p -> p.getName().equals("textures"))
                .map(p -> p.getValue())
                .findFirst().orElse(null);
    }

    private String getSignatureFor(Player player) {
        return player.getPlayerProfile().getProperties().stream()
                .filter(p -> p.getName().equals("textures"))
                .map(p -> p.getSignature())
                .findFirst().orElse(null);
    }

    @Override
    public boolean removeMemo(UUID playerId) {
        Integer memoId = memoIdByPlayer.remove(playerId);
        if (memoId == null) return false;
        var npc = npcByMemoId.remove(memoId);
        if (npc != null) npc.despawn();
        plugin.getDataSource().deleteMemo(memoId);
        return true;
    }

    @Override
    public void setAkaUsername(Player player, String akaUsername) {
        plugin.getDataSource().updateAkaUsername(player.getUniqueId(), akaUsername);
        Integer memoId = memoIdByPlayer.get(player.getUniqueId());
        if (memoId == null) return;
        var npc = npcByMemoId.get(memoId);
        if (npc != null) npc.setAka(akaUsername);
    }

    @Override
    public void onPlayerJoin(Player player, MemoPlayer row) {
        Integer memoId = memoIdByPlayer.get(player.getUniqueId());
        if (memoId == null) return;
        var npc = npcByMemoId.get(memoId);
        if (npc == null) return;

        if (!Objects.equals(npc.getCurrentSkinTexture(), row.skinTexture())) {
            npc.refreshSkin(row.skinTexture(), row.skinSignature());
        }
    }

    public Team ensureNpcTeam() {
        var scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
        var team = scoreboard.getTeam("memocraft_npcs");
        if (team == null) {
            team = scoreboard.registerNewTeam("memocraft_npcs");
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        }
        return team;
    }
}
