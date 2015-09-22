package org.commonmark.ext.subscript.internal;

import org.commonmark.ext.subscript.Subscript;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.DelimiterProcessor;

public class SubscriptDelimiterProcessor implements DelimiterProcessor {

    // subscript: 2^10^.
    // subscript: H~2~0.

    @Override
    public char getDelimiterChar() {
        return '~';
    }

    @Override
    public int getMinDelimiterCount() {
        return 1;
    }

    @Override
    public int getDelimiterUse(int openerCount, int closerCount) {
        return 1;
    }

    @Override
    public void process(Text opener, Text closer, int delimiterCount) {

        // Normal case, wrap nodes between delimiters in subscript.
        Node subscript = new Subscript(getDelimiterChar(), delimiterCount);

        Node tmp = opener.getNext();
        while (tmp != null && tmp != closer) {
            Node next = tmp.getNext();
            subscript.appendChild(tmp);
            tmp = next;
        }

        opener.insertAfter(subscript);
    }

}
