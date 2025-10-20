package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class AnswerList extends AnswerListAbstract {

    public AnswerList() {}

    public AnswerList(int initialSize) {
        super(initialSize);
    }

    public AnswerList(Collection c) {
        super(c);
    }

    public AnswerList(Operation operation) {
        super(operation);
    }
}
