package com.demmodders.clan;

import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.commands.ClanCommandList;
import com.demmodders.clan.commands.CommandRegister;
import com.demmodders.clan.delayedevents.ClanCleanout;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.FlagDescriptions;
import com.demmodders.clan.util.structures.Version;
import com.demmodders.datmoddingapi.delayedexecution.DelayHandler;
import com.google.gson.Gson;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Mod(modid = Clans.MODID, name = Clans.NAME, serverSideOnly = true, version = Clans.VERSION, acceptableRemoteVersions = "*")
public class Clans {
    public static final String MODID = "clans";
    public static final String NAME = "Clan";
    public static final String VERSION = "1.1.0-beta";
    public static final int COMMANDSVERSION = 2;
    public static final int FLAGSVERSION = 1;

    public static final Logger LOGGER = LogManager.getLogger(Clans.MODID);


    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Load Clan data
        ClanManager.getInstance();
        File translationsDir = new File(event.getModConfigurationDirectory(), "/Clan/Translations");
        if (!translationsDir.exists() || !translationsDir.isDirectory()) {
            if (!translationsDir.mkdir()) {
                LOGGER.error("Failed to create translations directory, command description and flag translations won't load");
                return;
            }
        }

        Gson gson = new Gson();
        Version fileVersion;

        try (InputStream file = getClass().getClassLoader().getResourceAsStream("JSON/Commands.json")) {
            File commandList = new File(translationsDir, "Commands.json");
            if (!commandList.exists()) {
                Files.copy(file, commandList.toPath());
                ClanCommandList.commandFile = commandList;
            } else {
                fileVersion = gson.fromJson(new FileReader(commandList), Version.class);
                if(fileVersion.version != COMMANDSVERSION) {
                    LOGGER.warn("Command List file out of date, updated to latest version, this means it will be cleared");
                    commandList.delete();
                    Files.copy(file, commandList.toPath());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create Commands translations file, command description and flag translations won't load");
            return;
        }

        try (InputStream file = getClass().getClassLoader().getResourceAsStream("JSON/Flags.json")) {
            File flagList = new File(translationsDir, "Flags.json");
            if (!flagList.exists()) {
                Files.copy(file, flagList.toPath());
                FlagDescriptions.flagFile = flagList;
            } else {
                fileVersion = gson.fromJson(new FileReader(flagList), Version.class);
                if(fileVersion.version != FLAGSVERSION) {
                    LOGGER.warn("Flag List file out of date, updated to latest version, this means it will be cleared");
                    flagList.delete();
                    Files.copy(file, flagList.toPath());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create flags translations file, command description and flag translations won't load");
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info(Clans.NAME + " says hi!");
        // Register permissions
        CommandRegister.RegisterPermissionNodes();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @EventHandler
    public void serverLoad(FMLServerStartingEvent e){
        // register commands
        CommandRegister.RegisterCommands(e);
        if (ClanConfig.clanSubCat.clearClanPeriod != 0) DelayHandler.addEvent(new ClanCleanout());
    }
}
