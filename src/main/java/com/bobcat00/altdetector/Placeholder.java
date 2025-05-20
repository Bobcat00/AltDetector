// AltDetector - Detects possible alt accounts
// Copyright 2025
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.bobcat00.altdetector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.OfflinePlayer;

public class Placeholder extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
    
    private final AltDetector plugin;
    private static final Pattern ALT_PATTERN = Pattern.compile("alts_(.+)");
    
    // Constructor
    
    public Placeholder(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier()
    {
        return "altdetector";
    }
    
    @Override
    public String getAuthor()
    {
        return String.join(", ", plugin.getDescription().getAuthors());
    }
    
    @Override
    public String getVersion()
    {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist()
    {
        return true; // Keep the expansion loaded on PlaceholderAPI reload
    }
    
    // -------------------------------------------------------------------------
    
    @Override
    public String onRequest(OfflinePlayer player, String identifier)
    {
        // Check for alts_{PlayerName} pattern
        Matcher matcher = ALT_PATTERN.matcher(identifier);
        if (matcher.matches())
        {
            String targetPlayerName = matcher.group(1);
            String alts = plugin.database.getCachedAlts(targetPlayerName);
            return alts;
        }
        return null; // Placeholder is not recognized
    }
}
