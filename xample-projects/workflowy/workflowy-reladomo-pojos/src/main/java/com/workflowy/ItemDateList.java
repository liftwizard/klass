package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class ItemDateList extends ItemDateListAbstract
{
	public ItemDateList()
	{
		super();
	}

	public ItemDateList(int initialSize)
	{
		super(initialSize);
	}

	public ItemDateList(Collection c)
	{
		super(c);
	}

	public ItemDateList(Operation operation)
	{
		super(operation);
	}
}
