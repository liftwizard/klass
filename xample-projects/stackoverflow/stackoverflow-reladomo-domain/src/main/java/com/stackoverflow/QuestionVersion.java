package com.stackoverflow;

import java.sql.Timestamp;

import com.gs.fw.common.mithra.util.DefaultInfinityTimestamp;

public class QuestionVersion extends QuestionVersionAbstract
{
    public QuestionVersion(Timestamp system)
    {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public QuestionVersion()
    {
        this(DefaultInfinityTimestamp.getDefaultInfinity());
    }
}
