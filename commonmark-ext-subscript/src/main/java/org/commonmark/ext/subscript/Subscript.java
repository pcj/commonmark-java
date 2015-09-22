package org.commonmark.ext.subscript;

import org.commonmark.node.CustomNode;
import org.commonmark.node.Delimited;

/**
 * A subscript node containing text and other inline nodes nodes as children.
 */
public class Subscript extends CustomNode implements Delimited {

    private final char delimiterChar;
    private final int delimiterCount;

    public Subscript(char delimiterChar, int delimiterCount) {
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
