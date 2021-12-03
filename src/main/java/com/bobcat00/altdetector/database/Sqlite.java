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

import java.io.File;
import java.io.IOException;

import com.bobcat00.altdetector.AltDetector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// This class is used to access the SQLite database. It creates the SQL statements.

public class Sqlite extends Database
{
    // SQL statements to create the required tables. The foreign key constraint means:
    // 1. Inserts into iptable must have a valid playerid referencing playertable
    // 2. Deletes from playertable will also delete referenced entries in iptable
    
    private String[] sqlInit = {
        "CREATE TABLE IF NOT EXISTS playertable (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid CHAR(36) UNIQUE NOT NULL, name VARCHAR(255) NOT NULL);",
        "CREATE TABLE IF NOT EXISTS iptable (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ipaddr VARCHAR(255) NOT NULL, playerid INTEGER NOT NULL REFERENCES playertable(id) ON DELETE CASCADE, date DATETIME NOT NULL);",
        "CREATE UNIQUE INDEX IF NOT EXISTS uuid_index ON playertable(uuid);",
        "CREATE INDEX IF NOT EXISTS ipaddr_index ON iptable(ipaddr);",
        "CREATE INDEX IF NOT EXISTS playerid_index ON iptable(playerid);"};
    
    private String dbFilename;
    
    // Constructor
    
    public Sqlite(AltDetector plugin, boolean debug, String prefix)
    {
        super(plugin, debug, prefix);
    }
    
    // -------------------------------------------------------------------------
    
    // Returns name of database
    
    public String toString()
    {
        return "SQLite";
    }
    
    // -------------------------------------------------------------------------
    
    // Formats the expiration time as required by SQLite
    
    String formatExpirationTime(int expirationTime)
    {
        return "-" + expirationTime + " days"; // negative value
    }
    
    // -------------------------------------------------------------------------
    
    // Initialize the database. Call after creating an instance of this class.
    
    public boolean initialize()
    {
        // Check if file exists, create if not
        
        File dbFile = new File(plugin.getDataFolder(), "altdetector.db");
        
        if (!dbFile.exists())
        {
            try
            {
                dbFile.createNewFile();
            }
            catch (IOException e)
            {
                plugin.getLogger().warning("Unable to create database file: " + e.getMessage());
                return false;
            }
        }
        dbFilename = dbFile.toString();
        
        // Initialize HikariCP. Set the maximum pool size to 1 since this is
        // SQLite, and enable foreign key support.
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFilename);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setConnectionInitSql("PRAGMA foreign_keys = ON");
        dataSource = new HikariDataSource(hikariConfig);
        
        // Send initial SQL statements
        
        for (String sql : sqlInit)
        {
            boolean success = executeStatement(sql);
            if (!success)
            {
                return false;
            }
        }
        
        return true;
    }
    
}
