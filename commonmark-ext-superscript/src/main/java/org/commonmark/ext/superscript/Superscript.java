package org.commonmark.ext.superscript;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;

/**
 * A superscript node containing text and other inline nodes nodes as children.
 */
public class Superscript extends CustomNode implements Delimited {

    private final char delimiterChar;
    private final int delimiterCount;

    public Superscript(char delimiterChar, int delimiterCount) {
        this.delimiterChar = delimiterChar;
        this.delimiterCount = delimiterCount;
    }

    @Override
    public char getDelimiterChar() {
        return delimiterChar;
    }

    @Override
    public int getDelimiterCount() {
        return delimiterCount;
    }

}
