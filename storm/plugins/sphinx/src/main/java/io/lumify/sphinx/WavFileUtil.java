package io.lumify.sphinx;

import java.io.*;

public class WavFileUtil {
    // see https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
    public static void fixWavHeaders(File wavFile, File wavFileNoHeaders) throws IOException {
        byte[] buffer = new byte[1024];
        InputStream in = new FileInputStream(wavFile);
        OutputStream out = new FileOutputStream(wavFileNoHeaders);
        try {
            int read;

            // read RIFF head
            read = in.read(buffer, 0, 12);
            if (read < 12) {
                throw new IOException("Could not read RIFF header");
            }
            out.write(buffer, 0, 12);

            // skip non-data subchunks
            while (true) {
                read = in.read(buffer, 0, 8);
                if (read < 8) {
                    throw new IOException("Could not read subchunk");
                }
                String subchunkName = new String(buffer, 0, 4);
                if (subchunkName.equals("data")) {
                    out.write(buffer, 0, 8);
                    break;
                }
                int chunkSize = ((((int) buffer[4]) << 0) | (((int) buffer[5]) << 8) | (((int) buffer[6]) << 16) | (((int) buffer[7]) << 24));
                while (chunkSize > 0) {
                    read = Math.min(chunkSize, buffer.length);
                    in.read(buffer, 0, read);
                    chunkSize -= read;
                }
            }

            // copy remaining data
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }
}
