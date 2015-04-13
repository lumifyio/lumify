package io.lumify.youtube;

import io.lumify.core.ingest.video.VideoTranscript;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class YoutubeccReader {
    public static VideoTranscript read(File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        try {
            return read(in);
        } finally {
            in.close();
        }
    }

    public static VideoTranscript read(InputStream in) throws Exception {
        VideoTranscript videoTranscript = new VideoTranscript();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        NodeList textElements = doc.getElementsByTagName("text");
        for (int i = 0; i < textElements.getLength(); i++) {
            Element textElement = (Element) textElements.item(i);
            double start = Double.parseDouble(textElement.getAttribute("start"));
            double duration = Double.parseDouble(textElement.getAttribute("dur"));
            String text = textElement.getTextContent().trim();
            VideoTranscript.Time time = new VideoTranscript.Time((long) (start * 1000), (long) ((start + duration) * 1000));
            videoTranscript.add(time, text);
        }
        return videoTranscript;
    }
}
