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

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;

import com.bobcat00.altdetector.Config.ConvertFromType;
import com.bobcat00.altdetector.database.Database;
import com.bobcat00.altdetector.database.Mysql;
import com.bobcat00.altdetector.database.Sqlite;

public class AltDetector extends JavaPlugin
{
    int expirationTime = 60;
    public Config config;
    Database database;
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
        
        // Database
        
        if (config.getDatabaseType().equalsIgnoreCase("mysql"))
        {
            database = new Mysql(this, config.getSqlDebug(), config.getMysqlPrefix());
        }
        else
        {
            database = new Sqlite(this, config.getSqlDebug(), ""); // no prefix for SQLite
        }
        
        // Initialize database
        boolean initSuccessful = database.initialize();
        
        if (initSuccessful)
        {
            getLogger().info("Using " + database.toString() + " database, version " + database.getSqlVersion() + ", driver version " + database.getDriverVersion());
            
            // Database conversion
            
            ConvertFromType convertFrom = config.getConvertFrom();
            
            switch(convertFrom)
            {
            case NONE:
                break;
                
            case YML:
            case SQLITE:
            case MYSQL:
                // Convert database
                convertDb(convertFrom);
                break;
                
            case ERROR:
                getLogger().warning("Invalid convert-from database conversion option specified in config.yml.");
                break;
            }
            
            // Database purge
            
            int entriesRemoved = database.purge(expirationTime);
            getLogger().info(entriesRemoved + " record" + (entriesRemoved == 1 ? "" : "s") + " removed, expiration time " + expirationTime + " days.");
            
            // Generate player list
            database.generatePlayerList();
        }
        else
        {
            // Database init failed
            getLogger().warning("Initialization of " + database.toString() + " database failed.");
        }
        
        // Listeners
        
        listeners = new Listeners(this);
        
        // Commands
        
        this.getCommand("alt").setExecutor(new Commands(this));
        this.getCommand("alt").setTabCompleter(new TabComplete(this));
        
        // Metrics
        
        int pluginId = 4862;
        Metrics metrics = new Metrics(this, pluginId);

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
        metrics.addCustomChart(new SimplePie("expiration_time", () -> setting));
        
        metrics.addCustomChart(new SimplePie("database_type", () -> database.toString()));

        getLogger().info("Metrics enabled if allowed by plugins/bStats/config.yml");
    }
    
    // -------------------------------------------------------------------------
    
    // Convert database from 'convertFrom' to the database-type type specified
    // in the config file
    
    private void convertDb(ConvertFromType convertFrom)
    {
        boolean conversionSuccessful = false;

        if (convertFrom == ConvertFromType.YML)
        {
            // Convert from YAML
            getLogger().info("Converting from YML to " + database.toString() + " database. This may take a while, please be patient.");
            ConvertYaml convertYml = new ConvertYaml(this);
            conversionSuccessful = convertYml.convert();
        }
        else if (convertFrom == ConvertFromType.SQLITE || convertFrom == ConvertFromType.MYSQL)
        {
            Database oldDb = null;
            if (convertFrom == ConvertFromType.MYSQL)
            {
                oldDb = new Mysql(this, config.getSqlDebug(), config.getMysqlPrefix());
            }
            else
            {
                oldDb = new Sqlite(this, config.getSqlDebug(), ""); // no prefix for SQLite
            }
            
            // Convert between SQL databases - make sure they're different types
            if (!database.getClass().equals(oldDb.getClass()))
            {
                boolean initSuccessful = oldDb.initialize();
                if (initSuccessful)
                {
                    getLogger().info("Converting from " + oldDb.toString() + " to " + database.toString() + " database. This may take a while, please be patient.");
                    ConvertSql convertSql = new ConvertSql(this);
                    conversionSuccessful = convertSql.convert(oldDb, database);
                }
                else
                {
                    getLogger().warning("Initialization of " + oldDb.toString() + " database failed.");
                }
                oldDb.closeDataSource(); // only opened databases should be closed
            }
            else
            {
                getLogger().warning("Invalid database conversion options specified in config.yml.");
            }
        }
        
        if (conversionSuccessful)
        {
            // Set to not convert in the future
            getConfig().set("convert-from", "none");
            config.saveConfig();
            getLogger().info("Successfully converted to " + database.toString() + " database.");
        }
        else
        {
            getLogger().warning("Conversion to " + database.toString() + " database failed. Old data not converted.");
        }
        
    }
    
    // -------------------------------------------------------------------------
    
    @Override
    public void onDisable()
    {
        // Wait up to 5 seconds for our async tasks to complete
        for (int i=0; i<50; ++i)
        {
            List<BukkitWorker> workers = Bukkit.getScheduler().getActiveWorkers();
            boolean taskFound = false;

            for (BukkitWorker worker : workers)
            {
                if (worker.getOwner().equals(this))
                {
                    taskFound = true;
                    break; // inner loop
                }
            }

            if (!taskFound)
            {
                break; // outer loop
            }

            try
            {
                Thread.sleep(100); // msec
            }
            catch (InterruptedException e)
            {
            }
        }

        // Close database

        database.closeDataSource();
    }

}
