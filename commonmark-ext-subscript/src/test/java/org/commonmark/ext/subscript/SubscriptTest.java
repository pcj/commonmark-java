package org.commonmark.ext.subscript;

import org.commonmark.Extension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.test.RenderingTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

public class SubscriptTest extends RenderingTestCase {

    private static final Set<Extension> EXTENSIONS = Collections.singleton(SubscriptExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    @Test
    public void oneTildeYay() {
        assertRendering("2~10~", "<p>2<sub>10</sub></p>\n");
    }

    @Test
    public void twoTildesIsTooManyButStillSupported() {
        assertRendering("2~~10~~", "<p>2<sub><sub>10</sub></sub></p>\n");
    }

    //@Test
    public void subscriptAndStrikethroughTogether() {
        assertRendering("~~H~2~0~~", "<p><strike>H<sub>2</sub>0</strike></p>\n");
    }

     @Test
     public void fourTildesNope() {
         assertRendering("foo ~~~~", "<p>foo ~~~~</p>\n");
     }

    @Test
    public void unmatched() {
        assertRendering("~~foo", "<p>~~foo</p>\n");
        assertRendering("foo~~", "<p>foo~~</p>\n");
    }

    // @Test
    // public void threeInnerThree() {
    //     assertRendering("~~~foo~~~", "<p>~<sub>foo</sub>~</p>\n");
    // }

    @Test
    public void oneInnerTwo() {
        assertRendering("~foo~~", "<p><sub>foo</sub>~</p>\n");
    }

    // @Test
    // public void twoSubscriptsWithoutSpacing() {
    //     assertRendering("~~foo~~~~bar~~", "<p><sub>foo</sub><sub>bar</sub></p>\n");
    // }

    @Test
    public void subscriptWholeParagraphWithOtherSupimiters() {
        assertRendering("~Paragraph with *emphasis* and __strong emphasis__~",
                "<p><sub>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></sub></p>\n");
    }

    @Test
    public void insideBlockQuote() {
        assertRendering("> up ~that~",
                        "<blockquote>\n<p>up <sub>that</sub></p>\n</blockquote>\n");
    }

    @Override
    protected String render(String source) {
        return RENDERER.render(PARSER.parse(source));
    }
}
