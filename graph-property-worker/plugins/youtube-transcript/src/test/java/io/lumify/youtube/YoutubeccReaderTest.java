package io.lumify.youtube;

import io.lumify.core.ingest.video.VideoTranscript;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class YoutubeccReaderTest {
    @Test
    public void testRead() throws Exception {
        YoutubeccReader reader = new YoutubeccReader();
        InputStream in = IOUtils.toInputStream("<?xml version=\"1.0\" encoding=\"utf-8\" ?><transcript><text start=\"3.502\" dur=\"4.739\">PRESIDENT MR. PRESIDENT THANKS </text><text start=\"7.173\" dur=\"1.735\">SO MUCH FOR JOINING US. </text></transcript>");
        VideoTranscript videoTranscript = reader.read(in);
        List<VideoTranscript.TimedText> entries = videoTranscript.getEntries();
        assertEquals(2, entries.size());

        VideoTranscript.TimedText entry1 = entries.get(0);
        assertEquals("PRESIDENT MR. PRESIDENT THANKS", entry1.getText());
        assertEquals(3502L, entry1.getTime().getStart().longValue());
        assertEquals(3502L + 4739L, entry1.getTime().getEnd().longValue());

        VideoTranscript.TimedText entry2 = entries.get(1);
        assertEquals("SO MUCH FOR JOINING US.", entry2.getText());
        assertEquals(7173L, entry2.getTime().getStart().longValue());
        assertEquals(7173L + 1735L, entry2.getTime().getEnd().longValue());
    }
}
