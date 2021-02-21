package com.demmodders.clan.util;

import java.io.File;

public class ClanFileHelper {
    /**
     * Gets the base directory the Clan data is stored in
     *
     * @return A File object at the base directory
     */
    public static File getBaseDir() {
        File dir = new File("./Clan");
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            assert success : "Unable to create Clan Directory";
        }
        return dir;
    }

    /**
     * Gets the directory the Clan data is stored in
     *
     * @return A file object at the directory the Clan data is stored in
     */
    public static File getClansDir() {
        File dir = new File(getBaseDir(), "Clan");
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            assert success : "Unable to create Clan Directory";
        }
        return dir;
    }

    /**
     * Gets the directory the default Clan data is stored in
     *
     * @return A file object at the directory the Clan data is stored in
     */
    public static File getDefaultClanDir() {
        File dir = new File(getBaseDir(), "DefaultFactions");
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            assert success : "Unable to create Default Clan Directory";
        }
        return dir;
    }

    /**
     * Gets the directory the claimed chunks data is stored in
     * @return A file object at the directory the claimed chunk data is stored in
     */
    public static File getClaimedDir(){
        File dir = new File(getBaseDir(), "Claimed");
        if(!dir.exists()){
            boolean success = dir.mkdirs();
            assert success : "Unable to create Claimed Directory";
        }
        return dir;
    }

    /**
     * Gets the directory the player data is stored in
     * @return A file object at the directory the player data is stored in
     */
    public static File getPlayerDir(){
        File dir = new File(getBaseDir(), "Players");
        if(!dir.exists()){
            boolean success = dir.mkdirs();
            assert success : "Unable to create Player Directory";
        }
        return dir;
    }
}