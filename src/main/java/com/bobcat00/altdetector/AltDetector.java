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

import org.bukkit.plugin.java.JavaPlugin;

public class AltDetector extends JavaPlugin
{
    long expirationTime = 60;
    Config config;
    DataStore dataStore;
    Listeners listeners;
    
    @Override
    public void onEnable()
    {
        config = new Config(this);
        dataStore = new DataStore(this);
        listeners = new Listeners(this);
        
        this.getCommand("alt").setExecutor(new Commands(this));
        
        saveDefaultConfig();
        
        dataStore.saveDefaultConfig();
        dataStore.reloadIpDataConfig();
        
        expirationTime = config.getExpirationTime();
        
        int entriesRemoved = dataStore.purge();
        dataStore.saveIpDataConfig();
         
        getLogger().info(entriesRemoved + " record" + (entriesRemoved == 1 ? "" : "s") + " removed, expiration time " + expirationTime + " days.");
    }
    
    @Override
    public void onDisable()
    {
        //
    }

}