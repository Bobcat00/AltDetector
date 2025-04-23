// AltDetector - Detects possible alt accounts
// Copyright 2023
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bobcat00.altdetector;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.bobcat00.altdetector.database.Database.PlayerDataType;

/**
 * PlaceholderAPI integration for AltDetector
 */
public class Placeholder extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    
    private final AltDetector plugin;
    private static final Pattern ALT_PATTERN = Pattern.compile("alts_(.+)");
    
    /**
     * Constructor for the placeholder
     * 
     * @param plugin The AltDetector plugin instance
     */
    public Placeholder(AltDetector plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "altdetector";
    }
    
    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Keep the expansion loaded on PlaceholderAPI reload
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        // Check for alts_{PlayerName} pattern
        Matcher matcher = ALT_PATTERN.matcher(identifier);
        if (matcher.matches()) {
            String targetPlayerName = matcher.group(1);
            return getAltsForPlayer(targetPlayerName);
        }
        
        return null; // Placeholder is not recognized
    }
    
    /**
     * Get alt accounts for the specified player
     * 
     * @param playerName The name of the player to check
     * @return A comma-separated list of alt accounts, or "None" if no alts found
     */
    private String getAltsForPlayer(String playerName) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuid = findPlayerUuid(playerName, future);
            if (uuid == null) {
                return;
            }

            List<String> altNames = plugin.database.getAltNames(uuid, uuid, plugin.expirationTime);
            if (altNames.isEmpty()) {
                future.complete("None");
            } else {
                future.complete(String.join(", ", altNames));
            }
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().warning("Interrupted while getting alt list for placeholder: " + e.getMessage());
            return "Error";
        } catch (ExecutionException e) {
            plugin.getLogger().warning("Error getting alt list for placeholder: " + e.getMessage());
            return "Error";
        }
    }
    
    /**
     * Find a player's UUID by name
     * 
     * @param playerName The player name to search for
     * @param future The future to complete if player not found
     * @return The player's UUID as a string, or null if not found
     */
    private String findPlayerUuid(String playerName, CompletableFuture<String> future) {
        // Try to find from online players first (using non-deprecated method)
        Player onlinePlayer = Bukkit.getServer().getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId().toString();
        }
        
        // Try to find from database
        PlayerDataType playerData = plugin.database.lookupOfflinePlayer(playerName);
        if (playerData == null || playerData.uuid == null || playerData.uuid.isEmpty()) {
            // Player not found
            future.complete("Player not found");
            return null;
        }
        
        return playerData.uuid;
    }
}
