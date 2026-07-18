package id.rumahkita.trade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import id.rumahkita.economy.RumahKitaEconomyRupiahPlugin;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager implements Listener {
    private final id.rumahkita.economy.RumahKitaEconomyRupiahPlugin plugin;
    private final Map<UUID, UUID> invites = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();

    public TradeManager(id.rumahkita.economy.RumahKitaEconomyRupiahPlugin plugin) {
        this.plugin = plugin;
    }

    public void inviteTrade(Player inviter, Player target) {
        if (activeTrades.containsKey(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "You are currently in a trade session.");
            return;
        }
        if (activeTrades.containsKey(target.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "That player is currently in a trade session.");
            return;
        }

        invites.put(target.getUniqueId(), inviter.getUniqueId());
        
        inviter.sendMessage(ChatColor.GREEN + "Successfully sent trade request to " + target.getName() + ".");
        
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a--- &lTRADE REQUEST &a---"));
        target.sendMessage(ChatColor.YELLOW + inviter.getName() + " has requested to trade with you.");
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&eType &a/trade accept &eor &c/trade deny"));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (invites.containsKey(target.getUniqueId()) && invites.get(target.getUniqueId()).equals(inviter.getUniqueId())) {
                    invites.remove(target.getUniqueId());
                    if (inviter.isOnline()) inviter.sendMessage(ChatColor.RED + "Trade request to " + target.getName() + " expired.");
                    if (target.isOnline()) target.sendMessage(ChatColor.RED + "Trade request from " + inviter.getName() + " expired.");
                }
            }
        }.runTaskLater(plugin, 600L);
    }

    public void acceptTrade(Player target) {
        if (!invites.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "You do not have any active trade requests.");
            return;
        }

        UUID inviterId = invites.remove(target.getUniqueId());
        Player inviter = plugin.getServer().getPlayer(inviterId);

        if (inviter == null || !inviter.isOnline()) {
            target.sendMessage(ChatColor.RED + "The player who invited you is now offline.");
            return;
        }

        if (activeTrades.containsKey(inviter.getUniqueId()) || activeTrades.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "One of you is already in a trade session.");
            return;
        }

        startTrade(inviter, target);
    }

    public void denyTrade(Player target) {
        if (!invites.containsKey(target.getUniqueId())) {
            target.sendMessage(ChatColor.RED + "You do not have any active trade requests.");
            return;
        }

        UUID inviterId = invites.remove(target.getUniqueId());
        Player inviter = plugin.getServer().getPlayer(inviterId);

        target.sendMessage(ChatColor.RED + "You declined the trade request.");
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(ChatColor.RED + target.getName() + " declined your trade request.");
        }
    }

    private void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(p1, p2);
        activeTrades.put(p1.getUniqueId(), session);
        activeTrades.put(p2.getUniqueId(), session);
        session.open();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session == null) return;
        
        if (e.getInventory().equals(session.getInvP1()) || e.getInventory().equals(session.getInvP2())) {
            if (session.isLocked()) {
                e.setCancelled(true);
                return;
            }

            int slot = e.getRawSlot();
            boolean isTop = slot < 54 && slot >= 0;

            if (isTop) {
                if (slot == TradeSession.LEFT_ACCEPT_SLOT) {
                    e.setCancelled(true);
                    session.toggleReady(p);
                    checkReady(session);
                    return;
                }
                if (slot == TradeSession.LEFT_CANCEL_SLOT) {
                    e.setCancelled(true);
                    cancelTrade(session, p.getName() + " cancelled the trade.");
                    return;
                }
                if (slot == TradeSession.LEFT_MONEY_SLOT) {
                    e.setCancelled(true);
                    if (session.isP1(p)) session.p1Typing = true;
                    if (session.isP2(p)) session.p2Typing = true;
                    p.closeInventory();
                    p.sendMessage(ChatColor.GOLD + "=== TRADE MONEY ===");
                    p.sendMessage(ChatColor.YELLOW + "Type the amount of money you want to add in chat.");
                    p.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel.");
                    return;
                }

                if (!session.isLeftSlot(slot)) {
                    e.setCancelled(true);
                } else {
                    session.resetReady();
                    Bukkit.getScheduler().runTask(plugin, session::syncInventories);
                }
            } else {
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                    p.sendMessage(ChatColor.RED + "Shift-click is disabled in Trade menu for security.");
                    return;
                }
                session.resetReady();
                Bukkit.getScheduler().runTask(plugin, session::syncInventories);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session == null) return;
        
        if (e.getInventory().equals(session.getInvP1()) || e.getInventory().equals(session.getInvP2())) {
            if (session.isLocked()) {
                e.setCancelled(true);
                return;
            }

            for (int slot : e.getRawSlots()) {
                if (slot < 54) {
                    if (!session.isLeftSlot(slot)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            session.resetReady();
            Bukkit.getScheduler().runTask(plugin, session::syncInventories);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null) {
            if (!session.isLocked()) {
                if (session.isP1(p) && session.p1Typing) return;
                if (session.isP2(p) && session.p2Typing) return;
                
                cancelTrade(session, p.getName() + " closed the trade menu.");
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null && !session.isLocked()) {
            if ((session.isP1(p) && session.p1Typing) || (session.isP2(p) && session.p2Typing)) {
                e.setCancelled(true);
                String msg = e.getMessage().trim().toLowerCase();
                
                if (session.isP1(p)) session.p1Typing = false;
                if (session.isP2(p)) session.p2Typing = false;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("batal")) {
                        p.sendMessage(ChatColor.RED + "Input cancelled.");
                    } else {
                        try {
                            long amount = Long.parseLong(msg);
                            if (amount < 0) throw new NumberFormatException();
                            RumahKitaEconomyRupiahPlugin eco = RumahKitaEconomyRupiahPlugin.getInstance();
                            if (eco.getBalance(p.getUniqueId()) < amount) {
                                p.sendMessage(ChatColor.RED + "Insufficient balance!");
                            } else {
                                if (session.isP1(p)) session.setP1Money(amount);
                                if (session.isP2(p)) session.setP2Money(amount);
                                session.resetReady();
                                p.sendMessage(ChatColor.GREEN + "Successfully set trade money to " + eco.formatRp(amount));
                            }
                        } catch (Exception ex) {
                            p.sendMessage(ChatColor.RED + "Invalid number!");
                        }
                    }
                    if (activeTrades.containsKey(p.getUniqueId())) {
                        session.open(p);
                    }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        TradeSession session = activeTrades.get(p.getUniqueId());
        if (session != null) {
            if (!session.isLocked()) {
                cancelTrade(session, p.getName() + " logged out.");
            }
        }
    }

    private void checkReady(TradeSession session) {
        if (session.isBothReady()) {
            session.setLocked(true);
            
            Player p1 = session.getP1();
            Player p2 = session.getP2();
            
            p1.sendMessage(ChatColor.YELLOW + "Trade will process in 3 seconds...");
            p2.sendMessage(ChatColor.YELLOW + "Trade will process in 3 seconds...");
            p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            
            new BukkitRunnable() {
                int count = 3;
                @Override
                public void run() {
                    if (count > 0) {
                        p1.sendTitle(ChatColor.GREEN + "" + count, "", 5, 10, 5);
                        p2.sendTitle(ChatColor.GREEN + "" + count, "", 5, 10, 5);
                        p1.playSound(p1.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        p2.playSound(p2.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        count--;
                    } else {
                        session.finishTrade();
                        activeTrades.remove(p1.getUniqueId());
                        activeTrades.remove(p2.getUniqueId());
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 20L, 20L);
        }
    }

    private void cancelTrade(TradeSession session, String reason) {
        session.setLocked(true);
        session.returnItems();
        
        Player p1 = session.getP1();
        Player p2 = session.getP2();
        
        activeTrades.remove(p1.getUniqueId());
        activeTrades.remove(p2.getUniqueId());
        
        if (p1.isOnline()) {
            p1.sendMessage(ChatColor.RED + "Trade cancelled: " + reason);
            session.closeSafely();
        }
        if (p2.isOnline()) {
            p2.sendMessage(ChatColor.RED + "Trade cancelled: " + reason);
            session.closeSafely();
        }
    }

    public void cancelAllActiveTrades() {
        for (TradeSession session : activeTrades.values()) {
            if (!session.isLocked()) {
                cancelTrade(session, "Server reload/restart.");
            }
        }
        activeTrades.clear();
    }
}
