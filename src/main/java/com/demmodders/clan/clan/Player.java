package com.demmodders.clan.clan;

import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.DemUtils;
import com.demmodders.clan.util.enums.ClanChatMode;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.clan.util.structures.Power;
import com.demmodders.datmoddingapi.util.DemConstants;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.UUID;

public class Player {
    public UUID Clan = null;
    public ClanRank clanRank = null;
    public Power power = null;
    public String lastKnownName = "";
    public long lastOnline = 0L;
    public transient UUID lastClanLand = null;
    public transient boolean autoClaim = false;
    public transient ArrayList<UUID> invites = new ArrayList<>();
    public transient ClanChatMode clanChat = ClanChatMode.NORMAL;

    public transient long lastTeleport = 0L;

    public Player() {
    }

    public Player(UUID Clan, ClanRank Rank, Power power, String name) {
        this.Clan = Clan;
        this.clanRank = Rank;
        this.power = power;
        this.lastKnownName = name;
    }

    public boolean hasInvite(UUID clanId) {
        return invites.contains(clanId);
    }

    /**
     * Removes any Clan stuff from the player
     */
    public void clearFaction() {
        this.Clan = null;
        this.clanRank = null;
        clanChat = ClanChatMode.NORMAL;
    }

    /**
     * Adds the given amount of power to the player, clamped below their maximum power
     * @param Power The amount of power to add to the player
     */
    public void addPower(int Power){
        power.setPower(Power + power.power);
    }

    /**
     * Adds the given amount of power to the player's max power
     * @param MaxPower The amount of max power to add to the player
     */
    public void addMaxPower(int MaxPower){
        if (MaxPower + power.maxPower > ClanConfig.playerSubCat.playerMaxPowerCap)
            power.maxPower = ClanConfig.playerSubCat.playerMaxPowerCap;
        else power.maxPower = MaxPower + power.maxPower;
    }

    public String printPlayerInfo(UUID askingFaction) {
        ClanManager clanManager = ClanManager.getInstance();

        String relationColour = clanManager.getRelationColour(askingFaction, Clan);

        StringBuilder message = new StringBuilder();

        message.append(DemConstants.TextColour.INFO).append("======").append(ClanConstants.TextColour.OWN).append(lastKnownName).append(DemConstants.TextColour.INFO).append("======\n");
        message.append(DemConstants.TextColour.INFO).append("Clan: ").append(TextFormatting.RESET).append(relationColour).append(Clan != ClanManager.WILDID ? clanManager.getClan(Clan).name : "N/A").append("\n");
        message.append(DemConstants.TextColour.INFO).append("Rank: ").append(clanRank != null ? ClanRank.getClanRankString(clanRank) : "N/A").append("\n");
        if (lastOnline == 0L) {
            message.append(DemConstants.TextColour.INFO).append("Last Online: ").append(DemUtils.displayAge(DemUtils.calculateAge(lastOnline) / 60000)).append(" ago").append("\n");
        }
        message.append(DemConstants.TextColour.INFO).append("Personal Power: ").append(power.power).append("\n");
        message.append(DemConstants.TextColour.INFO).append("Personal Max Power: ").append(power.maxPower);

        return message.toString();
    }
}

