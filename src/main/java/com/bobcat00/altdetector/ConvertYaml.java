// AltDetector - Detects possible alt accounts
// Copyright 2021 Bobcat00
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConvertYaml
{
    private AltDetector plugin;
    
    private File ipDataFile = null; // The file on the disk
    private FileConfiguration ipDataConfig = null; // The contents of the configuration
    private static final String IP_FILE_NAME = "ipdata.yml";
    
    // Constructor
    
    public ConvertYaml(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    // -------------------------------------------------------------------------
    
    // File management
    
    // Reload file
    public void reloadIpDataConfig()
    {
        if (ipDataFile == null)
        {
            ipDataFile = new File(plugin.getDataFolder(), IP_FILE_NAME);
        }
        ipDataConfig = YamlConfiguration.loadConfiguration(ipDataFile);
    }
    
    // Get file
    public FileConfiguration getIpDataConfig()
    {
        if (ipDataConfig == null)
        {
            reloadIpDataConfig();
        }
        return ipDataConfig;
    }
    
    // -------------------------------------------------------------------------
    
    // Main method for converting YAML to SQL.
    
    public boolean convert()
    {
        boolean success = convertPlayertable();
        if (success)
        {
            success = convertIptable();
        }

        return success;
    }

    // -------------------------------------------------------------------------

    private class DateNameType
    {
        public DateNameType(long date2, String name2)
        {
            date = date2;
            name = name2;
        }
        long date;
        String name;
    }
    
    // -------------------------------------------------------------------------
    
    // Convert playertable by building a Map with UUIDs and the name and date,
    // choosing each name with the most recent date. Then insert each UUID and
    // name into the database.

    private boolean convertPlayertable()
    {
        Map<String, DateNameType> uuidTable = new HashMap<String, DateNameType>();
        
        ConfigurationSection ipConfSect = getIpDataConfig().getConfigurationSection("ip");
        if (ipConfSect != null)
        {
            // Loop through the IP keys
            for (String ip : ipConfSect.getKeys(false))
            {
                // Get list of UUID keys for this IP
                Set<String> uuidKeys = getIpDataConfig().getConfigurationSection("ip." + ip).getKeys(false);

                // Loop through the UUID keys
                for (String uuid : uuidKeys)
                {
                    String uuidData = getIpDataConfig().getString("ip." + ip + "." + uuid);
                    String[] arg = uuidData.split(","); // arg[0]=date, arg[1]=name
                    long date = Long.valueOf(arg[0]).longValue();
                    String name = arg[1];
                    
                    DateNameType dateName = uuidTable.get(uuid);
                    if ((dateName == null) || (date > dateName.date))
                    {
                        // Put in local table - may replace an existing entry
                        uuidTable.put(uuid, new DateNameType(date, name));
                    }
                
                } // end for each UUID key
            
            } // end for each IP key
        
        } // end if ipConfSect != null
        
        // Insert into database
        
        boolean success = true;
        
        for (Map.Entry<String, DateNameType> entry : uuidTable.entrySet())
        {
            String uuid = entry.getKey();
            DateNameType dateName = entry.getValue();
            success &= plugin.database.addPlayertableEntry(dateName.name, uuid);
        }
        
        return success;
    }

    // -------------------------------------------------------------------------

    // Convert iptable by creating a new record for every entry in the YAML file

    private boolean convertIptable()
    {
        ConfigurationSection ipConfSect = getIpDataConfig().getConfigurationSection("ip.");
        Map<String,Object> confMap = ipConfSect.getValues(true);
        
        boolean success = true;
        
        // Go through all the keys which contain player data
        for (Map.Entry<String,Object> entry: confMap.entrySet())
        {
            if (entry.getValue() instanceof String)
            {
                // Split key into IP address and UUID
                String[] key = entry.getKey().split("\\.");
                String ip   = key[0].replace('_','.');
                String uuid = key[1];
                
                // Split value into timestamp and player name
                String[] value = ((String)entry.getValue()).split(",");
                String timestamp = value[0];
                long date = Long.valueOf(timestamp).longValue()/1000; // convert from msec to seconds
                
                success &= plugin.database.addIptableEntry(ip, uuid, date);
            }
        }
        
        return success;
    }

}
