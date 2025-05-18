package work.aemnet.data;

import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import work.aemnet.Memocraft;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

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

        // Check if the player already exists in the database
        try (var statement = connection.prepareStatement("SELECT * FROM players WHERE playerId = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // Player exists, update their information
                try (var updateStatement = connection.prepareStatement("UPDATE players SET name = ?, skinTexture = ?, skinSignature = ?, WHERE playerId = ?")) {
                    updateStatement.setString(1, player.getName());
                    updateStatement.setString(2, texture);
                    updateStatement.setString(3, signature);
                    updateStatement.setString(4, player.getUniqueId().toString());
                    updateStatement.executeUpdate();
                }
                id = resultSet.getInt("id");
            } else {
                // Player does not exist, insert a new record
                try (var insertStatement = connection.prepareStatement("INSERT INTO players (playerId, name, skinTexture, skinSignature) VALUES (?, ?, ?, ?)")) {
                    insertStatement.setString(1, player.getUniqueId().toString());
                    insertStatement.setString(2, player.getName());
                    insertStatement.setString(3, texture);
                    insertStatement.setString(4, signature);
                    id = insertStatement.executeUpdate();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to insert or update MemoPlayer: " + e.getMessage());
            return null;
        }

        // Create and return the MemoPlayer object
        return new MemoPlayer(id, player.getUniqueId(), player.getName(), texture, signature, null);
    }

    public Memo createMemo(Player player, String memo) {
        return null;
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
                            "FOREIGN KEY(playerId) REFERENCES MemoPlayer(id)" +
                            ");"
            );
        } catch (Exception e) {
            throw new Exception("Failed to create tables: " + e.getMessage());
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
}
