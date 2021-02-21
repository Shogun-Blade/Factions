package com.demmodders.clan.delayedevents;

import com.demmodders.clan.clan.Clan;
import com.demmodders.clan.clan.ClanManager;
import com.demmodders.clan.util.ClanConfig;
import com.demmodders.clan.util.DemUtils;
import com.demmodders.datmoddingapi.delayedexecution.delayedevents.BaseDelayedEvent;

public class ClanCleanout extends BaseDelayedEvent {
    boolean cancelled = false;
    ClanManager clanManager;

    public ClanCleanout() {
        super(ClanConfig.clanSubCat.clearClanPeriod * 60);
        clanManager = ClanManager.getInstance();
    }

    @Override
    public void execute() {
        for (Clan clan : clanManager.getListOfClans()) {
            if (DemUtils.calculateAge(clan.getLastOnline()) > (long) ClanConfig.clanSubCat.maxClanOffline * 60000) {
                clanManager.disbandClan(clan.clanId, null);
            }
        }
        exeTime = System.currentTimeMillis() + ClanConfig.clanSubCat.clearClanPeriod * 60000L;
    }

    @Override
    public boolean shouldRequeue(boolean hasFinished) {
        return !cancelled;
    }
}
