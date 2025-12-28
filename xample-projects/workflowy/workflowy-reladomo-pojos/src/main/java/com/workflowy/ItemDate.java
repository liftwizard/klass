package com.workflowy;
import java.sql.Timestamp;
public class ItemDate extends ItemDateAbstract
{
	public ItemDate(Timestamp system
	)
	{
		super(system
		);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public ItemDate()
	{
		this(com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity());
	}
}
