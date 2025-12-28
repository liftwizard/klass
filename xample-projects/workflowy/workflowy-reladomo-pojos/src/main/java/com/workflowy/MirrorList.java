package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class MirrorList extends MirrorListAbstract
{
	public MirrorList()
	{
		super();
	}

	public MirrorList(int initialSize)
	{
		super(initialSize);
	}

	public MirrorList(Collection c)
	{
		super(c);
	}

	public MirrorList(Operation operation)
	{
		super(operation);
	}
}
