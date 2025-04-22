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

package com.bobcat00.altdetector.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bobcat00.altdetector.AltDetector;
import com.zaxxer.hikari.HikariDataSource;

// This abstract class is used to access the database, regardless of type.
// It should be called with async threads. Methods called from onEnable()
// may be called on the main thread.

public abstract class Database
{
    AltDetector plugin;
    boolean debug;
    String prefix;
    HikariDataSource dataSource;
    
    // Contains all the player names in the data file. This is intended for use with the
    // tab complete capability. Mojang's Brigadier acts weird with mixed-case names, so
    // the names here will be all lower case. A Set is used so duplicate entries will
    // not occur.
    private Set<String> playerList = Collections.synchronizedSet(new HashSet<String>());
    
    // Default SQL statements. These are for SQLite. Other implementations can replace them.
    
    String sqlVersion        = "SELECT sqlite_version() AS version;";
    String getAllNames       = "SELECT DISTINCT name FROM {prefix}playertable;";
    String purgeByDate1      = "DELETE FROM {prefix}iptable WHERE date < datetime('now', ?);";
    String purgeByDate2      = "DELETE FROM {prefix}playertable WHERE id NOT IN (SELECT playerid FROM {prefix}iptable);";
    String purgeByName       = "DELETE FROM {prefix}playertable WHERE lower(name) = lower(?);";
    String getNameByUuid     = "SELECT name FROM {prefix}playertable WHERE uuid = ?;";
    String addPlayerEntry    = "INSERT INTO {prefix}playertable (uuid, name) VALUES (?, ?);";
    String updatePlayerEntry = "UPDATE {prefix}playertable SET name = ? WHERE uuid = ?;";
    String checkIpEntry      = "SELECT EXISTS (SELECT 1 FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE ipaddr = ? AND uuid = ?);";
    String addIpEntry        = "INSERT INTO {prefix}iptable (ipaddr, playerid, date) VALUES (?, (SELECT id FROM {prefix}playertable WHERE uuid = ?), datetime('now'));";
    String updateIpEntry     = "UPDATE {prefix}iptable SET date = datetime('now') WHERE ipaddr = ? AND playerid = (SELECT id FROM {prefix}playertable WHERE uuid = ?);";
    String addIpWithDate     = "INSERT INTO {prefix}iptable (ipaddr, playerid, date) VALUES (?, (SELECT id FROM {prefix}playertable WHERE uuid = ?), datetime(?, 'unixepoch'));";
    String getAlts           = "SELECT DISTINCT name FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE ipaddr IN (SELECT ipaddr FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE uuid = ?) AND uuid <> ? AND date >= datetime('now', ?) ORDER BY lower(name);";
    String getOfflinePlayer  = "SELECT uuid, name FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE lower(name) = lower(?) ORDER BY date DESC LIMIT 1;";
    String getPlayertable    = "SELECT name, uuid FROM {prefix}playertable;";
    String getIptable        = "SELECT ipaddr, uuid, strftime('%s',date) FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id;";
    
    // Constructor
    
    public Database(AltDetector plugin, boolean debug, String prefix)
    {
        this.plugin = plugin;
        this.debug = debug;
        this.prefix = prefix;
    }
    
    // -------------------------------------------------------------------------
    
    // Abstract members
    
    public abstract boolean initialize();
    
    public abstract String toString();
    
    abstract String formatExpirationTime(int expirationTime);
    
    // -------------------------------------------------------------------------
    
    // Replace {prefix} in SQL statements
    
    String replacePrefix(String statement)
    {
        return statement.replace("{prefix}", prefix);
    }
    
    // -------------------------------------------------------------------------
    
    // Close the DataSource. This is normally done when the plugin is disabled.
    
    public void closeDataSource()
    {
        if (dataSource != null)
        {
            dataSource.close();
        }
    }
    
    // -------------------------------------------------------------------------
    
    // Get a Connection to the database
    
    Connection getConnection()
    {
        try
        {
            Connection connection = dataSource.getConnection();
            return connection;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Error getting database connection: " + e.getMessage());
            //e.printStackTrace();
        }
        return null;
    }
    
    // -------------------------------------------------------------------------
    
    // Execute an arbitrary SQL statement
    
    Boolean executeStatement(String statement)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(statement))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.execute();
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error executing statement: " + statement + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Get version of the SQL database
    
    public String getSqlVersion()
    {
        String version = "";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(sqlVersion)))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next())
            {
                version = resultSet.getString("version");
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving SQL version: " + e.getMessage());
        }
        
        return version;
    }
    
    // -------------------------------------------------------------------------
    
    // Get version of the database driver
    
    public String getDriverVersion()
    {
        String version = "";

        try (Connection conn = getConnection())
        {
            DatabaseMetaData meta = conn.getMetaData();
            String ver = meta.getDriverVersion();
            if (ver != null)
            {
                version = ver;
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving driver version: " + e.getMessage());
        }
        
        return version;
    }
    
    // -------------------------------------------------------------------------
    
    // Generate playerList
    
    public void generatePlayerList()
    {
        playerList.clear();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getAllNames)))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next())
            {
                playerList.add(resultSet.getString("name").toLowerCase());
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving player list from playertable: " + e.getMessage());
        }
        
        return;
    }
    
    // -------------------------------------------------------------------------
    
    // Get playerlist. This can be called on the main thread.
    
    public List<String> getPlayerList()
    {
        List<String> pl = new ArrayList<>(playerList);
        pl.sort(null);
        return pl;
    }
    
    // -------------------------------------------------------------------------
    
    // Purge entries older than expiration time
    // Returns a count of the number of records purged
    
    public int purge(int expirationTime)
    {
        int recordsPurged = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(purgeByDate1)))
        {
            stmt.setString(1, formatExpirationTime(expirationTime));
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            recordsPurged = stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error purging records: " + e.getMessage());
        }
        
        // Now delete playertable entries with no children
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(purgeByDate2)))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            int recordsPurged2 = stmt.executeUpdate();
            if (debug) {plugin.getLogger().info(recordsPurged2 + " playertable entries deleted");}
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error purging playertable: " + e.getMessage());
        }
        
        return recordsPurged;
     }
    
    // -------------------------------------------------------------------------
    
    // Purge entries for specified player
    // Returns a count of the number of records purged
    //
    // The Foreign key constraint means entries deleted from playertable will
    // delete referenced entries in iptable.
    //
    // A race condition can occur if a player is joining the server at the same
    // time that a delete command is issued for the same player. This can cause
    // error messages regarding either table, but the foreign key constraint
    // will ensure consistency of the database is maintained.
    
    public int purge(String name)
    {
        int recordsPurged = 0;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(purgeByName)))
        {
            stmt.setString(1, name);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            recordsPurged = stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error purging records for " + name + ": " + e.getMessage());
        }
        
        return recordsPurged;
    }
    
    // -------------------------------------------------------------------------
    
    // The following six methods are used when a player joins the server
    
    // Get name from playertable with specified UUID
    // Returns the name if found, "" if not found
    
    public String getNameFromPlayertable(String uuid)
    {
        String name = "";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getNameByUuid)))
        {
            stmt.setString(1, uuid);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next())
            {
                name = resultSet.getString("name");
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving name from playertable for " + uuid + ": " + e.getMessage());
        }
        
        return name;
    }
    
    // -------------------------------------------------------------------------
    
    // Add new playertable entry
    
    public boolean addPlayertableEntry(String name, String uuid)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(addPlayerEntry)))
        {
            stmt.setString(1, uuid);
            stmt.setString(2, name);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.executeUpdate();
            playerList.add(name.toLowerCase()); // add to playerList
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error adding playertable entry for " + name + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Update name in playertable
    
    public boolean updateNameInPlayertable(String name, String uuid)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(updatePlayerEntry)))
        {
            stmt.setString(1, name);
            stmt.setString(2, uuid);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.executeUpdate();
            playerList.add(name.toLowerCase()); // add to playerList
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error updating playertable entry for " + name + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Check if iptable entry exists for specified IP address and UUID
    // The playertable entry for this player must be created first
    
    public boolean checkIptableEntry(String ip, String uuid)
    {
        boolean success = false;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(checkIpEntry)))
        {
            stmt.setString(1, ip);
            stmt.setString(2, uuid);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();
            
            resultSet.next();
            success = resultSet.getBoolean(1);
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving name from playertable entry for " + ip + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Add new entry in iptable for specified IP address and UUID
    // The playertable entry for this player must be created first
    
    public boolean addIptableEntry(String ip, String uuid)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(addIpEntry)))
        {
            stmt.setString(1, ip);
            stmt.setString(2, uuid);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.executeUpdate();
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error adding iptable entry for " + ip + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Update date in iptable for specified IP address and UUID
    // The playertable entry for this player must be created first
    
    public boolean updateIptableEntry(String ip, String uuid)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(updateIpEntry)))
        {
            stmt.setString(1, ip);
            stmt.setString(2, uuid);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.executeUpdate();
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error updating iptable entry for " + ip + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Add new entry in iptable for specified IP address and UUID with a
    // specific date. This is used for the conversion from other databases. The
    // playertable entry for this player must be created first.
    
    public boolean addIptableEntry(String ip, String uuid, long unixdate)
    {
        boolean success = false;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(addIpWithDate)))
        {
            stmt.setString(1, ip);
            stmt.setString(2, uuid);
            stmt.setLong(3, unixdate);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            stmt.executeUpdate();
            success = true;
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error adding iptable entry with date for " + ip + " and " + uuid + ": " + e.getMessage());
        }
        
        return success;
    }
    
    // -------------------------------------------------------------------------
    
    // Get list of names, case-insensitive sort, matching IP addresses used by
    // the specified uuid, excluding excludeUuid, and newer than expiration time
    
    public List<String> getAltNames(String uuid, String excludeUuid, int expirationTime)
    {
        List<String> altList = new ArrayList<String>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getAlts)))
        {
            stmt.setString(1, uuid);
            stmt.setString(2, excludeUuid);
            stmt.setString(3, formatExpirationTime(expirationTime));
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next())
            {
                altList.add(resultSet.getString("name"));
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving list of names from playertable for " + uuid + ": " + e.getMessage());
        }
        
        return altList;
    }
    
    // -------------------------------------------------------------------------
    
    // Class to allow getOfflinePlayer to return two values.
    
    public class PlayerDataType
    {
        public String uuid;
        public String name;
    }
    
    // -------------------------------------------------------------------------
    
    // Get uuid and name for most recent entry for specified player
    
    public PlayerDataType lookupOfflinePlayer(String name)
    {
        PlayerDataType playerData = null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getOfflinePlayer)))
        {
            stmt.setString(1, name);
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next())
            {
                playerData = new PlayerDataType();
                playerData.uuid = resultSet.getString("uuid");
                playerData.name = resultSet.getString("name");
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving offline player from playertable for " + name + ": " + e.getMessage());
        }
        
        return playerData;
    }
    
    // -------------------------------------------------------------------------
    
    // Look up the alts for a player, and return a formatted string using the &
    // color codes. If no alts are found, the returned string is null.
    
    public String getFormattedAltString(String name,
                                        String uuid,
                                        String playerFormat,
                                        String playerListFormat,
                                        String playerSeparator,
                                        int expirationTime)
    {
        // Get possible alts
        List<String> altList = getAltNames(uuid, uuid, expirationTime);
        
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
    
    // -------------------------------------------------------------------------
    
    // Class to allow getPlayertable to return a List of two values.
    
    public class PlayertableType
    {
        public String uuid;
        public String name;
    }
    
    // -------------------------------------------------------------------------
    
    // Get playertable for conversion from one SQL database to another
    
    public List<PlayertableType> getPlayertable()
    {
        List<PlayertableType> playertable = new ArrayList<PlayertableType>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getPlayertable)))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next())
            {
                PlayertableType pt = new PlayertableType();
                pt.uuid = resultSet.getString("uuid");
                pt.name = resultSet.getString("name");
                playertable.add(pt);
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving Playertable: " + e.getMessage());
        }
        
        return playertable;
    }
    
    // -------------------------------------------------------------------------
    
    // Class to allow getIptable to return a List of three values.
    
    public class IptableType
    {
        public String ipaddr;
        public String uuid;
        public long unixdate;
    }
    
    // -------------------------------------------------------------------------
    
    // Get iptable for conversion from one SQL database to another
    
    public List<IptableType> getIptable()
    {
        List<IptableType> iptable = new ArrayList<IptableType>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(replacePrefix(getIptable)))
        {
            if (debug) {plugin.getLogger().info("Executing statement: " + stmt.toString());}
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next())
            {
                IptableType ipt = new IptableType();
                ipt.ipaddr = resultSet.getString("ipaddr");
                ipt.uuid = resultSet.getString("uuid");
                ipt.unixdate = resultSet.getLong(3);
                iptable.add(ipt);
            }
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("Database error retrieving Playertable: " + e.getMessage());
        }
        
        return iptable;
    }
}
