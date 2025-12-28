package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class TagList extends TagListAbstract
{
	public TagList()
	{
		super();
	}

	public TagList(int initialSize)
	{
		super(initialSize);
	}

	public TagList(Collection c)
	{
		super(c);
	}

	public TagList(Operation operation)
	{
		super(operation);
	}
}
