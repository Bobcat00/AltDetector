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

import java.util.List;

import com.bobcat00.altdetector.database.Database;
import com.bobcat00.altdetector.database.Database.IptableType;
import com.bobcat00.altdetector.database.Database.PlayertableType;

public class ConvertSql
{
    @SuppressWarnings("unused")
    private AltDetector plugin;
    
    // Constructor
    
    public ConvertSql(AltDetector plugin)
    {
        this.plugin = plugin;
    }
    
    // -------------------------------------------------------------------------
    
    // Convert from one SQL database to another
    
    public boolean convert(Database fromDb, Database toDb)
    {
        boolean success = true;
        
        // Convert playertable
        
        List<PlayertableType> playertableList = fromDb.getPlayertable();
        
        for (PlayertableType pt : playertableList)
        {
            success &= toDb.addPlayertableEntry(pt.name, pt.uuid);
        }
        
        if (success)
        {
            // Convert iptable

            List<IptableType> iptableList = fromDb.getIptable();

            for (IptableType ipt : iptableList)
            {
                success &= toDb.addIptableEntry(ipt.ipaddr, ipt.uuid, ipt.unixdate);
            }
        }
        
        return success;
    }

}
