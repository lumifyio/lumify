package io.lumify.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.lumify.core.ingest.term.extraction.TermExtractionResult;
import io.lumify.mapping.csv.CsvDocumentMapping;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import org.securegraph.Visibility;

/**
 * An interface for DocumentMappings.
 */
@JsonTypeInfo(include = As.PROPERTY, property = "type", use = Id.NAME)
@JsonSubTypes({
        @Type(CsvDocumentMapping.class)
})
public interface DocumentMapping {
    /**
     * Get the subject of the document.  This will be used as the
     * document title on ingest.
     *
     * @return the document subject or an empty string if none is provided
     */
    @JsonProperty("subject")
    String getSubject();

    /**
     * Read the contents of a document that is the target of this mapping and
     * write them to the provided OutputStream in a format that can later be
     * processed by this mapping.
     *
     * @param inputDoc  the document to read
     * @param outputDoc the writer to write the document to
     * @throws IOException if errors occur while reading or writing the document
     */
    void ingestDocument(final InputStream inputDoc, final Writer outputDoc) throws IOException;

    /**
     * Execute this mapping against the provided document, extracting all Term mentions
     * and relationships found.
     *
     * @param inputDoc   the document to read; typically reading from output Writer provided to
     *                   <code>ingestDocument()</code>
     * @param processId  the ID of the process reading this document
     * @param visibility
     * @return the Term mentions and relationships found in the provided document as indicated by
     * this mapping
     * @throws IOException if an error occurs while applying the mapping
     */
    TermExtractionResult mapDocument(final Reader inputDoc, final String processId, String propertyKey, Visibility visibility) throws IOException;

    /**
     * Execute this mapping against the provided document, returning an iterator over the TermExtractionResults
     * for each distinct element in the structured data (e.g. a CSV line, a JSON object, etc.). Where correct,
     * this method may return a single-element iterator whose contents are the same as a call to mapDocument().
     *
     * @param inputDoc   the document to read; typically reading from output Writer provided to
     *                   <code>ingestDocument()</code>
     * @param processId  the ID of the process reading this document
     * @param visibility the visibility constraints for the extracted terms
     * @return an iterator over the term mentions and relationships found in each distinct entity in
     * the provided document, as indicated by this mapping
     * @throws IOException if an error occurs while applying the mapping
     */
    Iterator<TermExtractionResult> mapDocumentElements(final Reader inputDoc, final String processId, final String propertyKey,
            final Visibility visibility) throws IOException;
}
