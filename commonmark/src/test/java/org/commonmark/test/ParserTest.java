package org.commonmark.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import static org.junit.Assert.assertEquals;

import org.commonmark.spec.SpecReader;
import org.junit.Test;

public class ParserTest {

    @Test
    public void ioReaderTest() throws IOException {
        Parser parser = Parser.builder().build();
        
        InputStream input1 = SpecReader.getSpecInputStream();
        Node document1;
        try (InputStreamReader reader = new InputStreamReader(input1)) {
            document1 = parser.parseReader(reader);
        }

        String spec = SpecReader.readSpec();
        Node document2 = parser.parse(spec);
        
        HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();
        assertEquals(renderer.render(document2), renderer.render(document1));
    }
}