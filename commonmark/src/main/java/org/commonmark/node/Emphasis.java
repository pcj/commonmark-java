package org.commonmark.node;

public class Emphasis extends Inline {

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
