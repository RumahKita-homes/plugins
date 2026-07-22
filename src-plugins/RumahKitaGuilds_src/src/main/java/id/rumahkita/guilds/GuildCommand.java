/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabExecutor
 *  org.bukkit.entity.Player
 */
package id.rumahkita.guilds;

import id.rumahkita.guilds.Guild;
import id.rumahkita.guilds.GuildChatManager;
import id.rumahkita.guilds.GuildGui;
import id.rumahkita.guilds.GuildHomeManager;
import id.rumahkita.guilds.GuildManager;
import id.rumahkita.guilds.GuildRole;
import id.rumahkita.guilds.GuildWarManager;
import id.rumahkita.guilds.RumahKitaGuildsPlugin;
import id.rumahkita.guilds.EconomyManager;
import id.rumahkita.guilds.Text;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class GuildCommand
implements TabExecutor {
    private final RumahKitaGuildsPlugin plugin;
    private final GuildManager guildManager;
    private final GuildHomeManager homeManager;
    private final GuildChatManager chatManager;
    private final GuildGui gui;
    private final GuildWarManager warManager;
    private final EconomyManager economyManager;
    private final GuildSettingsGui settingsGui;
    private final Map<UUID, Invite> invites = new HashMap<UUID, Invite>();
    private final Map<String, Long> allyRequests = new HashMap<String, Long>();

    public GuildCommand(RumahKitaGuildsPlugin plugin, GuildManager guildManager, GuildHomeManager homeManager, GuildChatManager chatManager, GuildGui gui, GuildWarManager warManager, EconomyManager economyManager, GuildSettingsGui settingsGui) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.homeManager = homeManager;
        this.chatManager = chatManager;
        this.gui = gui;
        this.warManager = warManager;
        this.economyManager = economyManager;
        this.settingsGui = settingsGui;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub;
        if (command.getName().equalsIgnoreCase("guildchat")) {
            Player player = this.requirePlayer(sender);
            if (player == null || !this.hasUse(sender)) {
                return true;
            }
            if (args.length == 0) {
                this.chatManager.toggle(player);
            } else {
                this.chatManager.sendGuildMessage(player, this.join(args, 0));
            }
            return true;
        }
        if (args.length == 0) {
            Player player = this.requirePlayer(sender);
            if (player != null) {
                this.gui.open(player);
            }
            return true;
        }
        switch (sub = args[0].toLowerCase(Locale.ROOT)) {
            case "create": {
                this.create(sender, args);
                break;
            }
            case "invite": {
                this.invite(sender, args);
                break;
            }
            case "accept": {
                this.accept(sender, args);
                break;
            }
            case "members": {
                this.members(sender);
                break;
            }
            case "list": {
                this.list(sender);
                break;
            }
            case "leave": {
                this.leave(sender);
                break;
            }
            case "kick": {
                this.kick(sender, args);
                break;
            }
            case "promote": {
                this.promote(sender, args);
                break;
            }
            case "demote": {
                this.demote(sender, args);
                break;
            }
            case "role": {
                this.role(sender, args);
                break;
            }
            case "transfer": {
                this.transfer(sender, args);
                break;
            }
            case "rename": {
                this.rename(sender, args);
                break;
            }
            case "settag": {
                this.settag(sender, args);
                break;
            }
            case "sethome": {
                this.sethome(sender);
                break;
            }
            case "home": {
                this.home(sender);
                break;
            }
            case "delhome": {
                this.delhome(sender);
                break;
            }
            case "chat": 
            case "guildchat": 
            case "gc": {
                this.guildChat(sender, args);
                break;
            }
            case "war": {
                this.warManager.handleCommand(sender, args);
                break;
            }
            case "deposit": {
                this.deposit(sender, args);
                break;
            }
            case "withdraw": {
                this.withdraw(sender, args);
                break;
            }
            case "bank": {
                this.bank(sender);
                break;
            }
            case "bal":
            case "balance": {
                this.balance(sender);
                break;
            }
            case "pvp": {
                this.pvp(sender);
                break;
            }
            case "ally": {
                this.ally(sender, args);
                break;
            }
            case "neutral": {
                this.neutral(sender, args);
                break;
            }
            case "allies": {
                this.allies(sender);
                break;
            }
            case "upgrade": {
                this.upgrade(sender);
                break;
            }
            case "settings": {
                this.settings(sender);
                break;
            }
            case "v":
            case "vault": {
                this.vault(sender);
                break;
            }
            case "log":
            case "logs": {
                this.log(sender);
                break;
            }
            case "disband": {
                this.disband(sender);
                break;
            }
            case "adminreload": 
            case "reload": {
                this.adminReload(sender);
                break;
            }
            case "save": {
                this.adminSave(sender);
                break;
            }
            case "info": {
                this.adminInfo(sender, args);
                break;
            }
            case "forcedisband": {
                this.adminForceDisband(sender, args);
                break;
            }
            default: {
                this.help(sender);
            }
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            Text.msg(sender, Text.prefixed(this.plugin, "only-player"));
            return null;
        }
        Player player = (Player)sender;
        return player;
    }

    private boolean hasUse(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.use")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return false;
        }
        return true;
    }

    private void guildChat(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length <= 1) {
            this.chatManager.toggle(player);
        } else {
            this.chatManager.sendGuildMessage(player, this.join(args, 1));
        }
    }

    private void create(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (!player.hasPermission("rumahkitaguilds.create")) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 3) {
            Text.msg((CommandSender)player, "&eUsage: /guild create <TAG> <Guild Name>");
            return;
        }
        String tag = args[1];
        String name = this.join(args, 2);
        GuildManager.CreateResult result = this.guildManager.createGuild(player, tag, name);
        switch (result) {
            case SUCCESS: {
                Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "guild-created"), "%name%", name, "%tag%", tag.toUpperCase()));
                break;
            }
            case ALREADY_IN_GUILD: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "already-in-guild"));
                break;
            }
            case INVALID_TAG: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-tag"));
                break;
            }
            case INVALID_NAME: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-name"));
                break;
            }
            case DUPLICATE_TAG: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "duplicate-tag"));
                break;
            }
            case DUPLICATE_NAME: {
                Text.msg((CommandSender)player, Text.prefixed(this.plugin, "duplicate-name"));
                break;
            }
            case NOT_ENOUGH_COST: {
                double cost = this.plugin.getConfig().getDouble("settings.create-cost.money", 1000.0);
                Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "create-cost-missing"), "%amount%", this.economyManager.format(cost)));
            }
        }
    }

    private void invite(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild invite <player>");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        Player target = Bukkit.getPlayerExact((String)args[1]);
        if (target == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        if (this.guildManager.hasGuild(target.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "target-already-guild"));
            return;
        }
        if (guild.size() >= this.guildManager.getMaxMembers(guild)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "max-members"));
            return;
        }
        long expireAt = System.currentTimeMillis() + (long)this.plugin.getConfig().getInt("settings.invite-expire-seconds", 120) * 1000L;
        guild.addLog(player.getName() + " invited " + target.getName() + ".");
        this.invites.put(target.getUniqueId(), new Invite(guild.getTag(), expireAt, player.getUniqueId()));
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "invite-sent"), "%player%", target.getName()));
        Text.msg((CommandSender)target, Text.replace(Text.prefixed(this.plugin, "invite-received"), "%player%", player.getName(), "%guild%", guild.getName(), "%tag%", guild.getTag()));
    }

    private void accept(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild accept <TAG>");
            return;
        }
        if (this.guildManager.hasGuild(player.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "already-in-guild"));
            return;
        }
        Invite invite = this.invites.get(player.getUniqueId());
        if (invite == null || !this.guildManager.normalizeTag(invite.guildTag).equals(this.guildManager.normalizeTag(args[1])) || invite.expireAt < System.currentTimeMillis()) {
            this.invites.remove(player.getUniqueId());
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invite-expired"));
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(invite.guildTag);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        if (guild.size() >= this.guildManager.getMaxMembers(guild)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "max-members"));
            return;
        }
        guild.addLog(player.getName() + " joined the guild.");
        this.guildManager.addMember(guild, player);
        this.invites.remove(player.getUniqueId());
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "joined-guild"), "%guild%", guild.getName()));
    }

    private void upgrade(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        this.plugin.getUpgradeGui().open(player, guild);
    }

    private void vault(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        int minMembers = this.plugin.getConfig().getInt("settings.upgrades.min-members", 3);
        if (guild.size() < minMembers) {
            Text.msg(sender, "&cYou need at least &e" + minMembers + " &cmembers to use the Guild Vault.");
            return;
        }
        if (guild.getBalance() < 0) {
            Text.msg(sender, "&cVault locked! Guild in debt: &e" + this.plugin.getEconomyManager().format(guild.getBalance()));
            Text.msg(sender, "&cDeposit money to unlock it.");
            return;
        }

        int vaultLvl = guild.getVaultLevel();
        if (vaultLvl <= 0) {
            Text.msg(sender, Text.prefixed(this.plugin, "prefix") + "&cYour guild has not unlocked the vault yet. Use &e/guild upgrade &cto unlock it.");
            return;
        }
        
        org.bukkit.inventory.Inventory inv = guild.getVaultInventory(1);
        
        player.openInventory(inv);
        Text.msg(sender, "&aOpened Guild Vault.");
    }

    private void members(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        this.gui.openMembers(player, guild);
    }

    private void list(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player)sender;
            this.gui.openGuildList(player);
        } else {
            for (Guild guild : this.guildManager.getGuilds()) {
                Text.msg(sender, "&b" + guild.getTag() + " &7- &f" + guild.getName() + " &8(" + guild.size() + " member)");
            }
        }
    }

    private void leave(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) == GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "leave-leader"));
            return;
        }
        this.guildManager.removeMember(guild, player.getUniqueId());
        this.chatManager.removeToggle(player.getUniqueId());
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "left-guild"));
    }

    private void kick(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild kick <player>");
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        GuildRole actorRole = guild.getRole(player.getUniqueId());
        if (!actorRole.atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        GuildRole targetRole = guild.getRole(targetUuid);
        if (targetRole == GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "cannot-kick-leader"));
            return;
        }
        if (actorRole == GuildRole.ADMIN && targetRole != GuildRole.MEMBER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.guildManager.removeMember(guild, targetUuid);
        this.chatManager.removeToggle(targetUuid);
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "kicked"), "%player%", this.guildManager.getOfflineName(targetUuid)));
    }

    private void promote(CommandSender sender, String[] args) {
        this.setRoleCommand(sender, args, true);
    }

    private void demote(CommandSender sender, String[] args) {
        this.setRoleCommand(sender, args, false);
    }

    private void setRoleCommand(CommandSender sender, String[] args, boolean promote) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, promote ? "&eUsage: /guild promote <player>" : "&eUsage: /guild demote <player>");
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        if (promote) {
            if (guild.getRole(targetUuid) != GuildRole.MEMBER) {
                Text.msg((CommandSender)player, "&cThat player is not a Member.");
                return;
            }
            guild.setRole(targetUuid, GuildRole.ADMIN);
            this.guildManager.save(guild);
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "promoted"), "%player%", this.guildManager.getOfflineName(targetUuid)));
        } else {
            if (guild.getRole(targetUuid) != GuildRole.ADMIN) {
                Text.msg((CommandSender)player, "&cThat player is not an Admin.");
                return;
            }
            guild.setRole(targetUuid, GuildRole.MEMBER);
            this.guildManager.save(guild);
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "demoted"), "%player%", this.guildManager.getOfflineName(targetUuid)));
        }
    }

    private void role(CommandSender sender, String[] args) {
        UUID target;
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        UUID uUID = target = args.length >= 2 ? this.guildManager.findMember(guild, args[1]) : player.getUniqueId();
        if (target == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        Text.msg((CommandSender)player, "&eRole &f" + this.guildManager.getOfflineName(target) + "&e: &f" + guild.getRole(target).displayName(this.plugin));
    }

    private void transfer(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild transfer <player>");
            return;
        }
        UUID targetUuid = this.guildManager.findMember(guild, args[1]);
        if (targetUuid == null || targetUuid.equals(player.getUniqueId())) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "player-not-found"));
            return;
        }
        guild.setRole(targetUuid, GuildRole.LEADER);
        guild.addLog(player.getName() + " transferred leadership to " + this.guildManager.getOfflineName(targetUuid) + ".");
        guild.setRole(player.getUniqueId(), GuildRole.ADMIN);
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "transfer-done"), "%player%", this.guildManager.getOfflineName(targetUuid)));
    }

    private void rename(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild rename <new name>");
            return;
        }
        String newName = this.join(args, 1);
        if (this.guildManager.renameGuild(guild, newName)) {
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "rename-done"), "%name%", newName));
        } else {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-name"));
        }
    }

    private void settag(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        if (args.length < 2) {
            Text.msg((CommandSender)player, "&eUsage: /guild settag <new tag>");
            return;
        }
        if (this.guildManager.changeTag(guild, args[1])) {
            Text.msg((CommandSender)player, Text.replace(Text.prefixed(this.plugin, "tag-done"), "%tag%", args[1].toUpperCase()));
        } else {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "invalid-tag"));
        }
    }

    private Guild leaderGuild(Player player) {
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return null;
        }
        if (guild.getRole(player.getUniqueId()) != GuildRole.LEADER) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return null;
        }
        return guild;
    }

    private void sethome(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        guild.setHome(player.getLocation());
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "home-set"));
    }

    private void home(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        this.homeManager.teleport(player, guild);
    }

    private void delhome(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) {
            Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        guild.setHome(null);
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, Text.prefixed(this.plugin, "home-deleted"));
    }

    private void disband(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.leaderGuild(player);
        if (guild == null) {
            return;
        }
        String name = guild.getName();
        this.guildManager.disband(guild);
        Bukkit.broadcastMessage((String)Text.color(Text.replace(Text.prefixed(this.plugin, "disbanded"), "%guild%", name)));
    }

    private void deposit(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        if (args.length < 2) { Text.msg((CommandSender)player, "&eUsage: /guild deposit <amount>"); return; }
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { Text.msg((CommandSender)player, "&cInvalid amount."); return; }
        if (amount <= 0) { Text.msg((CommandSender)player, "&cAmount must be greater than 0."); return; }
        if (!this.economyManager.has((org.bukkit.OfflinePlayer)player, amount)) { Text.msg((CommandSender)player, "&cYou don't have enough money."); return; }
        this.economyManager.withdraw((org.bukkit.OfflinePlayer)player, amount);
        guild.addBalance(amount);
        guild.addLog(player.getName() + " deposited $" + this.economyManager.format(amount));
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, "&aYou deposited &e" + this.economyManager.format(amount) + " &ainto the guild bank.");
    }

    private void balance(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) {
            return;
        }
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        Text.msg(sender, "&aYour guild balance is &e$" + this.economyManager.format(guild.getBalance()) + "&a.");
    }

    private void withdraw(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission")); return; }
        if (args.length < 2) { Text.msg((CommandSender)player, "&eUsage: /guild withdraw <amount>"); return; }
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (NumberFormatException e) { Text.msg((CommandSender)player, "&cInvalid amount."); return; }
        if (amount <= 0) { Text.msg((CommandSender)player, "&cAmount must be greater than 0."); return; }
        if (guild.getBalance() < amount) { Text.msg((CommandSender)player, "&cThe guild doesn't have enough money in the bank."); return; }
        guild.withdrawBalance(amount);
        this.economyManager.deposit((org.bukkit.OfflinePlayer)player, amount);
        guild.addLog(player.getName() + " withdrew $" + this.economyManager.format(amount));
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, "&aYou withdrew &e" + this.economyManager.format(amount) + " &afrom the guild bank.");
    }

    private void pvp(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission")); return; }
        guild.setFriendlyFire(!guild.isFriendlyFire());
        this.guildManager.save(guild);
        Text.msg((CommandSender)player, "&aGuild and Ally Friendly Fire is now " + (guild.isFriendlyFire() ? "&eON" : "&cOFF") + "&a.");
    }
    
    private void bank(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        this.gui.openBank(player, guild);
    }
    
    private void settings(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "not-in-guild"));
            return;
        }
        GuildRole role = guild.getRole(player.getUniqueId());
        if (!player.hasPermission("rumahkitaguilds.admin") && !role.atLeast(GuildRole.ADMIN)) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.settingsGui.open(player, guild);
    }

    private void ally(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission")); return; }
        
        if (args.length < 2) { Text.msg((CommandSender)player, "&eUsage: /guild ally <tag> [accept]"); return; }
        
        if (args[1].equalsIgnoreCase("accept")) {
            if (args.length < 3) { Text.msg((CommandSender)player, "&eUsage: /guild ally accept <tag>"); return; }
            String targetTag = args[2].toUpperCase(Locale.ROOT);
            String key = targetTag + ":" + guild.getTag().toUpperCase(Locale.ROOT);
            Long expire = allyRequests.get(key);
            if (expire == null || expire < System.currentTimeMillis()) {
                allyRequests.remove(key);
                Text.msg((CommandSender)player, "&cAlly request not found or has expired.");
                return;
            }
            Guild target = this.guildManager.getGuildByTag(targetTag);
            if (target == null) { Text.msg((CommandSender)player, "&cGuild not found."); return; }
            
            guild.addAlly(target.getTag());
            target.addAlly(guild.getTag());
            this.guildManager.save(guild);
            allyRequests.remove(key);
            
            Bukkit.broadcastMessage((String)Text.color(this.plugin.getConfig().getString("settings.prefix", "&8[&bRumahKitaGuilds&8] ") + "&dGuild &f" + guild.getName() + " &dand &f" + target.getName() + " &dare now ALLIES!"));
            return;
        }
        
        String targetTag = args[1].toUpperCase(Locale.ROOT);
        if (guild.getTag().equalsIgnoreCase(targetTag)) { Text.msg((CommandSender)player, "&cYou cannot ally with your own guild."); return; }
        Guild target = this.guildManager.getGuildByTag(targetTag);
        if (target == null) { Text.msg((CommandSender)player, "&cGuild not found."); return; }
        if (guild.isAlly(target.getTag())) { Text.msg((CommandSender)player, "&cYou are already allied with that guild."); return; }
        
        String key = guild.getTag().toUpperCase(Locale.ROOT) + ":" + target.getTag().toUpperCase(Locale.ROOT);
        allyRequests.put(key, System.currentTimeMillis() + 120000L);
        Text.msg((CommandSender)player, "&aAlly request sent to guild &e" + target.getTag() + "&a.");
        
        for (Player p : target.getMembers().keySet().stream().map(Bukkit::getPlayer).filter(p -> p != null).collect(Collectors.toList())) {
            if (target.getRole(p.getUniqueId()).atLeast(GuildRole.ADMIN)) {
                Text.msg((CommandSender)p, "&eGuild &b" + guild.getTag() + " &ewants to form an Alliance! Type &a/guild ally accept " + guild.getTag());
            }
        }
    }

    private void neutral(CommandSender sender, String[] args) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        if (!guild.getRole(player.getUniqueId()).atLeast(GuildRole.ADMIN)) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "no-permission")); return; }
        if (args.length < 2) { Text.msg((CommandSender)player, "&eUsage: /guild neutral <target tag>"); return; }
        
        String targetTag = args[1].toUpperCase(Locale.ROOT);
        Guild target = this.guildManager.getGuildByTag(targetTag);
        if (target == null) { Text.msg((CommandSender)player, "&cGuild not found."); return; }
        
        if (!guild.isAlly(target.getTag())) {
            Text.msg((CommandSender)player, "&cYou are not allied with that guild.");
            return;
        }
        
        guild.removeAlly(target.getTag());
        target.removeAlly(guild.getTag());
        this.guildManager.save(guild);
        
        Bukkit.broadcastMessage((String)Text.color(this.plugin.getConfig().getString("settings.prefix", "&8[&bRumahKitaGuilds&8] ") + "&cGuild &f" + guild.getName() + " &cand &f" + target.getName() + " &care no longer allies."));
    }

    private void allies(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        
        if (guild.getAllies().isEmpty()) {
            Text.msg((CommandSender)player, "&cYour guild has no allies.");
            return;
        }
        Text.msg((CommandSender)player, "&dAllied Guilds: &f" + String.join("&7, &f", guild.getAllies()));
    }

    private void adminReload(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.plugin.reloadAll();
        Text.msg(sender, Text.prefixed(this.plugin, "reloaded"));
    }

    private void adminSave(CommandSender sender) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        this.guildManager.save();
        Text.msg(sender, Text.prefixed(this.plugin, "admin-saved"));
    }

    private void adminInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "&eUsage: /guild info <tag>");
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(args[1]);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        Text.msg(sender, "&bGuild: &f" + guild.getName() + " &8[&b" + guild.getTag() + "&8]");
        Text.msg(sender, "&7Leader: &f" + this.guildManager.getOfflineName(guild.getLeader()));
        Text.msg(sender, "&7Members: &f" + guild.size());
        Text.msg(sender, "&7Home: &f" + (guild.getHome() == null ? "Not set" : "Set"));
    }
    
    private void log(CommandSender sender) {
        Player player = this.requirePlayer(sender);
        if (player == null || !this.hasUse(sender)) return;
        Guild guild = this.guildManager.getGuild(player);
        if (guild == null) { Text.msg((CommandSender)player, Text.prefixed(this.plugin, "not-in-guild")); return; }
        
        java.util.List<String> logs = guild.getLogs();
        if (logs.isEmpty()) {
            Text.msg(sender, "&cNo logs found for this guild.");
            return;
        }
        
        Text.msg(sender, "&8[&b" + guild.getName() + " Logs&8]");
        for (String log : logs) {
            Text.msg(sender, "&7" + log);
        }
    }

    private void adminForceDisband(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rumahkitaguilds.admin")) {
            Text.msg(sender, Text.prefixed(this.plugin, "no-permission"));
            return;
        }
        if (args.length < 2) {
            Text.msg(sender, "&eUsage: /guild forcedisband <tag>");
            return;
        }
        Guild guild = this.guildManager.getGuildByTag(args[1]);
        if (guild == null) {
            Text.msg(sender, Text.prefixed(this.plugin, "guild-not-found"));
            return;
        }
        String name = guild.getName();
        this.guildManager.disband(guild);
        Text.msg(sender, "&aGuild " + name + " has been forcefully disbanded.");
    }

    private void help(CommandSender sender) {
        Text.msg(sender, "&8&m----------------------------");
        Text.msg(sender, "&bRumahKitaGuilds Commands");
        Text.msg(sender, "&e/guild &7- Open GUI");
        Text.msg(sender, "&e/guild create <TAG> <Name> &7- Create a guild");
        Text.msg(sender, "&e/guild disband &7- Disband your guild");
        Text.msg(sender, "&e/guild invite <player> &7- Invite a player");
        Text.msg(sender, "&e/guild accept <tag> &7- Accept an invite");
        Text.msg(sender, "&e/guild leave &7- Leave current guild");
        Text.msg(sender, "&e/guild kick <player> &7- Kick a member");
        Text.msg(sender, "&e/guild members &7- View member list");
        Text.msg(sender, "&e/guild list &7- View all guilds");
        Text.msg(sender, "&e/guild settings &7- Guild Claim Settings (Admin/Leader only)");
        Text.msg(sender, "&e/guild chat (or /guildchat) &7- Toggle guild chat");
        Text.msg(sender, "&e/guild home &7- Teleport to guild home");
        Text.msg(sender, "&e/guild sethome &7- Set guild home");
        Text.msg(sender, "&e/guild delhome &7- Delete guild home");
        Text.msg(sender, "&e/guild deposit <amount> &7- Deposit to bank");
        Text.msg(sender, "&e/guild withdraw <amount> &7- Withdraw from bank");
        Text.msg(sender, "&e/guild bal &7- Check guild balance");
        Text.msg(sender, "&e/guild vault &7- Open guild vault");
        Text.msg(sender, "&e/guild bank &7- Open guild bank");
        Text.msg(sender, "&e/guild upgrade &7- Open upgrade menu");
        Text.msg(sender, "&e/guild pvp &7- Toggle friendly fire");
        Text.msg(sender, "&e/guild war <tag> <bet> &7- Challenge a guild");
        Text.msg(sender, "&e/guild ally <tag> &7- Form an alliance");
        Text.msg(sender, "&e/guild neutral <tag> &7- Break an alliance");
        Text.msg(sender, "&e/guild allies &7- View alliances");
        Text.msg(sender, "&e/guild promote <player> &7- Promote member");
        Text.msg(sender, "&e/guild demote <player> &7- Demote admin");
        Text.msg(sender, "&e/guild rename <name> &7- Rename guild");
        Text.msg(sender, "&e/guild settag <tag> &7- Change guild tag");
        Text.msg(sender, "&8&m----------------------------");
    }

    private String join(String[] args, int start) {
        return Arrays.stream(args).skip(start).collect(Collectors.joining(" ")).trim();
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("guildchat")) {
            return List.of();
        }
        if (args.length == 1) {
            ArrayList<String> subs = new ArrayList<String>(List.of("create", "invite", "accept", "members", "list", "leave", "kick", "promote", "demote", "role", "transfer", "rename", "settag", "sethome", "home", "delhome", "chat", "war", "deposit", "withdraw", "bank", "bal", "balance", "pvp", "ally", "neutral", "allies", "disband", "upgrade", "vault", "log", "help", "settings"));
            if (sender.hasPermission("rumahkitaguilds.admin")) {
                subs.addAll(List.of("adminreload", "save", "info", "forcedisband"));
            }
            return this.filter(subs, args[0]);
        }
        if (args.length == 2 && List.of("invite", "kick", "promote", "demote", "role", "transfer").contains(args[0].toLowerCase())) {
            return this.filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("war")) {
            return this.filter(this.guildManager.getGuilds().stream().map(g -> this.guildManager.normalizeTag(g.getTag())).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 2 && List.of("accept", "info", "forcedisband", "ally", "neutral").contains(args[0].toLowerCase())) {
            return this.filter(this.guildManager.getGuilds().stream().map(g -> this.guildManager.normalizeTag(g.getTag())).collect(Collectors.toList()), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("ally") && args[1].equalsIgnoreCase("accept")) {
            return this.filter(this.guildManager.getGuilds().stream().map(g -> this.guildManager.normalizeTag(g.getTag())).collect(Collectors.toList()), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return list.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().collect(Collectors.toList());
    }

    private record Invite(String guildTag, long expireAt, UUID inviter) {
    }
}

