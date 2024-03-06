package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class QuestionList extends QuestionListAbstract
{
    public QuestionList()
    {
    }

    public QuestionList(int initialSize)
    {
        super(initialSize);
    }

    public QuestionList(Collection collection)
    {
        super(collection);
    }

    public QuestionList(Operation operation)
    {
        super(operation);
    }
}
