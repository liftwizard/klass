package com.workflowy;
import java.sql.Timestamp;
public class SharedItem extends SharedItemAbstract
{
	public SharedItem(Timestamp system
	)
	{
		super(system
		);
		// You must not modify this constructor. Mithra calls this internally.
		// You can call this constructor. You can also add new constructors.
	}

	public SharedItem()
	{
		this(com.gs.fw.common.mithra.util.DefaultInfinityTimestamp.getDefaultInfinity());
	}
}
