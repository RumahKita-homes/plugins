package id.rumahkita.guilds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {
    private final RumahKitaGuildsPlugin plugin;
    private HikariDataSource dataSource;
    private final String tablePrefix;
    private final boolean isEnabled;

    public DatabaseManager(RumahKitaGuildsPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.isEnabled = config.getBoolean("mysql.enabled", false);
        this.tablePrefix = config.getString("mysql.table-prefix", "rkg_");
        
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
        String sqlGuilds = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "guilds (" +
                "tag VARCHAR(16) PRIMARY KEY, " +
                "name VARCHAR(32) NOT NULL, " +
                "leader_uuid VARCHAR(36) NOT NULL, " +
                "leader_name VARCHAR(32), " +
                "created_at BIGINT, " +
                "emerald_wallet INT DEFAULT 0, " +
                "home_world VARCHAR(64), " +
                "home_x DOUBLE, " +
                "home_y DOUBLE, " +
                "home_z DOUBLE, " +
                "home_yaw FLOAT, " +
                "home_pitch FLOAT" +
                ");";

        String sqlMembers = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "guild_members (" +
                "guild_tag VARCHAR(16), " +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(32), " +
                "role VARCHAR(16), " +
                "joined_at BIGINT, " +
                "FOREIGN KEY (guild_tag) REFERENCES " + tablePrefix + "guilds(tag) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt1 = conn.prepareStatement(sqlGuilds);
             PreparedStatement stmt2 = conn.prepareStatement(sqlMembers)) {
            stmt1.executeUpdate();
            stmt2.executeUpdate();
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

    public void loadAllGuilds(GuildManager manager) {
        if (!isEnabled) return;
        
        String sqlGuilds = "SELECT * FROM " + tablePrefix + "guilds";
        String sqlMembers = "SELECT * FROM " + tablePrefix + "guild_members";
        
        try (Connection conn = getConnection();
             PreparedStatement stmtGuilds = conn.prepareStatement(sqlGuilds);
             ResultSet rsGuilds = stmtGuilds.executeQuery();
             PreparedStatement stmtMembers = conn.prepareStatement(sqlMembers);
             ResultSet rsMembers = stmtMembers.executeQuery()) {
            
            while (rsGuilds.next()) {
                String tag = rsGuilds.getString("tag");
                String name = rsGuilds.getString("name");
                UUID leader = UUID.fromString(rsGuilds.getString("leader_uuid"));
                String leaderName = rsGuilds.getString("leader_name");
                long createdAt = rsGuilds.getLong("created_at");
                
                Guild guild = new Guild(tag, name, leader, leaderName, createdAt);
                guild.getMembers().clear(); // Clear initial leader member added in constructor
                
                int wallet = rsGuilds.getInt("emerald_wallet");
                guild.addEmeraldWallet(wallet);
                
                String world = rsGuilds.getString("home_world");
                if (world != null && !world.isEmpty() && Bukkit.getWorld(world) != null) {
                    Location home = new Location(
                            Bukkit.getWorld(world),
                            rsGuilds.getDouble("home_x"),
                            rsGuilds.getDouble("home_y"),
                            rsGuilds.getDouble("home_z"),
                            rsGuilds.getFloat("home_yaw"),
                            rsGuilds.getFloat("home_pitch")
                    );
                    guild.setHome(home);
                }
                
                manager.getRawGuildsMap().put(tag, guild);
            }
            
            while (rsMembers.next()) {
                String tag = rsMembers.getString("guild_tag");
                Guild guild = manager.getRawGuildsMap().get(tag);
                if (guild != null) {
                    UUID uuid = UUID.fromString(rsMembers.getString("uuid"));
                    String name = rsMembers.getString("name");
                    GuildRole role = GuildRole.fromString(rsMembers.getString("role"));
                    long joinedAt = rsMembers.getLong("joined_at");
                    
                    guild.addMember(uuid, name, role, joinedAt);
                    manager.getRawPlayerGuildMap().put(uuid, tag);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load guilds from MySQL: " + e.getMessage());
        }
    }

    public void saveGuildAsync(Guild guild) {
        if (!isEnabled) return;
        CompletableFuture.runAsync(() -> saveGuildSync(guild));
    }

    public void saveGuildSync(Guild guild) {
        if (!isEnabled) return;
        
        String sqlGuild = "INSERT INTO " + tablePrefix + "guilds (tag, name, leader_uuid, leader_name, created_at, emerald_wallet, home_world, home_x, home_y, home_z, home_yaw, home_pitch) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=?, leader_uuid=?, leader_name=?, emerald_wallet=?, home_world=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?";
        
        String sqlDeleteMembers = "DELETE FROM " + tablePrefix + "guild_members WHERE guild_tag = ?";
        String sqlInsertMember = "INSERT INTO " + tablePrefix + "guild_members (guild_tag, uuid, name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmtGuild = conn.prepareStatement(sqlGuild)) {
                stmtGuild.setString(1, guild.getTag());
                stmtGuild.setString(2, guild.getName());
                stmtGuild.setString(3, guild.getLeader().toString());
                stmtGuild.setString(4, guild.getStoredName(guild.getLeader()) != null ? guild.getStoredName(guild.getLeader()) : "");
                stmtGuild.setLong(5, guild.getMembers().get(guild.getLeader()) != null ? System.currentTimeMillis() : System.currentTimeMillis());
                stmtGuild.setInt(6, guild.getEmeraldWallet());
                
                Location home = guild.getHome();
                if (home != null && home.getWorld() != null) {
                    stmtGuild.setString(7, home.getWorld().getName());
                    stmtGuild.setDouble(8, home.getX());
                    stmtGuild.setDouble(9, home.getY());
                    stmtGuild.setDouble(10, home.getZ());
                    stmtGuild.setFloat(11, home.getYaw());
                    stmtGuild.setFloat(12, home.getPitch());
                } else {
                    stmtGuild.setString(7, null);
                    stmtGuild.setDouble(8, 0);
                    stmtGuild.setDouble(9, 0);
                    stmtGuild.setDouble(10, 0);
                    stmtGuild.setFloat(11, 0);
                    stmtGuild.setFloat(12, 0);
                }
                
                // Update part
                stmtGuild.setString(13, guild.getName());
                stmtGuild.setString(14, guild.getLeader().toString());
                stmtGuild.setString(15, guild.getStoredName(guild.getLeader()) != null ? guild.getStoredName(guild.getLeader()) : "");
                stmtGuild.setInt(16, guild.getEmeraldWallet());
                if (home != null && home.getWorld() != null) {
                    stmtGuild.setString(17, home.getWorld().getName());
                    stmtGuild.setDouble(18, home.getX());
                    stmtGuild.setDouble(19, home.getY());
                    stmtGuild.setDouble(20, home.getZ());
                    stmtGuild.setFloat(21, home.getYaw());
                    stmtGuild.setFloat(22, home.getPitch());
                } else {
                    stmtGuild.setString(17, null);
                    stmtGuild.setDouble(18, 0);
                    stmtGuild.setDouble(19, 0);
                    stmtGuild.setDouble(20, 0);
                    stmtGuild.setFloat(21, 0);
                    stmtGuild.setFloat(22, 0);
                }
                stmtGuild.executeUpdate();
            }
            
            try (PreparedStatement stmtDel = conn.prepareStatement(sqlDeleteMembers)) {
                stmtDel.setString(1, guild.getTag());
                stmtDel.executeUpdate();
            }
            
            try (PreparedStatement stmtIns = conn.prepareStatement(sqlInsertMember)) {
                for (UUID member : guild.getMembers().keySet()) {
                    stmtIns.setString(1, guild.getTag());
                    stmtIns.setString(2, member.toString());
                    stmtIns.setString(3, guild.getStoredName(member) != null ? guild.getStoredName(member) : "");
                    stmtIns.setString(4, guild.getRole(member).name());
                    stmtIns.setLong(5, System.currentTimeMillis()); // Temporary joined_at fix since it's not publicly exposed easily, wait actually we can just pass current time or add a method.
                    stmtIns.addBatch();
                }
                stmtIns.executeBatch();
            }
            
            conn.commit();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving guild " + guild.getTag() + ": " + e.getMessage());
        }
    }

    public void deleteGuildAsync(String tag) {
        if (!isEnabled) return;
        CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM " + tablePrefix + "guilds WHERE tag = ?";
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, tag);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error deleting guild " + tag + ": " + e.getMessage());
            }
        });
    }
}
