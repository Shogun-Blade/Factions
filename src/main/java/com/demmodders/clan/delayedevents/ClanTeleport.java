package com.demmodders.clan.delayedevents;

import com.demmodders.clan.clan.ClanManager;
import com.demmodders.datmoddingapi.delayedexecution.delayedevents.DelayedTeleportEvent;
import com.demmodders.datmoddingapi.structures.Location;
import net.minecraft.entity.player.EntityPlayerMP;

public class ClanTeleport extends DelayedTeleportEvent {
    public ClanTeleport(Location Destination, EntityPlayerMP Player, int Delay) {
        super(Destination, Player, Delay);
    }

    @Override
    public void execute() {
        super.execute();
        ClanManager.getInstance().getPlayer(player.getUniqueID()).lastTeleport = System.currentTimeMillis();
    }
}