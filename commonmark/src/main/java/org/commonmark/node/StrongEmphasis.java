package org.commonmark.node;

public class StrongEmphasis extends Emphasis {

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }


}
