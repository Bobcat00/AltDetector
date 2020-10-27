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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bobcat00.altdetector.DataStore.PlayerDataType;

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
                // You do not have permission for this command
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getAltCmdNoPerm()));
                return true;
            }
            
            List<String> playerList = new ArrayList<String>();
            
            switch (args.length)
            {
            case 0:
                
                // Get a list of all the players
                for (Player player : plugin.getServer().getOnlinePlayers())
                {
                    if (!player.hasPermission("altdetector.exempt"))
                    {
                        playerList.add(player.getName());
                    }
                }
                Collections.sort(playerList, String.CASE_INSENSITIVE_ORDER);
                break;

            case 1:
                
                // Get the player
                @SuppressWarnings("deprecation")
                Player player = Bukkit.getServer().getPlayerExact(args[0]);
                // Make sure the player is online
                if (player == null)
                {
                    handleOfflinePlayer(sender, args[0]);
                    return true;
                }
                if (!player.hasPermission("altdetector.exempt"))
                {
                    playerList.add(player.getName());
                }
                break;
            
            case 2:
                
                // Delete player's records
                if (args[0].equalsIgnoreCase("delete"))
                {
                    if (sender instanceof Player && !sender.hasPermission("altdetector.alt.delete"))
                    {
                        // You do not have permission for this command
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getAltCmdNoPerm()));
                        return true;
                    }
                    else
                    {
                        int entriesRemoved = plugin.dataStore.purge(args[1]);
                        
                        if (entriesRemoved == 0)
                        {
                            // playerName not found
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getAltCmdPlayerNotFound(), args[1])));
                        }
                        else if (entriesRemoved == 1)
                        {
                            // 1 record removed
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getDelCmdRemovedSingular(), entriesRemoved)));
                        }
                        else
                        {
                            // n records removed
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getDelCmdRemovedPlural(), entriesRemoved)));
                        }
                    }
                }
                else
                {
                    // Must specify at most one player
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getAltCmdParamError()));
                }
                
                return true;
                
            default:
                
                // Must specify at most one player
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getAltCmdParamError()));
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
                
                altsFound |= outputAlts(sender, name, ip, uuid);
                
            } // end for each name
            
            if (!altsFound)
            {
                if (args.length == 0)
                {
                    // No alts found
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.config.getAltCmdNoAlts()));
                }
                else
                {
                    // args[0] has no known alts
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getAltCmdPlayerNoAlts(), args[0])));
                }
            }
            
            // Normal return
            return true;
        }
        
        return false;
    }
    
    // -------------------------------------------------------------------------
    
    // Lookup an offline player and report any alts found.
    
    private void handleOfflinePlayer(CommandSender sender, String playerName)
    {
        // Lookup player; return is IP address, UUID, and name (may be null)
        PlayerDataType playerData = plugin.dataStore.lookupOfflinePlayer(playerName);
        
        if (playerData == null)
        {
            // playerName not found
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getAltCmdPlayerNotFound(), playerName)));
        }
        else
        {
            boolean altsFound = outputAlts(sender, playerData.name, playerData.ip, playerData.uuid);
            
            if (!altsFound)
            {
                // playerData.name has no known alts
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', MessageFormat.format(plugin.config.getAltCmdPlayerNoAlts(), playerData.name)));
            }
        }
    }
    
    // -------------------------------------------------------------------------
    
    // Output the alts for the given player, if any. Returns true if alts were
    // found. This is used for both online and offline players.
    
    private boolean outputAlts(CommandSender sender, String name, String ip, String uuid)
    {
        // Get possible alts
        String altString = plugin.dataStore.getFormattedAltString(name,
                                                                  ip,
                                                                  uuid,
                                                                  plugin.config.getAltCmdPlayer(),
                                                                  plugin.config.getAltCmdPlayerList(),
                                                                  plugin.config.getAltCmdPlayerSeparator());
        
        if (altString != null)
        {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', altString));
            return true;
        }
        
        return false;
    }
}
