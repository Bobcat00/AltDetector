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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Config
{
    private AltDetector plugin;
    
    public Config(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    public long getExpirationTime()
    {
        return plugin.getConfig().getLong("expiration-time");
    }
    
    public long getSaveInterval()
    {
        return plugin.getConfig().getLong("save-interval");
    }
    
    public String getJoinPlayerPrefix()
    {
        return plugin.getConfig().getString("join-player-prefix");
    }
    
    public String getJoinPlayer()
    {
        return plugin.getConfig().getString("join-player");
    }
    
    public String getJoinPlayerList()
    {
        return plugin.getConfig().getString("join-player-list");
    }
    
    public String getJoinPlayerSeparator()
    {
        return plugin.getConfig().getString("join-player-separator");
    }
    
    public String getAltCmdPlayer()
    {
        return plugin.getConfig().getString("altcmd-player");
    }
    
    public String getAltCmdPlayerList()
    {
        return plugin.getConfig().getString("altcmd-player-list");
    }
    
    public String getAltCmdPlayerSeparator()
    {
        return plugin.getConfig().getString("altcmd-player-separator");
    }
    
    public String getAltCmdPlayerNoAlts()
    {
        return plugin.getConfig().getString("altcmd-playernoalts");
    }
    
    public String getAltCmdNoAlts()
    {
        return plugin.getConfig().getString("altcmd-noalts");
    }
    
    public String getAltCmdPlayerNotFound()
    {
        return plugin.getConfig().getString("altcmd-playernotfound");
    }
    
    public String getAltCmdParamError()
    {
        return plugin.getConfig().getString("altcmd-paramerror");
    }
    
    public String getAltCmdNoPerm()
    {
        return plugin.getConfig().getString("altcmd-noperm");
    }
    
    public String getDelCmdRemovedSingular()
    {
        return plugin.getConfig().getString("delcmd-removedsingular");
    }
    
    public String getDelCmdRemovedPlural()
    {
        return plugin.getConfig().getString("delcmd-removedplural");
    }
    
    //--------------------------------------------------------------------------
    
    // Update the config file with new fields.
    
    private boolean contains(String path, boolean ignoreDefault)
    {
        // This duplicates the method added in 1.9, Bukkit commit facc9c353c3
        return ((ignoreDefault) ? plugin.getConfig().get(path, null) : plugin.getConfig().get(path)) != null;
    }
    
    public void updateConfig()
    {
        if (!contains("expiration-time", true))
        {
            if (contains("expirationtime", true))
            {
                plugin.getConfig().set("expiration-time", plugin.getConfig().getLong("expirationtime"));
            }
            else
            {
                plugin.getConfig().set("expiration-time", 60L);
            }
        }
        
        if (!contains("save-interval", true))
        {
            if (contains("saveinterval", true))
            {
                plugin.getConfig().set("save-interval", plugin.getConfig().getLong("saveinterval"));
            }
            else
            {
                plugin.getConfig().set("save-interval", 1L);
            }
        }
        
        if (!contains("join-player-prefix", true))
        {
            plugin.getConfig().set("join-player-prefix", "&b[AltDetector] ");
        }
        
        if (!contains("join-player", true))
        {
            plugin.getConfig().set("join-player", "{0} may be an alt of ");
        }
        
        if (!contains("join-player-list", true))
        {
            plugin.getConfig().set("join-player-list", "{0}");
        }
        
        if (!contains("join-player-separator", true))
        {
            plugin.getConfig().set("join-player-separator", ", ");
        }
        
        if (!contains("altcmd-player", true))
        {
            plugin.getConfig().set("altcmd-player", "&c{0}&6 may be an alt of ");
        }
        
        if (!contains("altcmd-player-list", true))
        {
            plugin.getConfig().set("altcmd-player-list", "&c{0}");
        }
        
        if (!contains("altcmd-player-separator", true))
        {
            plugin.getConfig().set("altcmd-player-separator", "&6, ");
        }
        
        if (!contains("altcmd-playernoalts", true))
        {
            plugin.getConfig().set("altcmd-playernoalts", "&c{0}&6 has no known alts");
        }
        
        if (!contains("altcmd-noalts", true))
        {
            plugin.getConfig().set("altcmd-noalts", "&6No alts found");
        }
        
        if (!contains("altcmd-playernotfound", true))
        {
            plugin.getConfig().set("altcmd-playernotfound", "&4{0} not found");
        }
        
        if (!contains("altcmd-paramerror", true))
        {
            plugin.getConfig().set("altcmd-paramerror", "&4Must specify at most one player");
        }
        
        if (!contains("altcmd-noperm", true))
        {
            plugin.getConfig().set("altcmd-noperm", "&4You do not have permission for this command");
        }
        
        if (!contains("delcmd-removedsingular", true))
        {
            plugin.getConfig().set("delcmd-removedsingular", "&6{0} record removed");
        }
        
        if (!contains("delcmd-removedplural", true))
        {
            plugin.getConfig().set("delcmd-removedplural", "&6{0} records removed");
        }
        
        saveConfig();
    }
    
    //--------------------------------------------------------------------------
    
    // Save config to disk with embedded comments. Any change to the config file
    // format must also be changed here. Newlines are written as \n so they will
    // be the same on all platforms.
    
    public void saveConfig()
    {
        try
        {
            File outFile = new File(plugin.getDataFolder(), "config.yml");
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile.getAbsolutePath()));
            
            writer.write("# Data expiration time in days"                                    + "\n");
            writer.write("expiration-time: " + plugin.getConfig().getLong("expiration-time") + "\n");
            writer.write("\n");
            
            writer.write("# Save interval in minutes (0 for immediate)"                      + "\n");
            writer.write("save-interval: "   + plugin.getConfig().getLong("save-interval")   + "\n");
            writer.write("\n");
            
            writer.write("# Messages when player joins the server"                                                                                + "\n");
            writer.write("join-player-prefix: \""      + plugin.getConfig().getString("join-player-prefix").replaceAll("\n", "\\\\n")      + "\"" + "\n");
            writer.write("join-player: \""             + plugin.getConfig().getString("join-player").replaceAll("\n", "\\\\n")             + "\"" + "\n");
            writer.write("join-player-list: \""        + plugin.getConfig().getString("join-player-list").replaceAll("\n", "\\\\n")        + "\"" + "\n");
            writer.write("join-player-separator: \""   + plugin.getConfig().getString("join-player-separator").replaceAll("\n", "\\\\n")   + "\"" + "\n");
            writer.write("\n");

            writer.write("# Messages for alt command"                                                                                             + "\n");
            writer.write("altcmd-player: \""           + plugin.getConfig().getString("altcmd-player").replaceAll("\n", "\\\\n")           + "\"" + "\n");
            writer.write("altcmd-player-list: \""      + plugin.getConfig().getString("altcmd-player-list").replaceAll("\n", "\\\\n")      + "\"" + "\n");
            writer.write("altcmd-player-separator: \"" + plugin.getConfig().getString("altcmd-player-separator").replaceAll("\n", "\\\\n") + "\"" + "\n");
            writer.write("\n");
            
            writer.write("altcmd-playernoalts: \""     + plugin.getConfig().getString("altcmd-playernoalts").replaceAll("\n", "\\\\n")     + "\"" + "\n");
            writer.write("altcmd-noalts: \""           + plugin.getConfig().getString("altcmd-noalts").replaceAll("\n", "\\\\n")           + "\"" + "\n");
            writer.write("altcmd-playernotfound: \""   + plugin.getConfig().getString("altcmd-playernotfound").replaceAll("\n", "\\\\n")   + "\"" + "\n");
            writer.write("altcmd-paramerror: \""       + plugin.getConfig().getString("altcmd-paramerror").replaceAll("\n", "\\\\n")       + "\"" + "\n");
            writer.write("altcmd-noperm: \""           + plugin.getConfig().getString("altcmd-noperm").replaceAll("\n", "\\\\n")           + "\"" + "\n");
            writer.write("\n");
            
            writer.write("#Messages for alt delete command"                                                                                       + "\n");
            writer.write("delcmd-removedsingular: \""  + plugin.getConfig().getString("delcmd-removedsingular").replaceAll("\n", "\\\\n")  + "\"" + "\n");
            writer.write("delcmd-removedplural: \""    + plugin.getConfig().getString("delcmd-removedplural").replaceAll("\n", "\\\\n")    + "\"" + "\n");
            
            writer.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //plugin.getLogger().info("Exception creating config file.");
        }
        
        plugin.reloadConfig();
    }

}
