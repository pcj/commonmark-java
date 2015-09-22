package org.commonmark.ext.subscript;

import org.commonmark.Extension;
import org.commonmark.ext.subscript.internal.SubscriptDelimiterProcessor;
import org.commonmark.ext.subscript.internal.SubscriptHtmlRenderer;
import org.commonmark.parser.Parser;
import org.commonmark.html.HtmlRenderer;

/**
 * Extension for subscript using ^^ (similar to Pandoc).
 * <p>
 * Create it with {@link #create()} and then configure it on the builders
 * ({@link org.commonmark.parser.Parser.Builder#extensions(Iterable)},
 * {@link org.commonmark.html.HtmlRenderer.Builder#extensions(Iterable)}).
 * </p>
 * <p>
 * The parsed subscript text regions are turned into {@link Subscript} nodes.
 * </p>
 */
public class SubscriptExtension implements Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {

    private SubscriptExtension() {
    }

    public static Extension create() {
        return new SubscriptExtension();
    }

    @Override
    public void extend(Parser.Builder parserBuilder) {
        parserBuilder.customDelimiterProcessor(new SubscriptDelimiterProcessor());
    }

    @Override
    public void extend(HtmlRenderer.Builder rendererBuilder) {
        rendererBuilder.customHtmlRenderer(new SubscriptHtmlRenderer());
    }

}
