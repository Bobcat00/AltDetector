// AltDetector - Detects possible alt accounts
// Copyright 2018 Bobcat00
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bobcat00.altdetector;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Listeners implements Listener
{
    private AltDetector plugin;
    
    // Constructor
    
    public Listeners(AltDetector plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    // -------------------------------------------------------------------------
    
    // Callback method used for returning alt string to main thread
    
    private interface Callback<T>
    {
        public void execute(T response);
    }
    
    // -------------------------------------------------------------------------
    
    // Update database with player and IP data, and returns the alts of the
    // player via a callback. If there are no alts, the callback is not called.
    
    private void updateDatabaseGetAlts(final String ip,
                                       final String uuid,
                                       final String name,
                                       final Callback<String> callback)
    {
        final String joinPlayer          = plugin.config.getJoinPlayer();
        final String joinPlayerList      = plugin.config.getJoinPlayerList();
        final String joinPlayerSeparator = plugin.config.getJoinPlayerSeparator();
        
        // Go to async thread
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                // 1. Update playertable
                
                String playerName = plugin.database.getNameFromPlayertable(uuid);
                
                if (playerName.equals(""))
                {
                    // Not found, add to playertable
                    plugin.database.addPlayertableEntry(name, uuid);
                }
                else if (!playerName.equals(name))
                {
                    // Player changed name, update playertable
                    plugin.database.updateNameInPlayertable(name, uuid);
                }
                
                // 2. Update iptable
                
                boolean ipEntryExists = plugin.database.checkIptableEntry(ip, uuid);
                
                if (!ipEntryExists)
                {
                    // Add to iptable
                    plugin.database.addIptableEntry(ip, uuid);
                }
                else
                {
                    // Update date in iptable
                    plugin.database.updateIptableEntry(ip, uuid);
                }
                
                // 3. Get possible alts
                
                String altString = plugin.database.getFormattedAltString(name,
                                                                         ip,
                                                                         uuid,
                                                                         joinPlayer,
                                                                         joinPlayerList,
                                                                         joinPlayerSeparator,
                                                                         plugin.expirationTime);
                
                if (altString != null)
                {
                    // Go back to the main thread
                    Bukkit.getScheduler().runTask(plugin, new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Call the callback with the result
                            callback.execute(altString);
                        }
                    });
                }
            
            }
            
        }
        );
    
    }
    
    // -------------------------------------------------------------------------
    
    // This is the listener for the Player Join Event. It calls a method to
    // update the database asynchronously and has a callback to output the
    // String listing player's alts. 
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        // Skip if player is exempt
        if (player.hasPermission("altdetector.exempt"))
        {
            return;
        }
        
        // Get info about this player
        final String ip = player.getAddress().getAddress().getHostAddress().toLowerCase(Locale.ROOT).split("%")[0];
        final String uuid = player.getUniqueId().toString();
        final String name = player.getName();
        
        // Add to the database - async (mostly)
        updateDatabaseGetAlts(ip, uuid, name, new Callback<String>()
        {
            // Process alt string - main thread
            @Override
            public void execute(String altString)
            {
                // Output to log file without color codes
                plugin.getLogger().info(altString.replaceAll("&[0123456789AaBbCcDdEeFfKkLlMmNnOoRr]", ""));

                // Output including prefix to players with altdetector.notify
                String notifyString = ChatColor.translateAlternateColorCodes('&', plugin.config.getJoinPlayerPrefix() + altString);

                for (Player p : plugin.getServer().getOnlinePlayers())
                {
                    if (p.hasPermission("altdetector.notify"))
                    {
                        p.sendMessage(notifyString);
                    }
                }
            }
        }
        );
    }

}
