package com.demmodders.clan.clan;

import com.demmodders.clan.Clans;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.DemUtils;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.clan.util.enums.RelationState;
import com.demmodders.clan.util.structures.Power;
import com.demmodders.clan.util.structures.Relationship;
import com.demmodders.datmoddingapi.structures.ChunkLocation;
import com.demmodders.datmoddingapi.structures.Location;
import com.demmodders.datmoddingapi.util.DemConstants;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Clan {
    public transient UUID clanId;
    public String name = "";
    public String desc = "";
    public String motd = "";
    public Location homePos = null;
    public Long foundingTime = 0L;
    public Power power = null;
    public ArrayList<UUID> invites = new ArrayList<>();
    public ArrayList<String> flags = new ArrayList<>();
    public HashMap<UUID, Relationship> relationships = new HashMap<>();
    public transient ArrayList<UUID> members = new ArrayList<>();
    public transient HashMap<Integer, ArrayList<String>> land = new HashMap<>();

    Clan() {

    }

    Clan(UUID clanId, String Name, String Desc, ArrayList<String> Flags) {
        this.clanId = clanId;
        name = Name;
        desc = Desc;

        power = new Power(ClanConfig.clanSubCat.clanStartingPower, ClanConfig.clanSubCat.clanStartingMaxPower);
        flags = Flags;
    }

    public Clan(UUID clanId, String name, UUID playerID) {
        this.clanId = clanId;
        this.name = name;
        this.foundingTime = System.currentTimeMillis();

        power = new Power(ClanConfig.clanSubCat.clanStartingPower, ClanConfig.clanSubCat.clanStartingMaxPower);
        invites = new ArrayList<>();

        flags = new ArrayList<>();
        relationships = new HashMap<>();

        this.land = new HashMap<>();
        this.members.add(playerID);
    }

    /**
     * Adds the specified player to the Clan
     *
     * @param PlayerID the ID of the player to add
     */
    public void addPlayer(UUID PlayerID){
        if (!members.contains(PlayerID)) {
            members.add(PlayerID);
        } else {
            Clans.LOGGER.info("Tried to add player " + PlayerID + " to Clan " + name + " when it already has that player, ignoring");
        }
    }

    /**
     * Removes the specified player to the Clan
     * @param PlayerID the ID of the player to remove
     */
    public void removePlayer(UUID PlayerID){
        if (members.contains(PlayerID)) {
            members.remove(PlayerID);
        } else {
            Clans.LOGGER.info("Tried to remove player " + PlayerID + " from Clan " + name + " when it didn't have that player, ignoring");
        }
    }

    /**
     * Adds chunk to the Clan
     *
     * @param Dimension The dimension of the chunk
     * @param Land      The position of the chunk
     */
    public void addLandToClan(int Dimension, String Land) {
        if (!land.containsKey(Dimension)) {
            land.put(Dimension, new ArrayList<>());
        }

        if (!land.get(Dimension).contains(Land)) {
            land.get(Dimension).add(Land);
        } else {
            Clans.LOGGER.info("Tried to add claimed land " + Land + " in Dim " + Dimension + " to Clan " + name + " when it already has that land, ignoring");
        }
    }

    /**
     * removes chunk from the Clan
     *
     * @param Location The Location of the chunk
     */
    public void removeLandFromClan(ChunkLocation Location) {
        String chunkKey = ClanManager.makeChunkKey(Location.x, Location.z);
        removeLandFromClan(Location.dim, chunkKey);
    }

    /**
     * removes chunk from the Clan
     *
     * @param Dimension The dimension of the chunk
     * @param Land      The position of the chunk
     */
    public void removeLandFromClan(int Dimension, String Land) {
        if (land.containsKey(Dimension) && land.get(Dimension).contains(Land)) {
            land.get(Dimension).remove(Land);
        } else {
            Clans.LOGGER.info("Tried to remove claimed land " + Land + " in Dim " + Dimension + " from Clan " + name + " when it didn't have that land, ignoring");
        }
    }

    /**
     * Get the relation with the given Clan ID
     *
     * @param clanId The Clan to check the relation with
     * @return The relation with the other Clan, null if neutral
     */
    @Nullable
    public RelationState getRelation(UUID clanId) {
        Relationship relation = relationships.getOrDefault(clanId, null);
        if (relation != null) {
            return relation.relation;
        }
        return null;
    }

    public long getLastOnline() {
        long latest = foundingTime;
        ClanManager clanManager = ClanManager.getInstance();

        for (UUID playerID : members) {
            if (FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(playerID) != null) {
                return 0L;
            }
            Player player = clanManager.getPlayer(playerID);
            latest = Math.max(latest, player.lastOnline);
        }

        return latest;
    }

    /**
     * Gets the owner of the Clan
     * @return The owner of the Clan
     */
    public UUID getOwnerID(){
        for (UUID member : members) {
            if (ClanManager.getInstance().getPlayer(member).clanRank == ClanRank.OWNER) {
                return member;
            }
        }
        return null;
    }

    public List<String> getMemberNames(){
        List<String> memberList = new ArrayList<>();
        for (UUID member : members) {
            memberList.add(ClanManager.getInstance().getPlayer(member).lastKnownName);
        }
        return memberList;
    }

    /**
     * Checks if the Clan has the specified flag
     * @param Flag The flag to check for
     * @return Whether the Clan has the flag
     */
    public boolean hasFlag(String Flag){
        return flags.contains(Flag);
    }

    /**
     * Adds the specified flag to the Clan
     * @param Flag The flag to add
     */
    public void setFlag(String Flag){
        if (!hasFlag(Flag)){
            flags.add(Flag);
        }
    }

    /**
     * Removes the specified flag to the Clan
     * @param Flag The flag to remove
     */
    public void removeFlag(String Flag){
        if (hasFlag(Flag)){
            flags.remove(Flag);
        }
    }

    /**
     * Calculates the amount of power this Clan has
     * @return The amount of power the Clan has
     */
    public int calculatePower() {
        if (hasFlag("infinitepower")) return Integer.MAX_VALUE;
        int clanPower = power.power;
        ClanManager clanManager = ClanManager.getInstance();
        for (UUID memberID : members) {
            clanPower += clanManager.getPlayer(memberID).power.power;
        }
        return clanPower;
    }

    /**
     *  Calculates the maximum amount of power this Clan can have
     * @return the maximum amount of power the Clan can have
     */
    public int calculateMaxPower() {
        if (hasFlag("infinitepower")) return Integer.MAX_VALUE;
        int clanMaxPower = power.maxPower;
        ClanManager clanManager = ClanManager.getInstance();
        for (UUID memberID : members) {
            clanMaxPower += clanManager.getPlayer(memberID).power.maxPower;
        }
        return clanMaxPower;
    }

    /**
     * Calculates the cost of all the land this Clan owns
     * @return the cost of the land this Clan owns
     */
    public int calculateLandValue(){
        int landCount = 0;
        for (int dim: land.keySet()) {
            landCount += land.get(dim).size();
        }
        return landCount * ClanConfig.landSubCat.landPowerCost;
    }

    /**
     * Calculates whether the Clan can have the extra amount of land given
     * @param extraLand the amount more land to test
     * @return Whether the Clan can claim the amount of land given
     */
    public boolean checkCanAffordLand(int extraLand){
        int landCount = extraLand;
        for (int dim: land.keySet()) {
            landCount += land.get(dim).size();
        }
        return landCount * ClanConfig.landSubCat.landPowerCost <= calculatePower();
    }


    /**
     * Checks to see if the given chunk is connected to any other chunk owned by the Clan, also true if the player has no land in that dimension
     * @param Dim The dimension of the chunk to check
     * @param X The X Coord of the chunk
     * @param Z The Z Coord of the chunl
     * @return If the chuck is connected to another owned chunk, or the Clan has no land
     */
    public boolean checkLandTouches(int Dim, int X, int Z){
        if (land.size() == 0){
            return true;
        }
        else if (land.get(Dim).size() == 0) return true;
        String[] coords;
        for (String key : land.get(Dim)){
            coords = key.split(", ");
            if (X == Integer.parseInt(coords[0]) && Z == (Integer.parseInt(coords[1]) + 1) ||
                X == Integer.parseInt(coords[0]) && Z == (Integer.parseInt(coords[1]) - 1) ||
                X == (Integer.parseInt(coords[0]) + 1) && Z == Integer.parseInt(coords[1]) ||
                X == (Integer.parseInt(coords[0]) - 1) && Z == Integer.parseInt(coords[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds a message giving information about the Clan
     *
     * @return a long message detailing public information about the Clan
     */
    public String printClanInfo(UUID askingClan) {


        // Work out age to display
        StringBuilder message = new StringBuilder();

        long lastOnline = getLastOnline();


        // Work out invitation policy to display
        String invitePolicy;
        if (hasFlag("open")) {
            invitePolicy = "Open";
        } else {
            invitePolicy = "Invite only";
        }

        ClanManager clanManager = ClanManager.getInstance();

        // Format Members
        StringBuilder memberText = new StringBuilder();
        boolean first = true;
        for (UUID player : members) {
            if (!first) {
                memberText.append(TextFormatting.RESET).append(", ");
            } else {
                first = false;
            }

            memberText.append(clanManager.getPlayerStatusColour(player, false)).append(clanManager.getPlayer(player).lastKnownName);
        }

        StringBuilder allies = new StringBuilder();
        StringBuilder enemies = new StringBuilder();
        first = true;
        boolean enemyFirst = true;

        for (UUID clanId : relationships.keySet()) {
            if (relationships.get(clanId).relation == RelationState.ALLY) {
                if (!first) allies.append(TextFormatting.RESET).append(", ");
                else first = false;
                allies.append(ClanConstants.TextColour.ALLY).append(clanManager.getClan(clanId).name);
            } else if (relationships.get(clanId).relation == RelationState.ENEMY) {
                if (!enemyFirst) allies.append(TextFormatting.RESET).append(", ");
                else enemyFirst = false;
                enemies.append(ClanConstants.TextColour.ENEMY).append(clanManager.getClan(clanId).name);
            }
        }

        message.append(DemConstants.TextColour.INFO).append("======").append(TextFormatting.RESET).append(clanManager.getRelationColour(askingClan, clanId)).append(name).append(DemConstants.TextColour.INFO).append("======\n");
        message.append(DemConstants.TextColour.INFO).append("Description: ").append(TextFormatting.RESET).append(desc).append("\n");
        if (lastOnline != 0L) {
            message.append(DemConstants.TextColour.INFO).append("Last Online: ").append(TextFormatting.RESET).append(DemUtils.displayAge(DemUtils.calculateAge(lastOnline) / 60000)).append(" ago").append("\n");
        }
        message.append(DemConstants.TextColour.INFO).append("Age: ").append(TextFormatting.RESET).append(DemUtils.displayAge(DemUtils.calculateAge(foundingTime) / 60000)).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Invitation Policy: ").append(TextFormatting.RESET).append(invitePolicy).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Land worth: ").append(TextFormatting.RESET).append(calculateLandValue()).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Power: ").append(TextFormatting.RESET).append(calculatePower()).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Max Power: ").append(TextFormatting.RESET).append(calculateMaxPower()).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Members: ").append(memberText.toString()).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Allies: ").append(allies.toString()).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Enemies: ").append(enemies.toString());

        return message.toString();
    }
}


