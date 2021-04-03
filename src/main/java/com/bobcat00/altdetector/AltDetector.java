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
import java.util.List;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import com.bobcat00.altdetector.datastore.Datastore;
import com.bobcat00.altdetector.datastore.YamlData;

public class AltDetector extends JavaPlugin
{
    public long expirationTime = 60;
    public long saveInterval = 1;
    Config config;
    Datastore dataStore;
    Listeners listeners;
    
    @Override
    public void onEnable()
    {
        // Config
        
        config = new Config(this);
        
        saveDefaultConfig();
        // Update old config file
        config.updateConfig();
        
        expirationTime = config.getExpirationTime();
        saveInterval = config.getSaveInterval();
        if (saveInterval > 0)
        {
            getLogger().info("Database save interval " + saveInterval + " minute" + (saveInterval == 1 ? "." : "s."));
        }
        else
        {
            saveInterval = 0;
            getLogger().info("Saving to database as each player connects.");
        }
        
        // DataStore
        
        dataStore = new YamlData(this); // For YAML file
        
        dataStore.saveDefaultConfig();
        dataStore.reloadIpDataConfig();
        
        int entriesRemoved = dataStore.purge(""); // purged based on date
        dataStore.saveIpDataConfig();
        dataStore.generatePlayerList();
        
        getLogger().info(entriesRemoved + " record" + (entriesRemoved == 1 ? "" : "s") + " removed, expiration time " + expirationTime + " days.");
        
        // Listeners
        
        listeners = new Listeners(this);
        
        // Commands
        
        this.getCommand("alt").setExecutor(new Commands(this));
        this.getCommand("alt").setTabCompleter(new TabComplete(this));
        
        // Metrics
        
        int pluginId = 4862;
        Metrics metrics = new Metrics(this, pluginId);
        if (metrics.isEnabled())
        {
            String option = "Invalid";
            if (expirationTime < 0)
                option = "Invalid";
            else if (expirationTime == 0)
                option = "0";
            else if (expirationTime <= 30)
                option = "1-30";
            else if (expirationTime <= 60)
                option = "31-60";
            else if (expirationTime <= 90)
                option = "61-90";
            else if (expirationTime > 90)
                option = ">90";
            final String setting = option;
            metrics.addCustomChart(new Metrics.SimplePie("expiration_time", () -> setting));
            
            option = "Invalid";
            if (saveInterval < 0)
                option = "Invalid";
            else if (saveInterval == 0)
                option = "0";
            else if (saveInterval <= 5)
                option = "1-5";
            else if (saveInterval <= 10)
                option = "6-10";
            else if (saveInterval > 10)
                option = ">10";
            final String setting2 = option;
            metrics.addCustomChart(new Metrics.SimplePie("save_interval", () -> setting2));
            
            getLogger().info("Enabled metrics. You may opt-out by changing plugins/bStats/config.yml");
        }
    }
    
    // -------------------------------------------------------------------------
    
    @Override
    public void onDisable()
    {
        if (saveInterval > 0)
        {
            dataStore.saveIpDataConfig();
        }
    }

    // -------------------------------------------------------------------------
    
    // Look up the alts for a player, and return a formatted string using the &
    // color codes. If no alts are found, the returned string is null.
    
    protected String getFormattedAltString(String name,
                                           String ip,
                                           String uuid,
                                           String playerFormat,
                                           String playerListFormat,
                                           String playerSeparator)
    {
        // Get possible alts
        List<String> altList = dataStore.getAltNames(ip, uuid);
        
        if (!altList.isEmpty())
        {
            // name may be an alt of
            StringBuilder sb = new StringBuilder(MessageFormat.format(playerFormat, name));
            
            boolean outputComma = false;
            
            for (String altName : altList)
            {
                // comma
                if (outputComma)
                {
                    sb.append(playerSeparator);
                }
                outputComma = true;
                
                // altName
                sb.append(MessageFormat.format(playerListFormat, altName));
            }
            return sb.toString();
        }
        
        return null; // No alts found
    }

}
