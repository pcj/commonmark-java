package org.commonmark.internal;

import org.commonmark.internal.util.Parsing;
import org.commonmark.node.Block;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.block.AbstractBlockParser;
import org.commonmark.parser.block.BlockContinue;
import org.commonmark.parser.Options;
import org.commonmark.parser.InlineParser;
import org.commonmark.parser.block.ParserState;

public class ParagraphParser extends AbstractBlockParser {

    private final Paragraph block = new Paragraph();
    private BlockContent content = new BlockContent();

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public BlockContinue tryContinue(ParserState state) {
        if (!state.isBlank()) {
            return BlockContinue.atIndex(state.getIndex());
        } else {
            return BlockContinue.none();
        }
    }

    @Override
    public void addLine(CharSequence line) {
        content.add(line);
    }

    @Override
    public void closeBlock() {
    }

    public void closeBlock(InlineParserImpl inlineParser, Options options) {
        //System.out.println("Paragraph.closeBlock: " + content.getString());

        String contentString = content.getString();
        boolean hasReferenceDefs = false;

        //System.out.println("Para.closeBlock(): content: " + contentString);
        int pos;
        // try parsing the beginning as link reference definitions:

        // Basically what this seems to be doing is optimistically
        // trying to parse link ref definitions in raw text.  If
        // defintions are matched, they are created in the
        // parseReference() area and then will be spliced out here.
        // If all the paragraph contains is link ref defs, the block
        // is unlinked altogehter, otherwise it is added for block
        // content.

        while (contentString.length() > 3 && contentString.charAt(0) == '[' &&
               (pos = inlineParser.parseReference(contentString, options)) != 0) {
            String current = contentString;
            contentString = contentString.substring(pos);
            // System.out.println("Para.closeBlock: matched link reference definition {"
            //                    + current+ "} --> {" + contentString+ "} ");
            hasReferenceDefs = true;
        }
        if (hasReferenceDefs && Parsing.isBlank(contentString)) {
            block.unlink();
            content = null;
        } else {
            content = new BlockContent(contentString);
        }
    }

    @Override
    public void parseInlines(InlineParser inlineParser) {
        if (content != null) {
            inlineParser.parse(content.getString(), block);
        }
    }

    public boolean hasSingleLine() {
        return content.hasSingleLine();
    }

    public String getContentString() {
        return content.getString();
    }
}
