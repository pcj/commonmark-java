package org.commonmark.ext.subscript.internal;

import org.commonmark.ext.subscript.Subscript;
import org.commonmark.html.CustomHtmlRenderer;
import org.commonmark.html.HtmlWriter;
import org.commonmark.node.Node;
import org.commonmark.node.Visitor;

public class SubscriptHtmlRenderer implements CustomHtmlRenderer {

    @Override
    public boolean render(Node node, HtmlWriter htmlWriter, Visitor visitor) {
        if (node instanceof Subscript) {
            htmlWriter.tag("sub");
            visitChildren(node, visitor);
            htmlWriter.tag("/sub");
            return true;
        } else {
            return false;
        }
    }

    private void visitChildren(Node node, Visitor visitor) {
        Node child = node.getFirstChild();
        while (child != null) {
            child.accept(visitor);
            child = child.getNext();
        }
    }

}
