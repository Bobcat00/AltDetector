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

import java.util.List;

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
        
        // Get list of UUIDs/names/dates for this IP address
        List<String> altList = plugin.dataStore.getAltNames(ip, uuid);
        
        if (!altList.isEmpty())
        {
            StringBuilder sb = new StringBuilder(name + " may be an alt of ");
            for (String altName : altList)
            {
                sb.append(altName).append(", ");
            }
            plugin.getLogger().info(sb.toString().substring(0, sb.length()-2));
            
            for (Player p : plugin.getServer().getOnlinePlayers())
            {
                if (p.hasPermission("altdetector.notify"))
                {
                    p.sendMessage(ChatColor.AQUA + "[AltDetector] " + sb.toString().substring(0, sb.length()-2));
                }
            }

        }
        
    }

}
