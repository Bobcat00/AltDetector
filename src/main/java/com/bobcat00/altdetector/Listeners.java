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

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class Listeners implements Listener
{
    private AltDetector plugin;
    
    public Listeners(AltDetector plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
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
        String ip = player.getAddress().getAddress().getHostAddress();
        String uuid = player.getUniqueId().toString();
        String name = player.getName();
        
        // Add to the database
        plugin.dataStore.addUpdateIp(ip, uuid, name);
        if (plugin.saveInterval == 0)
        {
            plugin.dataStore.saveIpDataConfig();
        }
        
        // Get possible alts
        String altString = plugin.dataStore.getFormattedAltString(name,
                                                                  ip,
                                                                  uuid,
                                                                  plugin.config.getJoinPlayer(),
                                                                  plugin.config.getJoinPlayerList(),
                                                                  plugin.config.getJoinPlayerSeparator());
        
        if (altString != null)
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

}
