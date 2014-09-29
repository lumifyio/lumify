package org.apache.tika.parser.pdf;

public class LumifyParserConfig extends PDFParserConfig {
    @Override
    public void configure(PDF2XHTML pdf2XHTML) {
        super.configure(pdf2XHTML);

        // Better paragraph detection
        pdf2XHTML.setDropThreshold(2.0f);
    }
}
