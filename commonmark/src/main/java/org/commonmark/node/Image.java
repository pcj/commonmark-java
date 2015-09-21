package org.commonmark.node;

public class Image extends Linkable {

    public Image() {
    }

    public Image(String destination, String title) {
        super(destination, title);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
