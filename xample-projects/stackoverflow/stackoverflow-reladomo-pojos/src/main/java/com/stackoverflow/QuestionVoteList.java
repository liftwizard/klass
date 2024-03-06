package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class QuestionVoteList extends QuestionVoteListAbstract
{
    public QuestionVoteList()
    {
    }

    public QuestionVoteList(int initialSize)
    {
        super(initialSize);
    }

    public QuestionVoteList(Collection<?> collection)
    {
        super(collection);
    }

    public QuestionVoteList(Operation operation)
    {
        super(operation);
    }
}
