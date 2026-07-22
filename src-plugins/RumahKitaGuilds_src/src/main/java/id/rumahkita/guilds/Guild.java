/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.configuration.ConfigurationSection
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.GuildRole;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

public final class Guild {
    private String tag;
    private String name;
    private UUID leader;
    private final Map<UUID, GuildRole> members = new LinkedHashMap<UUID, GuildRole>();
    private final Map<UUID, String> memberNames = new LinkedHashMap<UUID, String>();
    private final Map<UUID, Long> joinedAt = new LinkedHashMap<UUID, Long>();
    private final Set<String> claimedChunks = new HashSet<String>();
    private long createdAt;
    private Location home;
    private int emeraldWallet;
    private int vaultLevel = 1;
    private ItemStack[] vaultItems = new ItemStack[54];
    private final List<String> logs = new ArrayList<>();
    private double balance = 0.0;
    private boolean friendlyFire = false;
    private final Set<String> allies = new HashSet<>();
    
    // Area Settings
    private boolean mobSpawning = false;
    private boolean mobGriefing = false;
    private boolean publicInteraction = false;
    private boolean allowPvp = false;
    
    public Guild(String tag, String name, UUID leader, String leaderName, long createdAt) {
        this.tag = tag;
        this.name = name;
        this.leader = leader;
        this.createdAt = createdAt <= 0L ? System.currentTimeMillis() : createdAt;
        this.addMember(leader, leaderName, GuildRole.LEADER, this.createdAt);
    }

    public String getTag() {
        return this.tag;
    }

    public String getName() {
        return this.name;
    }

    public UUID getLeader() {
        return this.leader;
    }

    public Map<UUID, GuildRole> getMembers() {
        return this.members;
    }

    public Location getHome() {
        return this.home == null ? null : this.home.clone();
    }

    public int size() {
        return this.members.size();
    }

    public int getEmeraldWallet() {
        return this.emeraldWallet;
    }

    public void addEmeraldWallet(int amount) {
        this.emeraldWallet = Math.max(0, this.emeraldWallet + amount);
    }

    public boolean withdrawEmeraldWallet(int amount) {
        if (amount <= 0 || this.emeraldWallet < amount) {
            return false;
        }
        this.emeraldWallet -= amount;
        return true;
    }

    public void setHome(Location home) {
        this.home = home == null ? null : home.clone();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Set<String> getClaimedChunks() {
        return this.claimedChunks;
    }

    public void addClaim(String chunkKey) {
        this.claimedChunks.add(chunkKey);
    }

    public void removeClaim(String chunkKey) {
        this.claimedChunks.remove(chunkKey);
    }

    public boolean hasClaim(String chunkKey) {
        return this.claimedChunks.contains(chunkKey);
    }

    public boolean isMember(UUID uuid) {
        return this.members.containsKey(uuid);
    }

    public GuildRole getRole(UUID uuid) {
        return this.members.getOrDefault(uuid, GuildRole.MEMBER);
    }

    public String getStoredName(UUID uuid) {
        return this.memberNames.get(uuid);
    }

    public void addMember(UUID uuid, String name, GuildRole role, long joinTime) {
        this.members.put(uuid, role == null ? GuildRole.MEMBER : role);
        if (name != null && !name.isBlank()) {
            this.memberNames.put(uuid, name);
        }
        this.joinedAt.put(uuid, joinTime <= 0L ? System.currentTimeMillis() : joinTime);
        if (role == GuildRole.LEADER) {
            this.leader = uuid;
        }
    }

    public void removeMember(UUID uuid) {
        this.members.remove(uuid);
        this.memberNames.remove(uuid);
        this.joinedAt.remove(uuid);
    }

    public void setRole(UUID uuid, GuildRole role) {
        if (!this.members.containsKey(uuid)) {
            return;
        }
        if (role == GuildRole.LEADER) {
            for (UUID member : this.members.keySet()) {
                if (this.members.get(member) != GuildRole.LEADER) continue;
                this.members.put(member, GuildRole.ADMIN);
            }
            this.leader = uuid;
        }
        this.members.put(uuid, role);
    }

    public void setStoredName(UUID uuid, String name) {
        if (name != null && !name.isBlank()) {
            this.memberNames.put(uuid, name);
        }
    }

    public void saveTo(ConfigurationSection section) {
        section.set("tag", this.tag);
        section.set("name", this.name);
        section.set("owner", this.leader.toString());
        section.set("leader", this.leader.toString());
        section.set("owner-name", this.memberNames.getOrDefault(this.leader, ""));
        section.set("created-at", this.createdAt);
        section.set("wallet.emerald", this.emeraldWallet);
        
        section.set("balance", this.balance);
        section.set("vault-level", this.vaultLevel);
        section.set("member-level", this.memberLevel);
        section.set("friendly-fire", this.friendlyFire);
        section.set("vault-items", DatabaseManager.itemStackArrayToBase64(this.getVaultInventory(this.vaultLevel * 9, "").getContents()));
        section.set("logs", new java.util.ArrayList<>(this.logs));
        section.set("allies", new java.util.ArrayList<>(this.allies));
        
        section.set("settings.mob-spawning", this.mobSpawning);
        section.set("settings.mob-griefing", this.mobGriefing);
        section.set("settings.public-interaction", this.publicInteraction);
        section.set("settings.allow-pvp", this.allowPvp);
        
        section.set("members", null);
        ConfigurationSection memberSection = section.createSection("members");
        for (java.util.Map.Entry<java.util.UUID, GuildRole> entry : this.members.entrySet()) {
            java.util.UUID uuid = entry.getKey();
            ConfigurationSection m = memberSection.createSection(uuid.toString());
            m.set("name", this.memberNames.getOrDefault(uuid, ""));
            m.set("role", entry.getValue().name());
            m.set("joined-at", this.joinedAt.getOrDefault(uuid, this.createdAt));
        }
        if (this.home != null && this.home.getWorld() != null) {
            section.set("home.world", this.home.getWorld().getName());
            section.set("home.x", this.home.getX());
            section.set("home.y", this.home.getY());
            section.set("home.z", this.home.getZ());
            section.set("home.yaw", this.home.getYaw());
            section.set("home.pitch", this.home.getPitch());
        } else {
            section.set("home", null);
        }
        
        section.set("claims", new java.util.ArrayList<>(this.claimedChunks));
    }

    public static Guild loadFrom(ConfigurationSection section) {
        if (section == null) return null;
        String tag = section.getString("tag");
        String name = section.getString("name");
        String leaderString = section.getString("leader", section.getString("owner"));
        String leaderName = section.getString("owner-name", "");
        long createdAt = section.getLong("created-at", System.currentTimeMillis());
        if (tag == null || name == null || leaderString == null) return null;
        
        java.util.UUID leader;
        try { leader = java.util.UUID.fromString(leaderString); } catch (Exception ex) { return null; }
        
        Guild guild = new Guild(tag, name, leader, leaderName, createdAt);
        guild.members.clear();
        guild.memberNames.clear();
        guild.joinedAt.clear();
        guild.emeraldWallet = Math.max(0, section.getInt("wallet.emerald", section.getInt("wallet-emerald", 0)));
        
        guild.balance = section.getDouble("balance", 0.0);
        guild.vaultLevel = section.getInt("vault-level", 1);
        guild.memberLevel = section.getInt("member-level", 1);
        guild.friendlyFire = section.getBoolean("friendly-fire", false);
        guild.setVaultItems(DatabaseManager.itemStackArrayFromBase64(section.getString("vault-items", "")));
        
        java.util.List<String> logsList = section.getStringList("logs");
        if (logsList != null) guild.logs.addAll(logsList);
        
        java.util.List<String> alliesList = section.getStringList("allies");
        if (alliesList != null) guild.allies.addAll(alliesList);
        
        guild.mobSpawning = section.getBoolean("settings.mob-spawning", false);
        guild.mobGriefing = section.getBoolean("settings.mob-griefing", false);
        guild.publicInteraction = section.getBoolean("settings.public-interaction", false);
        guild.allowPvp = section.getBoolean("settings.allow-pvp", false);
        
        ConfigurationSection membersSection = section.getConfigurationSection("members");
        if (membersSection != null) {
            for (String key : membersSection.getKeys(false)) {
                try {
                    java.util.UUID uuid = java.util.UUID.fromString(key);
                    Object raw = membersSection.get(key);
                    GuildRole role2;
                    String storedName = "";
                    long joined = createdAt;
                    if (raw instanceof String) {
                        role2 = GuildRole.fromString((String)raw);
                    } else {
                        ConfigurationSection m = membersSection.getConfigurationSection(key);
                        role2 = GuildRole.fromString(m == null ? null : m.getString("role"));
                        storedName = m == null ? "" : m.getString("name", "");
                        joined = m == null ? createdAt : m.getLong("joined-at", createdAt);
                        if (m == null || !m.contains("role")) {
                            role2 = uuid.equals(leader) ? GuildRole.LEADER : GuildRole.MEMBER;
                        }
                    }
                    guild.addMember(uuid, storedName, role2, joined);
                } catch (Exception ignored) {}
            }
        }
        if (!guild.members.containsKey(leader)) {
            guild.addMember(leader, leaderName, GuildRole.LEADER, createdAt);
        }
        if (guild.members.values().stream().noneMatch(r -> r == GuildRole.LEADER)) {
            guild.setRole(leader, GuildRole.LEADER);
        }
        ConfigurationSection homeSection = section.getConfigurationSection("home");
        if (homeSection != null) {
            String worldName = homeSection.getString("world");
            if (worldName != null && org.bukkit.Bukkit.getWorld(worldName) != null) {
                guild.home = new org.bukkit.Location(org.bukkit.Bukkit.getWorld(worldName), 
                    homeSection.getDouble("x"), homeSection.getDouble("y"), homeSection.getDouble("z"), 
                    (float)homeSection.getDouble("yaw"), (float)homeSection.getDouble("pitch"));
            }
        }
        
        java.util.List<String> claimsList = section.getStringList("claims");
        if (claimsList != null) guild.claimedChunks.addAll(claimsList);
        
        return guild;
    }

    public int getVaultLevel() { return this.vaultLevel; }
    public void setVaultLevel(int level) { this.vaultLevel = level; }
    
        public void setVaultItems(ItemStack[] items) { this.vaultItems = items; }
    
    public void addLog(String log) {
        if (!log.startsWith("[")) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM HH:mm");
            log = "[" + sdf.format(new java.util.Date()) + "] " + log;
        }
        this.logs.add(log);
        if (this.logs.size() > 50) this.logs.remove(0);
    }
    public List<String> getLogs() { return this.logs; }
    
    public void addBalance(double amount) { this.balance += amount; }
    public boolean withdrawBalance(double amount) { 
        if (this.balance < amount) return false;
        this.balance -= amount;
        return true;
    }
    public double getBalance() { return this.balance; }
    public void setBalance(double amount) { this.balance = amount; }
    
    public boolean isFriendlyFire() { return this.friendlyFire; }
    public void setFriendlyFire(boolean b) { this.friendlyFire = b; }
    
    public void addAlly(String guildTag) { this.allies.add(guildTag); }
    public boolean isAlly(String guildTag) { return this.allies.contains(guildTag); }
    public void removeAlly(String guildTag) { this.allies.remove(guildTag); }
    public Set<String> getAllies() { return this.allies; }

    public boolean isMobSpawning() { return mobSpawning; }
    public void setMobSpawning(boolean mobSpawning) { this.mobSpawning = mobSpawning; }
    
    public boolean isMobGriefing() { return mobGriefing; }
    public void setMobGriefing(boolean mobGriefing) { this.mobGriefing = mobGriefing; }
    
    public boolean isPublicInteraction() { return publicInteraction; }
    public void setPublicInteraction(boolean publicInteraction) { this.publicInteraction = publicInteraction; }
    
    public boolean isAllowPvp() { return allowPvp; }
    public void setAllowPvp(boolean allowPvp) { this.allowPvp = allowPvp; }


    private int memberLevel = 1;
    public int getMemberLevel() { return this.memberLevel; }
    public void setMemberLevel(int level) { this.memberLevel = level; }
    
    public long getCreatedAt() { return this.createdAt; }
    
    public long getJoinedAt(java.util.UUID uuid) {
        return this.joinedAt.getOrDefault(uuid, this.createdAt);
    }
    
    private org.bukkit.inventory.Inventory vaultInventory;
    public org.bukkit.inventory.Inventory getVaultInventory(int size, String title) {
        if (this.vaultInventory == null) {
            this.vaultInventory = org.bukkit.Bukkit.createInventory(new VaultHolder(this), size, title);
            if (this.vaultItems != null) {
                this.vaultInventory.setContents(this.vaultItems);
            }
        }
        return this.vaultInventory;
    }
    
    public void closeVaultInventory() {
        if (this.vaultInventory != null) {
            for (org.bukkit.entity.HumanEntity viewer : new java.util.ArrayList<>(this.vaultInventory.getViewers())) {
                viewer.closeInventory();
            }
        }
    }

}