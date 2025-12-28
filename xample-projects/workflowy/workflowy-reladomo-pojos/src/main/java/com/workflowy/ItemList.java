package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class ItemList extends ItemListAbstract
{
	public ItemList()
	{
		super();
	}

	public ItemList(int initialSize)
	{
		super(initialSize);
	}

	public ItemList(Collection c)
	{
		super(c);
	}

	public ItemList(Operation operation)
	{
		super(operation);
	}
}
