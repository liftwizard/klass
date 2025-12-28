package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class ItemVersionList extends ItemVersionListAbstract
{
	public ItemVersionList()
	{
		super();
	}

	public ItemVersionList(int initialSize)
	{
		super(initialSize);
	}

	public ItemVersionList(Collection c)
	{
		super(c);
	}

	public ItemVersionList(Operation operation)
	{
		super(operation);
	}
}
