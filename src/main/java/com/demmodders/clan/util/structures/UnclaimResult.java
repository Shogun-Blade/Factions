package com.demmodders.clan.util.structures;

import com.demmodders.clan.util.enums.EUnclaimResult;

public class UnclaimResult {
    public EUnclaimResult result;
    public int count;

    public UnclaimResult(EUnclaimResult result, int count) {
        this.result = result;
        this.count = count;
    }
}
