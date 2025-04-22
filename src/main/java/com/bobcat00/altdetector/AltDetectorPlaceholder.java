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
        // Create a future to get the result from async operation
        CompletableFuture<String> future = new CompletableFuture<>();
        
        // Run database operation async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String uuid = findPlayerUuid(playerName, future);
            if (uuid == null) {
                return; // Future already completed with error message
            }
            
            processAltList(playerName, uuid, future);
        });
        
        try {
            // Wait for the future to complete and get the result
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Properly re-interrupt
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
    
    /**
     * Process the alt list for a player
     * 
     * @param playerName The player's name
     * @param uuid The player's UUID
     * @param future The future to complete with result
     */
    private void processAltList(String playerName, String uuid, CompletableFuture<String> future) {
        // Get formatted alt string from database
        String altCmdPlayer = plugin.config.getAltCmdPlayer();
        String altCmdPlayerList = plugin.config.getAltCmdPlayerList();
        String altCmdPlayerSeparator = plugin.config.getAltCmdPlayerSeparator();
        
        String altString = plugin.database.getFormattedAltString(
            playerName, 
            uuid, 
            altCmdPlayer, 
            altCmdPlayerList, 
            altCmdPlayerSeparator, 
            plugin.expirationTime
        );
        
        if (altString == null || altString.isEmpty()) {
            future.complete("None");
            return;
        }
        
        // Extract just the names from the formatted string
        // The pattern is typically: "{0} may be an alt of name1, name2, name3"
        int startIndex = altString.indexOf("alt of ");
        if (startIndex != -1) {
            startIndex += 7; // Length of "alt of "
            String altNames = altString.substring(startIndex).trim();
            
            // Remove any color codes
            altNames = altNames.replaceAll("&[0-9a-fA-FkKlLmMnNoOrR]", "");
            
            future.complete(altNames);
        } else {
            // Fallback in case format changes
            future.complete(altString.replaceAll("&[0-9a-fA-FkKlLmMnNoOrR]", ""));
        }
    }
}
