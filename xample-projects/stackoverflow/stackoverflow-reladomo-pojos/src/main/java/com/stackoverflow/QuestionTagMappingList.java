package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class QuestionTagMappingList extends QuestionTagMappingListAbstract {

    public QuestionTagMappingList() {}

    public QuestionTagMappingList(int initialSize) {
        super(initialSize);
    }

    public QuestionTagMappingList(Collection c) {
        super(c);
    }

    public QuestionTagMappingList(Operation operation) {
        super(operation);
    }
}
