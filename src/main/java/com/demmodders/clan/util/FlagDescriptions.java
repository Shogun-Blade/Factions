package com.demmodders.clan.util;

import com.demmodders.clan.Clans;
import com.demmodders.clan.commands.ClanCommandList;
import com.google.gson.Gson;

import java.io.*;
import java.util.HashMap;

public class FlagDescriptions {
    private static HashMap<String, String> playerFlags = null;
    private static HashMap<String, String> adminFlags = null;
    public static File flagFile = null;

    public FlagDescriptions(){}

    /**
     * Loads the flags from the flags file
     */
    private static void loadFlags(){
        try {
            Gson gson = new Gson();
            InputStream json;
            if (flagFile != null) {
                try {
                    json = new FileInputStream(flagFile);
                } catch (FileNotFoundException e) {
                    json = ClanCommandList.class.getClassLoader().getResourceAsStream("JSON/Flags.json");
                    Clans.LOGGER.warn("Failed to find flag file, defaulting to default translation");
                }
            } else {
                json = ClanCommandList.class.getClassLoader().getResourceAsStream("JSON/Flags.json");
            }
            InputStreamReader reader = new InputStreamReader(json);
            Flags items = gson.fromJson(reader, Flags.class);
            playerFlags = new HashMap<>(items.playerFlags);
            adminFlags = new HashMap<>(items.adminFlags);
            json.close();
        } catch (IOException ignored) {

        }
    }

    /**
     * Returns the flags available to players
     * @return A map of flags to their descriptions
     */
    public static HashMap<String, String> getPlayerFlags(){
        if (playerFlags == null){
            loadFlags();
        }
        return playerFlags;
    }

    /**
     * Returns the flags available to admins
     * @return A map of flags to their descriptions
     */
    public static HashMap<String, String> getAdminFlags(){
        if (adminFlags == null){
            loadFlags();
        }
        return adminFlags;
    }
}

class Flags{
    public HashMap<String, String> playerFlags;
    public HashMap<String, String> adminFlags;

    public Flags(){

    }
}