package com.demmodders.clan.util.structures;

import com.demmodders.clan.util.enums.RelationState;

public class Relationship {
    public RelationState relation;
    public long timeOfHappening;

    public Relationship(){

    }
    public Relationship(RelationState Relation){
        this.relation = Relation;
        this.timeOfHappening = System.currentTimeMillis();
    }
}

