package com.demmodders.clan.api.event;

import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.clan.Player;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;
import java.util.UUID;

public class ClanEvent extends Event {
    public final UUID causingPlayerID;

    public ClanEvent(@Nullable UUID Player) {
        causingPlayerID = Player;
    }

    public EntityPlayerMP getPlayerMP() {
        return ClanManager.getPlayerMPFromUUID(causingPlayerID);
    }

    public Player getClanPlayer() {
        return ClanManager.getInstance().getPlayer(causingPlayerID);
    }
}
