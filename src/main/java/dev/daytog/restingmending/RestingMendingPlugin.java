package dev.daytog.restingmending;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sneak and stand still for 1.5 seconds, then spend one XP point
 * to restore up to two durability every 0.1 seconds.
 * Only the item in the main hand is repaired.
 */
public final class RestingMendingPlugin extends JavaPlugin {
    private static final int REST_TICKS = 30;
    private static final int REPAIR_TICKS = 2;
    private static final double MOVEMENT_EPSILON_SQUARED = 1.0e-8;
    private final Map<UUID, RestState> states = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        getLogger().info("Resting Mending enabled.");
    }

    @Override
    public void onDisable() {
        states.clear();
    }

    private void tick() {
        states.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            ItemStack item = player.getInventory().getItemInMainHand();
            if (!player.isSneaking() || !needsMending(item) || experiencePoints(player) <= 0) {
                states.remove(id);
                continue;
            }

            Location now = player.getLocation();
            int slot = player.getInventory().getHeldItemSlot();
            RestState state = states.get(id);
            if (state == null || state.slot != slot || !state.world.equals(now.getWorld().getUID())
                    || state.material != item.getType()) {
                states.put(id, new RestState(now, slot, item));
                continue;
            }

            double dx = now.getX() - state.x;
            double dy = now.getY() - state.y;
            double dz = now.getZ() - state.z;
            // Ignore camera rotation but reset on real movement, including knockback.
            if (dx * dx + dy * dy + dz * dz > MOVEMENT_EPSILON_SQUARED) {
                states.put(id, new RestState(now, slot, item));
                continue;
            }

            state.ticks++;
            if (state.ticks < REST_TICKS || (state.ticks - REST_TICKS) % REPAIR_TICKS != 0) {
                continue;
            }

            if (!spendOneExperiencePoint(player)) {
                states.remove(id);
                continue;
            }

            // Re-check the held item right before applying the repair.
            item = player.getInventory().getItemInMainHand();
            if (!needsMending(item)) {
                // An inventory change during the tick should never waste XP.
                addOneExperiencePoint(player);
                states.remove(id);
                continue;
            }

            Damageable meta = (Damageable) item.getItemMeta();
            meta.setDamage(Math.max(0, meta.getDamage() - 2));
            item.setItemMeta(meta);
            player.getInventory().setItemInMainHand(item);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.20f, 1.0f);

            if (!needsMending(item)) {
                states.remove(id);
            }
        }
    }

    private static boolean needsMending(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.containsEnchantment(Enchantment.MENDING)) {
            return false;
        }
        if (!(item.getItemMeta() instanceof Damageable meta)) {
            return false;
        }
        return meta.hasDamage() && meta.getDamage() > 0;
    }

    // Total spendable XP is derived from level and progress, not getTotalExperience():
    // Bukkit's total-experience counter may include XP previously spent on enchanting.
    private static int experiencePoints(Player player) {
        int level = player.getLevel();
        return xpAtLevel(level) + Math.round(player.getExp() * xpToNextLevel(level));
    }

    private static int xpAtLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    private static int xpToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    private static boolean spendOneExperiencePoint(Player player) {
        int points = experiencePoints(player);
        if (points <= 0) return false;
        setExperiencePoints(player, points - 1);
        return true;
    }

    private static void addOneExperiencePoint(Player player) {
        setExperiencePoints(player, experiencePoints(player) + 1);
    }

    private static void setExperiencePoints(Player player, int points) {
        int remaining = Math.max(0, points);
        int level = 0;
        // Avoid a potentially expensive level-by-level loop for high XP totals.
        int low = 0;
        int high = 1;
        while (xpAtLevel(high) <= remaining && high < 100_000) high *= 2;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (xpAtLevel(mid) <= remaining) low = mid;
            else high = mid - 1;
        }
        level = low;
        player.setLevel(level);
        player.setExp((remaining - xpAtLevel(level)) / (float) xpToNextLevel(level));
    }

    private static final class RestState {
        final UUID world;
        final int slot;
        final org.bukkit.Material material;
        final double x, y, z;
        int ticks;

        RestState(Location location, int slot, ItemStack item) {
            world = location.getWorld().getUID();
            this.slot = slot;
            material = item.getType();
            x = location.getX();
            y = location.getY();
            z = location.getZ();
        }
    }
}
