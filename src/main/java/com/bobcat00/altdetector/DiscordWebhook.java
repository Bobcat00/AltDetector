// AltDetector - Detects possible alt accounts
// Copyright 2025
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public class DiscordWebhook
{
    private final AltDetector plugin;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final int embedColor;
    
    // -------------------------------------------------------------------------
    
    // Constructor
    
    public DiscordWebhook(AltDetector plugin)
    {
        this.plugin     = plugin;
        this.webhookUrl = plugin.config.getDiscordWebhookUrl();
        this.username   = plugin.config.getDiscordUsername();
        this.avatarUrl  = plugin.config.getDiscordAvatarUrl();
        this.embedColor = plugin.config.getDiscordEmbedColor();
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * Sends a message about detected alts to Discord
     * 
     * @param content The alt detection message (already formatted and without color codes)
     * @param authorName The Discord author name, usually the name of the Minecraft server
     */
    public void sendAltMessage(final String content, final String authorName)
    {
        // Skip if content is null (no alts found)
        if (content == null)
        {
            return;
        }
        
        // Run async to not block the main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // Build the JSON payload
                    Map<String, Object> jsonMap = new HashMap<>();
                    
                    // Set username if provided
                    if (username != null && !username.isEmpty())
                    {
                        jsonMap.put("username", username);
                    }
                    
                    // Set avatar URL if provided
                    if (avatarUrl != null && !avatarUrl.isEmpty())
                    {
                        jsonMap.put("avatar_url", avatarUrl);
                    }
                    
                    // Create embed
                    Map<String, Object> embed = new HashMap<>();
                    embed.put("title", "Alt Account Detection");
                    embed.put("description", "`" + content + "`");
                    embed.put("color", embedColor);
                    
                    // Add author info
                    Map<String, Object> author = new HashMap<>();
                    author.put("name", authorName);
                    embed.put("author", author);
                    
                    // Add timestamp
                    embed.put("timestamp", java.time.OffsetDateTime.now().toString());
                    
                    // Add embed to payload
                    jsonMap.put("embeds", new Object[] { embed });
                    
                    // Convert to JSON string
                    String jsonString = mapToJson(jsonMap);
                    
                    // Send the webhook
                    URL url = new URI(webhookUrl).toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);
                    
                    try (OutputStream outputStream = connection.getOutputStream())
                    {
                        byte[] input = jsonString.getBytes(StandardCharsets.UTF_8);
                        outputStream.write(input, 0, input.length);
                    }
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode != 204)
                    {
                        plugin.getLogger().warning("Discord webhook returned response code: " + responseCode);
                    }
                }
                catch (IOException | URISyntaxException e)
                {
                    plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
                }
            }
        });
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * Simple method to convert a Map to a JSON string
     */
    private String mapToJson(Map<String, Object> map)
    {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            if (!first)
            {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            appendValueAsJson(json, entry.getValue());
        }
        
        json.append("}");
        return json.toString();
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * Append a value to the JSON string builder
     */
    private void appendValueAsJson(StringBuilder json, Object value)
    {
        if (value instanceof String)
        {
            json.append("\"").append(((String) value).replace("\"", "\\\"")).append("\"");
        }
        else if (value instanceof Number || value instanceof Boolean)
        {
            json.append(value);
        }
        else if (value instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            json.append(mapToJson(nestedMap));
        }
        else if (value instanceof Object[])
        {
            appendArrayAsJson(json, (Object[]) value);
        }
        else
        {
            json.append("null");
        }
    }
    
    // -------------------------------------------------------------------------
    
    /**
     * Append an array to the JSON string builder
     */
    private void appendArrayAsJson(StringBuilder json, Object[] array)
    {
        json.append("[");
        boolean arrayFirst = true;
        
        for (Object item : array)
        {
            if (!arrayFirst)
            {
                json.append(",");
            }
            arrayFirst = false;
            appendValueAsJson(json, item);
        }
        
        json.append("]");
    }

}
