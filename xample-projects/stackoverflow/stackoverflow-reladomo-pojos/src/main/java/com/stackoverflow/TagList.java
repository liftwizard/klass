package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class TagList extends TagListAbstract {

    public TagList() {}

    public TagList(int initialSize) {
        super(initialSize);
    }

    public TagList(Collection c) {
        super(c);
    }

    public TagList(Operation operation) {
        super(operation);
    }
}
