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
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TabComplete implements TabCompleter
{
    private AltDetector plugin;
    
    // Constructor
    
    public TabComplete(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    // -------------------------------------------------------------------------
    
    // Check permission if the sender is a player, otherwise return true
    
    private boolean hasPermission(CommandSender sender, String permission)
    {
        if (!(sender instanceof Player))
        {
            return true;
        }
        else
        {
            return sender.hasPermission(permission);
        }
    }
    
    // -------------------------------------------------------------------------
    
    // Return list elements starting with str, case insensitive comparison. This
    // is used instead of a stream filter in order to get a proper comparison.
    
    private List<String> filterList(List<String> list, String str)
    {
        List<String> al = new ArrayList<String>();
        
        for (String name : list)
        {
            if (str.length() == 0 || ((name.length() >= str.length()) && name.substring(0,str.length()).equalsIgnoreCase(str)))
            {
                al.add(name);
            }
        }
        
        return al;
    }
    
    // -------------------------------------------------------------------------
    
    // Tab Complete listener 
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
    {
        if (cmd.getName().equalsIgnoreCase("alt"))
        {
            //plugin.getLogger().info(args.length + ": " + Arrays.toString(args));

            // Commands are:
            // alt [player]
            // alt delete <player>
            
            List<String> argList = new ArrayList<>();
            
            if (args.length == 1)
            {
                if (hasPermission(sender, "altdetector.alt.delete"))
                {
                    argList.add("delete"); // unfortunately, Brigadier changes the order
                }
                if (hasPermission(sender, "altdetector.alt"))
                {
                    argList.addAll(plugin.database.getPlayerList());
                }
                return filterList(argList, args[0]);
            }
            
            if (args.length == 2 && args[0].equals("delete") && hasPermission(sender, "altdetector.alt.delete"))
            {
                argList.addAll(plugin.database.getPlayerList());
                return filterList(argList, args[1]);
            }
            
            return argList; // returns an empty list
            
         }
        
        return null; // default return
    }

}
