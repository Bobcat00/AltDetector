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
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class Config
{
    private AltDetector plugin;
    
    // Constants for configuration keys
    private static final String DISCORD_ENABLED = "discord.enabled";
    private static final String DISCORD_WEBHOOK_URL = "discord.webhook-url";
    private static final String DISCORD_USERNAME = "discord.username";
    private static final String DISCORD_AVATAR_URL = "discord.avatar-url";
    private static final String DISCORD_EMBED_COLOR = "discord.embed-color";
    private static final String DISCORD_NOTIFY_NO_ALTS = "discord.notify-no-alts";
    
    // Constants for config writing
    private static final String ALTCMD_PLAYERNOTFOUND_FORMAT = "altcmd-playernotfound: \"";
    private static final String ALTCMD_PARAMERROR_FORMAT = "altcmd-paramerror: \"";
    private static final String DELCMD_REMOVEDSINGULAR_FORMAT = "delcmd-removedsingular: \"";
    private static final String DELCMD_REMOVEDPLURAL_FORMAT = "delcmd-removedplural: \"";
    
    public Config(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    public int getExpirationTime()
    {
        return plugin.getConfig().getInt("expiration-time");
    }
    
    public String getDatabaseType()
    {
        return plugin.getConfig().getString("database-type");
    }
    
    public String getMysqlHostname()
    {
        return plugin.getConfig().getString("mysql.hostname");
    }
    
    public String getMysqlUsername()
    {
        return plugin.getConfig().getString("mysql.username");
    }
    
    public String getMysqlPassword()
    {
        return plugin.getConfig().getString("mysql.password");
    }
    
    public String getMysqlDatabase()
    {
        return plugin.getConfig().getString("mysql.database");
    }
    
    public String getMysqlPrefix()
    {
        return plugin.getConfig().getString("mysql.prefix");
    }
    
    public int getMysqlPort()
    {
        return plugin.getConfig().getInt("mysql.port");
    }
    
    public String getJdbcurlProperties()
    {
        return plugin.getConfig().getString("mysql.jdbcurl-properties");
    }
    
    enum ConvertFromType
    {
        NONE,
        YML,
        SQLITE,
        MYSQL,
        ERROR
    }
    
    public ConvertFromType getConvertFrom()
    {
        String cf = plugin.getConfig().getString("convert-from");
        if (cf.equalsIgnoreCase("none"))
        {
            return ConvertFromType.NONE;
        }
        if (cf.equalsIgnoreCase("yml") || cf.equalsIgnoreCase("yaml"))
        {
            return ConvertFromType.YML;
        }
        if (cf.equalsIgnoreCase("sqlite"))
        {
            return ConvertFromType.SQLITE;
        }
        if (cf.equalsIgnoreCase("mysql"))
        {
            return ConvertFromType.MYSQL;
        }
        return ConvertFromType.ERROR;
    }
    
    public boolean getSqlDebug()
    {
        return plugin.getConfig().getBoolean("sql-debug");
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
    
    public boolean isDiscordEnabled()
    {
        return plugin.getConfig().getBoolean(DISCORD_ENABLED);
    }
    
    public String getDiscordWebhookUrl()
    {
        return plugin.getConfig().getString(DISCORD_WEBHOOK_URL);
    }
    
    public String getDiscordUsername()
    {
        return plugin.getConfig().getString(DISCORD_USERNAME);
    }
    
    public String getDiscordAvatarUrl()
    {
        return plugin.getConfig().getString(DISCORD_AVATAR_URL);
    }
    
    public int getDiscordEmbedColor()
    {
        String colorStr = plugin.getConfig().getString(DISCORD_EMBED_COLOR);
        if (colorStr != null && colorStr.startsWith("#")) {
            try {
                // Parse hex color code
                return Integer.parseInt(colorStr.substring(1), 16);
            } catch (NumberFormatException e) {
                // If parsing fails, return default red color
                return 16711680; // Default red color (decimal value of #FF0000)
            }
        }
        // If not a hex code, treat as a direct integer value
        return plugin.getConfig().getInt(DISCORD_EMBED_COLOR, 16711680);
    }
    
    public boolean getDiscordNotifyNoAlts()
    {
        return plugin.getConfig().getBoolean(DISCORD_NOTIFY_NO_ALTS);
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
        
        if (!contains("database-type", true))
        {
            plugin.getConfig().set("database-type",            "sqlite");
            plugin.getConfig().set("mysql.hostanme",           "127.0.0.1");
            plugin.getConfig().set("mysql.username",           "username");
            plugin.getConfig().set("mysql.password",           "password");
            plugin.getConfig().set("mysql.database",           "database");
            plugin.getConfig().set("mysql.prefix",             "altdetector_");
            plugin.getConfig().set("mysql.port",               3306);
            plugin.getConfig().set("mysql.jdbcurl-properties", "");
        }
        
        // Set to yml if not found. This indicates an old version is to be converted.
        if (!contains("convert-from", true))
        {
                plugin.getConfig().set("convert-from", "yml");
        }
        
        if (!contains("sql-debug", true))
        {
                plugin.getConfig().set("sql-debug", false);
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
            plugin.getConfig().set("delcmd-removedsingular", "&6Removed &c{0}&6 IP address");
        }
        
        if (!contains(DISCORD_ENABLED, true))
        {
            plugin.getConfig().set(DISCORD_ENABLED, false);
            plugin.getConfig().set(DISCORD_WEBHOOK_URL, "");
            plugin.getConfig().set(DISCORD_USERNAME, "AltDetector");
            plugin.getConfig().set(DISCORD_AVATAR_URL, "");
            plugin.getConfig().set(DISCORD_EMBED_COLOR, "#FF0000");
            plugin.getConfig().set(DISCORD_NOTIFY_NO_ALTS, false);
        }
        }
    
    // Save config to disk with embedded comments. Any change to the config file
    // format must also be changed here. Newlines are written as \n so they will
    // be the same on all platforms.
    
    public void saveConfig()
    {
        try
        {
            File outFile = new File(plugin.getDataFolder(), "config.yml");
            
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile.getAbsolutePath()), Charset.forName("UTF-8")));
            
            writer.write("# Data expiration time in days"                                    + "\n");
            writer.write("expiration-time: " + plugin.getConfig().getLong("expiration-time") + "\n");
            writer.write("\n");
            
            writer.write("# Database type sqlite, mysql"                                                            + "\n");
            writer.write("database-type: "         + plugin.getConfig().getString("database-type")                  + "\n");
            writer.write("mysql:"                                                                                   + "\n");
            writer.write("  hostname: "            + plugin.getConfig().getString("mysql.hostname")                 + "\n");
            writer.write("  username: "            + plugin.getConfig().getString("mysql.username")                 + "\n");
            writer.write("  password: "            + plugin.getConfig().getString("mysql.password")                 + "\n");
            writer.write("  database: "            + plugin.getConfig().getString("mysql.database")                 + "\n");
            writer.write("  prefix: "              + plugin.getConfig().getString("mysql.prefix")                   + "\n");
            writer.write("  port: "                + plugin.getConfig().getInt   ("mysql.port")                     + "\n");
            writer.write("  jdbcurl-properties: '" + plugin.getConfig().getString("mysql.jdbcurl-properties") + "'" + "\n");
            writer.write("# Messages when player joins the server"                                                                                + "\n");
            writer.write("join-player-prefix: \""      + plugin.getConfig().getString("join-player-prefix").replace("\n", "\\\\n")      + "\"" + "\n");
            writer.write("join-player: \""             + plugin.getConfig().getString("join-player").replace("\n", "\\\\n")             + "\"" + "\n");
            writer.write("join-player-list: \""        + plugin.getConfig().getString("join-player-list").replace("\n", "\\\\n")        + "\"" + "\n");
            writer.write("join-player-separator: \""   + plugin.getConfig().getString("join-player-separator").replace("\n", "\\\\n")   + "\"" + "\n");
            writer.write("\n");

            writer.write("# Messages for alt command"                                                                                             + "\n");
            writer.write("altcmd-player: \""           + plugin.getConfig().getString("altcmd-player").replace("\n", "\\\\n")           + "\"" + "\n");
            writer.write("altcmd-player-list: \""      + plugin.getConfig().getString("altcmd-player-list").replace("\n", "\\\\n")      + "\"" + "\n");
            writer.write("altcmd-player-separator: \"" + plugin.getConfig().getString("altcmd-player-separator").replace("\n", "\\\\n") + "\"" + "\n");
            writer.write("\n");
            
            writer.write("altcmd-playernoalts: \""     + plugin.getConfig().getString("altcmd-playernoalts").replace("\n", "\\\\n")     + "\"" + "\n");
            writer.write("#Messages for alt delete command"                                                                                       + "\n");
            writer.write(DELCMD_REMOVEDSINGULAR_FORMAT  + plugin.getConfig().getString("delcmd-removedsingular").replace("\n", "\\\\n")  + "\"" + "\n");
            writer.write(DELCMD_REMOVEDPLURAL_FORMAT    + plugin.getConfig().getString("delcmd-removedplural").replace("\n", "\\\\n")    + "\"" + "\n");
            writer.write(ALTCMD_PLAYERNOTFOUND_FORMAT   + plugin.getConfig().getString("altcmd-playernotfound").replace("\n", "\\\\n")   + "\"" + "\n");
            writer.write(ALTCMD_PARAMERROR_FORMAT       + plugin.getConfig().getString("altcmd-paramerror").replace("\n", "\\\\n")       + "\"" + "\n");
            writer.write("# Discord webhook integration"                                                     + "\n");
            writer.write("# Discord webhook integration"                                                     + "\n");
            writer.write(DELCMD_REMOVEDSINGULAR_FORMAT  + plugin.getConfig().getString("delcmd-removedsingular").replace("\n", "\\\\n")  + "\"" + "\n");
            writer.write(DELCMD_REMOVEDPLURAL_FORMAT    + plugin.getConfig().getString("delcmd-removedplural").replace("\n", "\\\\n")    + "\"" + "\n");
            writer.write(ALTCMD_PLAYERNOTFOUND_FORMAT   + plugin.getConfig().getString("altcmd-playernotfound").replace("\n", "\\\\n")   + "\"" + "\n");
            writer.write(ALTCMD_PARAMERROR_FORMAT       + plugin.getConfig().getString("altcmd-paramerror").replace("\n", "\\\\n")       + "\"" + "\n");
            writer.write("# Discord webhook integration"                                                     + "\n");
            writer.write("discord:"                                                                          + "\n");
            writer.write("  enabled: "           + plugin.getConfig().getBoolean(DISCORD_ENABLED)            + "\n");
            writer.write("  webhook-url: '"      + plugin.getConfig().getString(DISCORD_WEBHOOK_URL)        + "'" + "\n");
            writer.write("  username: '"         + plugin.getConfig().getString(DISCORD_USERNAME)           + "'" + "\n");
            writer.write("  avatar-url: '"       + plugin.getConfig().getString(DISCORD_AVATAR_URL)         + "'" + "\n");
            writer.write("  embed-color: '"      + plugin.getConfig().getString(DISCORD_EMBED_COLOR)        + "'" + "\n");
            writer.write("  # Set to true to include all alts, false to only notify when alts are detected"  + "\n");
            writer.write("  notify-no-alts: "    + plugin.getConfig().getBoolean(DISCORD_NOTIFY_NO_ALTS)    + "\n");
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            //plugin.getLogger().info("Exception creating config file.");
        }
        
        plugin.reloadConfig();
    }

}
