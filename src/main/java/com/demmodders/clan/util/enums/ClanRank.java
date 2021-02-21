package com.demmodders.clan.util.enums;

import com.google.gson.annotations.SerializedName;

public enum ClanRank {
    @SerializedName("0")
    GRUNT,
    @SerializedName("1")
    LIEUTENANT,
    @SerializedName("2")
    OFFICER,
    @SerializedName("3")
    OWNER;

    public static String getClanRankString(ClanRank Rank) {
        switch (Rank) {
            case OWNER:
                return "Owner";
            case OFFICER:
                return "Officer";
            case LIEUTENANT:
                return "Lieutenant";
            case GRUNT:
                return "Grunt";
        }
        return null;
    }
}



