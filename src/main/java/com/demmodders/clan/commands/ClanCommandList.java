package com.demmodders.clan.commands;

import com.demmodders.clan.Clans;
import com.google.gson.Gson;

import java.io.*;
import java.util.LinkedHashMap;

public class ClanCommandList {
    private static LinkedHashMap<String, String> commands = null;
    private static LinkedHashMap<String, String> adminCommands = null;

    public static File commandFile = null;

    ClanCommandList() {
    }

    public static LinkedHashMap<String, String> getCommands() {
        if (commands == null) {
            loadCommands();
        }
        return commands;
    }

    public static LinkedHashMap<String, String> getAdminCommands(){
        if (adminCommands == null){
            loadCommands();
        }
        return adminCommands;
    }

    private static void loadCommands(){
        Gson gson = new Gson();
        InputStream json;
        try {
            if (commandFile != null) {
                try {
                    json = new FileInputStream(commandFile);
                } catch (FileNotFoundException e) {
                    json = ClanCommandList.class.getClassLoader().getResourceAsStream("JSON/Commands.json");
                    Clans.LOGGER.warn("Failed to find flag file, defaulting to default translation");
                }
            } else {
                json = ClanCommandList.class.getClassLoader().getResourceAsStream("JSON/Commands.json");
            }

            InputStreamReader reader = new InputStreamReader(json);
            Commands commandsList = gson.fromJson(reader, Commands.class);
            commands = new LinkedHashMap<>(commandsList.commands);
            adminCommands = new LinkedHashMap<>(commandsList.adminCommands);
            json.close();
        } catch (IOException ignored) {
        }
    }
}

class Commands{
    public LinkedHashMap<String, String> commands;
    public LinkedHashMap<String, String> adminCommands;
    Commands(){

    }
}
