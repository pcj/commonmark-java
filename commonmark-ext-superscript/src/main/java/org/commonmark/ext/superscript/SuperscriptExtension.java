package org.commonmark.ext.superscript;

import org.commonmark.Extension;
import org.commonmark.ext.superscript.internal.SuperscriptDelimiterProcessor;
import org.commonmark.ext.superscript.internal.SuperscriptHtmlRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.html.HtmlRenderer;

/**
 * Extension for superscript using ^^ (similar to Pandoc).
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * ({@link org.commonmark.parser.Parser.Builder#extensions(Iterable)},
 * {@link org.commonmark.html.HtmlRenderer.Builder#extensions(Iterable)}).
 * </p>
 * <p>
 * The parsed superscript text regions are turned into {@link Superscript} nodes.
 * </p>
 */
public class SuperscriptExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private SuperscriptExtension() {
    }

    public static Extension create() {
        return new SuperscriptExtension();
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new SuperscriptDelimiterProcessor());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.customHtmlRenderer(new SuperscriptHtmlRenderer());
    }

}
