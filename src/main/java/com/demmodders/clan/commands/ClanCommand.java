package com.demmodders.clan.commands;

import com.demmodders.clan.clan.Clan;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.delayedevents.ClanTeleport;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.DemUtils;
import com.demmodders.clan.util.FlagDescriptions;
import com.demmodders.clan.util.enums.ClaimType;
import com.demmodders.clan.util.enums.ClanChatMode;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.clan.util.enums.CommandResult;
import com.demmodders.clan.util.structures.ClaimResult;
import com.demmodders.clan.util.structures.UnclaimResult;
import com.demmodders.datmoddingapi.delayedexecution.DelayHandler;
import com.demmodders.datmoddingapi.structures.Location;
import com.demmodders.datmoddingapi.util.DemConstants;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.util.*;

public class ClanCommand extends CommandBase {
    // Map symbols
    final static String[] symbols = {"/", "\\", "#", "?", "!", "%", "$", "&", "*", "£", "[", "]"};
    final static String[][] compass = {
            {"\\", "N", "/"},
            {"W", "+", "E"},
            {"/", "S", "\\"},
    };

    @Override
    public String getName() {
        return "Clan";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        if (sender instanceof EntityPlayerMP) {
            return printHelp(1, true);
        }
        return "Only a player can use these commands";
    }

    @Override
    public List<String> getAliases() {
        ArrayList<String> aliases = new ArrayList<>();
        aliases.add("f");
        return aliases;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        List<String> possibilities = new ArrayList<>();
        ClanManager clanManager = ClanManager.getInstance();
        UUID clanId = clanManager.getPlayersClanID(((EntityPlayerMP) sender).getUniqueID());

        if (args.length == 1) {
            // All commands are possible
            HashMap<String, String> commands = ClanCommandList.getCommands();
            if (commands != null) {
                possibilities = new ArrayList<>(commands.keySet());
                possibilities.add("show");
            }
        } else if (args.length == 2) {
            // Only the the first argument of commands with 1 or more arguments are possible
            switch(args[0].toLowerCase()) {
                // Argument is a page
                case "help":
                    possibilities.add("1");
                    possibilities.add("2");
                    possibilities.add("3");
                    possibilities.add("4");
                    break;
                case "list":
                    for (int i = 1; i <= (int) Math.ceil((float) clanManager.getListOfClansUUIDs().size() / 10); i++) {
                        possibilities.add(String.valueOf(i));
                    }
                    break;

                // Argument is an invite in the player
                case "join":
                case "reject":
                    possibilities = clanManager.getListOfClansNamesFromClanList(clanManager.getPlayer(((EntityPlayerMP) sender).getUniqueID()).invites);
                    break;

                // Argument is a Clan name
                case "info":
                case "show":
                case "neutral":
                case "ally":
                case "enemy":
                    possibilities = clanManager.getListOfClansNames();
                    break;

                // Argument is a member of the Clan
                case "kick":
                case "setrank":
                case "demote":
                case "promote":
                case "setowner":
                    possibilities = clanManager.getClan(clanId).getMemberNames();
                    break;

                // Argument is a currently online player
                case "invite":
                case "uninvite":
                case "rank":
                case "playerinfo":
                case "pinfo":
                    possibilities = Arrays.asList(server.getOnlinePlayerNames());
                    break;

                // Specifics
                case "flag":
                    possibilities.add("set");
                    possibilities.add("remove");
                    break;

                case "chat":
                    possibilities.add("normal");
                    possibilities.add("Clan");
                    possibilities.add("ally");
                    break;

                case "claim":
                    possibilities.add("one");
                    possibilities.add("square");
                    possibilities.add("auto");
                    break;

                case "unclaim":
                    possibilities.add("one");
                    possibilities.add("square");
                    possibilities.add("all");
                    break;
            }
        } else if (args.length == 3) {
            // Only the the second argument of commands with 2 arguments are possible
            switch (args[0].toLowerCase()) {
                case "setrank":
                    possibilities.add("grunt");
                    possibilities.add("lieutenant");
                    possibilities.add("sergeant");
                    break;
                case "flag":
                    possibilities.addAll(FlagDescriptions.getPlayerFlags().keySet());
                    break;
            }
        }
        return getListOfStringsMatchingLastWord(args, possibilities);
    }

    private String printHelp(int Page, boolean dump) throws IndexOutOfBoundsException {
        LinkedHashMap<String, String> commands = ClanCommandList.getCommands();

        // Check the help file was successfully loaded
        if (commands != null) {
            List<String> keyList = new ArrayList<>(commands.keySet());
            StringBuilder helpText = new StringBuilder();
            // Header
            int firstIndex;
            int lastIndex;
            if (dump) {
                firstIndex = 0;
                lastIndex = commands.size();
            } else {
                helpText.append(DemConstants.TextColour.HEADER).append("Showing help page ").append(Page).append(" of ").append((int) Math.ceil(commands.size() / 10f)).append("\n");
                firstIndex = (Page - 1) * 10;
                lastIndex = (10 * Page);
            }


            helpText.append(DemConstants.TextColour.COMMAND).append(keyList.get(firstIndex)).append(DemConstants.TextColour.INFO).append(" - ").append(commands.get(keyList.get(firstIndex)));
            for (int i = firstIndex + 1; i < commands.size() && i < lastIndex; i++) {
                helpText.append("\n").append(DemConstants.TextColour.COMMAND);
                if (dump) helpText.append("/Clan ");
                helpText.append(keyList.get(i)).append(DemConstants.TextColour.INFO).append(" - ").append(commands.get(keyList.get(i)));
            }
            return helpText.toString();
        }
        return DemConstants.TextColour.ERROR + "Could not generate help";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if(!(sender instanceof EntityPlayerMP)) return;

        // Commonly used objects
        ClanManager clanManager = ClanManager.getInstance();
        UUID playerID = ((EntityPlayerMP) sender).getUniqueID();
        UUID clanId = clanManager.getPlayersClanID(playerID);

        CommandResult commandResult = CommandResult.SUCCESS;
        String replyMessage = null;

        if(args.length == 0){
            // If no arguments given, tell the user how to use the command
            sender.sendMessage(new TextComponentString(getUsage(sender)));
        } else {
            // Check for the command
            switch (args[0].toLowerCase()) {
                // Global
                case "help":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.info")) {
                        try {
                            int page = ((args.length == 1) ? 1 : Integer.parseInt(args[1]));
                            replyMessage = printHelp(page, false);
                        } catch (NumberFormatException e) {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan help <page>";
                        } catch (IndexOutOfBoundsException e) {
                            replyMessage = DemConstants.TextColour.INFO + "There aren't that many pages";
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;
                case "list":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.info")) {
                        try {
                            // Get which page to display
                            int page = ((args.length == 1) ? 1 : Integer.parseInt(args[1]));

                            List<UUID> Clan = clanManager.getListOfClansUUIDs();

                            if (Clan.size() > 0) {
                                // Ensure some Clan actually exist
                                StringBuilder factionText = new StringBuilder();
                                // Header
                                factionText.append(DemConstants.TextColour.HEADER).append("Showing Clan page ").append(page).append(" of ").append((int) Math.ceil(Clan.size() / 10f)).append("\n").append(TextFormatting.RESET);

                                int start = (page - 1) * 10;

                                // First Clan, without comma
                                factionText.append(clanManager.getRelationColour(clanId, Clan.get(start))).append(clanManager.getClan(Clan.get(start)).name).append(TextFormatting.RESET);
                                for (int i = (start) + 1; i < Clan.size() && i < ((10 * page)); i++) {
                                    // Highlight green if their own Clan
                                    factionText.append(", ").append(clanManager.getRelationColour(clanId, Clan.get(i))).append(clanManager.getClan(Clan.get(i)).name).append(TextFormatting.RESET);
                                }
                                replyMessage = factionText.toString();

                            } else {
                                replyMessage = DemConstants.TextColour.INFO + "There are no Clan";
                            }

                        } catch (NumberFormatException e){
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan list <page>";
                        } catch (IndexOutOfBoundsException e) {
                            replyMessage = DemConstants.TextColour.INFO + "There aren't that many pages";
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "info":
                case "show":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.info")) {
                        // If they haven't given an argument, show info on their Clan
                        if (args.length == 1) {
                            if (!clanId.equals(ClanManager.WILDID)) {
                                replyMessage = clanManager.getClan(clanId).printClanInfo(clanId);
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "You don't belong to a Clan, you may only look up other Clan";
                            }
                        } else {
                            UUID OtherClan = clanManager.getClanIDFromName(args[1]);
                            if (OtherClan != null) {
                                replyMessage = clanManager.getClan(OtherClan).printClanInfo(clanId);
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "That Clan doesn't exist";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;
                case "playerinfo":
                case "pinfo":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.info")) {
                        // If they haven't given an argument, show info on themselves
                        if (args.length == 1) {
                            replyMessage = clanManager.getPlayer(playerID).printPlayerInfo(clanId);
                        } else {
                            UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (otherPlayer != null) {
                                replyMessage = clanManager.getPlayer(otherPlayer).printPlayerInfo(clanId);
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "That player doesn't exist";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;
                case "rank":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.info")) {
                        // If they haven't given an argument, show info on their Clan
                        if (args.length == 1) {
                            if (!clanId.equals(ClanManager.WILDID)) {
                                replyMessage = DemConstants.TextColour.INFO + "You have the rank of ";
                                replyMessage += ClanRank.getClanRankString(clanManager.getPlayer(playerID).clanRank);

                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "You don't belong to a Clan, you don't have a rank";
                            }
                        } else {
                            UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (otherPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "That player doesn't exist";
                            } else if (!clanId.equals(ClanManager.WILDID)) {
                                replyMessage = DemConstants.TextColour.INFO + args[1] + " has the rank of ";
                                replyMessage += ClanRank.getClanRankString(clanManager.getPlayer(otherPlayer).clanRank);

                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + args[1] + " doesn't belong to a Clan, they don't have a rank";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "map":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.map")) {
                        StringBuilder message = new StringBuilder();

                        // Map for Clan -> symbol
                        HashMap<UUID, String> symbolMap = new HashMap<>();

                        // Determine start corner and end corner
                        int startX = ((EntityPlayerMP) sender).chunkCoordX - (ClanConfig.playerSubCat.mapWidth / 2);
                        int endX = ((EntityPlayerMP) sender).chunkCoordX + (ClanConfig.playerSubCat.mapWidth / 2);

                        int startZ = ((EntityPlayerMP) sender).chunkCoordZ - (ClanConfig.playerSubCat.mapHeight / 2);
                        int endZ = ((EntityPlayerMP) sender).chunkCoordZ + (ClanConfig.playerSubCat.mapHeight / 2);

                        float yaw = ((EntityPlayerMP) sender).rotationYaw;

                        if (yaw < 0) yaw += 360;

                        int dirX = 0, dirY = 0;

                        if (yaw < 66.5 || yaw > 292.5) {
                            dirY = 2;
                        } else if (yaw < 112.5 || yaw > 247.5) {
                            dirY = 1;
                        } else {
                            dirY = 0;
                        }

                        if (yaw > 22.5 && yaw < 157.5) {
                            dirX = 0;
                        } else if (yaw > 202.5 && yaw < 337.5) {
                            dirX = 2;
                        } else {
                            dirX = 1;
                        }

                        int compassX = 0, compassY = 0;

                        // Iterate over all the chunks within those coords
                        for (int i = startZ; i <= endZ; i++) {
                            for (int j = startX; j <= endX; j++) {
                                // Check if the chunk is owned
                                UUID theClan = clanManager.getChunkOwningFaction(((EntityPlayerMP) sender).dimension, j, i);
                                message.append(TextFormatting.RESET);
                                if (compassY < 3 && compassX < 3) {
                                    message.append(compassX == dirX && compassY == dirY ? DemConstants.TextColour.COMMAND : DemConstants.TextColour.HEADER);
                                    message.append(compass[compassY][compassX]);
                                }
                                // If its the same coord as the player's coord, display the centre symbol
                                else if (i == ((EntityPlayerMP) sender).chunkCoordZ && j == ((EntityPlayerMP) sender).chunkCoordX)
                                    message.append(TextFormatting.BLUE).append("+");

                                    // If the chunk is owned, mark it
                                else if (!theClan.equals(ClanManager.WILDID) && !clanManager.getClan(theClan).hasFlag("uncharted")) {
                                    if (!symbolMap.containsKey(theClan))
                                        symbolMap.put(theClan, symbols[symbolMap.size() % symbols.length]);

                                    message.append(clanManager.getRelationColour(clanId, theClan)).append(symbolMap.get(theClan));
                                }

                                // Otherwise place a dash
                                else message.append("-");

                                compassX += 1;
                            }
                            compassX = 0;
                            compassY += 1;

                            // Go to the next line
                            message.append("\n");
                        }

                        // Display symbol mapping
                        message.append("-: Wild");

                        for (UUID theClan : symbolMap.keySet()) {
                            message.append(TextFormatting.RESET).append(", ").append(clanManager.getRelationColour(clanId, theClan)).append(symbolMap.get(theClan)).append(": ").append(clanManager.getClan(theClan).name);
                        }

                        replyMessage = message.toString();
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                // No Clan
                case "join":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        if (args.length == 1) {
                            // Make sure they give a Clan name
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan join <Clan name>";
                        } else {
                            // Make sure they're not in a Clan
                            if (clanId.equals(ClanManager.WILDID)) {

                                // Make sure they gave a valid Clan
                                clanId = clanManager.getClanIDFromName(args[1].toLowerCase());
                                if (clanId != null) {

                                    // Check this player can actually join the Clan
                                    if (clanManager.canAddPlayerToClan(playerID, clanId)) {
                                        clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + sender.getName() + " has just joined your Clan"));
                                        clanManager.setPlayerClan(playerID, clanId, true);
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully joined " + ClanConstants.TextColour.OWN + clanManager.getClan(clanId).name;
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "You're not invited to that Clan";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "That Clan doesn't exist";
                                }
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "You can't join a Clan while you're a member of a different Clan, you must leave your current one first";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "invites":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        ArrayList<UUID> invites = clanManager.getPlayer(playerID).invites;
                        // Ensure they have some invites
                        if (invites.size() > 0) {
                            int page = ((args.length == 1) ? 1 : Integer.parseInt(args[1]));
                            StringBuilder inviteText = new StringBuilder();
                            // Header
                            inviteText.append(DemConstants.TextColour.HEADER).append("Showing invites page ").append(page).append(" of ").append((int) Math.ceil(invites.size() / 10f)).append("\n").append(TextFormatting.RESET);

                            int start = (page - 1) * 10;

                            // First Clan, without comma
                            inviteText.append(clanManager.getClan(invites.get(start)).name);
                            for (int i = (start) + 1; i < invites.size() && i < ((10 * page)); i++) {
                                inviteText.append(", ").append(clanManager.getClan(invites.get(i)).name);
                            }
                            replyMessage = inviteText.toString();

                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "You don't have any invites";
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "reject":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Make sure the player has entered a Clan name
                        if (args.length == 1) {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan reject <Clan name>";
                        } else {
                            // Check there's a Clan with that name
                            clanId = clanManager.getClanIDFromName(args[1]);
                            if (clanId != null && clanManager.getPlayer(playerID).hasInvite(clanId)) {
                                // Remove the invite, notify everyone involved
                                clanManager.removePlayerInvite(playerID, clanId);
                                clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + sender.getName() + " has rejected your invite"));
                                replyMessage = DemConstants.TextColour.INFO + "You have successfully rejected your invite from " + args[1];
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "You don't have an invite from that Clan";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "create":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.create")) {
                        // Make sure they give a name
                        if (args.length == 1) {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan create <name>";
                        } else {
                            // Make sure they're not already in the Clan
                            if (ClanManager.WILDID.equals(clanManager.getPlayer(playerID).Clan)) {
                                int result = clanManager.createClan(args[1], playerID);
                                // Make sure the name is valid
                                switch (result) {
                                    case 0:
                                        replyMessage = DemConstants.TextColour.INFO + "Clan " + ClanConstants.TextColour.OWN + args[1] + DemConstants.TextColour.INFO + " successfully created, add a description with " + DemConstants.TextColour.COMMAND + "/Clan desc <Description>, " + DemConstants.TextColour.INFO + "and invite players with " + DemConstants.TextColour.COMMAND + "/Clan invite <Player>";
                                        break;
                                    case 1:
                                        replyMessage = DemConstants.TextColour.ERROR + "That name is too long";
                                        break;
                                    case 2:
                                        replyMessage = DemConstants.TextColour.ERROR + "That name is too short";
                                        break;
                                    case 3:
                                        replyMessage = DemConstants.TextColour.ERROR + "A Clan with that name already exists";
                                        break;
                                    case 4:
                                        replyMessage = DemConstants.TextColour.ERROR + "Failed to create Clan";
                                        break;
                                }
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "You cannot create a Clan while you're in a Clan";
                            }
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                // Clan member
                case "home":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Make sure they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Make sure they have a Clan home
                            if (clanManager.getClan(clanId).homePos != null) {
                                // Make sure they're teleport cooldown has passed
                                int age = (int) (DemUtils.calculateAge(clanManager.getPlayer(playerID).lastTeleport) / 1000);

                                // Create a delayed event to teleport the player
                                if (age > ClanConfig.playerSubCat.reTeleportDelay) {
                                    EntityPlayerMP playerMP = (EntityPlayerMP) sender;
                                    DelayHandler.addEvent(new ClanTeleport(clanManager.getClan(clanId).homePos, playerMP, ClanConfig.playerSubCat.teleportDelay));
                                    replyMessage = DemConstants.TextColour.INFO + "Teleporting in " + ClanConfig.playerSubCat.teleportDelay + " Seconds";
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "You must wait " + (ClanConfig.playerSubCat.reTeleportDelay - age) + " more seconds before you can do that again";
                                }
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "Your Clan doesn't have a home";
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "members":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Make sure they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            StringBuilder message = new StringBuilder();

                            List<UUID> grunts = new ArrayList<>();
                            List<UUID> lieutenants = new ArrayList<>();
                            List<UUID> officers = new ArrayList<>();
                            UUID owner = new UUID(0L, 0L);

                            for (UUID player : clanManager.getClan(clanId).members) {
                                switch (clanManager.getPlayer(player).clanRank) {
                                    case GRUNT:
                                        grunts.add(player);
                                        break;
                                    case LIEUTENANT:
                                        lieutenants.add(player);
                                        break;
                                    case OFFICER:
                                        officers.add(player);
                                        break;
                                    case OWNER:
                                        owner = player;
                                        break;
                                }
                            }

                            // Owner
                            message.append(DemConstants.TextColour.INFO).append("Owner: ").append(clanManager.getPlayerStatusColour(owner, true)).append(clanManager.getPlayer(owner).lastKnownName);

                            // Officers
                            if (!officers.isEmpty()) {
                                message.append("\n").append(DemConstants.TextColour.INFO).append("Officers: ").append("\n");
                                message.append(clanManager.getPlayerStatusColour(officers.get(0), true)).append(clanManager.getPlayer(officers.get(0)).lastKnownName);
                                for (int i = 1; i < officers.size(); i++) {
                                    message.append(DemConstants.TextColour.INFO).append(", ").append(clanManager.getPlayerStatusColour(officers.get(0), true)).append(clanManager.getPlayer(officers.get(i)).lastKnownName);
                                }
                            }

                            // Lieutenants
                            if (!lieutenants.isEmpty()) {
                                message.append("\n").append(DemConstants.TextColour.INFO).append("Lieutenants: ").append("\n");
                                message.append(clanManager.getPlayerStatusColour(lieutenants.get(0), true)).append(clanManager.getPlayer(lieutenants.get(0)).lastKnownName);
                                for (int i = 1; i < lieutenants.size(); i++) {
                                    message.append(DemConstants.TextColour.INFO).append(", ").append(clanManager.getPlayerStatusColour(lieutenants.get(0), true)).append(clanManager.getPlayer(lieutenants.get(i)).lastKnownName);
                                }
                            }

                            // Grunts
                            if (!grunts.isEmpty()) {
                                message.append("\n").append(DemConstants.TextColour.INFO).append("Grunts: ").append("\n");
                                message.append(clanManager.getPlayerStatusColour(grunts.get(0), true)).append(clanManager.getPlayer(grunts.get(0)).lastKnownName);
                                for (int i = 1; i < grunts.size(); i++) {
                                    message.append(DemConstants.TextColour.INFO).append(", ").append(clanManager.getPlayerStatusColour(grunts.get(0), true)).append(clanManager.getPlayer(grunts.get(i)).lastKnownName);
                                }
                            }

                            replyMessage = message.toString();

                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "leave":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Ensure they're not the owner
                            if (clanManager.getPlayer(playerID).clanRank == ClanRank.OWNER) {
                                replyMessage = DemConstants.TextColour.ERROR + "You are the leader of this Clan, you must disband your Clan or pass on your status as the owner";
                            } else {
                                clanManager.setPlayerClan(playerID, ClanManager.WILDID, true);
                                replyMessage = DemConstants.TextColour.INFO + "You have successfully left your Clan";
                                clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + clanManager.getPlayer(playerID).lastKnownName + " has left the Clan"));
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "motd":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            Clan clan = clanManager.getClan(clanId);
                            replyMessage = DemConstants.TextColour.HEADER + String.format(ClanConfig.clanSubCat.clanMOTDHeader, clan.name) + "\n" + clan.motd;
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "chat":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.default")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (args.length == 1) {
                                replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan chat normal|Clan|ally";
                            } else {
                                try {
                                    clanManager.getPlayer(playerID).clanChat = ClanChatMode.valueOf(args[1].toUpperCase());
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully set chat mode to " + args[1];
                                } catch (IllegalArgumentException e) {
                                    replyMessage = DemConstants.TextColour.ERROR + "Unknown chat mode, available chat modes are normal, Clan, and ally";
                                }
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                // Clan Lieutenant
                case "claim":
                    // TODO: Repair
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Make sure they're the correct rank
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.LIEUTENANT.ordinal()) {
                                // Work out claim type
                                if (args.length > 1) {
                                    ClaimResult result = null;

                                    replyMessage = "";

                                    switch (args[1]) {
                                        case "auto":
                                            clanManager.getPlayer(playerID).autoClaim = !clanManager.getPlayer(playerID).autoClaim;
                                            if (clanManager.getPlayer(playerID).autoClaim) {
                                                replyMessage = DemConstants.TextColour.INFO + "Enabled auto claim\n";
                                            } else {
                                                replyMessage = DemConstants.TextColour.INFO + "Disabled auto claim";
                                                break;
                                            }
                                        case "one":
                                            result = clanManager.claimLand(clanId, playerID, ((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ, true);
                                            break;
                                        case "square":
                                            if (args.length > 2) {
                                                try {
                                                    result = clanManager.squareClaimLand(clanId, playerID, ((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ, parseInt(args[2]));
                                                } catch (NumberInvalidException e) {
                                                    replyMessage = DemConstants.TextColour.ERROR + "The claim radius must be a whole number";
                                                }
                                            } else {
                                                replyMessage = DemConstants.TextColour.ERROR + "Missing argument " + DemConstants.TextColour.COMMAND + "radius";
                                            }
                                            break;
                                        default:
                                            replyMessage = DemConstants.TextColour.ERROR + "Unknown claim method, available types are: " + DemConstants.TextColour.COMMAND + "one|square|auto";
                                    }
                                    if (result != null) {
                                        switch (result.result) {
                                            case SUCCESS:
                                                replyMessage += DemConstants.TextColour.INFO + "Successfully claimed " + (result.count == 1 ? "a chunk" : result.count + " chunks") + " for your Clan";
                                                break;
                                            case STOLEN:
                                                replyMessage += DemConstants.TextColour.INFO + "Successfully stolen " + (result.count == 1 ? "a chunk" : result.count + " chunks") + " for your Clan from " + clanManager.getRelationColour(clanId, result.owner) + clanManager.getClan(result.owner).name;
                                                break;
                                            case LACKPOWER:
                                                replyMessage += DemConstants.TextColour.ERROR + "You do not have enough power to claim this land";
                                                break;
                                            case OWNED:
                                                replyMessage += clanManager.getRelationColour(clanId, result.owner) + clanManager.getClan(result.owner).name + DemConstants.TextColour.ERROR + " Owns that land and has enough power to keep it";
                                                break;
                                            case YOUOWN:
                                                replyMessage += DemConstants.TextColour.INFO + "You already own that land";
                                                break;
                                            case MUSTCONNECT:
                                                replyMessage += DemConstants.TextColour.ERROR + "You can only claim land that connects to land you already own";
                                                break;
                                            case NAH:
                                                replyMessage += DemConstants.TextColour.ERROR + "You're unable to claim this land";
                                                break;
                                        }
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan claim one|auto" + DemConstants.TextColour.INFO + " or " + DemConstants.TextColour.COMMAND + "/Clan claim square [radius]";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "unclaim":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Make sure they're the correct rank
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.LIEUTENANT.ordinal()) {
                                // Work out unclaim type
                                if (args.length > 1) {
                                    UnclaimResult result = null;
                                    switch (args[1]) {
                                        case "one":
                                            result = clanManager.unClaimLand(clanId, playerID, ((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ, ClaimType.ONE, 1);
                                            break;
                                        case "square":
                                            if (args.length > 2) {
                                                try {
                                                    result = clanManager.unClaimLand(clanId, playerID, ((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ, ClaimType.SQUARE, parseInt(args[2]));
                                                } catch (NumberInvalidException e) {
                                                    replyMessage = DemConstants.TextColour.ERROR + "The radius must be a whole number";
                                                }
                                            } else {
                                                replyMessage = DemConstants.TextColour.ERROR + "Missing argument " + DemConstants.TextColour.COMMAND + "radius";
                                            }
                                            break;
                                        case "all":
                                            if (args.length > 2 && args[2].equalsIgnoreCase(clanManager.getClan(clanId).name)) {
                                                result = clanManager.unClaimLand(clanId, playerID, ((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ, ClaimType.ALL, 0);
                                            } else {
                                                replyMessage = DemConstants.TextColour.ERROR + "Are you sure you want to relinquish all your Clan's land? Type: " + DemConstants.TextColour.COMMAND + "/Clan unclaim all " + clanManager.getClan(clanId).name;
                                            }
                                            break;
                                        default:
                                            replyMessage = DemConstants.TextColour.ERROR + "Unknown unclaim method, available types are: " + DemConstants.TextColour.COMMAND + "one|square|all";
                                    }

                                    if (result != null) {
                                        switch (result.result) {
                                            case SUCCESS:
                                                replyMessage = DemConstants.TextColour.INFO + "Successfully unclaimed " + (result.count == 1 ? "a chunk" : result.count + " chunks");
                                                break;
                                            case NOLAND:
                                                replyMessage = DemConstants.TextColour.ERROR + "There isn't any land to unclaim";
                                                break;
                                            case NAH:
                                                replyMessage = DemConstants.TextColour.ERROR + "Failed to unclaim land";
                                                break;
                                        }
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan unclaim one|all" + DemConstants.TextColour.INFO + " or " + DemConstants.TextColour.COMMAND + "/Clan unclaim square [radius]";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                // Clan Officer
                case "ally":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        // Check they're in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Check they have a high enough rank
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID OtherClan = clanManager.getClanIDFromName(args[1]);
                                    // Check the other Clan exists
                                    if (OtherClan != null) {
                                        int result = clanManager.addAlly(clanId, OtherClan, playerID);
                                        switch (result) {
                                            //TODO: Replace with string building kinda thing
                                            case 0:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ALLY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now you ally" + (ClanConfig.clanSubCat.allyBuild ? ", this means they can build on your land, but you can't build on theirs till they add you as an ally as well" : "")));
                                                break;
                                            case 1:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ALLY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now your mutual ally" + (ClanConfig.clanSubCat.allyBuild ? ", this means you can build on their land, and they can build on yours too" : "")));
                                                break;
                                            case 2:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ALLY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now you ally" + (ClanConfig.clanSubCat.allyBuild ? ", this means you can build on their land, and they can build on yours too" : "") + ClanConstants.TextColour.ENEMY + ", however, they still regard you as an enemy"));
                                                break;
                                            case 3:
                                                replyMessage = DemConstants.TextColour.ERROR + "That Clan is already an ally";
                                                break;
                                            case 4:
                                                replyMessage = DemConstants.TextColour.ERROR + "That's your Clan";
                                                break;
                                            case 5:
                                                replyMessage = DemConstants.TextColour.ERROR + "You cannot add that Clan as an ally";
                                                break;
                                            case 6:
                                                replyMessage = DemConstants.TextColour.ERROR + "Failed to add that Clan as an ally";
                                                break;
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "That Clan does not exist";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan ally <Clan name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "enemy":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        // Check the player is in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Check the player has permission in their Clan
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                // Check the player gives an an argument
                                if (args.length > 1) {
                                    UUID OtherClan = clanManager.getClanIDFromName(args[1]);

                                    // Check the other Clan exists
                                    if (OtherClan != null) {
                                        int result = clanManager.addEnemy(clanId, OtherClan, playerID);
                                        switch (result) {
                                            //TODO: Replace with string building kinda thing
                                            case 0:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ENEMY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now you enemy" + (ClanConfig.clanSubCat.enemyBuild ? ", this means you can build on their land, and they can build on yours too" : "") + ", they don't regard you as an enemy yet though"));
                                                break;
                                            case 1:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ENEMY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now your mutual enemy" + (ClanConfig.clanSubCat.enemyBuild ? ", this means you can build on their land, and they can build on yours too" : "")));
                                                break;
                                            case 2:
                                                clanManager.sendClanwideMessage(clanId, new TextComponentString(ClanConstants.TextColour.ENEMY + clanManager.getClan(OtherClan).name + DemConstants.TextColour.INFO + " is now your enemy" + (ClanConfig.clanSubCat.enemyBuild ? ", this means you can build on their land, and they can build on yours too" : "") + ClanConstants.TextColour.ALLY + ", however, they still regard you as an ally"));
                                                break;
                                            case 3:
                                                replyMessage = DemConstants.TextColour.ERROR + "That Clan is already an enemy";
                                                break;
                                            case 4:
                                                replyMessage = DemConstants.TextColour.ERROR + "That's your Clan";
                                                break;
                                            case 5:
                                                replyMessage = DemConstants.TextColour.ERROR + "You cannot add that Clan as an enemy";
                                                break;
                                            case 6:
                                                replyMessage = DemConstants.TextColour.ERROR + "Failed to add that Clan as an enemy";
                                                break;
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.INFO + "That Clan does not exist";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan enemy <Clan name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "neutral":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        // Check the player is in a Clan
                        if (!clanId.equals(ClanManager.WILDID)) {
                            // Check the player has permission in their Clan
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                // Check the player gives an argument
                                if (args.length > 1) {
                                    UUID OtherClan = clanManager.getClanIDFromName(args[1]);

                                    int result = clanManager.addNeutral(clanId, OtherClan, playerID);
                                    switch (result) {
                                        //TODO: Replace with string building kinda thing
                                        case 0:
                                            clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + "You no longer have any relations with " + clanManager.getClan(OtherClan).name));
                                            break;
                                        case 1:
                                            clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + "You're no longer regard " + clanManager.getClan(OtherClan).name + " as an enemy"));
                                            break;
                                        case 2:
                                            clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + "You no longer regard " + clanManager.getClan(OtherClan).name + " as an ally"));
                                            break;
                                        case 3:
                                        case 4:
                                            replyMessage = DemConstants.TextColour.ERROR + "You do not have relation with that Clan";
                                            break;
                                        case 5:
                                            replyMessage = DemConstants.TextColour.ERROR + "That's your Clan";
                                            break;
                                        case 6:
                                            replyMessage = DemConstants.TextColour.ERROR + "Failed to set that Clan as neutral";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan neutral <Clan name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "sethome":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                boolean result = clanManager.setClanHome(clanId, new Location(((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).posX, ((EntityPlayerMP) sender).posY, ((EntityPlayerMP) sender).posZ, ((EntityPlayerMP) sender).rotationPitch, ((EntityPlayerMP) sender).rotationYaw));
                                if (result)
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully set Clan home, you and your members can travel to it with " + DemConstants.TextColour.COMMAND + "/Clan home";
                                else
                                    replyMessage = DemConstants.TextColour.ERROR + "Unable to set Clan home, you don't own this land";
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "kick":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer == null || !clanManager.getPlayer(otherPlayer).Clan.equals(clanId)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is not in your Clan";
                                    } else if (otherPlayer.equals(playerID)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "If you want to leave the Clan, use  " + DemConstants.TextColour.COMMAND + "/Clan leave";
                                    } else if (clanManager.getPlayer(otherPlayer).clanRank == ClanRank.OWNER) {
                                        replyMessage = DemConstants.TextColour.ERROR + "You cannot kick the owner of the Clan";
                                    } else {
                                        clanManager.setPlayerClan(otherPlayer, ClanManager.WILDID, true);
                                        clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + clanManager.getPlayer(otherPlayer).lastKnownName + " has been kicked from the Clan"));
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan kick <member name>";
                                }
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "invite":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer != null) {
                                        if (otherPlayer != playerID) {
                                            switch (clanManager.invitePlayerToClan(otherPlayer, clanId)) {
                                                case 0:
                                                    replyMessage = DemConstants.TextColour.INFO + args[1] + " was successfully invited to the Clan";
                                                    break;
                                                case 1:
                                                    replyMessage = DemConstants.TextColour.ERROR + args[1] + " already has an invite from you";
                                                    break;
                                                case 2:
                                                    replyMessage = DemConstants.TextColour.ERROR + args[1] + " is a member of your Clan";
                                            }
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + "That's you";
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "The Clan system doesn't know who that is, they must have joined the server before they can be invited to a Clan";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan invite <player name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "uninvite":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer != null) {
                                        if (clanManager.removePlayerInvite(otherPlayer, clanId)) {
                                            replyMessage = DemConstants.TextColour.INFO + "Successfully removed invite to " + args[1];
                                            clanManager.sendMessageToPlayer(otherPlayer, DemConstants.TextColour.INFO + clanManager.getClan(clanId).name + " Has revoked your invite to join their Clan");
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + args[1] + " doesn't have an invite from you";
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "The Clan system doesn't know who that is, they must have joined the server before they can be invited to a Clan";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan uninvite <player name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "setmotd":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    StringBuilder mOTD = new StringBuilder();
                                    for (int i = 1; i < args.length; i++) {
                                        mOTD.append(args[i]).append(" ");
                                    }
                                    if (mOTD.toString().length() <= ClanConfig.clanSubCat.maxClanMOTDLength) {
                                        clanManager.getClan(clanId).motd = mOTD.toString();
                                        clanManager.saveClan(clanId);
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully set MOTD to " + mOTD.toString();
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "That MOTD is too long";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan setmotd <MOTD>";

                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "promote":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer == null || !clanManager.getPlayer(otherPlayer).Clan.equals(clanId)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is not in your Clan";
                                    } else if (otherPlayer.equals(playerID)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "You can't promote yourself";
                                    } else {
                                        switch (clanManager.getPlayer(otherPlayer).clanRank) {
                                            case GRUNT:
                                                clanManager.setPlayerRank(otherPlayer, ClanRank.LIEUTENANT);
                                                replyMessage = DemConstants.TextColour.INFO + "Promoted " + args[1] + " to Lieutenant";

                                                clanManager.sendMessageToPlayer(otherPlayer, DemConstants.TextColour.INFO + "You were promoted to Lieutenant");
                                                break;
                                            case LIEUTENANT:
                                                if (clanManager.getPlayer(playerID).clanRank == ClanRank.OWNER) {
                                                    replyMessage = DemConstants.TextColour.ERROR + "Only the owner of the Clan can promote members to Officers";
                                                } else {
                                                    clanManager.setPlayerRank(otherPlayer, ClanRank.OFFICER);
                                                    replyMessage = DemConstants.TextColour.INFO + "Promoted " + args[1] + " to Officer";
                                                    clanManager.sendMessageToPlayer(otherPlayer, DemConstants.TextColour.INFO + "You were promoted to Officer");
                                                }
                                                break;
                                            case OFFICER:
                                                replyMessage = DemConstants.TextColour.ERROR + "That player cannot be promoted anymore";
                                                break;
                                            case OWNER:
                                                replyMessage = DemConstants.TextColour.ERROR + "That player is the maximum rank possible";
                                        }
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan promote <member name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "demote":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OFFICER.ordinal()) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer == null || !clanManager.getPlayer(otherPlayer).Clan.equals(clanId)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is not in your Clan";
                                    } else if (otherPlayer.equals(playerID)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "You can't demote yourself";
                                    } else {
                                        switch (clanManager.getPlayer(otherPlayer).clanRank) {
                                            case GRUNT:
                                                replyMessage = DemConstants.TextColour.ERROR + "That player is the minimum rank possible";
                                                break;
                                            case LIEUTENANT:
                                                clanManager.setPlayerRank(otherPlayer, ClanRank.GRUNT);
                                                replyMessage = DemConstants.TextColour.INFO + "Demoted " + args[1] + " to Grunt";
                                                clanManager.sendMessageToPlayer(otherPlayer, DemConstants.TextColour.INFO + "You were demoted to Grunt");
                                                break;
                                            case OFFICER:
                                                if (clanManager.getPlayer(playerID).clanRank == ClanRank.OWNER) {
                                                    replyMessage = DemConstants.TextColour.ERROR + "You cannot demote other Officers";
                                                } else {
                                                    clanManager.setPlayerRank(otherPlayer, ClanRank.LIEUTENANT);
                                                    replyMessage = DemConstants.TextColour.INFO + "Demoted " + args[1] + " to Lieutenant";

                                                    clanManager.sendMessageToPlayer(otherPlayer, DemConstants.TextColour.INFO + "You were demoted to Lieutenant");
                                                }
                                                break;
                                            case OWNER:
                                                replyMessage = DemConstants.TextColour.ERROR + "You cannot demote the Owner";
                                        }
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan demote <member name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                // Clan Owner
                case "disband":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OWNER.ordinal()) {
                                if (args.length == 1) {
                                    replyMessage = DemConstants.TextColour.ERROR + "Are you sure? type " + DemConstants.TextColour.COMMAND + "/Clan disband " + clanManager.getClan(clanId).name;
                                } else {
                                    String ClanName = clanManager.getClan(clanId).name;
                                    if (args[1].equals(ClanName)) {
                                        if (clanManager.disbandClan(clanId, playerID)) {
                                            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(new TextComponentString(DemConstants.TextColour.INFO + ClanName + " has been disbanded"));
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + "Failed to disband Clan";
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "Failed to disband Clan, to disband your Clan type " + DemConstants.TextColour.COMMAND + "/Clan disband " + clanManager.getClan(clanId).name;
                                    }
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "setrank":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OWNER.ordinal()) {
                                if (args.length > 2) {
                                    try {
                                        UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                        ClanRank rank = ClanRank.valueOf(args[2].toUpperCase());

                                        if (otherPlayer == null || !clanManager.getPlayer(otherPlayer).Clan.equals(clanId)) {
                                            replyMessage = DemConstants.TextColour.ERROR + "That player is not in your Clan";
                                        } else if (otherPlayer.equals(playerID)) {
                                            replyMessage = DemConstants.TextColour.ERROR + "You cannot set your own rank";
                                        } else if (clanManager.getPlayer(otherPlayer).clanRank == ClanRank.OFFICER) {
                                            replyMessage = DemConstants.TextColour.ERROR + "You cannot change the rank of the owner";
                                        } else if (clanManager.getPlayer(otherPlayer).clanRank.ordinal() >= clanManager.getPlayer(playerID).clanRank.ordinal()) {
                                            replyMessage = DemConstants.TextColour.ERROR + "You cannot change the rank of a player of the same rank or higher than you";
                                        } else if (rank == ClanRank.OWNER) {
                                            replyMessage = DemConstants.TextColour.ERROR + "To set a player as the owner, use " + DemConstants.TextColour.COMMAND + "/Clan setowner <player>";
                                        } else {
                                            clanManager.setPlayerRank(otherPlayer, rank);
                                            replyMessage = DemConstants.TextColour.INFO + "Set " + args[1] + " to " + rank.toString().toLowerCase();
                                        }
                                    } catch (IllegalArgumentException e){
                                        replyMessage = DemConstants.TextColour.ERROR + "Unknown rank, available ranks are: grunt, lieutenant, and officer";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan setrank <member name> <new rank>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "rename":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OWNER.ordinal()) {
                                if (args.length > 2) {
                                    String name = args[1];
                                    int rc = clanManager.setClanName(playerID, clanId, name);
                                    switch (rc) {
                                        case 0:
                                            replyMessage = DemConstants.TextColour.INFO + "Successfully changed your Clan's name to " + ClanConstants.TextColour.OWN + clanManager.getClan(clanId).name;
                                            break;
                                        case 1:
                                            replyMessage = DemConstants.TextColour.ERROR + "That name is too long";
                                            break;
                                        case 2:
                                            replyMessage = DemConstants.TextColour.ERROR + "That name is too short";
                                            break;
                                        case 3:
                                            replyMessage = DemConstants.TextColour.ERROR + "Failed to change Clan name";
                                            break;
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan rename <name>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "setowner":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank == ClanRank.OWNER) {
                                if (args.length > 1) {
                                    UUID otherPlayer = clanManager.getPlayerIDFromName(args[1]);
                                    if (otherPlayer == null || !clanManager.getPlayer(otherPlayer).Clan.equals(clanId)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is not in your Clan";
                                    } else if (otherPlayer.equals(playerID)) {
                                        replyMessage = DemConstants.TextColour.ERROR + "You are already the owner";
                                    } else {
                                        clanManager.setPlayerRank(playerID, ClanRank.OFFICER);
                                        clanManager.setPlayerRank(otherPlayer, ClanRank.OWNER);
                                        clanManager.sendClanwideMessage(clanId, new TextComponentString(DemConstants.TextColour.INFO + clanManager.getPlayer(otherPlayer).lastKnownName + " is now the new leader of " + ClanConstants.TextColour.OWN + clanManager.getClan(clanId).name));
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan setowner <member name>";
                                }
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "flag":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OWNER.ordinal()) {
                                if (args.length > 2) {
                                    HashMap<String, String> flags = FlagDescriptions.getPlayerFlags();
                                    if (FlagDescriptions.getPlayerFlags().containsKey(args[2].toLowerCase())) {
                                        if (args[1].equals("set")) {
                                            if (!clanManager.getClan(clanId).hasFlag(args[2].toLowerCase())) {
                                                clanManager.getClan(clanId).setFlag(args[2].toLowerCase());
                                                clanManager.saveClan(clanId);
                                                replyMessage = DemConstants.TextColour.INFO + "Successfully set flag";
                                            } else {
                                                replyMessage = DemConstants.TextColour.ERROR + "Your Clan already has that flag set";
                                            }
                                        } else if (args[1].equals("remove")) {
                                            if (clanManager.getClan(clanId).hasFlag(args[2].toLowerCase())) {
                                                clanManager.getClan(clanId).removeFlag(args[2].toLowerCase());
                                                clanManager.saveClan(clanId);
                                                replyMessage = DemConstants.TextColour.INFO + "Successfully removed flag";
                                            } else {
                                                replyMessage = DemConstants.TextColour.ERROR + "Your Clan doesn't have that flag set";
                                            }
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + "Unknown flag operation, correct operations are: set, remove";
                                        }
                                    } else {
                                        StringBuilder flagsMessage = new StringBuilder();
                                        flagsMessage.append(DemConstants.TextColour.ERROR).append("Unknown flag, available flags are:");
                                        for(String flag : flags.keySet()){
                                            flagsMessage.append("\n").append(DemConstants.TextColour.INFO).append(flag).append(" - ").append(flags.get(flag));
                                        }
                                        replyMessage = flagsMessage.toString();
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan flag set|remove <flag>";

                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;

                case "desc":
                    if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "demfactions.Clan.manage")) {
                        if (!clanId.equals(ClanManager.WILDID)) {
                            if (clanManager.getPlayer(playerID).clanRank.ordinal() >= ClanRank.OWNER.ordinal()) {
                                if (args.length > 1) {
                                    StringBuilder desc = new StringBuilder();
                                    for (int i = 1; i < args.length; i++) {
                                        desc.append(args[i]).append(" ");
                                    }
                                    if (desc.toString().length() <= ClanConfig.clanSubCat.maxClanDescLength) {
                                        clanManager.getClan(clanId).desc = desc.toString();
                                        clanManager.saveClan(clanId);
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully set description to " + desc.toString();
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "That description is too long";
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/Clan desc <description>";
                                }
                            } else {
                                commandResult = CommandResult.NOCLANPERMISSION;
                            }
                        } else {
                            commandResult = CommandResult.NOCLAN;
                        }
                    } else {
                        commandResult = CommandResult.NOPERMISSION;
                    }
                    break;
                default:
                    replyMessage = DemConstants.TextColour.INFO + "Unknown command, use " + DemConstants.TextColour.COMMAND + "/Clan help " + DemConstants.TextColour.INFO + "for a list of available commands";
            }
            switch (commandResult) {
                case NOPERMISSION:
                    replyMessage = DemConstants.TextColour.ERROR + "You do not have permission to execute this command";
                    break;
                case NOCLAN:
                    replyMessage = DemConstants.TextColour.ERROR + "You must be a member of a Clan to do that";
                    break;
                case NOCLANPERMISSION:
                    replyMessage = DemConstants.TextColour.ERROR + "You're not a high enough rank in your Clan to do that";
                    break;
            }
            if (replyMessage != null) sender.sendMessage(new TextComponentString(replyMessage));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}

