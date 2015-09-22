package org.commonmark.ext.superscript;

import org.commonmark.Extension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.test.RenderingTestCase;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

public class SuperscriptTest extends RenderingTestCase {

    private static final Set<Extension> EXTENSIONS = Collections.singleton(SuperscriptExtension.create());
    private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().extensions(EXTENSIONS).build();

    // @Test
    // public void oneCaretIsNotEnough() {
    //     assertRendering("~foo~", "<p>~foo~</p>\n");
    // }

    @Test
    public void oneCaretYay() {
        System.out.println("Starting test...");
        assertRendering("2^10^", "<p>2<sup>10</sup></p>\n");
    }


    @Test
    public void twoCaretsIsTooManyButStillSupported() {
        System.out.println("Starting test...");
        assertRendering("2^^10^^", "<p>2<sup><sup>10</sup></sup></p>\n");
    }

     @Test
     public void fourCaretsNope() {
         assertRendering("foo ^^^^", "<p>foo ^^^^</p>\n");
     }

    @Test
    public void unmatched() {
        assertRendering("^^foo", "<p>^^foo</p>\n");
        assertRendering("foo^^", "<p>foo^^</p>\n");
    }

    // @Test
    // public void threeInnerThree() {
    //     assertRendering("^^^foo^^^", "<p>^<sup>foo</sup>^</p>\n");
    // }

    @Test
    public void oneInnerTwo() {
        assertRendering("^foo^^", "<p><sup>foo</sup>^</p>\n");
    }

    // @Test
    // public void twoSuperscriptsWithoutSpacing() {
    //     assertRendering("^^foo^^^^bar^^", "<p><sup>foo</sup><sup>bar</sup></p>\n");
    // }

    @Test
    public void superscriptWholeParagraphWithOtherSupimiters() {
        assertRendering("^Paragraph with *emphasis* and __strong emphasis__^",
                "<p><sup>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></sup></p>\n");
    }

    @Test
    public void insideBlockQuote() {
        assertRendering("> up ^that^",
                        "<blockquote>\n<p>up <sup>that</sup></p>\n</blockquote>\n");
    }

    @Override
    protected String render(String source) {
        return RENDERER.render(PARSER.parse(source));
    }
}
