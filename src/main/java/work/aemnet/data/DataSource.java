package work.aemnet.data;

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
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS memos (" +
                    "player_id TEXT PRIMARY KEY," +
                    "player_name TEXT," +
                    "aka_name TEXT," +
                    "message TEXT," +
                    "timestamp BIGINT," +
                    "x REAL," +
                    "y REAL," +
                    "z REAL," +
                    "pitch REAL," +
                    "yaw REAL" +
                    ")");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
