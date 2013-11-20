package com.altamiracorp.lumify.model.query.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StandardAnalyzer.class)
public class LuceneTokenizerTest {

    private static final String TERM_VALUE = "foo";

    @Mock
    private Analyzer analyzer;

    @Before
    public void setupTests() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void testTokenizeStringInvalidAnalyzer() {
        LuceneTokenizer.tokenizeString(null, TERM_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testTokenizeStringInvalidString() {
        LuceneTokenizer.tokenizeString(analyzer, null);
    }

    @Test(expected = RuntimeException.class)
    public void testTokenizeStringStreamException() throws IOException {
        final Analyzer analyzer = PowerMockito.spy(new StandardAnalyzer(Version.LUCENE_42));

        when(analyzer.tokenStream(Matchers.anyString(), any(Reader.class))).thenThrow(new IOException());

        LuceneTokenizer.tokenizeString(analyzer, TERM_VALUE);
    }

    @Test
    public void testStandardTokenizeEmailAddress() {
        final List<String> expectedTokens = ImmutableList.<String>of("somebody", "foo.com");

        final List<String> actualTokens = LuceneTokenizer.standardTokenize("somebody@foo.com");

        assertEquals(expectedTokens.size(), actualTokens.size());
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testStandardTokenizePhoneNumber() {
        final List<String> expectedTokens = ImmutableList.<String>of("123.456.7890");

        final List<String> actualTokens = LuceneTokenizer.standardTokenize("123.456.7890");

        assertEquals(expectedTokens.size(), actualTokens.size());
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testStandardTokenizePhoneNumberHyphens() {
        final List<String> expectedTokens = ImmutableList.<String>of("123", "456", "7890");

        final List<String> actualTokens = LuceneTokenizer.standardTokenize("123-456-7890");

        assertEquals(expectedTokens.size(), actualTokens.size());
        assertThat(actualTokens, is(expectedTokens));
    }

    @Test
    public void testStandardTokenizeSentence() {
        final List<String> expectedTokens = ImmutableList.<String>of("quick", "brown", "fox");

        final List<String> actualTokens = LuceneTokenizer.standardTokenize("The quick brown fox.");

        assertEquals(expectedTokens.size(), actualTokens.size());
        assertThat(actualTokens, is(expectedTokens));
    }
}

