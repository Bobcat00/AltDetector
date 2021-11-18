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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class DataStore
{
    private AltDetector plugin;
    
    private volatile boolean saveData = false;
    
    // Contains all the player names in the data file.  This is intended for use with the
    // tab complete capability.  Mojang's Brigadier acts weird with mixed-case names, so
    // the names here will be all lower case.
    private Set<String> playerList = new HashSet<>();
    
    private File ipDataFile = null; // The file on the disk
    private FileConfiguration ipDataConfig = null; // The contents of the configuration
    private static final String IP_FILE_NAME = "ipdata.yml";
    
    // Constructor
    @SuppressWarnings("deprecation")
    public DataStore(AltDetector plugin)
    {
        this.plugin = plugin;
        
        // Periodic task to save data
        if (plugin.saveInterval > 0)
        {
            Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, new Runnable() {
                @Override
                public void run()
                {
                    if (saveData)
                    {
                        saveIpDataConfig();
                    }
                }
            }, plugin.saveInterval * 60L * 20L,  // delay
               plugin.saveInterval * 60L * 20L); // period
        }
    }
    
    // -------------------------------------------------------------------------
    
    // File management
    
    // Reload file
    public synchronized void reloadIpDataConfig()
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
    public synchronized FileConfiguration getIpDataConfig()
    {
        if (ipDataConfig == null)
        {
            reloadIpDataConfig();
        }
        return ipDataConfig;
    }
    
    // Save file
    public synchronized void saveIpDataConfig()
    {
        if (ipDataConfig == null || ipDataFile == null)
        {
            return;
        }
        try
        {
            getIpDataConfig().save(ipDataFile);
            saveData = false;
        }
        catch (IOException ex)
        {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + ipDataFile, ex);
        }
    }
    
    // Save defaults
    public synchronized void saveDefaultConfig()
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
    
    // Generates playerList 
    
    public synchronized void generatePlayerList()
    {
        playerList.clear();
        
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
                    playerList.add(arg[1].toLowerCase());
                }
            
            } // end for each IP key
        
        } // end if ipConfSect != null
        
        return;
    }
    
    // -------------------------------------------------------------------------
    
    public synchronized List<String> getPlayerList()
    {
        List<String> pl = new ArrayList<>(playerList);
        pl.sort(null);
        return pl;
    }
    
    // -------------------------------------------------------------------------
    
    // This method purges all entries for the specified player name
    // OR
    // all entries older than the expiration time (if the name is "").
    // It returns a count of the number purged.
    
    public synchronized int purge(String name)
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

                    // Check if entry should be purged
                    if ((name.equals("") && date.before(oldestDate)) || 
                        (name.equalsIgnoreCase(arg[1])))
                    {
                        // Add to removal list
                        removeList.add("ip." + ip + "." + uuid);
                        --remainingKeys;
                        ++recordsPurged;
                        // Remove from playerList
                        playerList.remove(arg[1].toLowerCase());
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
        
        if (recordsPurged > 0)
        {
            saveData = true;
        }
        
        return recordsPurged;
    }
    
    // -------------------------------------------------------------------------
    
    // This method adds or updates an entry.  The date is included.
    
    public synchronized void addUpdateIp(String ip, String uuid, String name)
    {
        Date date = new Date();
        getIpDataConfig().set("ip." +
                              ip.replace('.', '_') + "." +
                              uuid,
                              date.getTime() + "," + name);
        playerList.add(name.toLowerCase());
        saveData = true;
    }
    
    // -------------------------------------------------------------------------
    
    // This method returns a list of names that match the given IP address.  It
    // may contain the (current) player, unless it is excluded.
    
    private synchronized List<String> getAltNames(String ip, String excludeUuid)
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
    
    // -------------------------------------------------------------------------
    
    // Class to allow lookupOfflinePlayer to return three values.
    
    public class PlayerDataType
    {
        String ip;
        String uuid;
        String name;
    }
    
    // -------------------------------------------------------------------------
    
    // Take a playerName and look up the most recent IP address, UUID, and name.
    
    public synchronized PlayerDataType lookupOfflinePlayer(String playerName)
    {
        // Return value
        PlayerDataType playerData = null;
        
        Date oldestDate = new Date(System.currentTimeMillis() - (plugin.expirationTime*24L*60L*60L*1000L));
        Date newestDate = new Date(0L);
        
        ConfigurationSection ipConfSect = getIpDataConfig().getConfigurationSection("ip.");
        Map<String,Object> confMap = ipConfSect.getValues(true);
        
        // Go through all the keys which contain player data
        for (Map.Entry<String,Object> entry: confMap.entrySet())
        {
            if (entry.getValue() instanceof String)
            {
                // Split key into IP address and UUID
                String[] key = entry.getKey().split("\\.");
                String ip   = key[0];
                String uuid = key[1];
                
                // Split value into timestamp and player name
                String[] value = ((String)entry.getValue()).split(",");
                String timestamp = value[0];
                String name      = value[1];
                
                // Check if it's for the desired player name
                if (playerName.equalsIgnoreCase(name))
                {
                    Date date = new Date(Long.valueOf(timestamp).longValue());
                    if (date.after(oldestDate) && date.after(newestDate))
                    {
                        playerData = new PlayerDataType();
                        playerData.ip   = ip.replace('_','.');
                        playerData.uuid = uuid;
                        playerData.name = name;
                        
                        newestDate = date;
                    }
                }
            }
        }
        
        return playerData;
    }
    
    // -------------------------------------------------------------------------
    
    // Look up the alts for a player, and return a formatted string using the &
    // color codes. If no alts are found, the returned string is null.
    
    public String getFormattedAltString(String name,
                                        String ip,
                                        String uuid,
                                        String playerFormat,
                                        String playerListFormat,
                                        String playerSeparator)
    {
        // Get possible alts
        List<String> altList = getAltNames(ip, uuid);
        
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
