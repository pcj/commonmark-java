package org.commonmark.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import org.commonmark.internal.util.Parsing;
import org.commonmark.internal.util.Substring;
import org.commonmark.node.*;
import org.commonmark.parser.block.*;

import java.util.*;

public class DocumentParser implements ParserState {

    private static List<BlockParserFactory> CORE_FACTORIES = Arrays.<BlockParserFactory>asList(
            new BlockQuoteParser.Factory(),
            new HeaderParser.Factory(),
            new FencedCodeBlockParser.Factory(),
            new HtmlBlockParser.Factory(),
            new HorizontalRuleParser.Factory(),
            new ListBlockParser.Factory(),
            new IndentedCodeBlockParser.Factory());

    private CharSequence line;

    /**
     * current index (offset) in input line (0-based)
     */
    private int index = 0;

    /**
     * current column of input line (tab causes column to go to next 4-space tab stop) (0-based)
     */
    private int column = 0;

    private int nextNonSpace = 0;
    private int nextNonSpaceColumn = 0;
    private boolean blank;

    private int indent = 0;

    private final List<BlockParserFactory> blockParserFactories;
    private final InlineParserImpl inlineParser;
    private final DocumentBlockParser documentBlockParser;

    private List<BlockParser> activeBlockParsers = new ArrayList<>();
    private Set<BlockParser> allBlockParsers = new HashSet<>();
    private Map<Node, Boolean> lastLineBlank = new HashMap<>();

    public DocumentParser(List<BlockParserFactory> blockParserFactories, InlineParserImpl inlineParser) {
        this.blockParserFactories = blockParserFactories;
        this.inlineParser = inlineParser;
        
        this.documentBlockParser = new DocumentBlockParser();
        activateBlockParser(this.documentBlockParser);
    }

    public static List<BlockParserFactory> calculateBlockParserFactories(List<BlockParserFactory> customBlockParserFactories) {
        List<BlockParserFactory> list = new ArrayList<>();
        // By having the custom factories come first, extensions are able to change behavior of core syntax.
        list.addAll(customBlockParserFactories);
        list.addAll(DocumentParser.CORE_FACTORIES);
        return list;
    }

    /**
     * The main parsing function. Returns a parsed document AST.
     */
    public Document parse(String input) {
        int lineStart = 0;
        int lineBreak;
        while ((lineBreak = Parsing.findLineBreak(input, lineStart)) != -1) {
            CharSequence line = Substring.of(input, lineStart, lineBreak);
            incorporateLine(line);
            if (lineBreak + 1 < input.length() && input.charAt(lineBreak) == '\r' && input.charAt(lineBreak + 1) == '\n') {
                lineStart = lineBreak + 2;
            } else {
                lineStart = lineBreak + 1;
            }
        }
        if (input.length() > 0 && (lineStart == 0 || lineStart < input.length())) {
            incorporateLine(Substring.of(input, lineStart, input.length()));
        }

        return finalizeAndProcess();
    }
    
    public Document parse(Reader input) throws IOException {
        BufferedReader bufferedReader;
        if (input instanceof BufferedReader) {
            bufferedReader = (BufferedReader) input;
        } else {
            bufferedReader = new BufferedReader(input);
        }
        
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            incorporateLine(line);
        }

        return finalizeAndProcess();
    }

    @Override
    public CharSequence getLine() {
        return line;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getNextNonSpaceIndex() {
        return nextNonSpace;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getIndent() {
        return indent;
    }

    @Override
    public boolean isBlank() {
        return blank;
    }

    @Override
    public BlockParser getActiveBlockParser() {
        return activeBlockParsers.get(activeBlockParsers.size() - 1);
    }

    /**
     * Analyze a line of text and update the document appropriately. We parse markdown text by calling this on each
     * line of input, then finalizing the document.
     */
    private void incorporateLine(CharSequence ln) {
        line = Parsing.prepareLine(ln);
        index = 0;
        column = 0;
        nextNonSpace = 0;
        nextNonSpaceColumn = 0;

        // For each containing block, try to parse the associated line start.
        // Bail out on failure: container will point to the last matching block.
        // Set all_matched to false if not all containers match.
        // The document will always match, can be skipped
        int matches = 1;
        for (BlockParser blockParser : activeBlockParsers.subList(1, activeBlockParsers.size())) {
            findNextNonSpace();

            BlockContinue result = blockParser.tryContinue(this);
            if (result instanceof BlockContinueImpl) {
                BlockContinueImpl blockContinue = (BlockContinueImpl) result;
                if (blockContinue.isFinalize()) {
                    finalize(blockParser);
                    return;
                } else {
                    if (blockContinue.getNewIndex() != -1) {
                        setNewIndex(blockContinue.getNewIndex());
                    } else if (blockContinue.getNewColumn() != -1) {
                        setNewColumn(blockContinue.getNewColumn());
                    }
                    matches++;
                }
            } else {
                break;
            }
        }

        List<BlockParser> unmatchedBlockParsers = new ArrayList<>(activeBlockParsers.subList(matches, activeBlockParsers.size()));
        BlockParser lastMatchedBlockParser = activeBlockParsers.get(matches - 1);
        BlockParser blockParser = lastMatchedBlockParser;
        boolean allClosed = unmatchedBlockParsers.isEmpty();

        // Check to see if we've hit 2nd blank line; if so break out of list:
        if (isBlank() && isLastLineBlank(blockParser.getBlock())) {
            List<BlockParser> matchedBlockParsers = new ArrayList<>(activeBlockParsers.subList(0, matches));
            breakOutOfLists(matchedBlockParsers);
        }

        // Unless last matched container is a code block, try new container starts,
        // adding children to the last matched container:
        boolean tryBlockStarts = blockParser.getBlock() instanceof Paragraph || blockParser.isContainer();
        while (tryBlockStarts) {
            findNextNonSpace();

            // this is a little performance optimization:
            if (isBlank() || (indent < IndentedCodeBlockParser.INDENT && Parsing.isLetter(line, nextNonSpace))) {
                setNewIndex(nextNonSpace);
                break;
            }

            BlockStartImpl blockStart = findBlockStart(blockParser);
            if (blockStart == null) {
                setNewIndex(nextNonSpace);
                break;
            }

            if (!allClosed) {
                finalizeBlocks(unmatchedBlockParsers);
                allClosed = true;
            }

            if (blockStart.getNewIndex() != -1) {
                setNewIndex(blockStart.getNewIndex());
            } else if (blockStart.getNewColumn() != -1) {
                setNewColumn(blockStart.getNewColumn());
            }

            if (blockStart.isReplaceActiveBlockParser()) {
                removeActiveBlockParser();
            }

            for (BlockParser newBlockParser : blockStart.getBlockParsers()) {
                blockParser = addChild(newBlockParser);
                tryBlockStarts = newBlockParser.isContainer();
            }
        }

        // What remains at the offset is a text line. Add the text to the
        // appropriate block.

        // First check for a lazy paragraph continuation:
        if (!allClosed && !isBlank() &&
                getActiveBlockParser() instanceof ParagraphParser) {
            // lazy paragraph continuation
            addLine();

        } else {

            // finalize any blocks not matched
            if (!allClosed) {
                finalizeBlocks(unmatchedBlockParsers);
            }
            propagateLastLineBlank(blockParser, lastMatchedBlockParser);

            if (!blockParser.isContainer()) {
                addLine();
            } else if (!isBlank()) {
                // create paragraph container for line
                addChild(new ParagraphParser());
                addLine();
            }
        }
    }

    private void findNextNonSpace() {
        int i = index;
        int cols = column;

        blank = true;
        while (i < line.length()) {
            char c = line.charAt(i);
            switch (c) {
                case ' ':
                    i++;
                    cols++;
                    continue;
                case '\t':
                    i++;
                    cols += (4 - (cols % 4));
                    continue;
            }
            blank = false;
            break;
        }

        nextNonSpace = i;
        nextNonSpaceColumn = cols;
        indent = nextNonSpaceColumn - column;
    }

    private void setNewIndex(int newIndex) {
        if (newIndex >= nextNonSpace) {
            // We can start from here, no need to calculate tab stops again
            index = nextNonSpace;
            column = nextNonSpaceColumn;
        }
        while (index < newIndex && index != line.length()) {
            advance();
        }
    }

    private void setNewColumn(int newColumn) {
        if (newColumn >= nextNonSpaceColumn) {
            // We can start from here, no need to calculate tab stops again
            index = nextNonSpace;
            column = nextNonSpaceColumn;
        }
        while (column < newColumn && index != line.length()) {
            advance();
        }
    }

    private void advance() {
        char c = line.charAt(index);
        if (c == '\t') {
            index++;
            column += (4 - (column % 4));
        } else {
            index++;
            column++;
        }
    }

    private BlockStartImpl findBlockStart(BlockParser blockParser) {
        MatchedBlockParser matchedBlockParser = new MatchedBlockParserImpl(blockParser);
        for (BlockParserFactory blockParserFactory : blockParserFactories) {
            BlockStart result = blockParserFactory.tryStart(this, matchedBlockParser);
            if (result instanceof BlockStartImpl) {
                return (BlockStartImpl) result;
            }
        }
        return null;
    }

    /**
     * Finalize a block. Close it and do any necessary postprocessing, e.g. creating string_content from strings,
     * setting the 'tight' or 'loose' status of a list, and parsing the beginnings of paragraphs for reference
     * definitions.
     */
    private void finalize(BlockParser blockParser) {
        if (getActiveBlockParser() == blockParser) {
            deactivateBlockParser();
        }

        blockParser.closeBlock();

        if (blockParser instanceof ParagraphParser) {
            ParagraphParser paragraphParser = (ParagraphParser) blockParser;
            paragraphParser.closeBlock(inlineParser);
        } else if (blockParser instanceof ListBlockParser) {
            ListBlockParser listBlockParser = (ListBlockParser) blockParser;
            finalizeListTight(listBlockParser);
        }
    }

    /**
     * Walk through a block & children recursively, parsing string content into inline content where appropriate.
     */
    private void processInlines() {
        for (BlockParser blockParser : allBlockParsers) {
            blockParser.parseInlines(inlineParser);
        }
    }

    private void finalizeListTight(ListBlockParser listBlockParser) {
        Node item = listBlockParser.getBlock().getFirstChild();
        while (item != null) {
            // check for non-final list item ending with blank line:
            if (endsWithBlankLine(item) && item.getNext() != null) {
                listBlockParser.setTight(false);
                break;
            }
            // recurse into children of list item, to see if there are
            // spaces between any of them:
            Node subItem = item.getFirstChild();
            while (subItem != null) {
                if (endsWithBlankLine(subItem) && (item.getNext() != null || subItem.getNext() != null)) {
                    listBlockParser.setTight(false);
                    break;
                }
                subItem = subItem.getNext();
            }
            item = item.getNext();
        }
    }

    private boolean endsWithBlankLine(Node block) {
        while (block != null) {
            if (isLastLineBlank(block)) {
                return true;
            }
            if (block instanceof ListBlock || block instanceof ListItem) {
                block = block.getLastChild();
            } else {
                break;
            }
        }
        return false;
    }

    /**
     * Break out of all containing lists, resetting the tip of the document to the parent of the highest list,
     * and finalizing all the lists. (This is used to implement the "two blank lines break of of all lists" feature.)
     */
    private void breakOutOfLists(List<BlockParser> blockParsers) {
        int lastList = -1;
        for (int i = blockParsers.size() - 1; i >= 0; i--) {
            BlockParser blockParser = blockParsers.get(i);
            if (blockParser instanceof ListBlockParser) {
                lastList = i;
            }
        }

        if (lastList != -1) {
            finalizeBlocks(blockParsers.subList(lastList, blockParsers.size()));
        }
    }

    /**
     * Add a line to the block at the tip. We assume the tip can accept lines -- that check should be done before
     * calling this.
     */
    private void addLine() {
        getActiveBlockParser().addLine(line.subSequence(index, line.length()));
    }

    /**
     * Add block of type tag as a child of the tip. If the tip can't  accept children, close and finalize it and try
     * its parent, and so on til we find a block that can accept children.
     */
    private <T extends BlockParser> T addChild(T blockParser) {
        while (!getActiveBlockParser().canContain(blockParser.getBlock())) {
            finalize(getActiveBlockParser());
        }

        getActiveBlockParser().getBlock().appendChild(blockParser.getBlock());
        activateBlockParser(blockParser);

        return blockParser;
    }

    private void activateBlockParser(BlockParser blockParser) {
        activeBlockParsers.add(blockParser);
        allBlockParsers.add(blockParser);
    }

    private void deactivateBlockParser() {
        activeBlockParsers.remove(activeBlockParsers.size() - 1);
    }

    private void removeActiveBlockParser() {
        BlockParser old = getActiveBlockParser();
        deactivateBlockParser();
        allBlockParsers.remove(old);

        old.getBlock().unlink();
    }

    private void propagateLastLineBlank(BlockParser blockParser, BlockParser lastMatchedBlockParser) {
        if (isBlank() && blockParser.getBlock().getLastChild() != null) {
            setLastLineBlank(blockParser.getBlock().getLastChild(), true);
        }

        Block block = blockParser.getBlock();

        // Block quote lines are never blank as they start with >
        // and we don't count blanks in fenced code for purposes of tight/loose
        // lists or breaking out of lists. We also don't set lastLineBlank
        // on an empty list item.
        boolean lastLineBlank = isBlank() &&
                !(block instanceof BlockQuote ||
                        block instanceof FencedCodeBlock ||
                        (block instanceof ListItem &&
                                block.getFirstChild() == null &&
                                blockParser != lastMatchedBlockParser));

        // Propagate lastLineBlank up through parents
        Node node = blockParser.getBlock();
        while (node != null) {
            setLastLineBlank(node, lastLineBlank);
            node = node.getParent();
        }
    }

    private void setLastLineBlank(Node node, boolean value) {
        lastLineBlank.put(node, value);
    }

    private boolean isLastLineBlank(Node node) {
        Boolean value = lastLineBlank.get(node);
        return value != null && value;
    }

    /**
     * Finalize blocks of previous line. Returns true.
     */
    private boolean finalizeBlocks(List<BlockParser> blockParsers) {
        for (int i = blockParsers.size() - 1; i >= 0; i--) {
            BlockParser blockParser = blockParsers.get(i);
            finalize(blockParser);
        }
        return true;
    }

    private Document finalizeAndProcess() {
        finalizeBlocks(this.activeBlockParsers);
        this.processInlines();
        return this.documentBlockParser.getBlock();
    }
    
    private static class MatchedBlockParserImpl implements MatchedBlockParser {

        private final BlockParser matchedBlockParser;

        public MatchedBlockParserImpl(BlockParser matchedBlockParser) {
            this.matchedBlockParser = matchedBlockParser;
        }

        @Override
        public BlockParser getMatchedBlockParser() {
            return matchedBlockParser;
        }

        @Override
        public CharSequence getParagraphStartLine() {
            if (matchedBlockParser instanceof ParagraphParser) {
                ParagraphParser paragraphParser = (ParagraphParser) matchedBlockParser;
                if (paragraphParser.hasSingleLine()) {
                    return paragraphParser.getContentString();
                }
            }
            return null;
        }
    }
}
