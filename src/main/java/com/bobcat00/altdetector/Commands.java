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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor
{
    private AltDetector plugin;
    
    public Commands(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (cmd.getName().equalsIgnoreCase("alt"))
        {
            if (sender instanceof Player && !sender.hasPermission("altdetector.alt"))
            {
                sender.sendMessage("You do not have permission for this command");
                return true;
            }
            
            List<String> playerList = new ArrayList<String>();
            
            if (args.length == 0)
            {
                // Get a list of all the players
                for (Player player : plugin.getServer().getOnlinePlayers())
                {
                    playerList.add(player.getName());
                }
                Collections.sort(playerList, String.CASE_INSENSITIVE_ORDER);
            }
            else if (args.length == 1)
            {
                // Get the player
                @SuppressWarnings("deprecation")
                Player player = Bukkit.getServer().getPlayerExact(args[0]);
                // Make sure the player is online
                if (player == null)
                {
                    sender.sendMessage(ChatColor.DARK_RED + args[0] + " is not currently online");
                    return true;
                }
                playerList.add(player.getName());
            }
            else
            {
                sender.sendMessage(ChatColor.DARK_RED + "Must specify at most one player");
                return true;
            }
            
            boolean altsFound = false;
            
            // Loop through the list of players
            for (String name : playerList)
            {
                // Get player's IP address and uuid
                @SuppressWarnings("deprecation")
                Player player = Bukkit.getServer().getPlayerExact(name);
                String ip = player.getAddress().getAddress().getHostAddress();
                String uuid = player.getUniqueId().toString();
                
                // Get possible alts
                List<String> altList = plugin.dataStore.getAltNames(ip, uuid);
                
                if (!altList.isEmpty())
                {
                    StringBuilder sb = new StringBuilder(ChatColor.RED + name + ChatColor.GOLD + " may be an alt of ");
                    for (String altName : altList)
                    {
                        sb.append(ChatColor.RED + altName + ChatColor.GOLD + ", ");
                    }
                    sender.sendMessage(sb.toString().substring(0, sb.length()-4)); // Account for ChatColor
                    altsFound = true;
                }
                
            } // end for each name
            
            if (!altsFound)
            {
                if (args.length == 0)
                {
                    sender.sendMessage(ChatColor.GOLD + "No alts found");
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + args[0] + ChatColor.GOLD + " has no known alts");
                }
            }
            
            // Normal return
            return true;
        }
        
        return false;
    }
    
}