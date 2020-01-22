package com.demmodders.factions.events;

import com.demmodders.factions.Factions;
import com.demmodders.factions.faction.FactionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.Sys;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Factions.MODID)
public class PlayerEvents {
    public static final Logger LOGGER = LogManager.getLogger(Factions.MODID);

    @SubscribeEvent
    public static void playerLogin(PlayerLoggedInEvent e){
        if (FactionManager.getInstance().isPlayerRegistered(e.player.getUniqueID())) {
            // record username for use when they're offline
            FactionManager.getInstance().setPlayerLastKnownName(e.player.getUniqueID(), e.player.getName());
        } else {
            FactionManager.getInstance().registerPlayer(e.player);
        }
    }

    @SubscribeEvent
    public static void chunkTraversal(EntityEvent.EnteringChunk e){
        if(e.getEntity() instanceof EntityPlayer) {
            FactionManager.getInstance().getChunkOwningFaction(e.getEntity().dimension, e.getNewChunkX(), e.getNewChunkZ());
            LOGGER.info("Dimension ID = " + e.getEntity().dimension);
        }
    }

    // TODO: log last seen when player leaves
}
