package com.demmodders.clan.clan;

import com.demmodders.clan.Clans;
import com.demmodders.clan.api.event.InClanEvent;
import com.demmodders.clan.api.event.OutClanEvent;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.ClanFileHelper;
import com.demmodders.clan.util.UnknownPlayerException;
import com.demmodders.clan.util.enums.*;
import com.demmodders.clan.util.structures.ClaimResult;
import com.demmodders.clan.util.structures.Power;
import com.demmodders.clan.util.structures.Relationship;
import com.demmodders.clan.util.structures.UnclaimResult;
import com.demmodders.datmoddingapi.structures.ChunkLocation;
import com.demmodders.datmoddingapi.structures.Location;
import com.demmodders.datmoddingapi.util.DemConstants;
import com.demmodders.datmoddingapi.util.FileHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class ClanManager {
    public static final Logger LOGGER = LogManager.getLogger(Clans.MODID);
    public static final UUID WILDID = new UUID(0L, 0L);
    public static final UUID SAFEID = new UUID(0L, 1L);
    public static final UUID WARID = new UUID(0L, 2L);

    // Singleton
    private static ClanManager instance = null;
    // Clan Objects
    private final Map<UUID, Clan> ClanMap = new HashMap<>();
    private final Map<UUID, Player> PlayerMap = new HashMap<>();

    ClanManager() {
        // Load Clan details
        LOGGER.info(Clans.MODID + " Loading Clans");
        LOGGER.debug(Clans.MODID + " Loading Clan data");
        loadClans();

        LOGGER.debug(Clans.MODID + " Loading Default Clan data");
        loadDefaultClans();


        LOGGER.debug(Clans.MODID + " Loading Player data");
        loadPlayers();
        LOGGER.debug(Clans.MODID + " Loading Claimed Chunks data");
        loadClaimedChunks();

        // Calculate metadata that we haven't saved
        LOGGER.debug(Clans.MODID + " Adding players to Clans");
        addPlayersToClans();

        LOGGER.debug(Clans.MODID + " Adding chunks to Clans");
        addLandToClans();

        LOGGER.debug(Clans.MODID + " Adding invites to players");
        addInvitesToPlayers();
    }

    public static ClanManager getInstance() {
        if (instance == null) {
            instance = new ClanManager();
        }
        return instance;
    }

    private final Map<Integer, HashMap<String, UUID>> ClaimedLand = new HashMap<>();

    // Getters

    /**
     * Generates the key for the chunk that is used to identify it in the Clan system
     *
     * @param ChunkX The X coordinate of the chunk
     * @param ChunkZ The Z coordinate of the chunk
     * @return the key used to identify the chunk to Clan
     */
    public static String makeChunkKey(int ChunkX, int ChunkZ) {
        return ChunkX + ", " + ChunkZ;
    }

    /**
     * Gets the Clan object that has the given ID
     *
     * @param ID The ID of the Clan
     * @return The Clan object of the ID
     */
    public Clan getClan(UUID ID) {
        return ClanMap.get(ID);
    }

    /**
     * Gets the Player object that has the given ID
     * @param ID the ID of the player
     * @return the player object of the ID
     */
    public Player getPlayer(UUID ID){
        return PlayerMap.get(ID);
    }

    /**
     * Gets the ID of the player object that has the given name
     * @param Name the name of the player
     * @return The player with the given name, null if no player of that name is known
     */
    @Nullable
    public UUID getPlayerIDFromName(String Name){
        for (UUID playerID : PlayerMap.keySet()){
            if (PlayerMap.get(playerID).lastKnownName.equalsIgnoreCase(Name)){
                return playerID;
            }
        }
        return null;
    }

    /**
     * Gets the player object from the player's uuid
     * @param PlayerID The ID of the player
     * @return The player object of the player
     */
    @Nullable
    public static EntityPlayerMP getPlayerMPFromUUID(UUID PlayerID){
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(PlayerID);
    }

    // Setters
    /**
     * Sets the last known name of the player
     * @param PlayerID the ID of the player being updated
     * @param Name the new name of the player
     */
    public void setPlayerLastKnownName(UUID PlayerID, String Name) {
        PlayerMap.get(PlayerID).lastKnownName = Name;
        savePlayer(PlayerID);
    }

    public void setPlayerLastOnline(UUID PlayerID, long LastOnline) {
        getPlayer(PlayerID).lastOnline = LastOnline;
        savePlayer(PlayerID);
    }

    /**
     * Gets the ID of the Clan that has the given name
     *
     * @param Name the name of the Clan
     * @return The UUID of the Clan with the given name
     */
    @Nullable
    public UUID getClanIDFromName(String Name) {
        for (UUID clanId : ClanMap.keySet()) {
            if (ClanMap.get(clanId).name.equalsIgnoreCase(Name)) {
                return clanId;
            }
        }
        return null;
    }

    /**
     * Sets the Clan of the given player
     *
     * @param PlayerID       the player who's Clan is being change
     * @param clanId         the Clan the player is being added to
     * @param removeFromClan Whether to remove the player info from the Clan, useful to disable when iterating through and removing the members
     */
    public void setPlayerClan(UUID PlayerID, UUID clanId, boolean removeFromClan) {
        Player player = PlayerMap.get(PlayerID);

        if (removeFromClan) {
            ClanMap.get(player.Clan).removePlayer(PlayerID);
        }

        player.Clan = clanId;

        ClanMap.get(clanId).addPlayer(PlayerID);
        player.clanRank = ClanRank.GRUNT;
        if (player.invites.contains(clanId)) {
            player.invites.remove(clanId);
            ClanMap.get(clanId).invites.remove(PlayerID);
        }

        savePlayer(PlayerID);
    }

    // Utilities
    // Clan

    /**
     * Sets the given player's rank
     *
     * @param PlayerID The player who's rank is being changed
     * @param Rank     The new rank
     */
    public void setPlayerRank(UUID PlayerID, ClanRank Rank) {
        PlayerMap.get(PlayerID).clanRank = Rank;
        savePlayer(PlayerID);
        String rank = "";
        switch (Rank) {
            case GRUNT:
                rank = "a Grunt";
                break;
            case LIEUTENANT:
                rank = "a Lieutenant";
                break;
            case OFFICER:
                rank = "an Officer";
                break;
            case OWNER:
                rank = "";
                break;
        }
        if (!rank.isEmpty()) {
            sendMessageToPlayer(PlayerID, DemConstants.TextColour.INFO + "You are now " + rank);
        }
    }

    /**
     * Attempts to invite the given player to the given Clan
     *
     * @param PlayerID The player being invited
     * @param clanId   The Clan the player is invited to
     * @return The result of the invite (0: Success, 1: Already invited, 2: already a member)
     */
    public int invitePlayerToClan(UUID PlayerID, UUID clanId) {
        if (ClanMap.get(clanId).invites.contains(PlayerID)) return 1;

        if (PlayerMap.get(PlayerID).Clan.equals(clanId)) return 2;

        ClanMap.get(clanId).invites.add(PlayerID);
        PlayerMap.get(PlayerID).invites.add(clanId);
        saveClan(clanId);
        sendMessageToPlayer(PlayerID, DemConstants.TextColour.INFO + ClanMap.get(clanId).name + " Has invited you to join, accept their invite with " + DemConstants.TextColour.COMMAND + "/Clan join " + ClanConstants.TextColour.OWN + ClanMap.get(clanId).name);
        return 0;
    }

    /**
     * Remove any invites to the player if they have any
     *
     * @param PlayerID The player who's getting the invite
     * @param clanId   The Clan the player's invited to
     * @return Whether there were any invites to remove
     */
    public boolean removePlayerInvite(UUID PlayerID, UUID clanId) {
        boolean removed = false;
        if (ClanMap.get(clanId).invites.contains(PlayerID)) {
            ClanMap.get(clanId).invites.remove(PlayerID);
            removed = true;
            saveClan(clanId);
        }
        if (PlayerMap.get(PlayerID).invites.contains(clanId)) {
            PlayerMap.get(PlayerID).invites.remove(clanId);

            removed = true;
        }
        return removed;
    }

    /**
     * Iterates through all the players that Clan is aware of and gives a reference to them to their owning Clan
     */
    private void addPlayersToClans() {
        for (UUID playerID : PlayerMap.keySet()) {
            UUID clanId = PlayerMap.get(playerID).Clan;
            if (ClanMap.containsKey(clanId)) {
                ClanMap.get(PlayerMap.get(playerID).Clan).members.add(playerID);
            } else {
                LOGGER.warn(Clans.MODID + " Player references Clan that doesn't exist, removing reference");
                PlayerMap.get(playerID).clearFaction();
                savePlayer(playerID);
            }
        }
    }

    /**
     * Iterates through all the claimed land and gives references of them to their owning clans
     */
    private void addLandToClans() {
        boolean pruned;
        for (int dim : ClaimedLand.keySet()) {
            pruned = false;
            for (String land : ClaimedLand.get(dim).keySet()) {
                if (ClanMap.containsKey(ClaimedLand.get(dim).get(land))) {
                    ClanMap.get(ClaimedLand.get(dim).get(land)).addLandToClan(dim, land);
                } else {
                    LOGGER.warn("Discovered land owned by a Clan that doesn't exist, removing owner");
                    ClaimedLand.get(dim).remove(land);
                    pruned = true;
                }
            }
            if (pruned) {
                saveClaimedChunks(dim);
            }
        }
    }

    /**
     * Iterates through all the clans that Clan and adds their invites to the invited players
     */
    private void addInvitesToPlayers() {
        for (UUID clanId : ClanMap.keySet()) {
            for (UUID playerID : ClanMap.get(clanId).invites) {
                PlayerMap.get(playerID).invites.add(clanId);
            }
        }
    }

    /**
     * Gets a list of all the clans in the game
     *
     * @return A list of all the clans
     */
    public List<Clan> getListOfClans() {
        return new ArrayList<>(ClanMap.values());
    }

    /**
     * Gets a list of all IDs of the clans in the game
     *
     * @return A list of all the Clan IDs
     */
    public List<UUID> getListOfClansUUIDs() {
        return new ArrayList<>(ClanMap.keySet());
    }

    /**
     * Gets a list of the names of all the Clan in the game
     *
     * @return A list of all the Clan names
     */
    public List<String> getListOfClansNames() {
        ArrayList<String> names = new ArrayList<>();
        for (UUID Clan : ClanMap.keySet()) {
            names.add(ClanMap.get(Clan).name);
        }
        return names;
    }

    /**
     * Gets a list of the names of all the clans passed to the function by their UUIDs
     *
     * @param IDs a list of the IDs
     * @return A list of the Clan names
     */
    public List<String> getListOfClansNamesFromClanList(ArrayList<UUID> IDs) {
        ArrayList<String> names = new ArrayList<>();
        for (UUID Clan : IDs) {
            names.add(ClanMap.get(Clan).name);
        }
        return names;
    }

    // Players

    /**
     * Gets the colour code for chat that should be shown based off the Clan
     *
     * @param originalClan The Clan that has the relation
     * @param otherClan    The Clan the Original Clan has a relation with
     * @return The colour code for the relation
     */
    public String getRelationColour(UUID originalClan, UUID otherClan) {
        if (originalClan != WILDID && otherClan != WILDID) {
            RelationState relationship = ClanMap.get(originalClan).getRelation(otherClan);
            if (originalClan.equals(otherClan)) return ClanConstants.TextColour.OWN.toString();
            else if (relationship == RelationState.ALLY) return ClanConstants.TextColour.ALLY.toString();
            else if (relationship == RelationState.ENEMY) return ClanConstants.TextColour.ENEMY.toString();
        }

        return "";
    }

    /**
     * Checks if a player is registered to the Clan system
     * @param PlayerID The ID of the player
     * @return True if the player is registered with Clan
     */
    public boolean isPlayerRegistered(UUID PlayerID) {
        return PlayerMap.containsKey(PlayerID);
    }

    /**
     * Registers a player to the Clan system
     * @param Player The player object to register
     */
    public void registerPlayer(EntityPlayer Player) {
        if (isPlayerRegistered(Player.getUniqueID())) {
            return;
        }
        PlayerMap.put(Player.getUniqueID(), new Player(WILDID, null, new Power(ClanConfig.playerSubCat.playerStartingPower, ClanConfig.playerSubCat.playerStartingMaxPower), Player.getName()));
        savePlayer(Player.getUniqueID());
    }

    /**
     * Checks whether the given player can join the given Clan
     *
     * @param PlayerID The player trying to join the Clan
     * @param clanId   The Clan the player is trying to join
     * @return Whether the player can join the given Clan
     */
    public boolean canAddPlayerToClan(UUID PlayerID, UUID clanId) {
        Clan Clan = ClanMap.get(clanId);
        return (ClanConfig.clanSubCat.maxMembers == 0 || Clan.members.size() < ClanConfig.clanSubCat.maxMembers) && (Clan.invites.contains(PlayerID) || Clan.hasFlag("open"));
    }

    /**
     * Gets the Clan that the given player belongs to
     *
     * @param PlayerID The ID of the player
     * @return The Clan object the player belongs to
     */
    public Clan getPlayersClan(UUID PlayerID){
        UUID clanId = getPlayersClanID(PlayerID);
        return ClanMap.get(clanId);
    }

    /**
     * Sends the given message to the given player
     * @param Player The ID of the player to send the message to
     * @param Message The Message to send to the player
     */
    public void sendMessageToPlayer(UUID Player, String Message){
        sendMessageToPlayer(Player, new TextComponentString(Message));
    }

    /**
     * Sends the given message to the given player
     * @param Player The ID of the player to send the message to
     * @param Message The Message to send to the player
     */
    public void sendMessageToPlayer(UUID Player, ITextComponent Message){
        EntityPlayerMP playerMP = getPlayerMPFromUUID(Player);
        if (playerMP != null){
            playerMP.sendMessage(Message);
        }
    }



    public String getPlayerStatusColour(UUID PlayerID, boolean ShowAway){
        PlayerList playerList = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList();
        if(playerList.getPlayerByUUID(PlayerID) != null) {
            return DemConstants.playerColour.ONLINE.toString();
        } else return DemConstants.playerColour.OFFLINE.toString();
    }

    // Chunks

    /**
     * Gets the ID of the Clan that owns the given player
     *
     * @param playerID The ID of the player
     * @return The UUID of the Clan that owns the player
     */
    public UUID getPlayersClanID(UUID playerID) throws UnknownPlayerException {
        Player player = PlayerMap.get(playerID);
        if (player != null) return player.Clan;
        else throw new UnknownPlayerException();
    }

    /**
     * Gets the owner of the chunk at the given coordinates
     * @param Dim The dimension containing the chunk
     * @param ChunkX The X coordinate of the chunk
     * @param ChunkZ The Z coordinate of the chunk
     * @return The UUID of the Clan that owns the chunk
     */
    public UUID getChunkOwningFaction(int Dim, int ChunkX, int ChunkZ){
        if (ClaimedLand.containsKey(Dim)){
            String chunkKey = makeChunkKey(ChunkX, ChunkZ);
            if (ClaimedLand.get(Dim).containsKey(chunkKey)) {
                return ClaimedLand.get(Dim).get(chunkKey);
            }
        }
        return WILDID;
    }

    /**
     * Gets the owner of the chunk at the given ChunkLocation
     * @param Chunk the ChunkLocation
     * @return The UUID of the Clan that owns the chunk
     */
    public UUID getChunkOwningFaction(ChunkLocation Chunk) {
        return getChunkOwningFaction(Chunk.dim, Chunk.x, Chunk.z);
    }

    /**
     * Checks the given player can build on the given chunk
     * @param Dim The dimension the chunk is in
     * @param ChunkX The Chunk's X coord
     * @param ChunkZ The Chunk's Z coord
     * @param PlayerID The ID of the player trying to build
     * @return Whether the player can build in the given chunk
     */
    public boolean getPlayerCanBuild(int Dim, int ChunkX, int ChunkZ, UUID PlayerID) {
        return getPlayerCanBuild(getChunkOwningFaction(Dim, ChunkX, ChunkZ), PlayerID);
    }

    /**
     * Checks the given player can build on the given Clan land
     *
     * @param owningClan The Clan that owns the land
     * @param PlayerID   The player trying to build
     * @return Whether the player can build on the Clan's land
     */
    public boolean getPlayerCanBuild(UUID owningClan, UUID PlayerID) {
        if (owningClan.equals(WILDID)) return true;

        try {
            UUID playerFaction = getPlayersClanID(PlayerID);

            if (owningClan.equals(playerFaction)) return true;

            RelationState relation = ClanMap.get(owningClan).getRelation(playerFaction);
            return (relation == RelationState.ALLY && ClanConfig.clanSubCat.allyBuild) || ((relation == RelationState.ENEMY || relation == RelationState.PENDINGENEMY) && ClanConfig.clanSubCat.enemyBuild);
        } catch (UnknownPlayerException e) {
            LOGGER.warn("Caught a fake player trying to build, allowing");
            return true;
        }
    }

    // Clan Functions

    /**
     * Checks the given Clan name is valid
     *
     * @param Name The name to test
     * @return 0 for success, 1 for name too long, 2 for name too short, 3 for Clan with name exists
     */
    public int checkClanName(String Name) {
        if (Name.length() > ClanConfig.clanSubCat.maxClanNameLength) {
            return 1;
        } else if (Name.length() < 1) {
            return 2;
        } else if (getClanIDFromName(Name) != null) {
            return 3;
        } else {
            return 0;
        }
    }

    /**
     * Creates a Clan
     *
     * @param Name     The name of the Clan
     * @param PlayerID The ID of the player who's creating the Clan
     * @return The result of creating the Clan (0 for success, 1 for name too long, 2 for name too short, 3 for Clan exists, 4 cancelled)
     */
    public int createClan(String Name, UUID PlayerID) {
        int rc = checkClanName(Name);
        if (rc != 0) {
            return rc;
        }

        OutClanEvent.ClanCreateEvent event = new OutClanEvent.ClanCreateEvent(PlayerID, Name);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 4;

        UUID clanId = UUID.randomUUID();
        ClanMap.put(clanId, new Clan(clanId, event.clanName, PlayerID));

        saveClan(clanId);

        PlayerMap.get(PlayerID).Clan = clanId;
        PlayerMap.get(PlayerID).clanRank = ClanRank.OWNER;
        savePlayer(PlayerID);

        return 0;
    }

    /**
     * Creates a Clan
     *
     * @param clanId The ID of the Clan who's name is being changed
     * @param Name   The name of the Clan
     * @return The result of changing the name (0 for success, 1 for name too long, 2 for name too short, 3 for Clan with name exists, 4 cancelled)
     */
    public int setClanName(@Nullable UUID PlayerID, UUID clanId, String Name) {
        int rc = checkClanName(Name);
        if (rc != 0) {
            return rc;
        }

        InClanEvent.ClanRenameEvent event = new InClanEvent.ClanRenameEvent(PlayerID, clanId, Name);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 4;

        Clan Clan = getClan(event.clanID);
        Clan.name = event.newName;

        saveClan(event.clanID);

        return 0;
    }

    /**
     * Safely disbands the given Clan
     *
     * @param clanId   The Clan being disbanded
     * @param PlayerID The player disbanding the Clan
     * @return Whether the Clan was successfully disbanded
     */
    public boolean disbandClan(UUID clanId, @Nullable UUID PlayerID) {
        Clan Clan = ClanMap.get(clanId);

        // Ensure the Clan can be disbanded
        if (Clan.hasFlag("permanent")) return false;

        // Post event, fail if something cancelled it
        InClanEvent.ClanDisbandEvent event = new InClanEvent.ClanDisbandEvent(PlayerID, clanId);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return false;

        releaseAllClanLand(clanId);

        // Remove members
        for (UUID playerID : Clan.members) {
            setPlayerClan(playerID, WILDID, false);
        }

        // Remove invites
        for (UUID invitedPlayerID : Clan.invites) {
            PlayerMap.get(invitedPlayerID).invites.remove(clanId);
        }

        // Release relations
        for (UUID OtherClanID : Clan.relationships.keySet()) {
            ClanMap.get(OtherClanID).relationships.remove(clanId);
        }

        ClanMap.remove(clanId);
        deleteClanFile(clanId);
        return true;
    }

    /**
     * Sends the given message to all the online members of the given Clan
     *
     * @param clanId  The ID of the Clan
     * @param Message The message to send to all the users
     */
    public void sendClanwideMessage(UUID clanId, ITextComponent Message) {
        for (UUID playerID : ClanMap.get(clanId).members) {
            sendMessageToPlayer(playerID, Message);
        }
    }

    /**
     * Removes all land owned by a Clan
     *
     * @param clanId The ID of the Clan
     */
    public void releaseAllClanLand(UUID clanId) {
        for (int dim : ClaimedLand.keySet()) {
            ClaimedLand.get(dim).values().removeIf(value -> value.equals(clanId));
            saveClaimedChunks(dim);
        }
        if (ClanMap.containsKey(clanId)) ClanMap.get(clanId).land.clear();
    }

    /**
     * Removes all land owned by a Clan in the specified dimension
     *
     * @param clanId The ID of the Clan
     * @param dim    The dimension to remove land from
     */
    public void releaseAllClanLand(UUID clanId, int dim) {
        if (ClaimedLand.containsKey(dim)) {
            ClaimedLand.get(dim).values().removeIf(value -> value.equals(clanId));
            saveClaimedChunks(dim);


            Clan Clan = getClan(clanId);
            if (Clan != null) Clan.land.get(dim).clear();
        }
    }

    /**
     * Removes all land owned by a Clan except for those on a blacklist
     *
     * @param clanId    The ID of the Clan
     * @param Blacklist Chunks to not remove
     */
    public void releaseAllClanLand(UUID clanId, List<ChunkLocation> Blacklist) {
        // TODO: Speed up
        HashMap<Integer, List<String>> blackList = new HashMap<>();

        for (ChunkLocation chunk : Blacklist) {
            if (!blackList.containsKey(chunk.dim)) {
                blackList.put(chunk.dim, new ArrayList<>());
            }

            blackList.get(chunk.dim).add(makeChunkKey(chunk.x, chunk.z));
        }

        for (int dim : ClaimedLand.keySet()) {
            HashMap<String, UUID> land = ClaimedLand.get(dim);

            land.keySet().removeIf(value -> land.get(value).equals(clanId) && !blackList.get(dim).contains(value));
            saveClaimedChunks(dim);

            if (ClanMap.containsKey(clanId))
                ClanMap.get(clanId).land.get(dim).removeIf(value -> !blackList.get(dim).contains(value));
        }
    }

    /**
     * Removes all land owned by a Clan in the specified dimension
     *
     * @param clanId    The ID of the Clan
     * @param Dim       The dimension to remove land from
     * @param Blacklist Chunks to not remove
     */
    public void releaseAllClanLand(UUID clanId, int Dim, List<ChunkLocation> Blacklist) {
        final List<String> blackList = new ArrayList<>();

        for (ChunkLocation chunk : Blacklist) {
            if (chunk.dim == Dim) blackList.add(makeChunkKey(chunk.x, chunk.z));
        }

        if (ClaimedLand.containsKey(Dim)) {
            HashMap<String, UUID> land = ClaimedLand.get(Dim);
            land.keySet().removeIf(value -> land.get(value).equals(clanId) && !blackList.contains(value));
            saveClaimedChunks(Dim);

            Clan Clan = getClan(clanId);
            if (Clan != null) {
                Clan.land.get(Dim).removeIf(value -> !blackList.contains(value));
            }
        }


    }

    /**
     * Attempts to claim some chunks for the given Clan
     *
     * @param clanId   The Clan claiming the chunk
     * @param PlayerID The player claiming the chunk
     * @param Dim      The dimension the chunk is in
     * @param ChunkX   The X Coord of the chunk
     * @param ChunkZ   The Z Coord of the chunk
     * @return The result of the claim
     */
    public ClaimResult claimLand(UUID clanId, @Nullable UUID PlayerID, int Dim, int ChunkX, int ChunkZ, boolean AllowSteal) {
        Clan Clan = ClanMap.get(clanId);
        ChunkLocation loc = new ChunkLocation(Dim, ChunkX, ChunkZ);
        boolean stolen = false;

        UUID currentOwner = getChunkOwningFaction(loc);

        // Check owner
        if (currentOwner.equals(clanId)) return new ClaimResult(EClaimResult.YOUOWN, 0, clanId);
        else if (!currentOwner.equals(WILDID)) {
            Clan owningClan = getClan(currentOwner);
            if (AllowSteal && owningClan.calculateLandValue() > owningClan.calculatePower()) stolen = true;
            else return new ClaimResult(EClaimResult.OWNED, 0, currentOwner);
        }

        // Check connects
        if (!(ClanConfig.landSubCat.landRequireConnect || (ClanConfig.landSubCat.landRequireConnectWhenStealing && stolen)) || Clan.checkLandTouches(Dim, ChunkX, ChunkZ)) {
            // Call event
            InClanEvent.ChunkEvent.ClanClaimEvent event = new InClanEvent.ChunkEvent.ClanClaimEvent(Collections.singletonList(loc), PlayerID, clanId, ClaimType.ONE);
            MinecraftForge.EVENT_BUS.post(event);

            if (event.isCanceled()) return new ClaimResult(EClaimResult.NAH, 0, null);
            else {
                // Check power
                if (!Clan.checkCanAffordLand(event.positions.size()))
                    return new ClaimResult(EClaimResult.LACKPOWER, event.positions.size(), null);

                setManyChunksOwner(event.positions, event.clanID);
                if (stolen) {
                    sendClanwideMessage(currentOwner, new TextComponentString(DemConstants.TextColour.INFO + getRelationColour(currentOwner, clanId) + Clan.name + DemConstants.TextColour.INFO + " has stolen some of your land!"));
                    return new ClaimResult(EClaimResult.STOLEN, event.positions.size(), currentOwner);
                }
                return new ClaimResult(EClaimResult.SUCCESS, event.positions.size(), currentOwner);
            }
        } else return new ClaimResult(EClaimResult.MUSTCONNECT, 0, null);
    }

    /**
     * Attempts to claim multiple chunks in a square radius from the given location
     *
     * @param clanId   The Clan claiming the chunks
     * @param PlayerID The player claiming the chunks
     * @param Dim      The dimension the chunks are in
     * @param ChunkX   The X coord of the chunk
     * @param ChunkZ   The Z coord of th echunk
     * @param radius   The radius of the square
     * @return The result of the claim
     */
    public ClaimResult squareClaimLand(UUID clanId, @Nullable UUID PlayerID, int Dim, int ChunkX, int ChunkZ, int radius) {
        Clan Clan = getClan(clanId);

        List<ChunkLocation> chunks = new ArrayList<>();
        boolean connects = false;

        for (int x = ChunkX - radius; x <= ChunkX + radius; ++x) {
            for (int z = ChunkZ - radius; z <= ChunkZ + radius; ++z) {
                ChunkLocation loc = new ChunkLocation(Dim, x, z);
                UUID currentOwner = getChunkOwningFaction(loc);
                if (WILDID.equals(currentOwner)) {
                    chunks.add(loc);
                    connects = connects || Clan.checkLandTouches(Dim, x, z);
                } else if (!clanId.equals(currentOwner)) {
                    return new ClaimResult(EClaimResult.OWNED, 0, null);
                }
            }
        }

        if (!ClanConfig.landSubCat.landRequireConnect || connects) {
            // Call event
            InClanEvent.ChunkEvent.ClanClaimEvent event = new InClanEvent.ChunkEvent.ClanClaimEvent(chunks, PlayerID, clanId, ClaimType.SQUARE);
            MinecraftForge.EVENT_BUS.post(event);

            if (event.isCanceled()) return new ClaimResult(EClaimResult.NAH, 0, null);
            else {
                // Check power
                if (!Clan.checkCanAffordLand(event.positions.size())) return new ClaimResult(EClaimResult.LACKPOWER, event.positions.size(), null);

                setManyChunksOwner(event.positions, event.clanID);
                return new ClaimResult(EClaimResult.SUCCESS, event.positions.size(), null);
            }
        } else return new ClaimResult(EClaimResult.MUSTCONNECT, 0, null);
    }

    /**
     * Sets the owner of many chunks, accounting for if the chunk is owned
     *
     * @param chunks The chunks being claimed
     * @param clanId The Clan claiming the chunks
     */
    public void setManyChunksOwner(List<ChunkLocation> chunks, UUID clanId) {
        Set<Integer> dims = new HashSet<>();
        for (ChunkLocation chunk : chunks) {
            dims.add(chunk.dim);
            setChunkOwner(chunk, clanId, false);
        }

        for (int dim : dims) saveClaimedChunks(dim);
    }

    /**
     * Sets the owner of a chunk, accounting for if the chunk is already owned
     *
     * @param chunk  The chunk being claimed
     * @param clanId The Clan claiming the chunk
     */
    public void setChunkOwner(ChunkLocation chunk, UUID clanId, boolean save) {
        if (!ClaimedLand.containsKey(chunk.dim)) {
            // Create dimension entry
            ClaimedLand.put(chunk.dim, new HashMap<>());
        }


        String chunkKey = makeChunkKey(chunk.x, chunk.z);

        UUID currentFaction = getChunkOwningFaction(chunk);
        if (!WILDID.equals(currentFaction)) {
            getClan(currentFaction).land.get(chunk.dim).remove(chunkKey);
            ClaimedLand.get(chunk.dim).remove(chunkKey);
        }

        if (clanId != null && !clanId.equals(WILDID)) {
            ClaimedLand.get(chunk.dim).put(chunkKey, clanId);

            Clan Clan = getClan(clanId);
            if (!Clan.land.containsKey(chunk.dim)) {
                // Create dimension entry
                Clan.land.put(chunk.dim, new ArrayList<>());
            }

            Clan.land.get(chunk.dim).add(chunkKey);
        }
        if (save) saveClaimedChunks(chunk.dim);
    }

    /**
     * Attempts to remove a Clan claim on a chunk
     *
     * @param clanId   The Clan unclaiming the chunk
     * @param PlayerID The player unclaiming the chunk
     * @param Dim      The dimension the chunk is in
     * @param ChunkX   The X Coord of the chunk
     * @param ChunkZ   The Z Coord of the chunk
     * @return The result of the claim (0: Success, 1: Clan doesn't own that chunk, 2: Cancelled)
     */
    public UnclaimResult unClaimLand(UUID clanId, @Nullable UUID PlayerID, int Dim, int ChunkX, int ChunkZ, ClaimType Type, int Radius) {
        List<ChunkLocation> chunks = new ArrayList<>();

        switch (Type) {
            case ONE:
                ChunkLocation loc = new ChunkLocation(Dim, ChunkX, ChunkZ);
                if (clanId.equals(getChunkOwningFaction(loc))) chunks.add(loc);
                break;

            case SQUARE:
                for (int x = ChunkX - Radius; x <= ChunkX + Radius; ++x) {
                    for (int z = ChunkZ - Radius; z <= ChunkZ + Radius; ++z) {
                        ChunkLocation cLoc = new ChunkLocation(Dim, x, z);
                        if (clanId.equals(getChunkOwningFaction(cLoc))) chunks.add(cLoc);
                    }
                }
        }

        InClanEvent.ChunkEvent.ClanUnClaimEvent event = new InClanEvent.ChunkEvent.ClanUnClaimEvent(chunks, PlayerID, clanId, Type);
        MinecraftForge.EVENT_BUS.post(event);

        if (event.isCanceled()) {
            return new UnclaimResult(EUnclaimResult.NAH, 0);
        } else {
            int count = 0;
            if (event.positions.isEmpty()) return new UnclaimResult(EUnclaimResult.NOLAND, 0);
            if (Type == ClaimType.ALL) {
                count = countClanLand(clanId) - event.positions.size();
                releaseAllClanLand(event.clanID, event.positions);
            } else {
                count = event.positions.size();
                setManyChunksOwner(event.positions, WILDID);
            }
            return new UnclaimResult(EUnclaimResult.SUCCESS, count);
        }

    }

    /**
     * Counts all the land a Clan owns
     * @param clanId The ID of the Clan whose land you're counting
     * @return The amount of land the Clan owns
     */
    public int countClanLand(UUID clanId) {
        int landCount = 0;
        Clan Clan = ClanMap.get(clanId);
        for (ArrayList<String> landList : Clan.land.values()) {
            landCount += landList.size();
        }
        return landCount;
    }

    /**
     * Counts all the land a Clan owns in a dimension
     * @param clanId The ID of the Clan whose land you're counting
     * @param Dim The dimension that contains the land you want to count
     * @return The amount of land the given Clan owns in the given dimension
     */
    public int countClanLand(UUID clanId, int Dim) {
        if (ClanMap.get(clanId).land.get(Dim) != null)
            return ClanMap.get(clanId).land.get(Dim).size();
        return 0;
    }

    /**
     * Attempts to add the other Clan as an ally to the given Clan
     *
     * @param clanId    The Clan creating the alliance
     * @param OtherClan the Clan the alliance is with
     * @param PlayerID  The ID of the player creating the alliance
     * @return The result of the alliance (0: Success them pending, 1: Success both allies, 2: Success but enemy, 3: Already allies, 4: That's you, 5: No)
     */
    public int addAlly(UUID clanId, UUID OtherClan, @Nullable UUID PlayerID) {
        Relationship currentRelation = ClanMap.get(clanId).relationships.get(OtherClan);
        if (clanId.equals(OtherClan)) return 4;
        if (ClanMap.get(OtherClan).hasFlag("unrelateable")) return 5;
        if (currentRelation != null && currentRelation.relation == RelationState.ALLY) return 3;

        InClanEvent.ClanRelationEvent event = new InClanEvent.ClanRelationEvent(PlayerID, clanId, OtherClan, RelationState.ALLY);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 6;

        setClanRelation(clanId, OtherClan, RelationState.ALLY);

        if (!ClanMap.get(OtherClan).relationships.containsKey(clanId)) {
            setClanRelation(OtherClan, clanId, RelationState.PENDINGALLY);
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_GREEN + ClanMap.get(clanId).name + " has made you their allies" + (ClanConfig.clanSubCat.allyBuild ? ", this means you can build on their land, but they can't build on yours" : "") + ", add them back with /Clan ally " + ClanMap.get(clanId).name));
            return 0;
        } else if (ClanMap.get(OtherClan).relationships.get(clanId).relation == RelationState.ALLY) {
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_GREEN + ClanMap.get(clanId).name + " has added you back as an ally, " + (ClanConfig.clanSubCat.allyBuild ? ", this means you can build on their land" : "")));
            return 1;
        } else {
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_GREEN + ClanMap.get(clanId).name + " has added you as their allies," + TextFormatting.DARK_RED + " you still think they're enemies though"));
            return 2;
        }
    }

    /**
     * Attempts to add the other Clan as an enemy to the given Clan
     *
     * @param clanId    The Clan declaring the enemy
     * @param OtherClan The Clan becoming an enemy
     * @param PlayerID  The ID of the player declaring the enemy
     * @return The result of the declaration (0: Success, 1: Success enemies all round, 2: Success but you're an ally to them, 3: already enemies, 4: That's you, 5: No)
     */
    public int addEnemy(UUID clanId, UUID OtherClan, @Nullable UUID PlayerID) {
        Relationship currentRelation = ClanMap.get(clanId).relationships.get(OtherClan);
        if (clanId.equals(OtherClan)) return 4;
        if (ClanMap.get(OtherClan).hasFlag("unrelateable")) return 5;
        if (currentRelation != null && currentRelation.relation == RelationState.ENEMY) return 3;

        InClanEvent.ClanRelationEvent event = new InClanEvent.ClanRelationEvent(PlayerID, clanId, OtherClan, RelationState.ENEMY);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 6;

        setClanRelation(clanId, OtherClan, RelationState.ENEMY);

        if (!ClanMap.get(OtherClan).relationships.containsKey(clanId) || ClanMap.get(OtherClan).relationships.get(clanId).relation == RelationState.PENDINGALLY) {
            setClanRelation(OtherClan, clanId, RelationState.PENDINGENEMY);
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_RED + ClanMap.get(clanId).name + " has declared you an enemy" + (ClanConfig.clanSubCat.enemyBuild ? ", this means they can build on your land, and you can build on theirs" : "") + ", let them know you got the message with with /Clan enemy " + ClanMap.get(clanId).name));
            return 0;
        } else if (ClanMap.get(OtherClan).relationships.get(clanId).relation == RelationState.ALLY) {
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_RED + ClanMap.get(clanId).name + " has declared you an enemy" + (ClanConfig.clanSubCat.enemyBuild ? ", this means they can build on your land, and you can build on theirs" : "") + ", but you still think they're an ally, let them know you got the message with /Clan enemy " + ClanMap.get(clanId).name));
            return 2;
        } else {
            sendClanwideMessage(OtherClan, new TextComponentString(TextFormatting.DARK_RED + ClanMap.get(clanId).name + " has declared you an enemy as well, you are now at war"));
            return 1;
        }
    }

    /**
     * Adds the given the Clan as a neutral Clan
     *
     * @param clanId    The Clan becoming neutral
     * @param OtherClan The Clan to become neutral with
     * @param PlayerID  The ID of the player creating the alliance
     * @return The result of the declaration (0: Success, 1: Removed enemy, 2: Removed ally, 3: cannot deny request, 4: no relation, 5: that's you
     */
    public int addNeutral(UUID clanId, UUID OtherClan, UUID PlayerID) {
        Relationship currentRelation = ClanMap.get(clanId).relationships.get(OtherClan);
        if (clanId.equals(OtherClan)) return 5;
        if (currentRelation == null) return 4;
        if (currentRelation.relation == RelationState.PENDINGALLY || currentRelation.relation == RelationState.PENDINGENEMY)
            return 3;

        InClanEvent.ClanRelationEvent event = new InClanEvent.ClanRelationEvent(PlayerID, clanId, OtherClan, RelationState.ALLY);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return 6;

        setClanRelation(clanId, OtherClan, null);
        if (ClanMap.get(OtherClan).relationships.containsKey(clanId)) {
            switch (ClanMap.get(OtherClan).relationships.get(clanId).relation) {
                case ALLY:
                    sendClanwideMessage(OtherClan, new TextComponentString(DemConstants.TextColour.INFO + ClanMap.get(clanId).name + " is no longer your Ally, however you still regard them as one" + (ClanConfig.clanSubCat.allyBuild ? ", this means they can build on your land, but you can't build on theirs" : "") + ", you can remove them as allies with /Clan neutral " + ClanMap.get(clanId).name));
                    break;
                case ENEMY:
                    sendClanwideMessage(OtherClan, new TextComponentString(DemConstants.TextColour.INFO + ClanMap.get(clanId).name + " is no longer your enemy, however you still regard them as one" + (ClanConfig.clanSubCat.enemyBuild ? ", this means they you can still build on each other's land" : "") + ", you can remove them as an enemy with /Clan neutral " + ClanMap.get(clanId).name));
                    break;
                case PENDINGALLY:
                case PENDINGENEMY:
                    sendClanwideMessage(OtherClan, new TextComponentString(DemConstants.TextColour.INFO + ClanMap.get(clanId).name + " No longer wants to be your " + (ClanMap.get(OtherClan).relationships.get(clanId).relation == RelationState.PENDINGALLY ? "ally, " + (ClanConfig.clanSubCat.allyBuild ? "you can no longer build on their land" : "") : "enemy" + (ClanConfig.clanSubCat.enemyBuild ? "you can no longer build on each other's land" : ""))));
                    setClanRelation(OtherClan, clanId, null);
                    break;
            }
        }
        switch (currentRelation.relation) {
            case ALLY:
                return 2;
            case ENEMY:
                return 1;
            default:
                return 0;
        }
    }

    /**
     * Sets the given Clan's relation with the other given Clan to the given relation
     * @param clanId The Clan the relation is being set for
     * @param OtherClanID The Clan the relation is being set about
     * @param NewRelation The new relation to set the Clan to, if null then removes the relationship
     */
    public void setClanRelation(UUID clanId, UUID OtherClanID, @Nullable RelationState NewRelation) {
        if (NewRelation != null) getClan(clanId).relationships.put(OtherClanID, new Relationship(NewRelation));
        else getClan(clanId).relationships.remove(OtherClanID);
        saveClan(clanId);
    }

    /**
     * Attempts to set the Clan home to the given position
     *
     * @param clanId   The ID of the Clan creating the home
     * @param position The position of the home
     * @return Whether the claim was a success
     */
    public boolean setClanHome(UUID clanId, Location position) {
        ChunkLocation chunk = ChunkLocation.coordsToChunkCoords(position.dim, position.x, position.z);
        String chunkKey = makeChunkKey(chunk.x, chunk.z);
        if (ClaimedLand.containsKey(position.dim) && ClaimedLand.get(position.dim).containsKey(chunkKey)) {
            if (ClaimedLand.get(position.dim).get(chunkKey).equals(clanId)) {
                ClanMap.get(clanId).homePos = position;
                saveClan(clanId);
                return true;
            }
        }
        return false;
    }

    // IO Functions
    // Save
    public void saveClan(UUID clanId) {
        if (ClanMap.containsKey(clanId)) {
            Gson gson = new Gson();
            File clanFile;
            if (clanId.equals(WILDID))
                clanFile = FileHelper.openFile(new File(ClanFileHelper.getDefaultClanDir(), "Wild.json"));
            else if (clanId.equals(SAFEID))
                clanFile = FileHelper.openFile(new File(ClanFileHelper.getDefaultClanDir(), "SafeZone.json"));
            else if (clanId.equals(WARID))
                clanFile = FileHelper.openFile(new File(ClanFileHelper.getDefaultClanDir(), "WarZone.json"));
            else clanFile = FileHelper.openFile(new File(ClanFileHelper.getClansDir(), clanId.toString() + ".json"));
            if (clanFile == null) {
                return;
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(clanFile));
                String json = gson.toJson(ClanMap.get(clanId));
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void savePlayer(UUID PlayerID){
        if (PlayerMap.containsKey(PlayerID)){
            Gson gson = new Gson();
            File playerFile = FileHelper.openFile(new File(ClanFileHelper.getPlayerDir(), PlayerID.toString() + ".json"));
            if (playerFile == null){
                return;
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(playerFile));
                String json = gson.toJson(PlayerMap.get(PlayerID));
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveClaimedChunks(int dim){
        if (ClaimedLand.containsKey(dim)){
            Gson gson = new Gson();
            File dimFile = FileHelper.openFile(new File(ClanFileHelper.getClaimedDir(), dim + ".json"));
            if (dimFile == null){
                return;
            }
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(dimFile));
                String json = gson.toJson(ClaimedLand.get(dim));
                writer.write(json);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Generate default Clan
    public void generateWild() {
        ArrayList<String> flags = new ArrayList<>();
        flags.add("default");
        flags.add("permanent");
        flags.add("strongborders");
        flags.add("infinitepower");
        flags.add("unlimitedland");
        flags.add("unrelateable");
        Clan wild = new Clan(WILDID, "TheWild", "Everywhere that isn't owned by a Clan", flags);
        ClanMap.put(WILDID, wild);
        saveClan(WILDID);
    }

    public void generateSafeZone() {
        ArrayList<String> flags = new ArrayList<>();
        flags.add("default");
        flags.add("permanent");
        flags.add("strongborders");
        flags.add("infinitepower");
        flags.add("unlimitedland");
        flags.add("unrelateable");
        flags.add("nodamage");
        flags.add("nobuild");
        Clan wild = new Clan(SAFEID, "TheSafeZone", "You're pretty safe here", flags);
        ClanMap.put(SAFEID, wild);
        saveClan(SAFEID);
    }

    public void generateWarZone() {
        ArrayList<String> flags = new ArrayList<>();
        flags.add("default");
        flags.add("permanent");
        flags.add("strongborders");
        flags.add("infinitepower");
        flags.add("unlimitedland");
        flags.add("unrelateable");
        flags.add("bonuspower");
        Clan wild = new Clan(WARID, "TheWarZone", "You're not safe here, you will lose more power when you die, but will gain more power when you kill", flags);
        ClanMap.put(WARID, wild);
        saveClan(WARID);
    }

    // Load
    public void loadDefaultClans() {
        File clanFile;
        // Wild
        clanFile = new File(ClanFileHelper.getDefaultClanDir(), "Wild.json");
        if (clanFile.exists()) {
            loadClan(clanFile, WILDID);
        } else {
            generateWild();
        }

        // SafeZone
        clanFile = new File(ClanFileHelper.getDefaultClanDir(), "SafeZone.json");
        if (clanFile.exists()) {
            loadClan(clanFile, SAFEID);
        } else {
            generateSafeZone();
        }

        // WarZone
        clanFile = new File(ClanFileHelper.getDefaultClanDir(), "WarZone.json");
        if (clanFile.exists()) {
            loadClan(clanFile, WARID);
        } else {
            generateWarZone();
        }
    }

    public void loadClans() {
        File[] Clan = ClanFileHelper.getClansDir().listFiles();
        if (Clan != null) {
            for (File clan : Clan) {
                loadClan(clan);
            }
        }
    }

    public void loadClan(File clanFile) {
        loadClan(clanFile, UUID.fromString(FileHelper.getBaseName(clanFile.getName())));
    }

    public void loadClan(File clanFile, UUID ID) {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(clanFile)) {

            Clan clanObject = gson.fromJson(reader, Clan.class);
            if (clanObject != null) {
                clanObject.clanId = ID;
                ClanMap.put(ID, clanObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteClanFile(UUID clanId) {
        File clanFile = new File(ClanFileHelper.getClansDir(), clanId + ".json");
        if (clanFile.exists()) {
            clanFile.delete();
        }
    }

    public void loadClan(UUID id) {
        File theFile = new File(ClanFileHelper.getClansDir(), id.toString() + ".json");
        if (theFile.exists()) loadClan(theFile);
    }

    public void loadPlayers(){
        File[] players = ClanFileHelper.getPlayerDir().listFiles();
        if (players != null) {
            for (File player : players){
                loadPlayer(player);
            }
        }
    }

    public void loadPlayer(File playerFile){
        Gson gson = new Gson();
        try (Reader reader = new FileReader(playerFile)) {
            Player playerObject = gson.fromJson(reader, Player.class);
            if (playerObject != null) {
                PlayerMap.put(UUID.fromString(FileHelper.getBaseName(playerFile.getName())), playerObject);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayer(UUID id){
        File theFile = new File(ClanFileHelper.getPlayerDir(), id.toString()  + ".json");
        if (theFile.exists()) loadClan(theFile);
    }

    public void loadClaimedChunks(){
        File[] dims = ClanFileHelper.getClaimedDir().listFiles();
        if (dims != null) {
            for (File dim : dims){
                loadClaimedChunkDim(dim);
            }
        }
    }

    public void loadClaimedChunkDim(File dimFile){
        Gson gson = new Gson();
        try (Reader reader = new FileReader(dimFile)){
            Type typeOfHashMap = new TypeToken<HashMap<String, UUID>>() {
            }.getType();
            HashMap<String, UUID> dimChunks = gson.fromJson(reader, typeOfHashMap);
            ClaimedLand.put(Integer.parseInt(FileHelper.getBaseName(dimFile.getName())), dimChunks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadClaimedChunkDim(String dim){
        File theFile = new File(ClanFileHelper.getClaimedDir(), dim  + ".json");
        if (theFile.exists()) loadClaimedChunkDim(theFile);
    }
}
