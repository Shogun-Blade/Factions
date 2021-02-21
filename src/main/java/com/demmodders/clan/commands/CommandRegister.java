package com.demmodders.clan.commands;

import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class CommandRegister {
    public static void RegisterPermissionNodes() {
        // Default
        PermissionAPI.registerNode("clansmod.clans.default", DefaultPermissionLevel.ALL, "Enables a user to use basic clan features, like joining clans, viewing invites");
        PermissionAPI.registerNode("clansmod.clans.manage", DefaultPermissionLevel.ALL, "Enables a user to use clan management commands, like ally, invite, and claim");
        PermissionAPI.registerNode("clansmod.clans.create", DefaultPermissionLevel.ALL, "Enables a user to create a clan of their own");
        PermissionAPI.registerNode("clansmod.clans.info", DefaultPermissionLevel.ALL, "Enables a user to look up info about clans, like a clans info, or a list of all the clans");
        PermissionAPI.registerNode("clansmod.clans.map", DefaultPermissionLevel.ALL, "Enables a user to see a map of the clans around them");
        PermissionAPI.registerNode("clansmod.clans.claim", DefaultPermissionLevel.ALL, "Enables a user to claim land for their clan");
        // Admin
        PermissionAPI.registerNode("clansmod.admin", DefaultPermissionLevel.OP, "Enables a user to use /clanadmin to manage clans");
    }

    public static void RegisterCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new ClanCommand());
        event.registerServerCommand(new CAdminCommand());
    }
}
