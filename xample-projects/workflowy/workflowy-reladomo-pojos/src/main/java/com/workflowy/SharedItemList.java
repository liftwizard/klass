package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class SharedItemList extends SharedItemListAbstract
{
	public SharedItemList()
	{
		super();
	}

	public SharedItemList(int initialSize)
	{
		super(initialSize);
	}

	public SharedItemList(Collection c)
	{
		super(c);
	}

	public SharedItemList(Operation operation)
	{
		super(operation);
	}
}
