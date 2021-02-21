package com.demmodders.clan.events;

import com.demmodders.clan.Clans;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.clan.Player;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.enums.ClanChatMode;
import com.demmodders.clan.util.enums.RelationState;
import com.demmodders.clan.util.structures.Relationship;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.UUID;

import static net.minecraftforge.fml.common.eventhandler.EventPriority.NORMAL;

@EventBusSubscriber(modid = Clans.MODID)
public class ChatEvents {
    public static final Logger LOGGER = LogManager.getLogger(Clans.MODID);

    @SubscribeEvent(priority = NORMAL)
    public static void checkFactionChat(ServerChatEvent e) {
        UUID playerID = e.getPlayer().getUniqueID();
        Player player = ClanManager.getInstance().getPlayer(playerID);
        if (player == null) {
            LOGGER.warn("An unknown player tried to talk, maybe its a fake one from a mod, ignoring");
            return;
        }

        // Get Clan details
        ClanManager clanManager = ClanManager.getInstance();
        UUID clanId = clanManager.getPlayersClanID(playerID);

        if (ClanConfig.clanSubCat.chatFName && !ClanManager.WILDID.equals(clanId)) {
            e.setComponent((new TextComponentString(TextFormatting.DARK_GREEN + "[" + clanManager.getClan(clanId).name + "]")).appendSibling(e.getComponent()));
        }

        if (player.clanChat != ClanChatMode.NORMAL) {
            // Cancel event
            e.setCanceled(true);

            // Make message
            ITextComponent message = (new TextComponentString(TextFormatting.DARK_GREEN + "[Clan Chat]")).appendSibling(e.getComponent());
            clanManager.sendClanwideMessage(clanId, message);

            // Send to allies if enabled
            if (clanManager.getPlayer(playerID).clanChat == ClanChatMode.ALLY) {
                HashMap<UUID, Relationship> relationships = clanManager.getClan(clanId).relationships;
                for (UUID OtherClan : relationships.keySet()) {
                    // Only send message to allies
                    if (relationships.get(OtherClan).relation == RelationState.ALLY)
                        clanManager.sendClanwideMessage(OtherClan, message);
                }
            }
        }
    }
}
