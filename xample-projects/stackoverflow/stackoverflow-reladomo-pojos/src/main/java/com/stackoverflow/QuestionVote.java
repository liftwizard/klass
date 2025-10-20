package com.stackoverflow;

import java.sql.Timestamp;

import cool.klass.data.store.reladomo.UtcInfinityTimestamp;

public class QuestionVote extends QuestionVoteAbstract {

    public QuestionVote(Timestamp system) {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public QuestionVote() {
        this(UtcInfinityTimestamp.getDefaultInfinity());
    }
}
