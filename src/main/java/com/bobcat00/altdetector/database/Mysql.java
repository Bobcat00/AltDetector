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

import com.bobcat00.altdetector.AltDetector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// This class is used to access the SQLite database. It creates the SQL statements.

public class Mysql extends Database
{
    // SQL statements to create the required tables. The foreign key constraint means:
    // 1. Inserts into iptable must have a valid playerid referencing playertable
    // 2. Deletes from playertable will also delete referenced entries in iptable
    // Note that MySQL will automatically create an index for playerid.
    
    private String initPlayer = "CREATE TABLE IF NOT EXISTS {prefix}playertable (id INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, uuid CHAR(36) UNIQUE KEY NOT NULL, name VARCHAR(255) NOT NULL);";
    private String initIp     = "CREATE TABLE IF NOT EXISTS {prefix}iptable (id INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL, ipaddr VARCHAR(255) NOT NULL, playerid INTEGER NOT NULL, date DATETIME NOT NULL, INDEX ipaddr_index (ipaddr), FOREIGN KEY (playerid) REFERENCES {prefix}playertable(id) ON DELETE CASCADE);";
    
    // Constructor
    
    public Mysql(AltDetector plugin, boolean debug, String prefix)
    {
        super(plugin, debug, prefix);
        
        // MySQL-specific SQL statements (overwrites the ones in Database)
        // These are mostly differences with SQLite's datetime function
        
        sqlVersion    = "SELECT version() AS version;";
        purgeByDate1  = "DELETE FROM {prefix}iptable WHERE date < SUBDATE(now(),?);";
        addIpEntry    = "INSERT INTO {prefix}iptable (ipaddr, playerid, date) VALUES (?, (SELECT id FROM {prefix}playertable WHERE uuid = ?), now());";
        updateIpEntry = "UPDATE {prefix}iptable SET date = now() WHERE ipaddr = ? AND playerid = (SELECT id FROM {prefix}playertable WHERE uuid = ?);";
        addIpWithDate = "INSERT INTO {prefix}iptable (ipaddr, playerid, date) VALUES (?, (SELECT id FROM {prefix}playertable WHERE uuid = ?), FROM_UNIXTIME(?));";
        getAlts       = "SELECT DISTINCT name FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE ipaddr IN (SELECT ipaddr FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id WHERE uuid = ?) AND uuid <> ? AND date >= SUBDATE(now(),?) ORDER BY lower(name);";
        getIptable    = "SELECT ipaddr, uuid, UNIX_TIMESTAMP(date) FROM {prefix}iptable INNER JOIN {prefix}playertable ON {prefix}iptable.playerid = {prefix}playertable.id;";
    }
    
    // -------------------------------------------------------------------------
    
    // Returns name of database
    
    public String toString()
    {
        return "MySQL";
    }
    
    // -------------------------------------------------------------------------
    
    // Formats the expiration time as required by MySQL
    
    String formatExpirationTime(int expirationTime)
    {
        return Integer.toString(expirationTime);
    }
    
    // -------------------------------------------------------------------------
    
    // Initialize the database. Call after creating an instance of this class.
    
    public boolean initialize()
    {
        // Initialize HikariCP. The JDBC URL is constructed from the values in
        // the config file. The MySQL driver's statement cache is enabled.
        // MySQL's server-side prepared statements are intentionally not used.
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" +
                                plugin.config.getMysqlHostname() + ":" +
                                plugin.config.getMysqlPort() + "/" +
                                plugin.config.getMysqlDatabase() +
                                plugin.config.getJdbcurlProperties());
        hikariConfig.setUsername(plugin.config.getMysqlUsername());
        hikariConfig.setPassword(plugin.config.getMysqlPassword());
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "50");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "1024");
        if (debug) {plugin.getLogger().info("JDBC URL: " + hikariConfig.getJdbcUrl());}
        dataSource = new HikariDataSource(hikariConfig);
        
        // Send initial SQL statements
        
        boolean success = executeStatement(replacePrefix(initPlayer));
        if (!success)
        {
            return false;
        }
        success = executeStatement(replacePrefix(initIp));
        if (!success)
        {
            return false;
        }
        
        return true;
    }
    
}
