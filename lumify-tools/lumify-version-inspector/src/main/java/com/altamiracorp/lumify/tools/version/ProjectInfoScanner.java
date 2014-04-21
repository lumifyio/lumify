package com.altamiracorp.lumify.tools.version;

import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;

/**
 *
 */
public class ProjectInfoScanner implements Iterable<ProjectInfo>, Iterator<ProjectInfo> {
    private static final LumifyLogger LOG = LumifyLoggerFactory.getLogger(ProjectInfoScanner.class);

    private static final Pattern LUMIFY_BUILD_INFO_PATTERN =
            Pattern.compile(".*\\bMETA-INF[\\\\/]lumify[\\\\/].*-build.properties$");
    private static final Pattern ARCHIVE_PATTERN = Pattern.compile(".*\\.(jar|war|ear)$");
    private static final String[] TARGET_EXTENSIONS = new String[] {
        "properties",
        "jar",
        "war",
        "ear"
    };

    private final Deque<Iterator<Entry>> entryIterStack;
    private ProjectInfo nextInfo;

    public ProjectInfoScanner(final Collection<File> roots) {
        Set<File> fullTree = new TreeSet<File>();
        for (File root : roots) {
            if (root.isDirectory()) {
                fullTree.addAll(FileUtils.listFiles(root, TARGET_EXTENSIONS, true));
            } else {
                fullTree.add(root);
            }
        }
        List<Entry> scanRoots = new ArrayList<Entry>(fullTree.size());
        for (File file : fullTree) {
            scanRoots.add(new FileEntry(file));
        }

        entryIterStack = new LinkedList<Iterator<Entry>>();
        entryIterStack.push(scanRoots.iterator());
        advance();
    }

    @Override
    public Iterator<ProjectInfo> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return nextInfo != null;
    }

    @Override
    public ProjectInfo next() {
        if (nextInfo == null) {
            throw new NoSuchElementException();
        }
        ProjectInfo info = nextInfo;
        advance();
        return info;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }

    private void advance() {
        try {
            nextInfo = findNext();
        } catch (IOException ioe) {
            LOG.debug("Error finding next Lumify build info.", ioe);
            throw new IllegalStateException("Error finding next Lumify build info.", ioe);
        }
    }

    private ProjectInfo findNext() throws IOException {
        Iterator<Entry> currentIter;
        Entry next;
        while (!entryIterStack.isEmpty()) {
            currentIter = entryIterStack.peek();
            while (currentIter.hasNext()) {
                next = currentIter.next();
                if (next.isArchive()) {
                    entryIterStack.push(new ZipEntryContainer(next.getFullPath(), next.getInputStream()));
                    return findNext();
                } else if (next.isProjectInfo()) {
                    Properties props = new Properties();
                    props.load(next.getInputStream());
                    Map<String, String> propMap = new HashMap<String, String>();
                    for (String key : props.stringPropertyNames()) {
                        propMap.put(key, props.getProperty(key, ""));
                    }
                    return new ProjectInfo(next.getFullPath(), propMap);
                }
            }
            entryIterStack.pop();
        }
        return null;
    }

    private abstract static class Entry {
        public abstract String getFullPath();
        public abstract InputStream getInputStream() throws IOException;

        @Override
        public String toString() {
            return getFullPath();
        }

        public boolean isArchive() {
            return getFullPath() != null && ARCHIVE_PATTERN.matcher(getFullPath()).matches();
        }

        public boolean isProjectInfo() {
            return getFullPath() != null && LUMIFY_BUILD_INFO_PATTERN.matcher(getFullPath()).matches();
        }
    }

    private static class ZipEntryContainer implements Iterator<Entry> {
        private final String parentName;
        private final ZipInputStream zipStream;
        private boolean hasNext;

        public ZipEntryContainer(final String pName, final InputStream stream) throws IOException {
            zipStream = new ZipInputStream(stream);
            hasNext = true;
            parentName = pName;
        }

        private Entry advance() throws IOException {
            ZipEntry ze = zipStream.getNextEntry();
            String entryName = String.format("%s::%s", parentName, ze != null ? ze.getName() : "");
            return ze != null ? new ArchiveEntry(entryName, zipStream) : null;
        }

        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public Entry next() {
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            // assume we have entries until we reach the end of the zip file.
            // if we advance to the next entry before processing the current
            // entry, we can't read from the stream
            try {
                Entry entry = advance();
                if (entry == null) {
                    entry = new ArchiveEntry(null, null);
                    hasNext = false;
                }
                return entry;
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported");
        }
    }

    private static class ArchiveEntry extends Entry {
        private final String name;
        private final InputStream stream;

        public ArchiveEntry(String name, InputStream stream) {
            this.name = name;
            this.stream = stream;
        }

        @Override
        public String getFullPath() {
            return name;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return stream;
        }
    }

    private static class FileEntry extends Entry {
        private final File file;

        public FileEntry(File file) {
            this.file = file;
        }

        @Override
        public String getFullPath() {
            return file.getAbsolutePath();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new FileInputStream(file);
        }
    }
}
