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

package com.bobcat00.altdetector.datastore;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

public interface Datastore
{
    //Used for YAML only
    public void reloadIpDataConfig();
    public FileConfiguration getIpDataConfig();
    public void saveIpDataConfig();
    public void saveDefaultConfig();
    
    // Generate and return player list
    public void generatePlayerList();
    public List<String> getPlayerList();
    
    // This method purges all entries for the specified player name
    // OR
    // all entries older than the expiration time (if the name is "").
    // It returns a count of the number purged.
    public int purge(String name);
    
    // This method adds or updates an entry.  The date is included.
    public void addUpdateIp(String ip, String uuid, String name);
    
    // This method returns a list of names that match the given IP address.  It
    // may contain the (current) player, unless it is excluded.
    public List<String> getAltNames(String ip, String excludeUuid);
    
    public class PlayerDataType
    {
        public String ip;
        public String uuid;
        public String name;
    }
    
    // Take a playerName and look up the most recent IP address, UUID, and name.
    public PlayerDataType lookupOfflinePlayer(String playerName);
    
    // Look up the alts for a player, and return a formatted string using the &
    // color codes. If no alts are found, the returned string is null.
    public String getFormattedAltString(String name,
                                        String ip,
                                        String uuid,
                                        String playerFormat,
                                        String playerListFormat,
                                        String playerSeparator);
}
