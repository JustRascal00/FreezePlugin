package org.rascal00.freezePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public final class FreezePlugin extends JavaPlugin {

    private final HashMap<UUID, Boolean> frozenPlayers = new HashMap<>();
    private final int freezeDuration = 30; // Freeze duration in seconds
    private final int freezeInterval = 600; // Interval between freezes in seconds (10 minutes)
    private boolean pluginActive = false; // Track if the plugin is active

    @Override
    public void onEnable() {
        getLogger().info("FreezePlugin has been enabled!");

        if (getCommand("start") == null) {
            getLogger().severe("Command 'start' is not registered in plugin.yml!");
            return;
        }

        getCommand("start").setExecutor((sender, command, label, args) -> {
            if (!pluginActive) {
                pluginActive = true;
                Bukkit.broadcastMessage("FreezePlugin has started! Players can now freeze their opponents when the freeze cycle is active.");

                // Schedule the recurring freeze mechanism
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            frozenPlayers.put(player.getUniqueId(), false);
                        }
                        Bukkit.broadcastMessage("Players can now choose to freeze their opponent!");
                    }
                }.runTaskTimer(this, 0L, freezeInterval * 20L);
            } else {
                sender.sendMessage("The FreezePlugin is already active.");
            }
            return true;
        });

        if (getCommand("freeze") == null) {
            getLogger().severe("Command 'freeze' is not registered in plugin.yml!");
            return;
        }

        getCommand("freeze").setExecutor((sender, command, label, args) -> {
            if (!pluginActive) {
                sender.sendMessage("The FreezePlugin is not active. Use /start to activate it.");
                return true;
            }

            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (args.length != 1) {
                    player.sendMessage("Usage: /freeze <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("Player not found or not online.");
                    return true;
                }

                if (frozenPlayers.getOrDefault(player.getUniqueId(), false)) {
                    player.sendMessage("You have already used your freeze ability this cycle.");
                    return true;
                }

                freezePlayer(target);
                frozenPlayers.put(player.getUniqueId(), true);
                player.sendMessage("You have frozen " + target.getName() + " for " + freezeDuration + " seconds!");
            }
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("FreezePlugin has been disabled!");
    }

    private void freezePlayer(Player player) {
        Location freezeLocation = player.getLocation();
        UUID playerUUID = player.getUniqueId();

        frozenPlayers.put(playerUUID, true);
        player.sendMessage("You have been frozen for " + freezeDuration + " seconds!");

        // Freeze the player and add effects
        new BukkitRunnable() {
            int timeLeft = freezeDuration;

            @Override
            public void run() {
                if (timeLeft <= 0 || !player.isOnline()) {
                    this.cancel();
                    player.sendMessage("You are no longer frozen!");
                    frozenPlayers.put(playerUUID, false);
                    return;
                }

                // Keep the player at the same location
                player.teleport(freezeLocation);

                // Add particle effects
                player.getWorld().spawnParticle(
                        org.bukkit.Particle.SNOWFLAKE, // Particle type
                        freezeLocation.getX(),
                        freezeLocation.getY() + 1, // Slightly above the player's head
                        freezeLocation.getZ(),
                        10, // Number of particles
                        0.5, 0.5, 0.5, // Spread in X, Y, Z
                        0.1 // Speed of particles
                );

                // Play sound effect
                player.getWorld().playSound(
                        freezeLocation,
                        org.bukkit.Sound.BLOCK_GLASS_BREAK, // Sound type
                        1.0f, // Volume
                        1.0f // Pitch
                );

                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L); // Schedule every second
    }
}
