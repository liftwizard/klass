package com.stackoverflow;

import java.util.Collection;

import com.gs.fw.finder.Operation;

public class TagVersionList extends TagVersionListAbstract {

    public TagVersionList() {}

    public TagVersionList(int initialSize) {
        super(initialSize);
    }

    public TagVersionList(Collection c) {
        super(c);
    }

    public TagVersionList(Operation operation) {
        super(operation);
    }
}
