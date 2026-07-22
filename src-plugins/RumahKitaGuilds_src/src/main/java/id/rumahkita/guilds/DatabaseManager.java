package id.rumahkita.guilds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
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
            updateSchema();
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
                
        String sqlClaims = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "guild_claims (" +
                "guild_tag VARCHAR(16), " +
                "chunk_key VARCHAR(64) PRIMARY KEY, " +
                "FOREIGN KEY (guild_tag) REFERENCES " + tablePrefix + "guilds(tag) ON DELETE CASCADE ON UPDATE CASCADE" +
                ");";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlGuilds);
            stmt.execute(sqlMembers);
            stmt.execute(sqlClaims);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }
    
    private void updateSchema() {
        String[] updates = {
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN balance DOUBLE DEFAULT 0.0;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN vault_level INT DEFAULT 1;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN member_level INT DEFAULT 1;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN friendly_fire BOOLEAN DEFAULT FALSE;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN vault_items LONGTEXT;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN logs LONGTEXT;",
            "ALTER TABLE " + tablePrefix + "guilds ADD COLUMN allies LONGTEXT;"
        };
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            for (String sql : updates) {
                try {
                    stmt.execute(sql);
                } catch (SQLException ignore) {
                    // Column already exists
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update schema: " + e.getMessage());
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
        String sqlClaims = "SELECT * FROM " + tablePrefix + "guild_claims";
        
        int loadedGuilds = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement stmtGuilds = conn.prepareStatement(sqlGuilds);
             ResultSet rsGuilds = stmtGuilds.executeQuery()) {
            
            while (rsGuilds.next()) {
                try {
                    String tag = rsGuilds.getString("tag");
                    String name = rsGuilds.getString("name");
                    UUID leader = null;
                    try { leader = UUID.fromString(rsGuilds.getString("leader_uuid")); } catch (Exception e) {}
                    String leaderName = rsGuilds.getString("leader_name");
                    long createdAt = rsGuilds.getLong("created_at");
                    
                    Guild guild = new Guild(tag, name, leader, leaderName, createdAt);
                    guild.getMembers().clear();
                    
                    try { guild.addEmeraldWallet(rsGuilds.getInt("emerald_wallet")); } catch (Exception e) {}
                    try { guild.setBalance(rsGuilds.getDouble("balance")); } catch (Exception ignored) {}
                    try { guild.setVaultLevel(rsGuilds.getInt("vault_level")); } catch (Exception ignored) { guild.setVaultLevel(1); }
                    try { guild.setMemberLevel(rsGuilds.getInt("member_level")); } catch (Exception ignored) { guild.setMemberLevel(1); }
                    try { guild.setFriendlyFire(rsGuilds.getBoolean("friendly_fire")); } catch (Exception ignored) { guild.setFriendlyFire(false); }
                    
                    try {
                        String vaultRaw = rsGuilds.getString("vault_items");
                        if (vaultRaw != null && !vaultRaw.isEmpty()) {
                            guild.setVaultItems(itemStackArrayFromBase64(vaultRaw));
                        }
                    } catch (Exception ignored) {}
                    
                    try {
                        String logsRaw = rsGuilds.getString("logs");
                        if (logsRaw != null && !logsRaw.isEmpty()) {
                            String[] arr = logsRaw.split(";;");
                            for (String l : arr) {
                                if (!l.isEmpty()) guild.addLog(l);
                            }
                        }
                    } catch (Exception ignored) {}
                    
                    try {
                        String alliesRaw = rsGuilds.getString("allies");
                        if (alliesRaw != null && !alliesRaw.isEmpty()) {
                            String[] arr = alliesRaw.split(",");
                            for (String a : arr) {
                                if (!a.isEmpty()) guild.addAlly(a);
                            }
                        }
                    } catch (Exception ignored) {}
                    
                    manager.getRawGuildsMap().put(manager.normalizeTag(tag), guild);
                    loadedGuilds++;
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to parse a guild: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load guilds from MySQL: " + e.getMessage());
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmtMembers = conn.prepareStatement(sqlMembers);
             ResultSet rsMembers = stmtMembers.executeQuery()) {
             while (rsMembers.next()) {
                try {
                    String tag = rsMembers.getString("guild_tag");
                    Guild guild = manager.getRawGuildsMap().get(manager.normalizeTag(tag));
                    if (guild != null) {
                        UUID uuid = UUID.fromString(rsMembers.getString("uuid"));
                        String name = rsMembers.getString("name");
                        GuildRole role = GuildRole.fromString(rsMembers.getString("role"));
                        long joinedAt = rsMembers.getLong("joined_at");
                        
                        guild.addMember(uuid, name, role, joinedAt);
                        manager.getRawPlayerGuildMap().put(uuid, manager.normalizeTag(tag));
                    }
                } catch (Exception ex) {}
             }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load guild members from MySQL: " + e.getMessage());
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmtClaims = conn.prepareStatement(sqlClaims);
             ResultSet rsClaims = stmtClaims.executeQuery()) {
             while (rsClaims.next()) {
                try {
                    String tag = rsClaims.getString("guild_tag");
                    Guild guild = manager.getRawGuildsMap().get(manager.normalizeTag(tag));
                    if (guild != null) {
                        guild.addClaim(rsClaims.getString("chunk_key"));
                    }
                } catch (Exception ex) {}
             }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load guild claims from MySQL: " + e.getMessage());
        }
        
        plugin.getLogger().info("Successfully loaded " + loadedGuilds + " guilds from MySQL.");
    }
    
    public void saveGuildAsync(Guild guild) {
        if (!isEnabled) return;
        CompletableFuture.runAsync(() -> saveGuildSync(guild));
    }

    public void saveGuildClaimSync(String tag, String chunkKey) {
        if (!isEnabled) return;
        String sql = "INSERT IGNORE INTO " + tablePrefix + "guild_claims (guild_tag, chunk_key) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tag);
            stmt.setString(2, chunkKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save guild claim: " + e.getMessage());
        }
    }

    public void removeGuildClaimSync(String chunkKey) {
        if (!isEnabled) return;
        String sql = "DELETE FROM " + tablePrefix + "guild_claims WHERE chunk_key = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chunkKey);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove guild claim: " + e.getMessage());
        }
    }

    public void saveGuildSync(Guild guild) {
        if (!isEnabled) return;
        
        String sqlGuild = "INSERT INTO " + tablePrefix + "guilds (tag, name, leader_uuid, leader_name, created_at, emerald_wallet, home_world, home_x, home_y, home_z, home_yaw, home_pitch, balance, vault_level, member_level, friendly_fire, vault_items, logs, allies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=?, leader_uuid=?, leader_name=?, emerald_wallet=?, home_world=?, home_x=?, home_y=?, home_z=?, home_yaw=?, home_pitch=?, balance=?, vault_level=?, member_level=?, friendly_fire=?, vault_items=?, logs=?, allies=?";
        
        String sqlDeleteMembers = "DELETE FROM " + tablePrefix + "guild_members WHERE guild_tag = ?";
        String sqlInsertMember = "INSERT INTO " + tablePrefix + "guild_members (guild_tag, uuid, name, role, joined_at) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmtGuild = conn.prepareStatement(sqlGuild)) {
                stmtGuild.setString(1, guild.getTag());
                stmtGuild.setString(2, guild.getName());
                stmtGuild.setString(3, guild.getLeader().toString());
                stmtGuild.setString(4, guild.getStoredName(guild.getLeader()) != null ? guild.getStoredName(guild.getLeader()) : "");
                stmtGuild.setLong(5, guild.getCreatedAt());
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
                
                stmtGuild.setDouble(13, guild.getBalance());
                stmtGuild.setInt(14, guild.getVaultLevel());
                stmtGuild.setInt(15, guild.getMemberLevel());
                stmtGuild.setBoolean(16, guild.isFriendlyFire());
                stmtGuild.setString(17, itemStackArrayToBase64(guild.getVaultInventory(guild.getVaultLevel() * 9, "").getContents()));
                stmtGuild.setString(18, String.join(";;", guild.getLogs()));
                stmtGuild.setString(19, String.join(",", guild.getAllies()));

                stmtGuild.setString(20, guild.getName());
                stmtGuild.setString(21, guild.getLeader().toString());
                stmtGuild.setString(22, guild.getStoredName(guild.getLeader()) != null ? guild.getStoredName(guild.getLeader()) : "");
                stmtGuild.setInt(23, guild.getEmeraldWallet());
                if (home != null && home.getWorld() != null) {
                    stmtGuild.setString(24, home.getWorld().getName());
                    stmtGuild.setDouble(25, home.getX());
                    stmtGuild.setDouble(26, home.getY());
                    stmtGuild.setDouble(27, home.getZ());
                    stmtGuild.setFloat(28, home.getYaw());
                    stmtGuild.setFloat(29, home.getPitch());
                } else {
                    stmtGuild.setString(24, null);
                    stmtGuild.setDouble(25, 0);
                    stmtGuild.setDouble(26, 0);
                    stmtGuild.setDouble(27, 0);
                    stmtGuild.setFloat(28, 0);
                    stmtGuild.setFloat(29, 0);
                }
                
                stmtGuild.setDouble(30, guild.getBalance());
                stmtGuild.setInt(31, guild.getVaultLevel());
                stmtGuild.setInt(32, guild.getMemberLevel());
                stmtGuild.setBoolean(33, guild.isFriendlyFire());
                stmtGuild.setString(34, itemStackArrayToBase64(guild.getVaultInventory(guild.getVaultLevel() * 9, "").getContents()));
                stmtGuild.setString(35, String.join(";;", guild.getLogs()));
                stmtGuild.setString(36, String.join(",", guild.getAllies()));

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
                    stmtIns.setLong(5, guild.getJoinedAt(member));
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
    
    public static String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null) return "";
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack[] itemStackArrayFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[54];
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];
            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (Exception e) {
            return new ItemStack[54];
        }
    }
}
