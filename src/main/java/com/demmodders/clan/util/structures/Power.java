package com.demmodders.clan.util.structures;

import com.demmodders.clan.util.DemUtils;

public class Power {
    public int power;
    public int maxPower;
    public Power(){
    }

    public Power(int StartPower, int MaxPower){
        power = StartPower;
        maxPower = MaxPower;
    }

    public void setPower(int Power){
        power = DemUtils.clamp(Power, 0, maxPower);
    }
}
