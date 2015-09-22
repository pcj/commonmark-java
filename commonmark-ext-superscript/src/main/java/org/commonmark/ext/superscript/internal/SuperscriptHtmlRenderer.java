package org.commonmark.ext.superscript.internal;

import org.commonmark.ext.superscript.Superscript;
import org.commonmark.html.CustomHtmlRenderer;
import org.commonmark.html.HtmlWriter;
import org.commonmark.node.Node;
import org.commonmark.node.Visitor;

public class SuperscriptHtmlRenderer implements CustomHtmlRenderer {

    @Override
    public boolean render(Node node, HtmlWriter htmlWriter, Visitor visitor) {
        if (node instanceof Superscript) {
            htmlWriter.tag("sup");
            visitChildren(node, visitor);
            htmlWriter.tag("/sup");
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
