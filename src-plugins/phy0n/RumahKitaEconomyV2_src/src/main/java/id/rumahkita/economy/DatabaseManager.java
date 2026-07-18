package id.rumahkita.economy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final RumahKitaEconomyRupiahPlugin plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    private final boolean isEnabled;

    public DatabaseManager(RumahKitaEconomyRupiahPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.isEnabled = config.getBoolean("mysql.enabled", false);
        this.tablePrefix = config.getString("mysql.table-prefix", "rke_");
        
        if (isEnabled) {
            setupPool(config);
            createTables();
        }
    }

    private void setupPool(FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        String host = config.getString("mysql.host", "127.0.0.1");
        int port = config.getInt("mysql.port", 3306);
        String database = config.getString("mysql.database", "rumahkita_core");
        
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true");
        hikariConfig.setUsername(config.getString("mysql.username", "root"));
        hikariConfig.setPassword(config.getString("mysql.password", ""));
        
        hikariConfig.setMaximumPoolSize(config.getInt("mysql.pool.max-pool-size", 10));
        hikariConfig.setConnectionTimeout(config.getLong("mysql.pool.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("mysql.pool.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("mysql.pool.max-lifetime", 1800000));

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(hikariConfig);
        plugin.getLogger().info("Successfully connected to MySQL database: " + database);
    }

    private void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "balances (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "balance BIGINT NOT NULL DEFAULT 0" +
                ");";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isEnabled || dataSource == null) {
            throw new SQLException("MySQL is not enabled or initialized.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public CompletableFuture<Long> loadBalance(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT balance FROM " + tablePrefix + "balances WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong("balance");
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Error loading balance for " + uuid + ": " + e.getMessage());
            }
            return -1L; 
        });
    }

    public void saveBalanceAsync(UUID uuid, long balance) {
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO " + tablePrefix + "balances (uuid, balance) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE balance = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, balance);
                stmt.setLong(3, balance);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error saving balance for " + uuid + ": " + e.getMessage());
            }
        });
    }

    public void saveBalanceSync(UUID uuid, long balance) {
        String sql = "INSERT INTO " + tablePrefix + "balances (uuid, balance) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE balance = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setLong(2, balance);
            stmt.setLong(3, balance);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving balance sync for " + uuid + ": " + e.getMessage());
        }
    }
}
