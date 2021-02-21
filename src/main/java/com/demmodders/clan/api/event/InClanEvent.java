package com.demmodders.clan.api.event;

import com.demmodders.clan.clan.Clan;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.util.enums.ClaimType;
import com.demmodders.clan.util.enums.RelationState;
import com.demmodders.datmoddingapi.structures.ChunkLocation;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

import java.util.List;
import java.util.UUID;

public class InClanEvent extends ClanEvent {
    public final UUID clanID;

    public InClanEvent(UUID player, UUID clanID) {
        super(player);
        this.clanID = clanID;
    }

    public Clan getClan() {
        return ClanManager.getInstance().getClan(clanID);
    }

    public static class ChunkEvent extends InClanEvent {
        public final List<ChunkLocation> positions;
        public final ClaimType type;

        public ChunkEvent(List<ChunkLocation> positions, UUID player, UUID clanID, ClaimType type) {
            super(player, clanID);
            this.positions = positions;
            this.type = type;
        }

        /**
         * Fired when a chunk is about to be claimed
         */
        @Cancelable
        public static class ClanClaimEvent extends ChunkEvent {

            public ClanClaimEvent(List<ChunkLocation> positions, UUID player, UUID clanId, ClaimType type) {
                super(positions, player, clanId, type);
            }
        }

        /**
         * Fired when a chunk is about to be unclaimed
         */
        @Cancelable
        public static class ClanUnClaimEvent extends ChunkEvent {
            public ClanUnClaimEvent(List<ChunkLocation> chunkLocations, UUID playerID, UUID clanId, ClaimType type) {
                super(chunkLocations, playerID, clanId, type);
            }
        }
    }

    /**
     * Fired when a clan is about to be renamed
     */
    @Cancelable
    public static class ClanRenameEvent extends InClanEvent {
        public final String newName;

        public ClanRenameEvent(UUID player, UUID clanId, String newName) {
            super(player, clanId);
            this.newName = newName;
        }
    }

    /**
     * Fired when a clan is about to be disbanded
     */
    @Cancelable
    public static class ClanDisbandEvent extends InClanEvent {
        public ClanDisbandEvent(UUID player, UUID clanId) {
            super(player, clanId);
        }
    }

    /**
     * Fired when a clan is about to change relation with another Clan
     */
    @Cancelable
    public static class ClanRelationEvent extends InClanEvent {
        public final UUID otherClan;
        public final RelationState newRelation;

        public ClanRelationEvent(UUID player, UUID clanId, UUID otherClan, RelationState newRelation) {
            super(player, clanId);
            this.otherClan = otherClan;
            this.newRelation = newRelation;
        }
    }
}
