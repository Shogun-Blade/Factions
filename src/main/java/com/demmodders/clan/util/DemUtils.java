package com.demmodders.clan.util;

public class DemUtils {
    /**
     * Calculate the age based of a starting time and the given time
     *
     * @param startingDate starting Time in millis
     * @return the age in millis
     */
    public static long calculateAge(Long startingDate) {
        long time = System.currentTimeMillis();
        return time - startingDate;
    }

    public static String displayAge(Long ageInMinutes) {
        if (ageInMinutes < 60){
            return ageInMinutes + " Minutes";
        } else if (ageInMinutes < 1440){
            return (ageInMinutes / 60) + " Hours";
        } else {
            return (ageInMinutes / 1440) + " Days";
        }
    }

    public static int clamp(int Value, int Min, int Max) {
        if (Value < Min) return Min;
        else return Math.min(Value, Max);
    }

    public static float clamp(float Value, float Min, float Max) {
        if (Value < Min) return Min;
        else return Math.min(Value, Max);
    }

    public static double clamp(double Value, double Min, double Max) {
        if (Value < Min) return Min;
        else return Math.min(Value, Max);
    }
}
