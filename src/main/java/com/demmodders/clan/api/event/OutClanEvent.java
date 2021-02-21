package com.demmodders.clan.api.event;

import java.util.UUID;

public class OutClanEvent extends ClanEvent {
    public OutClanEvent(UUID Player) {
        super(Player);
    }

    /**
     * Fired when a clan is about to be created
     */
    public static class ClanCreateEvent extends OutClanEvent {
        public final String clanName;

        public ClanCreateEvent(UUID player, String clanName) {
            super(player);
            this.clanName = clanName;
        }
    }
}
