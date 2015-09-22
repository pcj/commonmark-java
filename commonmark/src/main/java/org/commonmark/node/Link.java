package org.commonmark.node;

public class Link extends Reference {

    public Link() {
    }

    public Link(String destination, String title) {
        super(destination, title);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
