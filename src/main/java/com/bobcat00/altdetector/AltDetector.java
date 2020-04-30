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

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class AltDetector extends JavaPlugin
{
    long expirationTime = 60;
    long saveInterval = 1;
    Config config;
    DataStore dataStore;
    Listeners listeners;
    
    @Override
    public void onEnable()
    {
        // Config
        
        config = new Config(this);
        
        saveDefaultConfig();
        // Update old config file
        if (!getConfig().contains("saveinterval", true))
        {
            getConfig().set("saveinterval", 1L);
        }
        getConfig().options().header(" Data expiration time in days; Save interval in minutes (0 for immediate)");
        saveConfig();
        
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
        
        dataStore = new DataStore(this);
        
        dataStore.saveDefaultConfig();
        dataStore.reloadIpDataConfig();
        
        int entriesRemoved = dataStore.purge();
        dataStore.saveIpDataConfig();
        
        getLogger().info(entriesRemoved + " record" + (entriesRemoved == 1 ? "" : "s") + " removed, expiration time " + expirationTime + " days.");
        
        // Listeners
        
        listeners = new Listeners(this);
        
        // Commands
        
        this.getCommand("alt").setExecutor(new Commands(this));
        
        // Metrics
        
        int pluginId = 4862;
        Metrics metrics = new Metrics(this, pluginId);
        if (metrics.isEnabled())
        {
            String option = "Invalid";
            if (expirationTime == 0)
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
            if (saveInterval == 0)
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
    
    @Override
    public void onDisable()
    {
        if (saveInterval > 0)
        {
            dataStore.saveIpDataConfig();
        }
    }

}
