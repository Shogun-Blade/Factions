package com.demmodders.clan.commands;

import com.demmodders.clan.clan.Clan;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.FlagDescriptions;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.clan.util.enums.CommandResult;
import com.demmodders.clan.util.enums.RelationState;
import com.demmodders.datmoddingapi.structures.ChunkLocation;
import com.demmodders.datmoddingapi.structures.Location;
import com.demmodders.datmoddingapi.util.DatTeleporter;
import com.demmodders.datmoddingapi.util.DemConstants;
import com.demmodders.datmoddingapi.util.DemStringUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.permission.PermissionAPI;

import javax.annotation.Nullable;
import java.util.*;

public class CAdminCommand extends CommandBase {
    @Override
    public String getName() {
        return "clanadmin";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return printHelp(1, true);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        final ClanManager clanManager = ClanManager.getInstance();
        final UUID clanId = clanManager.getPlayersClanID(((EntityPlayerMP) sender).getUniqueID());
        List<String> possibilities = new ArrayList<>();

        if (args.length == 1) {
            // All commands are possible
            final HashMap<String, String> commands = ClanCommandList.getAdminCommands();
            if (commands != null) possibilities = new ArrayList<>(commands.keySet());
        } else if (args.length == 2) {
            // Only the the first argument of commands with 1 or more arguments are possible
            switch (args[0].toLowerCase()) {
                // Argument is a page
                case "help":
                    possibilities.add("1");
                    possibilities.add("2");
                    possibilities.add("3");
                    break;

                // Argument is a clan name
                case "claim":
                case "ally":
                case "enemy":
                case "neutral":
                case "setmotd":
                case "sethome":
                case "desc":
                case "flag":
                case "disband":
                case "setpower":
                case "setmaxpower":
                case "resetpower":
                case "resetmaxpower":
                    possibilities = clanManager.getListOfClansNames();
                    break;

                // Argument is a currently online player
                case "setclan":
                case "kick":
                case "invite":
                case "uninvite":
                case "promote":
                case "demote":
                case "setrank":
                case "setowner":
                    possibilities = Arrays.asList(server.getOnlinePlayerNames());
                    break;
            }
        } else if (args.length == 3) {
            // Only the the second argument of commands with 2 arguments are possible
            switch (args[0].toLowerCase()) {
                // Argument is a clan name
                case "setclan":
                case "ally":
                case "enemy":
                case "neutral":
                case "invite":
                case "uninvite":
                    possibilities = clanManager.getListOfClansNames();
                    break;

                // Specifics
                case "flag":
                    possibilities.add("set");
                    possibilities.add("remove");
                    break;

                case "setrank":
                    possibilities.add("grunt");
                    possibilities.add("lieutenant");
                    possibilities.add("sergeant");
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "flag":
                    possibilities.addAll(FlagDescriptions.getPlayerFlags().keySet());
                    possibilities.addAll(FlagDescriptions.getAdminFlags().keySet());
                    break;
            }
        }
        return getListOfStringsMatchingLastWord(args, possibilities);
    }

    private String printHelp(int Page, boolean dump) throws IndexOutOfBoundsException {
        LinkedHashMap<String, String> commands = ClanCommandList.getAdminCommands();

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
                if (dump) helpText.append("/clanadmin ");
                helpText.append(keyList.get(i)).append(DemConstants.TextColour.INFO).append(" - ").append(commands.get(keyList.get(i)));
            }
            return helpText.toString();
        }
        return DemConstants.TextColour.ERROR + "Could not generate help";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // Commonly used objects
        ClanManager clanManager = ClanManager.getInstance();

        UUID playerID = null;
        UUID clanID = null;

        boolean console = true;
        if (sender instanceof EntityPlayerMP) {
            playerID = ((EntityPlayerMP) sender).getUniqueID();
            clanID = clanManager.getPlayersClanID(playerID);
            console = false;
        }

        CommandResult commandResult = CommandResult.SUCCESS;
        String replyMessage = null;
        if (args.length == 0) {
            // If no arguments given, tell the user how to use the command
            sender.sendMessage(new TextComponentString(getUsage(sender)));
        } else {
            if (PermissionAPI.hasPermission((EntityPlayerMP) sender, "clansmod.admin")) {
                // Check for the command
                switch (args[0].toLowerCase()) {
                    // Global
                    case "help":
                        try {
                            int page = ((args.length == 1) ? 1 : Integer.parseInt(args[1]));
                            replyMessage = printHelp(page, false);
                        } catch (NumberFormatException e) {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin help <page>";
                        } catch (IndexOutOfBoundsException e) {
                            replyMessage = DemConstants.TextColour.INFO + "There aren't that many pages";
                        }
                        break;
                    case "setclann":
                        if (args.length > 2) {
                            final UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            final UUID targetClan = clanManager.getClanIDFromName(args[2]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                clanManager.setPlayerClan(targetPlayer, targetClan, true);
                                replyMessage = DemConstants.TextColour.INFO + args[1] + " Successfully joined " + args[2];
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setfaction <Player name> <Clan name>";
                        }
                        break;
                    case "home":
                        if (!console) {
                            if (!clanID.equals(ClanManager.WILDID)) {
                                // Make sure they have a clan home
                                final Location destination = clanManager.getClan(clanID).homePos;
                                if (destination != null) {
                                    if (destination.dim != ((EntityPlayerMP) sender).dimension) {
                                        ((EntityPlayerMP) sender).changeDimension(destination.dim, new DatTeleporter(destination));
                                    } else {
                                        ((EntityPlayerMP) sender).connection.setPlayerLocation(destination.x, destination.y, destination.z, destination.yaw, destination.pitch);
                                    }
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "Your clan doesn't have a home";
                                }
                            } else {
                                commandResult = CommandResult.NOCLAN;
                            }
                        } else {
                            commandResult = CommandResult.NOCONSOLE;
                        }
                        break;
                    case "claim":
                        if (!console) {
                            UUID targetClan = (args.length > 1 ? clanManager.getClanIDFromName(args[1]) : clanID);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown clan";
                            } else {
                                clanManager.setChunkOwner(new ChunkLocation(((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ), targetClan, true);
                                replyMessage = DemConstants.TextColour.INFO + "Successfully claimed this chunk for " + clanManager.getClan(targetClan).name;
                                clanManager.getPlayer(playerID).lastClanLand = targetClan;
                                break;
                            }
                        } else {
                            commandResult = CommandResult.NOCONSOLE;
                        }
                        break;
                    case "unclaim":
                        if (!console) {
                            final ChunkLocation chunk = new ChunkLocation(((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).chunkCoordX, ((EntityPlayerMP) sender).chunkCoordZ);
                            final UUID owningClan = clanManager.getChunkOwningFaction(chunk);
                            if (ClanManager.WILDID.equals(owningClan)) {
                                replyMessage = DemConstants.TextColour.ERROR + "The land has not been claimed";
                            } else {
                                clanManager.setChunkOwner(chunk, null, true);
                                replyMessage = DemConstants.TextColour.INFO + "Successfully removed " + DemStringUtils.makePossessive(clanManager.getClan(owningClan).name) + " claim on this land";
                                clanManager.getPlayer(playerID).lastClanLand = ClanManager.WILDID;
                            }
                        } else {
                            commandResult = CommandResult.NOCONSOLE;
                        }
                        break;
                    case "ally":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            UUID targetClan2 = clanManager.getClanIDFromName(args[2]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown clan: " + args[1];
                            } else if (targetClan2 == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown clan: " + args[2];
                            } else {
                                clanManager.setClanRelation(targetClan, targetClan2, RelationState.ALLY);
                                clanManager.setClanRelation(targetClan2, targetClan, RelationState.ALLY);
                                replyMessage = DemConstants.TextColour.INFO + "Successfully made " + args[1] + " and " + args[2] + " allies";
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin ally <Clan1 name> <Clan2 name>";
                        }
                        break;
                    case "enemy":
                        if (args.length > 2) {
                            final UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            final UUID targetClan2 = clanManager.getClanIDFromName(args[2]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan: " + args[1];
                            } else if (targetClan2 == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan: " + args[2];
                            } else {
                                clanManager.setClanRelation(targetClan, targetClan2, RelationState.ENEMY);
                                clanManager.setClanRelation(targetClan2, targetClan, RelationState.ENEMY);
                                replyMessage = DemConstants.TextColour.INFO + "Successfully made " + args[1] + " and " + args[2] + " enemies";
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin enemy <Clan1 name> <Clan2 name>";
                        }
                        break;
                    case "neutral":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            UUID targetFaction2 = clanManager.getClanIDFromName(args[2]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan: " + args[1];
                            } else if (targetFaction2 == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan: " + args[2];
                            } else {
                                clanManager.setClanRelation(targetClan, targetFaction2, null);
                                clanManager.setClanRelation(targetFaction2, targetClan, null);
                                replyMessage = DemConstants.TextColour.INFO + "Successfully made " + args[1] + " and " + args[2] + " neutral";
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin neutral <Faction1 name> <Faction2 name>";
                        }
                        break;
                    case "kick":
                        if (args.length > 1) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (clanManager.getPlayersClanID(targetPlayer).equals(ClanManager.WILDID)) {
                                replyMessage = DemConstants.TextColour.ERROR + "That player isn't in a Clan";
                            } else {
                                if (clanManager.getPlayer(targetPlayer).clanRank != ClanRank.OWNER) {
                                    clanManager.setPlayerClan(targetPlayer, ClanManager.WILDID, true);
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully kicked " + args[1] + " from their Clan";
                                    clanManager.sendMessageToPlayer(targetPlayer, DemConstants.TextColour.INFO + "You have been kicked from your Clan by an admin");
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "The owner cannot be kicked from their Clan, you'll have to give away their rank first with " + DemConstants.TextColour.COMMAND + "/clanadmin setowner <Player name>" + DemConstants.TextColour.ERROR + " or disband their Clan with " + DemConstants.TextColour.COMMAND + "/clanadmin disband <Clan name>";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin kick <Player name>";
                        }
                        break;
                    case "invite":
                        if (args.length > 2) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            UUID targetClan = clanManager.getClanIDFromName(args[2]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else if (clanManager.getPlayer(targetPlayer).Clan != ClanManager.WILDID) {
                                replyMessage = DemConstants.TextColour.ERROR + args[1] + " cannot receive an invite as they are already a member of a Clan";
                            } else {
                                switch (clanManager.invitePlayerToClan(targetPlayer, targetClan)) {
                                    case 0:
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully invited " + args[1] + " to " + args[2];
                                        break;
                                    case 1:
                                        replyMessage = DemConstants.TextColour.ERROR + args[1] + " already has an invite from " + args[2];
                                        break;
                                    case 2:
                                        replyMessage = DemConstants.TextColour.ERROR + args[1] + " is already a member of " + args[2];
                                        break;
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin invite <Player name> <Clan name>";
                        }
                        break;
                    case "uninvite":
                        if (args.length > 2) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            UUID targetClan = clanManager.getClanIDFromName(args[2]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                if (clanManager.removePlayerInvite(targetPlayer, targetClan)) {
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully removed " + DemStringUtils.makePossessive(args[1]) + " invite " + " from " + args[2];
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + args[1] + " doesn't have an invite from " + args[2];
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin uninvite <Player name> <Clan name>";
                        }
                        break;
                    case "setmotd":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                StringBuilder mOTD = new StringBuilder();
                                for (int i = 2; i < args.length; i++) {
                                    mOTD.append(args[i]).append(" ");
                                }
                                if (mOTD.toString().length() <= ClanConfig.clanSubCat.maxClanMOTDLength) {
                                    clanManager.getClan(targetClan).motd = mOTD.toString();

                                    replyMessage = DemConstants.TextColour.INFO + "Successfully set " + DemStringUtils.makePossessive(args[1]) + " MOTD to " + mOTD.toString();
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "That MOTD is too long";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setmotd <Clan name> [MOTD]";
                        }
                        break;
                    case "sethome":
                        if (!console) {
                            if (args.length > 1) {
                                UUID targetClan = clanManager.getClanIDFromName(args[1]);
                                if (targetClan == null) {
                                    replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                                } else {
                                    boolean result = clanManager.setClanHome(targetClan, new Location(((EntityPlayerMP) sender).dimension, ((EntityPlayerMP) sender).posX, ((EntityPlayerMP) sender).posY, ((EntityPlayerMP) sender).posZ, ((EntityPlayerMP) sender).rotationPitch, ((EntityPlayerMP) sender).rotationYaw));
                                    if (result)
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully set " + DemStringUtils.makePossessive(args[1]) + " home";
                                    else
                                        replyMessage = DemConstants.TextColour.ERROR + "Unable to set Clan home, they don't own this land";
                                }
                            } else {
                                replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin sethome <Clan name>";
                            }
                        } else {
                            commandResult = CommandResult.NOCONSOLE;
                        }
                        break;
                    case "desc":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                StringBuilder desc = new StringBuilder();
                                for (int i = 2; i < args.length; i++) {
                                    desc.append(args[i]).append(" ");
                                }
                                if (desc.toString().length() <= ClanConfig.clanSubCat.maxClanMOTDLength) {
                                    clanManager.getClan(targetClan).desc = desc.toString();
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully set " + DemStringUtils.makePossessive(args[1]) + " description to " + desc.toString();
                                } else {
                                    replyMessage = DemConstants.TextColour.ERROR + "That description is too long";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin desc <Clan name> [description]";
                        }
                        break;
                    case "flag":
                        if (args.length > 3) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {

                                HashMap<String, String> flags = new HashMap<>(FlagDescriptions.getPlayerFlags());
                                flags.putAll(FlagDescriptions.getAdminFlags());
                                if (flags.containsKey(args[3].toLowerCase())) {
                                    if (args[2].equals("set")) {
                                        if (!clanManager.getClan(targetClan).hasFlag(args[3].toLowerCase())) {
                                            clanManager.getClan(targetClan).setFlag(args[3].toLowerCase());
                                            replyMessage = DemConstants.TextColour.INFO + "Successfully set flag";
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + args[1] + " already has that flag set";
                                        }
                                    } else if (args[2].equals("remove")) {
                                        if (clanManager.getClan(targetClan).hasFlag(args[3].toLowerCase())) {
                                            clanManager.getClan(targetClan).removeFlag(args[3].toLowerCase());
                                            replyMessage = DemConstants.TextColour.INFO + "Successfully removed flag";
                                        } else {
                                            replyMessage = DemConstants.TextColour.ERROR + args[1] + " doesn't have that flag set";
                                        }
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "Unknown flag operation, correct operations are: set, remove";
                                    }
                                } else {
                                    StringBuilder flagsMessage = new StringBuilder();
                                    flagsMessage.append(DemConstants.TextColour.ERROR).append("Unknown flag, available flags are:");
                                    for (String flag : flags.keySet()) {
                                        flagsMessage.append("\n").append(DemConstants.TextColour.INFO).append(flag).append(" - ").append(flags.get(flag));
                                    }
                                    replyMessage = flagsMessage.toString();
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin flag <Clan> set|remove <flag>";
                        }
                        break;
                    case "promote":
                        if (args.length > 1) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (clanManager.getPlayersClanID(targetPlayer).equals(ClanManager.WILDID)) {
                                replyMessage = DemConstants.TextColour.ERROR + args[1] + " isn't in a Clan";
                            } else {
                                switch (clanManager.getPlayer(targetPlayer).clanRank) {
                                    case GRUNT:
                                        clanManager.setPlayerRank(targetPlayer, ClanRank.LIEUTENANT);
                                        replyMessage = DemConstants.TextColour.INFO + "Promoted " + args[1] + " to Lieutenant";
                                        break;
                                    case LIEUTENANT:
                                        clanManager.setPlayerRank(targetPlayer, ClanRank.OFFICER);
                                        replyMessage = DemConstants.TextColour.INFO + "Promoted " + args[1] + " to Officer";
                                        break;
                                    case OFFICER:
                                        replyMessage = DemConstants.TextColour.ERROR + "That player has the highest rank they can be promoted to";
                                        break;
                                    case OWNER:
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is the maximum rank possible";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin promote <Player name>";
                        }
                        break;
                    case "demote":
                        if (args.length > 1) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (targetPlayer == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                            } else if (clanManager.getPlayersClanID(targetPlayer).equals(ClanManager.WILDID)) {
                                replyMessage = DemConstants.TextColour.ERROR + args[1] + " isn't in a Clan";
                            } else {
                                switch (clanManager.getPlayer(targetPlayer).clanRank) {
                                    case GRUNT:
                                        replyMessage = DemConstants.TextColour.ERROR + "That player is the minimum rank possible";
                                        break;
                                    case LIEUTENANT:
                                        clanManager.setPlayerRank(targetPlayer, ClanRank.GRUNT);
                                        replyMessage = DemConstants.TextColour.INFO + "Demoted " + args[1] + " to Grunt";
                                        break;
                                    case OFFICER:
                                        clanManager.setPlayerRank(targetPlayer, ClanRank.LIEUTENANT);
                                        replyMessage = DemConstants.TextColour.INFO + "Demoted " + args[1] + " to Lieutenant";
                                        break;
                                    case OWNER:
                                        replyMessage = DemConstants.TextColour.ERROR + "You cannot demote the owner";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin demote <Player name>";
                        }
                        break;
                    case "setrank":
                        if (args.length > 2) {
                            try {
                                ClanRank rank = ClanRank.valueOf(args[2].toUpperCase());
                                UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                                if (targetPlayer == null) {
                                    replyMessage = DemConstants.TextColour.ERROR + "Unknown player";
                                } else if (rank == ClanRank.OWNER) {
                                    replyMessage = DemConstants.TextColour.ERROR + "To set a player as the owner, use " + DemConstants.TextColour.COMMAND + "/clanadmin setowner <player>";
                                } else {
                                    clanManager.setPlayerRank(targetPlayer, rank);
                                    replyMessage = DemConstants.TextColour.INFO + "Set " + args[1] + " to " + rank.toString().toLowerCase();
                                }
                            } catch (IllegalArgumentException e) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown rank, available ranks are: grunt, lieutenant, and officer";
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setrank <member name> <new rank>";
                        }
                        break;
                    case "setowner":
                        if (args.length > 1) {
                            UUID targetPlayer = clanManager.getPlayerIDFromName(args[1]);
                            if (clanManager.getPlayersClan(targetPlayer) == null) {
                                replyMessage = DemConstants.TextColour.ERROR + args[1] + " is not in a Clan";
                            } else {
                                UUID ownerID = clanManager.getPlayersClan(targetPlayer).getOwnerID();
                                clanManager.setPlayerRank(ownerID, ClanRank.OFFICER);
                                clanManager.setPlayerRank(targetPlayer, ClanRank.OWNER);
                                clanManager.sendClanwideMessage(clanID, new TextComponentString(DemConstants.TextColour.INFO + clanManager.getPlayer(targetPlayer).lastKnownName + " is now the new leader of " + ClanConstants.TextColour.OWN + clanManager.getClan(clanID).name));
                                replyMessage = DemConstants.TextColour.INFO + "Successfully set " + args[1] + " as the owner of their Clan";
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setowner <Member name>";
                        }
                        break;
                    case "disband":
                        if (args.length > 1) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else if (args.length == 2) {
                                replyMessage = DemConstants.TextColour.INFO + "Are you sure you want to disband " + args[1] + "? Type " + DemConstants.TextColour.COMMAND + "/clanadmin disband " + args[1] + " confirm " + DemConstants.TextColour.INFO + "to confirm you want to disband them";
                            } else if (args[2].equalsIgnoreCase("confirm")) {
                                if (clanManager.disbandClan(targetClan, null))
                                    replyMessage = DemConstants.TextColour.INFO + "Successfully disbanded " + args[1];
                                else replyMessage = DemConstants.TextColour.ERROR + "Failed to disband " + args[1];
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin disband <Clan name>";
                        }
                        break;
                    case "setpower":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                try {
                                    Clan theClan = clanManager.getClan(targetClan);
                                    int newPower = Integer.parseInt(args[2]);
                                    if (theClan.power.maxPower < newPower) {
                                        theClan.power.power = newPower;
                                        replyMessage = DemConstants.TextColour.INFO + "Successfully set the power of " + args[1] + " to " + args[2];
                                    } else {
                                        replyMessage = DemConstants.TextColour.ERROR + "The new power must be less than the Clan's max power, you can set the Clan max power with " + DemConstants.TextColour.COMMAND + "/clanadmin setmaxpower <Clan Name> <New Max Power>";
                                    }
                                } catch (NumberFormatException e) {
                                    replyMessage = DemConstants.TextColour.ERROR + "The new power must be a number";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setpower <Clan Name> <New Power>";
                        }
                        break;
                    case "setmaxpower":
                        if (args.length > 2) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                try {
                                    Clan theClan = clanManager.getClan(targetClan);
                                    int newPower = Integer.parseInt(args[2]);
                                    theClan.power.maxPower = newPower;
                                    if (theClan.power.maxPower < theClan.power.power) {
                                        theClan.power.power = newPower;
                                    }

                                    replyMessage = DemConstants.TextColour.INFO + "Successfully set the max power of " + args[1] + " to " + args[2];
                                } catch (NumberFormatException e) {
                                    replyMessage = DemConstants.TextColour.ERROR + "The new power must be a number";
                                }
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin setmaxpower <Clan Name> <New Max Power>";
                        }
                        break;
                    case "resetpower":
                        if (args.length > 1) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                Clan theClan = clanManager.getClan(targetClan);
                                theClan.power.power = ClanConfig.clanSubCat.clanStartingPower;

                                replyMessage = DemConstants.TextColour.INFO + "Successfully reset the power of " + args[1] + " to " + ClanConfig.clanSubCat.clanStartingPower;
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin resetpower <Clan Name>";
                        }
                        break;
                    case "resetMaxPower":
                        if (args.length > 1) {
                            UUID targetClan = clanManager.getClanIDFromName(args[1]);
                            if (targetClan == null) {
                                replyMessage = DemConstants.TextColour.ERROR + "Unknown Clan";
                            } else {
                                Clan theClan = clanManager.getClan(targetClan);
                                theClan.power.maxPower = ClanConfig.clanSubCat.clanStartingMaxPower;
                                if (theClan.power.maxPower < theClan.power.power) {
                                    theClan.power.power = theClan.power.maxPower;
                                }
                                replyMessage = DemConstants.TextColour.INFO + "Successfully reset the max power of " + args[1] + " to " + ClanConfig.clanSubCat.clanStartingMaxPower;
                            }
                        } else {
                            replyMessage = DemConstants.TextColour.ERROR + "Bad argument, command should look like: " + DemConstants.TextColour.COMMAND + "/clanadmin resetmaxpower <Clan Name>";
                        }
                        break;
                    default:
                        replyMessage = DemConstants.TextColour.INFO + "Unknown command, use " + DemConstants.TextColour.COMMAND + "/clanadmin help " + DemConstants.TextColour.INFO + "for a list of available commands";
                }
            } else {
                commandResult = CommandResult.NOPERMISSION;
            }
            switch (commandResult) {
                case NOCLAN:
                    replyMessage = DemConstants.TextColour.ERROR + "You must be a member of a Clan to do that";
                    break;
                case NOCONSOLE:
                    replyMessage = DemConstants.TextColour.ERROR + "This command is only available to players";
                    break;
                case NOPERMISSION:
                    replyMessage = DemConstants.TextColour.ERROR + "You do not have permission to do that";
                    break;
            }
            if (replyMessage != null) sender.sendMessage(new TextComponentString(replyMessage));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List<String> getAliases() {
        final ArrayList<String> aliases = new ArrayList<>();
        aliases.add("cl");
        aliases.add("cadmin");
        aliases.add("clana");
        return aliases;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
