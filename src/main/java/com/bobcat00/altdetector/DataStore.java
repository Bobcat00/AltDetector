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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DataStore
{
    // hashmap: IP address, array of (date, UUID)
    // or
    // key_IP: array of (date, UUID)
    
    private AltDetector plugin;
    
    private File ipDataFile = null; // The file on the disk
    private FileConfiguration ipDataConfig = null; // The contents of the configuration
    private static final String IP_FILE_NAME = "ipdata.yml";
    
    // Constructor
    public DataStore(AltDetector plugin)
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

        // Look for defaults in the jar
        Reader defConfigStream;
        try
        {
            defConfigStream = new InputStreamReader(plugin.getResource(IP_FILE_NAME), "UTF8");
            if (defConfigStream != null)
            {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                ipDataConfig.setDefaults(defConfig);
            }
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
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
    
    // Save file
    public void saveIpDataConfig()
    {
        if (ipDataConfig == null || ipDataFile == null)
        {
            return;
        }
        try
        {
            getIpDataConfig().save(ipDataFile);
        }
        catch (IOException ex)
        {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + ipDataFile, ex);
        }
    }
    
    // Save defaults
    public void saveDefaultConfig()
    {
        if (ipDataFile == null)
        {
            ipDataFile = new File(plugin.getDataFolder(), IP_FILE_NAME);
        }
        if (!ipDataFile.exists())
        {
            plugin.saveResource(IP_FILE_NAME, false);
        }
    }
    
    // -------------------------------------------------------------------------
    
    // This method purges all entries older than the expiration time.  It
    // returns a count of the number purged.
    
    public int purge()
    {
        List<String> removeList = new ArrayList<String>();
        int recordsPurged = 0;
        Date oldestDate = new Date(System.currentTimeMillis() - (plugin.expirationTime*24L*60L*60L*1000L));
        
        ConfigurationSection ipConfSect = getIpDataConfig().getConfigurationSection("ip");
        if (ipConfSect != null)
        {
            // Loop through the IP keys
            for (String ip : ipConfSect.getKeys(false))
            {
                // Get list of UUID keys for this IP
                Set<String> uuidKeys = getIpDataConfig().getConfigurationSection("ip." + ip).getKeys(false);
                int remainingKeys = uuidKeys.size();

                // Loop through the UUID keys
                for (String uuid : uuidKeys)
                {
                    String uuidData = getIpDataConfig().getString("ip." + ip + "." + uuid);
                    String[] arg = uuidData.split(","); // arg[0]=date, arg[1]=name
                    Date date = new Date(Long.valueOf(arg[0]).longValue());

                    // Check if entry expired
                    if (date.before(oldestDate))
                    {
                        // Add to removal list
                        removeList.add("ip." + ip + "." + uuid);
                        --remainingKeys;
                        ++recordsPurged;
                    }

                } // end for each UUID key

                // Check if the IP needs to be removed
                if (remainingKeys <= 0)
                {
                    removeList.add("ip." + ip);
                }

            } // end for each IP key
        
        } // end if ipConfSect != null
        
        // Remove the keys from the file
        for (String key : removeList)
        {
            getIpDataConfig().set(key, null);
        }

        return recordsPurged;
    }
    
    // -------------------------------------------------------------------------
    
    // This method adds or updates an entry.  The date is included.
    
    public void addUpdateIp(String ip, String uuid, String name)
    {
        Date date = new Date();
        getIpDataConfig().set("ip." +
                              ip.replace('.', '_') + "." +
                              uuid,
                              date.getTime() + "," + name);
    }
    
    // -------------------------------------------------------------------------
    
    // This method returns a list of names that match the given IP address.  It
    // may contain the (current) player, unless it is excluded.
    
    public List<String> getAltNames(String ip, String excludeUuid)
    {
        List<String> altList = new ArrayList<String>();
        
        Date oldestDate = new Date(System.currentTimeMillis() - (plugin.expirationTime*24L*60L*60L*1000L));
        
        ConfigurationSection ipIpConfSect = getIpDataConfig().getConfigurationSection("ip." + ip.replace('.', '_'));
        if (ipIpConfSect != null)
        {
            // Get the UUIDs for this IP address
            for (String uuid : ipIpConfSect.getKeys(false))
            {
                String uuidData = getIpDataConfig().getString("ip." + ip.replace('.', '_') + "." + uuid);
                String[] arg = uuidData.split(","); // arg[0]=date, arg[1]=name
                Date date = new Date(Long.valueOf(arg[0]).longValue());

                // Check excluded UUID (if any) and expiration time
                if (!uuid.equals(excludeUuid) && date.after(oldestDate))
                {
                    // Add to list
                    altList.add(arg[1]);
                }
            
            } // end for each UUID
        
        } // end if ipIpConfSect != null
        
        Collections.sort(altList, String.CASE_INSENSITIVE_ORDER);
        
        return altList;
    }

}