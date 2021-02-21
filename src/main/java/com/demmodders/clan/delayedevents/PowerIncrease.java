package com.demmodders.clan.delayedevents;

import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.clan.Player;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.datmoddingapi.delayedexecution.delayedevents.BaseDelayedEvent;
import com.demmodders.datmoddingapi.util.DemConstants;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

public class PowerIncrease extends BaseDelayedEvent {
    public boolean cancelled = false;
    public EntityPlayerMP player;
    private final int delay;

    public PowerIncrease(int Delay, EntityPlayerMP Player) {
        super(Delay);
        delay = Delay;
        player = Player;
    }

    public double rankMultiplier(ClanRank rank) {
        if (rank != null) {
            switch (rank) {
                case GRUNT:
                    return ClanConfig.powerSubCat.powerGainGruntMultiplier;
                case LIEUTENANT:
                    return ClanConfig.powerSubCat.powerGainLieutenantMultiplier;
                case OFFICER:
                    return ClanConfig.powerSubCat.powerGainSergeantMultiplier;
                case OWNER:
                    return ClanConfig.powerSubCat.powerGainOwnerMultiplier;
            }
        }
        return 1.D;
    }

    @Override
    public void execute() {
        // Only execute when a player is in a Clan
        if (!ClanManager.getInstance().getPlayersClanID(player.getUniqueID()).equals(ClanManager.WILDID)) {
            Player clanPlayer = ClanManager.getInstance().getPlayer(this.player.getUniqueID());
            clanPlayer.addMaxPower((int) Math.ceil(ClanConfig.powerSubCat.maxPowerGainAmount * rankMultiplier(clanPlayer.clanRank)));
            clanPlayer.addPower((int) Math.ceil(ClanConfig.powerSubCat.powerGainAmount * rankMultiplier(clanPlayer.clanRank)));
            this.player.sendMessage(new TextComponentString(DemConstants.TextColour.INFO + "You just gained power! Your power is now " + clanPlayer.power.power + "/" + clanPlayer.power.maxPower));
        }
        exeTime = System.currentTimeMillis() + delay * 1000L;
    }

    @Override
    public boolean canExecute() {
        if (player.hasDisconnected()){
            cancelled = true;
        }
        return super.canExecute() && !cancelled;
    }

    @Override
    public boolean shouldRequeue(boolean hasFinished) {
        return !cancelled;
    }
}
