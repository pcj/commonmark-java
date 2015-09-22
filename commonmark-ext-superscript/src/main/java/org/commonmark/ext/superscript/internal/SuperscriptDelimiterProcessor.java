package org.commonmark.ext.superscript.internal;

import org.commonmark.ext.superscript.Superscript;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.DelimiterProcessor;

public class SuperscriptDelimiterProcessor implements DelimiterProcessor {

    // superscript: 2^10^.
    // subscript: H~2~0.

    @Override
    public char getDelimiterChar() {
        return '^';
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

        // Normal case, wrap nodes between delimiters in superscript.
        Node superscript = new Superscript(getDelimiterChar(), delimiterCount);

        Node tmp = opener.getNext();
        while (tmp != null && tmp != closer) {
            Node next = tmp.getNext();
            superscript.appendChild(tmp);
            tmp = next;
        }

        opener.insertAfter(superscript);
    }

}
