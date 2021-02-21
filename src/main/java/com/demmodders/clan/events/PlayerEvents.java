package com.demmodders.clan.events;

import com.demmodders.clan.Clans;
import com.demmodders.clan.clan.Clan;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.delayedevents.PowerIncrease;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.ClanConstants;
import com.demmodders.clan.util.enums.ClanRank;
import com.demmodders.clan.util.enums.RelationState;
import com.demmodders.clan.util.structures.ClaimResult;
import com.demmodders.datmoddingapi.delayedexecution.DelayHandler;
import com.demmodders.datmoddingapi.structures.ChunkLocation;
import com.demmodders.datmoddingapi.util.DemConstants;
import com.demmodders.datmoddingapi.util.DemStringUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = Clans.MODID)
public class PlayerEvents {
    public static final Logger LOGGER = LogManager.getLogger(Clans.MODID);

    @SubscribeEvent
    public static void playerLogin(PlayerLoggedInEvent e) {
        ClanManager clanManager = ClanManager.getInstance();
        if (clanManager.isPlayerRegistered(e.player.getUniqueID())) {
            // record username for use when they're offline, for listing members
            clanManager.setPlayerLastKnownName(e.player.getUniqueID(), e.player.getName());
            clanManager.setPlayerLastOnline(e.player.getUniqueID(), 0L);
        } else {
            LOGGER.info(e.player.getName() + " is not registered with Clan, amending");
            clanManager.registerPlayer(e.player);
        }

        // Add event for the player to
        DelayHandler.addEvent(new PowerIncrease(ClanConfig.powerSubCat.powerGainInterval, (EntityPlayerMP) e.player));
    }

    @SubscribeEvent
    public static void playerLogoff(PlayerLoggedOutEvent e) {
        ClanManager clanManager = ClanManager.getInstance();
        clanManager.setPlayerLastOnline(e.player.getUniqueID(), System.currentTimeMillis());
    }

    private static double rankModifier(ClanRank rank) {
        if (rank != null) {
            switch (rank) {
                case LIEUTENANT:
                    return ClanConfig.powerSubCat.lieutenantMultiplier;
                case OFFICER:
                    return ClanConfig.powerSubCat.officerMultiplier;
                case OWNER:
                    return ClanConfig.powerSubCat.ownerMultiplier;
            }
        }
        return 1.D;
    }

    @SubscribeEvent
    public static void playerKilled(LivingDeathEvent e){
        // TODO: CHECK
        if (e.getEntity() instanceof EntityPlayer && e.getSource().getTrueSource() instanceof EntityPlayer) {
            ClanManager clanManager = ClanManager.getInstance();
            UUID killedFaction = clanManager.getPlayersClanID(e.getEntity().getUniqueID());
            UUID killerFaction = clanManager.getPlayersClanID(e.getSource().getTrueSource().getUniqueID());
            UUID chunkOwner = clanManager.getChunkOwningFaction(e.getEntity().dimension, e.getEntity().chunkCoordX, e.getEntity().chunkCoordZ);
            RelationState relation = clanManager.getClan(killedFaction).getRelation(killerFaction);

            double relationModifier = 1.D;

            double landMultiplier = (clanManager.getClan(chunkOwner).hasFlag("bonuspower") ? ClanConfig.flagSubCat.bonusPowerMultiplier : 1.D);
            double rankMultiplier = rankModifier(clanManager.getPlayer(e.getEntity().getUniqueID()).clanRank);

            // Power Gain
            // Only gain power if they're not wild
            if (!killedFaction.equals(ClanManager.WILDID)) {
                if (relation != null) {
                    switch (relation) {
                        case ENEMY:
                            relationModifier = ClanConfig.powerSubCat.enemyMultiplier;
                            break;
                        case ALLY:
                            relationModifier = ClanConfig.powerSubCat.allyKillMultiplier;
                            break;
                    }
                }

                clanManager.getPlayer(e.getSource().getTrueSource().getUniqueID()).addMaxPower((int) Math.ceil(ClanConfig.powerSubCat.killMaxPowerGain * landMultiplier * relationModifier * rankMultiplier));
                clanManager.getPlayer(e.getSource().getTrueSource().getUniqueID()).addPower((int) Math.ceil(ClanConfig.powerSubCat.killPowerGain * landMultiplier * relationModifier * rankMultiplier));
                e.getSource().getTrueSource().sendMessage(new TextComponentString(TextFormatting.GREEN + "You've gained power, your power is now: " + clanManager.getPlayer(e.getSource().getTrueSource().getUniqueID()).power.power + "/" + clanManager.getPlayer(e.getSource().getTrueSource().getUniqueID()).power.maxPower));
            }

            // Power Loss
            if (relation != null) {
                switch (relation) {
                    case ENEMY:
                        relationModifier = ClanConfig.powerSubCat.enemyMultiplier;
                        break;
                    case ALLY:
                        relationModifier = ClanConfig.powerSubCat.killedByAllyMultiplier;
                        break;
                }
            }

            clanManager.getPlayer(e.getEntity().getUniqueID()).addPower((int) Math.ceil(-1.D * ClanConfig.powerSubCat.deathPowerLoss * landMultiplier * relationModifier));
            e.getEntity().sendMessage(new TextComponentString(TextFormatting.RED + "You've lost power, your power is now: " + clanManager.getPlayer(e.getEntity().getUniqueID()).power.power + "/" + clanManager.getPlayer(e.getEntity().getUniqueID()).power.maxPower));
        }
    }

    @SubscribeEvent
    public static void enterChunk(EntityEvent.EnteringChunk e) {
        // This fires really weirdly, sometimes 3 times giving: Clan land, wild land, Clan land, its really weird
        // Make sure its a player

        ClanManager clanManager = ClanManager.getInstance();
        if (e.getEntity() instanceof EntityPlayer && clanManager.isPlayerRegistered(e.getEntity().getUniqueID())) {
            UUID playerFaction = clanManager.getPlayersClanID(e.getEntity().getUniqueID());
            String message = "";

            // TODO: Test
            if (!playerFaction.equals(ClanManager.WILDID) && clanManager.getPlayer(e.getEntity().getUniqueID()).autoClaim) {
                ClaimResult result = clanManager.claimLand(playerFaction, e.getEntity().getUniqueID(), e.getEntity().dimension, e.getNewChunkX(), e.getNewChunkZ(), false);
                if (result != null) {
                    String replyMessage;
                    switch (result.result) {
                        case SUCCESS:
                            replyMessage = DemConstants.TextColour.INFO + "Successfully claimed " + (result.count == 1 ? "a chunk" : result.count + " chunks") + " for your Clan";
                            break;
                        case STOLEN:
                            replyMessage = DemConstants.TextColour.ERROR + "I don't know what to say, this isn't supposed to have happened, please inform me (The mod developer) on my discord server";
                            break;
                        case LACKPOWER:
                            replyMessage = DemConstants.TextColour.ERROR + "You do not have enough power to claim this land";
                            break;
                        case OWNED:
                            replyMessage = DemConstants.TextColour.ERROR + "You cannot auto-claim land from " + clanManager.getRelationColour(playerFaction, result.owner) + clanManager.getClan(result.owner).name;
                            break;
                        case YOUOWN:
                            replyMessage = DemConstants.TextColour.INFO + "You already own that land";
                            break;
                        case MUSTCONNECT:
                            replyMessage = DemConstants.TextColour.ERROR + "You can only claim land that connects to land you already own";
                            break;
                        case NAH:
                            replyMessage = DemConstants.TextColour.ERROR + "You're unable to claim this land";
                            break;
                    }
                }

                e.getEntity().sendMessage(new TextComponentString(message));
            }
            if (e.getOldChunkX() != e.getNewChunkX() || e.getOldChunkZ() != e.getNewChunkZ()) {
                UUID clanId = clanManager.getChunkOwningFaction(e.getEntity().dimension, e.getNewChunkX(), e.getNewChunkZ());
                Clan theClan = clanManager.getClan(clanId);

                if (clanManager.isPlayerRegistered(e.getEntity().getUniqueID()) && (clanManager.getPlayer(e.getEntity().getUniqueID()).lastClanLand == null || !clanManager.getPlayer(e.getEntity().getUniqueID()).lastClanLand.equals(clanId))) {
                    message = "";
                    if (clanId.equals(ClanManager.WILDID)) {
                        message = ClanConfig.clanSubCat.wildLandTag;
                    } else if (clanId.equals(ClanManager.SAFEID)) {
                        message = ClanConfig.clanSubCat.safeLandTag;
                    } else if (clanId.equals(ClanManager.WARID)) {
                        message = ClanConstants.TextColour.ENEMY + ClanConfig.clanSubCat.warLandTag;
                    } else if (playerFaction.equals(clanId)) {
                        message = "Now entering " + ClanConstants.TextColour.OWN + "your land";
                    } else {
                        if (theClan.desc.isEmpty()) message = ClanConfig.clanSubCat.clanLandTagNoDesc;
                        else message = ClanConfig.clanSubCat.clanLandTag;
                    }

                    e.getEntity().sendMessage(new TextComponentString(String.format(DemConstants.TextColour.INFO + message, clanManager.getRelationColour(playerFaction, clanId) + theClan.name + DemConstants.TextColour.INFO, clanManager.getRelationColour(playerFaction, clanId) + DemStringUtils.makePossessive((theClan.name)) + DemConstants.TextColour.INFO, theClan.desc)));
                    clanManager.getPlayer(e.getEntity().getUniqueID()).lastClanLand = clanId;
                }
            }
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent e) {
        ClanManager clanManager = ClanManager.getInstance();
        ChunkLocation chunk = ChunkLocation.coordsToChunkCoords(e.getPlayer().dimension, e.getPos().getX(), e.getPos().getZ());
        UUID chunkOwner = clanManager.getChunkOwningFaction(chunk);
        if (!clanManager.getPlayerCanBuild(chunkOwner, e.getPlayer().getUniqueID())) {
            e.getPlayer().sendMessage(new TextComponentString(DemConstants.TextColour.ERROR + "You're not allowed to build on " + DemStringUtils.makePossessive(clanManager.getClan(chunkOwner).name) + " Land"));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockPlace(BlockEvent.EntityPlaceEvent e){
        if (e.getEntity() instanceof EntityPlayer) {
            ClanManager clanManager = ClanManager.getInstance();
            ChunkLocation chunk = ChunkLocation.coordsToChunkCoords(e.getEntity().dimension, e.getPos().getX(), e.getPos().getZ());
            UUID chunkOwner = clanManager.getChunkOwningFaction(chunk);

            if (!clanManager.getPlayerCanBuild(chunkOwner, e.getEntity().getUniqueID())) {
                e.getEntity().sendMessage(new TextComponentString(DemConstants.TextColour.ERROR + "You're not allowed to build on " + DemStringUtils.makePossessive(clanManager.getClan(chunkOwner).name) + " Land"));
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void playerInteract(PlayerInteractEvent.RightClickBlock e) {
        if (ClanConfig.clanSubCat.inventoryBlockRestriction) {
            ClanManager clanManager = ClanManager.getInstance();
            ChunkLocation chunk = ChunkLocation.coordsToChunkCoords(e.getEntity().dimension, e.getPos().getX(), e.getPos().getZ());
            UUID chunkOwner = clanManager.getChunkOwningFaction(chunk);
            if (!clanManager.getPlayerCanBuild(chunkOwner, e.getEntity().getUniqueID())) {
                e.getEntity().sendMessage(new TextComponentString(DemConstants.TextColour.ERROR + "You're not allowed to interact with blocks on " + DemStringUtils.makePossessive(clanManager.getClan(chunkOwner).name) + " Land"));
                e.setUseBlock(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void playerAttack(LivingAttackEvent e){
        if (e.getEntity() instanceof EntityPlayer && e.getSource().getImmediateSource() instanceof EntityPlayer) {
            ClanManager clanManager = ClanManager.getInstance();
            UUID attackedPlayerFaction = clanManager.getPlayersClanID(e.getEntity().getUniqueID());
            UUID attackingPlayerFaction = clanManager.getPlayersClanID(e.getSource().getImmediateSource().getUniqueID());
            if (!attackedPlayerFaction.equals(ClanManager.WILDID) && !attackingPlayerFaction.equals(ClanManager.WILDID)) {
                if (attackingPlayerFaction.equals(attackedPlayerFaction) && !clanManager.getClan(attackedPlayerFaction).hasFlag("friendlyfire")) {
                    e.getSource().getImmediateSource().sendMessage(new TextComponentString(DemConstants.TextColour.ERROR + "You cannot damage other members of your Clan"));
                    e.setCanceled(true);
                }
            }
        }
    }
}
