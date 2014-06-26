package io.lumify.sphinx;

import io.lumify.core.ingest.video.VideoTranscript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SphinxOutputParser {
    public static VideoTranscript parse(String output, double offsetInSec) throws IOException {
        VideoTranscript transcript = new VideoTranscript();
        BufferedReader reader = new BufferedReader(new StringReader(output));

        Pattern wordPattern = Pattern.compile("^([^\\s]+) ([0-9\\.]+) ([0-9\\.]+) ([0-9\\.]+)$");
        String line;
        StringBuilder sentence = new StringBuilder();
        double endTime = 0.0;
        double sentenceStartTime = 0.0;
        while ((line = reader.readLine()) != null) {
            Matcher m = wordPattern.matcher(line);
            if (m.matches()) {
                String word = m.group(1);
                double startTime = Double.parseDouble(m.group(2));
                endTime = Double.parseDouble(m.group(2));
                double duration = Double.parseDouble(m.group(2));
                if ("<s>".equals(word) || "</s>".equals(word)) {
                    if (sentence.toString().length() > 0) {
                        long s = (long) ((sentenceStartTime + offsetInSec) * 1000);
                        long e = (long) ((endTime + offsetInSec) * 1000);
                        transcript.add(new VideoTranscript.Time(s, e), sentence.toString().trim());
                    }
                    sentence = new StringBuilder();
                    sentenceStartTime = startTime;
                } else {
                    sentence.append(word.replaceAll("\\s*\\(\\d+\\)\\s*$", ""));
                    sentence.append(' ');
                }
            }
        }

        if (sentence.toString().length() > 0) {
            long s = (long) ((sentenceStartTime + offsetInSec) * 1000);
            long e = (long) ((endTime + offsetInSec) * 1000);
            transcript.add(new VideoTranscript.Time(s, e), sentence.toString().trim());
        }

        return transcript;
    }
}
