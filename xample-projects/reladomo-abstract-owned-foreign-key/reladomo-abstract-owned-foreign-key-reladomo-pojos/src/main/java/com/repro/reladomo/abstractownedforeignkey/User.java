package com.repro.reladomo.abstractownedforeignkey;

import java.sql.Timestamp;

public class User extends UserAbstract
{
    public User(Timestamp system)
    {
        super(system);
        // You must not modify this constructor. Mithra calls this internally.
        // You can call this constructor. You can also add new constructors.
    }

    public User()
    {
        this(DefaultInfinityTimestamp.getDefaultInfinity());
    }
}
