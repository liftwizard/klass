package com.workflowy;
import java.util.*;

import com.gs.fw.finder.Operation;
public class UserList extends UserListAbstract
{
	public UserList()
	{
		super();
	}

	public UserList(int initialSize)
	{
		super(initialSize);
	}

	public UserList(Collection c)
	{
		super(c);
	}

	public UserList(Operation operation)
	{
		super(operation);
	}
}
