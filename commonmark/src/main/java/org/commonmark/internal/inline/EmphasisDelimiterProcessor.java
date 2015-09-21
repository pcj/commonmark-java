package org.commonmark.internal.inline;

import org.commonmark.parser.DelimiterProcessor;
import org.commonmark.node.Emphasis;
import org.commonmark.node.Node;
import org.commonmark.node.Inline;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.internal.Util;

public abstract class EmphasisDelimiterProcessor implements DelimiterProcessor {

    @Override
    public int getMinDelimiterCount() {
        return 1;
    }

    @Override
    public int getDelimiterUse(int openerCount, int closerCount) {
        // calculate actual number of delimiters used from this closer
        if (closerCount < 3 || openerCount < 3) {
            return closerCount <= openerCount ?
                    closerCount : openerCount;
        } else {
            return closerCount % 2 == 0 ? 2 : 1;
        }
    }

    @Override
    public void process(Text opener, Text closer, int delimiterUse) {
        Inline emphasis = delimiterUse == 1 ? new Emphasis() : new StrongEmphasis();

        Node tmp = opener.getNext();
        while (tmp != null && tmp != closer) {
            Node next = tmp.getNext();
            emphasis.appendChild(tmp);
            tmp = next;
        }

        emphasis.setOpener(opener.getLiteral());
        emphasis.setCloser(closer.getLiteral());

        String literal = opener.getLiteral();
        if (literal != null && literal.length() > 0) {
            //Util.log(getClass().getSimpleName(), "opener: " + opener.getLiteral() + ", closer: " + closer.getLiteral());
            emphasis.setDelimiterMarker(literal.charAt(0));
            emphasis.setDelimiterCount(literal.length());
        }

        opener.insertAfter(emphasis);
    }
}
