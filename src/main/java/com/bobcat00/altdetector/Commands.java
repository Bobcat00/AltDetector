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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.bobcat00.altdetector.database.Database.PlayerDataType;

public class Commands implements CommandExecutor
{
    private AltDetector plugin;
    
    // Constructor
    
    public Commands(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    // -------------------------------------------------------------------------
    
    // This processes the /alt command. After parsing the command and collecting
    // data about the player(s) involved, it uses an async task to do the
    // database work. Unlike the Player Join Event listener, which can output a
    // string to all players on the server, this method only outputs to the
    // command sender. It doesn't use a callback, but simply calls a method to
    // output the string(s) to the command sender on the main thread.
    
    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args)
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
                        final String altCmdPlayerNotFound  = plugin.config.getAltCmdPlayerNotFound();
                        final String delCmdRemovedSingular = plugin.config.getDelCmdRemovedSingular();
                        final String delCmdRemovedPlural   = plugin.config.getDelCmdRemovedPlural();
                        
                        // Go to async thread
                        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                int entriesRemoved = plugin.database.purge(args[1]);

                                if (entriesRemoved == 0)
                                {
                                    // playerName not found
                                    sendMessageSync(sender, MessageFormat.format(altCmdPlayerNotFound, args[1]));
                                }
                                else if (entriesRemoved == 1)
                                {
                                    // 1 record removed
                                    sendMessageSync(sender, MessageFormat.format(delCmdRemovedSingular, entriesRemoved));
                                }
                                else
                                {
                                    // n records removed
                                    sendMessageSync(sender, MessageFormat.format(delCmdRemovedPlural, entriesRemoved));
                                }
                            }
                        });
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
            
            // Lookup alt(s) and output to sender
            
            // Get ip and uuid for each player while on main thread
            List<PlayerDataType> playerDataList = new ArrayList<>();
            for (String name : playerList)
            {
                PlayerDataType playerData = plugin.database.new PlayerDataType();
                @SuppressWarnings("deprecation")
                Player player = Bukkit.getServer().getPlayerExact(name);
                playerData.ip = player.getAddress().getAddress().getHostAddress().toLowerCase(Locale.ROOT).split("%")[0];
                playerData.uuid = player.getUniqueId().toString();
                playerData.name = name;
                playerDataList.add(playerData);
            }
            
            final String altCmdPlayer          = plugin.config.getAltCmdPlayer();
            final String altCmdPlayerList      = plugin.config.getAltCmdPlayerList();
            final String altCmdPlayerSeparator = plugin.config.getAltCmdPlayerSeparator();
            final String altCmdNoAlts          = plugin.config.getAltCmdNoAlts();
            final String altCmdPlayerNoAlts    = plugin.config.getAltCmdPlayerNoAlts();
            
            // Go to async thread
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
            {
                @Override
                public void run()
                {
                    List<String> altStrings = new ArrayList<String>();
                    // Loop through the list of players
                    for (PlayerDataType playerData : playerDataList)
                    {
                        String altString = plugin.database.getFormattedAltString(playerData.name,
                                                                                 playerData.ip,
                                                                                 playerData.uuid,
                                                                                 altCmdPlayer,
                                                                                 altCmdPlayerList,
                                                                                 altCmdPlayerSeparator,
                                                                                 plugin.expirationTime);
                        if (altString != null)
                        {
                            altStrings.add(altString);
                        }
                    }
                    if (!altStrings.isEmpty())
                    {
                        sendMessageSync(sender, altStrings);
                    }
                    else
                    {
                        if (args.length == 0)
                        {
                            // No alts found
                            sendMessageSync(sender, altCmdNoAlts);
                        }
                        else
                        {
                            // args[0] has no known alts
                            sendMessageSync(sender, MessageFormat.format(altCmdPlayerNoAlts, args[0]));
                        }
                    }
                }
            });
            
            // Normal return
            return true;
        }
        
        return false;
    }
    
    // -------------------------------------------------------------------------
    
    // Lookup an offline player and report any alts found. Like onCommand, the
    // database work is done in an async thread and a method is called to output
    // the resulting string to the command sender on the main thread.
    
    private void handleOfflinePlayer(final CommandSender sender, final String playerName)
    {
        final String altCmdPlayer          = plugin.config.getAltCmdPlayer();
        final String altCmdPlayerList      = plugin.config.getAltCmdPlayerList();
        final String altCmdPlayerSeparator = plugin.config.getAltCmdPlayerSeparator();
        final String altCmdPlayerNotFound  = plugin.config.getAltCmdPlayerNotFound();
        final String altCmdPlayerNoAlts    = plugin.config.getAltCmdPlayerNoAlts();
        
        // Go to async thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                // Lookup player; return is IP address, UUID, and name (may be null)
                PlayerDataType playerData = plugin.database.lookupOfflinePlayer(playerName);

                if (playerData == null)
                {
                    // playerName not found
                    sendMessageSync(sender, MessageFormat.format(altCmdPlayerNotFound, playerName));
                }
                else
                {
                    String altString = plugin.database.getFormattedAltString(playerData.name,
                                                                             playerData.ip,
                                                                             playerData.uuid,
                                                                             altCmdPlayer,
                                                                             altCmdPlayerList,
                                                                             altCmdPlayerSeparator,
                                                                             plugin.expirationTime);
                    if (altString != null)
                    {
                        sendMessageSync(sender, altString);
                    }
                    else
                    {
                        // playerData.name has no known alts
                        sendMessageSync(sender, MessageFormat.format(altCmdPlayerNoAlts, playerData.name));
                    }
                }
            }
        });
    }
    
    // -------------------------------------------------------------------------
    
    // Switch to the main thread and send a message
    
    private void sendMessageSync(final CommandSender sender, final String message)
    {
        sendMessageSync(sender, new ArrayList<>(Arrays.asList(message)));
    }
    
    // -------------------------------------------------------------------------
    
    // Switch to the main thread and send a list of messages
    
    private void sendMessageSync(final CommandSender sender, final List<String> messages)
    {
        // go back to the main thread
        Bukkit.getScheduler().runTask(plugin, new Runnable()
        {
            @Override
            public void run() {
                for (String message : messages)
                {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                }
            }
        });
    }
    
}
