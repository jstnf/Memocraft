package work.aemnet.data;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import work.aemnet.Memocraft;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class DataSource {
    private final Memocraft plugin;
    private final File databaseFile;
    private Connection connection;

    public DataSource(Memocraft plugin) {
        this.plugin = plugin;
        databaseFile = new File(plugin.getDataFolder(), "data.db");
    }

    public void initialize() throws Exception {
        plugin.getDataFolder().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        createTables();
    }

    /**
     * Inserts or updates a MemoPlayer in the database after joining the server
     * @param player the player to insert or update
     * @return the MemoPlayer object if successfully insert or update, null otherwise
     */
    @Nullable
    public MemoPlayer insertOrUpdateOnJoin(Player player) {
        ProfileProperty skin = player.getPlayerProfile().getProperties().stream()
                .filter(property -> property.getName().equals("textures"))
                .findFirst()
                .orElse(null);
        String texture = skin != null ? skin.getValue() : null;
        String signature = skin != null ? skin.getSignature() : null;
        int id = -1;
        String akaUsername = null;

        // Check if the player already exists in the database
        try (var statement = connection.prepareStatement("SELECT * FROM players WHERE playerId = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Player exists, update their information
                try (var updateStatement = connection.prepareStatement("UPDATE players SET name = ?, skinTexture = ?, skinSignature = ? WHERE playerId = ?")) {
                    updateStatement.setString(1, player.getName());
                    updateStatement.setString(2, texture);
                    updateStatement.setString(3, signature);
                    updateStatement.setString(4, player.getUniqueId().toString());
                    updateStatement.executeUpdate();
                }
                id = resultSet.getInt("id");
                akaUsername = resultSet.getString("akaUsername");
            } else {
                // Player does not exist, insert a new record
                try (var insertStatement = connection.prepareStatement(
                        "INSERT INTO players (playerId, name, skinTexture, skinSignature) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insertStatement.setString(1, player.getUniqueId().toString());
                    insertStatement.setString(2, player.getName());
                    insertStatement.setString(3, texture);
                    insertStatement.setString(4, signature);
                    insertStatement.executeUpdate();
                    try (var generatedKeys = insertStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            id = generatedKeys.getInt(1);
                        } else {
                            throw new SQLException("INSERT into players did not return a generated key");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to insert or update MemoPlayer: " + e.getMessage());
            return null;
        }

        // Create and return the MemoPlayer object
        return new MemoPlayer(id, player.getUniqueId(), player.getName(), texture, signature, akaUsername);
    }

    @Nullable
    public Memo createMemo(int playerRowId, String message, org.bukkit.Location location) {
        String sql = "INSERT INTO memos (playerId, memo, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (var insertStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            insertStatement.setInt(1, playerRowId);
            insertStatement.setString(2, message);
            insertStatement.setDouble(3, location.getX());
            insertStatement.setDouble(4, location.getY());
            insertStatement.setDouble(5, location.getZ());
            insertStatement.setFloat(6, location.getYaw());
            insertStatement.setFloat(7, location.getPitch());
            insertStatement.executeUpdate();
            try (var keys = insertStatement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("INSERT into memos did not return a generated key");
                }
                int id = keys.getInt(1);
                return new Memo(id, playerRowId, System.currentTimeMillis(), message,
                        location.getX(), location.getY(), location.getZ(),
                        location.getYaw(), location.getPitch());
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to insert memo: " + e.getMessage());
            return null;
        }
    }

    private void createTables() throws Exception {
        // Make tables for MemoPlayer and Memo
        try {
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS players (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "playerId TEXT NOT NULL," +
                            "name TEXT NOT NULL," +
                            "skinTexture TEXT," +
                            "skinSignature TEXT," +
                            "akaUsername TEXT" +
                            ");"
            );
            connection.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS memos (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "playerId INTEGER NOT NULL," +
                            "memo TEXT NOT NULL," +
                            "createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "x REAL NOT NULL," +
                            "y REAL NOT NULL," +
                            "z REAL NOT NULL," +
                            "yaw REAL NOT NULL," +
                            "pitch REAL NOT NULL," +
                            "FOREIGN KEY(playerId) REFERENCES players(id)" +
                            ");"
            );
        } catch (Exception e) {
            throw new Exception("Failed to create tables: " + e.getMessage());
        }
    }

    public boolean deleteMemo(int memoId) {
        try (var statement = connection.prepareStatement("DELETE FROM memos WHERE id = ?")) {
            statement.setInt(1, memoId);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to delete memo " + memoId + ": " + e.getMessage());
            return false;
        }
    }

    public void deleteMemos(Collection<Integer> memoIds) {
        if (memoIds.isEmpty()) return;
        try (var statement = connection.prepareStatement("DELETE FROM memos WHERE id = ?")) {
            for (int id : memoIds) {
                statement.setInt(1, id);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to batch-delete memos: " + e.getMessage());
        }
    }

    public List<MemoWithPlayer> loadAllMemos() {
        String sql = "SELECT m.id AS memoId, m.playerId AS playerRowId, m.memo, " +
                     "CAST(strftime('%s', m.createdAt) AS INTEGER) * 1000 AS createdAtMillis, " +
                     "m.x, m.y, m.z, m.yaw, m.pitch, " +
                     "p.playerId AS playerUuid, p.name, p.skinTexture, p.skinSignature, p.akaUsername " +
                     "FROM memos m JOIN players p ON m.playerId = p.id " +
                     "ORDER BY m.createdAt DESC";
        var results = new java.util.ArrayList<MemoWithPlayer>();
        try (var statement = connection.prepareStatement(sql);
             var rs = statement.executeQuery()) {
            while (rs.next()) {
                Memo memo = new Memo(
                        rs.getInt("memoId"),
                        rs.getInt("playerRowId"),
                        rs.getLong("createdAtMillis"),
                        rs.getString("memo"),
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
                MemoPlayer player = new MemoPlayer(
                        rs.getInt("playerRowId"),
                        java.util.UUID.fromString(rs.getString("playerUuid")),
                        rs.getString("name"),
                        rs.getString("skinTexture"),
                        rs.getString("skinSignature"),
                        rs.getString("akaUsername"));
                results.add(new MemoWithPlayer(memo, player));
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load memos: " + e.getMessage());
        }
        return results;
    }

    public int getPlayerRowId(java.util.UUID playerUuid) {
        try (var statement = connection.prepareStatement("SELECT id FROM players WHERE playerId = ?")) {
            statement.setString(1, playerUuid.toString());
            try (var rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to look up player row id: " + e.getMessage());
            return -1;
        }
    }

    public boolean updateAkaUsername(UUID playerUuid, String akaUsername) {
        try (var statement = connection.prepareStatement("UPDATE players SET akaUsername = ? WHERE playerId = ?")) {
            statement.setString(1, akaUsername);
            statement.setString(2, playerUuid.toString());
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update aka for " + playerUuid + ": " + e.getMessage());
            return false;
        }
    }

    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to close the database connection: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public record MemoWithPlayer(Memo memo, MemoPlayer player) {}
}
