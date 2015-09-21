package org.commonmark.node;

public class Link extends Linkable {

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
