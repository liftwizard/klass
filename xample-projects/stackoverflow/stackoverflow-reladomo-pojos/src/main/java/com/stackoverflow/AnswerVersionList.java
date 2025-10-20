package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class AnswerVersionList extends AnswerVersionListAbstract {

    public AnswerVersionList() {}

    public AnswerVersionList(int initialSize) {
        super(initialSize);
    }

    public AnswerVersionList(Collection c) {
        super(c);
    }

    public AnswerVersionList(Operation operation) {
        super(operation);
    }
}
