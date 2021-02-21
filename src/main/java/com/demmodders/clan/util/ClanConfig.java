package com.demmodders.clan.util;

import com.demmodders.clan.Clans;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Clans.MODID, name = "Dat Clan/Dat Clan")
public class ClanConfig {
    public static Clan clanSubCat = new Clan();
    public static Player playerSubCat = new Player();
    public static Land landSubCat = new Land();
    public static Flags flagSubCat = new Flags();
    public static PowerConfig powerSubCat = new PowerConfig();

    public static class Clan {
        @Config.Name("Max Clan Name Length")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum length of characters in a Clan's name")
        public int maxClanNameLength = 20;

        @Config.Name("Max Clan Description Length")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum length of characters in a Clan's description")
        public int maxClanDescLength = 100;

        @Config.Name("Max Clan MOTD Length")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum length of characters in a Clan's MOTD")
        public int maxClanMOTDLength = 100;

        @Config.Name("MOTD Header")
        @Config.Comment("The text that appears at the top of every MOTD, use %s for the Clan name")
        public String clanMOTDHeader = "_[%s MOTD]_";

        @Config.Name("Clan Land Tag")
        @Config.Comment("The text that is displayed when entering a Clan's land, use %1$s for the Clan name, %2$s for the Clan's name as a possessive (with a 's or ' at the end), and %3$s for the Clan description")
        public String clanLandTag = "Now entering %2$s land - %3$s";

        @Config.Name("Clan Land Tag no description")
        @Config.Comment("The text that is displayed when entering a Clan's land, use %1$s for the Clan name, %2$s for the Clan's name as a possessive (with a 's or ' at the end)")
        public String clanLandTagNoDesc = "Now entering %2$s land";

        @Config.Name("Wild Land Tag")
        @Config.Comment("The text that is displayed when entering the wild, use %1$s for the wild's name, %2$s for the wild's name as a possessive (with a 's or ' at the end), and %3$s for the Wild's description")
        public String wildLandTag = "Now entering %1$s - %3$s";

        @Config.Name("Save Zone Land Tag")
        @Config.Comment("The text that is displayed when entering the SafeZone, use %1$s for the SafeZone's name, %2$s for the SafeZone's name as a possessive (with a 's or ' at the end), and %3$s for the SafeZone's description")
        public String safeLandTag = "Now entering %1$s - %3$s";

        @Config.Name("War Zone Land Tag")
        @Config.Comment("The text that is displayed when entering the WarZone, use %1$s for the WarZone's name, %2$s for the WarZone's name as a possessive (with a 's or ' at the end), and %3$s for the WarZone's description")
        public String warLandTag = "Now entering %1$s - %3$s";

        @Config.Name("Clan Starting Power")
        @Config.RangeInt(min = 1)
        @Config.Comment("The amount of power the Clan starts with when it's created")
        public int clanStartingPower = 100;

        @Config.Name("Clan Starting Max Power")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum amount of power the player can have when they it's created")
        public int clanStartingMaxPower = 100;

        @Config.Name("Allow ally build")
        @Config.Comment("Permit allies to build on each other's land")
        public boolean allyBuild = true;

        @Config.Name("Allow enemy build")
        @Config.Comment("Permit enemies to build on each other's land")
        public boolean enemyBuild = false;

        @Config.Name("Clan Name in Chat")
        @Config.Comment("Display the player's Clan name with their chat messages")
        public boolean chatFName = true;

        @Config.Name("Max Clan Members")
        @Config.RangeInt(min = 0)
        @Config.Comment("The maximum amount of members each Clan is allowed (0 for infinite)")
        public int maxMembers = 0;

        @Config.Name("Restrict Inventory Block Access")
        @Config.Comment("Prevents players from accessing inventory blocks, such as chests, on land they don't own")
        public boolean inventoryBlockRestriction = true;

        @Config.Name("Clear Clan Period")
        @Config.RangeInt(min = 0)
        @Config.Comment("The amount of time in minutes between each time the server checks to delete old Clan, 0 to disable")
        public int clearClanPeriod = 0;

        @Config.Name("Clear Clan Period")
        @Config.RangeInt(min = 0)
        @Config.Comment("The maximum age in minutes a Clan is allowed to be offline before it is removed")
        public int maxClanOffline = 10080;
    }

    public static class Player {
        @Config.Name("Player Starting Power")
        @Config.RangeInt(min = 1)
        @Config.Comment("The amount of power the player starts with when they first join the server")
        public int playerStartingPower = 100;

        @Config.Name("Player Starting Max Power")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum amount of power the player can have when they first join the server")
        public int playerStartingMaxPower = 100;

        @Config.Name("Player Max Power cap")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum amount of power the player can ever have")
        public int playerMaxPowerCap = 1000;

        @Config.Name("Clan home teleport delay")
        @Config.Comment("The delay in seconds before a player teleports when using /Clan home")
        public int teleportDelay = 3;

        @Config.Name("Clan home cooldown")
        @Config.Comment("The delay in seconds before a player can teleport when using /Clan home another time")
        public int reTeleportDelay = 0;

        @Config.Name("Clan map width")
        @Config.RangeInt(min = 0)
        @Config.Comment("How many chunks in the x direction to display to the player with /Clan map (must be odd, else will be +1)")
        public int mapWidth = 41;

        @Config.Name("Clan map height")
        @Config.RangeInt(min = 0)
        @Config.Comment("How many chunks in the y direction to display to the player with /Clan map (must be odd, else will be +1)")
        public int mapHeight = 11;
    }

    public static class PowerConfig {
        @Config.Name("Kill power gain")
        @Config.Comment("The amount a players power recharges by when they kill")
        @Config.RangeInt(min=0)
        public int killPowerGain = 40;

        @Config.Name("Kill max power gain")
        @Config.Comment("The amount a player's maximum power increases by when killing someone")
        @Config.RangeInt(min=0)
        public int killMaxPowerGain = 30;

        @Config.Name("Death power Loss")
        @Config.Comment("The amount of power lost for dying")
        @Config.RangeInt(min=0)
        public int deathPowerLoss = 10;

        @Config.Name("Enemy Power multiplier")
        @Config.RangeDouble(min=0f)
        @Config.Comment("How much more power lost/gained for dying at the hands of/killing an enemy")
        public double enemyMultiplier = 2f;

        @Config.Name("Ally kill multiplier")
        @Config.RangeDouble(min=0f)
        @Config.Comment("How much more power gained for killing an ally")
        public double allyKillMultiplier = -.5f;

        @Config.Name("Killed by ally multiplier")
        @Config.RangeDouble(min=0f)
        @Config.Comment("How much more power lost for being killed by an ally")
        public double killedByAllyMultiplier = .5f;

        @Config.Name("Lieutenant kill/killed Multiplier")
        @Config.RangeDouble(min=0f)
        @Config.Comment("How much extra power is gained/lost for killing/dying as a lieutenant")
        public double lieutenantMultiplier = 1.5f;

        @Config.Name("Officer kill/killed Multiplier")
        @Config.RangeDouble(min=0f)
        @Config.Comment("How much extra power is gained/lost for killing/dying as a officer")
        public double officerMultiplier = 2f;

        @Config.Name("Owner kill/killed Multiplier")
        @Config.RangeDouble(min = 0f)
        @Config.Comment("How much extra power is gained/lost for killing/dying as the owner of a Clan")
        public double ownerMultiplier = 3f;

        @Config.Name("Power gain rate")
        @Config.RangeInt(min=0)
        @Config.Comment("How many seconds in the interval between the player earning power for being on the server")
        public int powerGainInterval = 1800;

        @Config.Name("Power gain amount")
        @Config.RangeInt(min=0)
        @Config.Comment("How much power the player gains after each Power Gain Rate interval")
        public int powerGainAmount = 10;

        @Config.Name("Max Power gain amount")
        @Config.RangeInt(min=0)
        @Config.Comment("How much max power the player gains after each Power Gain Rate interval")
        public int maxPowerGainAmount = 5;

        @Config.Name("Grunt Power Gain Multiplier")
        @Config.RangeDouble(min = 0f)
        @Config.Comment("How much extra power is gained overtime for Clan grunts")
        public double powerGainGruntMultiplier = 0.D;

        @Config.Name("Lieutenant Power Gain Multiplier")
        @Config.RangeDouble(min = 0f)
        @Config.Comment("How much extra power is gained overtime for Clan lieutenants")
        public double powerGainLieutenantMultiplier = 1.5D;

        @Config.Name("Sergeant Power Gain Multiplier")
        @Config.RangeDouble(min = 0f)
        @Config.Comment("How much extra power is gained overtime for Clan sergeants")
        public double powerGainSergeantMultiplier = 1.75D;

        @Config.Name("Owner Power Gain Multiplier")
        @Config.RangeDouble(min = 0f)
        @Config.Comment("How much extra power is gained overtime for Clan owners")
        public double powerGainOwnerMultiplier = 2.25D;
    }

    public static class Land {
        @Config.Name("Land Power worth")
        @Config.RangeInt(min = 1)
        @Config.Comment("The amount of power each chunk takes up when claimed")
        public int landPowerCost = 20;

        @Config.Name("Require land to connect")
        @Config.Comment("Require newly claimed land to be right next to previously claimed land")
        public boolean landRequireConnect = true;

        @Config.Name("Require land to connect when stealing")
        @Config.Comment("Require newly claimed land to be right next to previously claimed land when stealing the land of other Clan")
        public boolean landRequireConnectWhenStealing = false;

        @Config.Name("Max claim radius")
        @Config.RangeInt(min = 1)
        @Config.Comment("The maximum radius the player can enter when making a square claim")
        public int maxClaimRadius = 4;
    }

    public static class Flags {
        @Config.Name("Bonus Power Multiplier")
        @Config.Comment("The multiplier for the amount of power you lose/gain in Clan with the BonusPower tag (Such as the War Zone)")
        public float bonusPowerMultiplier = 1.5f;
    }

    @Mod.EventBusSubscriber(modid = Clans.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void configChanged(final ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(Clans.MODID)) {
                ConfigManager.sync(Clans.MODID, Config.Type.INSTANCE);
            }
        }
    }


}
