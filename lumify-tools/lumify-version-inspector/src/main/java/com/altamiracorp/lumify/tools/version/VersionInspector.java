package com.altamiracorp.lumify.tools.version;

import static com.google.common.base.Preconditions.*;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * This utility searches all elements of the target classpath for
 * META-INF/lumify/*-build.properties files and outputs version
 * information for all available Lumify components.
 */
public class VersionInspector extends CommandLineBase {
    private static final String SEP_LINE = fill(60, '-');
    private static final String SCAN_PATH = "scanpath";

    private static final String fill(int count, char fillChar) {
        StringBuilder builder = new StringBuilder();
        for (int idx = 0; idx < count; idx++) {
            builder.append(fillChar);
        }
        return builder.toString();
    }

    private ProjectInfoScanner scanner;
    private OutputFormat format;

    public VersionInspector() {
        initFramework = false;
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(OptionBuilder
                .withLongOpt(SCAN_PATH)
                .withDescription("The Java classpath to scan for Lumify project information. "
                        + "This classpath may include both directories and zip archives (ZIP, JAR, EAR, WAR).")
                .hasArg(true)
                .withArgName(SCAN_PATH)
                .isRequired()
                .create());

        options.addOption(OptionBuilder
                .withLongOpt("short")
                .withDescription("Write minimal details about each Lumify component.")
                .create('s'));

        options.addOption(OptionBuilder
                .withLongOpt("verbose")
                .withDescription("Write full details about each Lumify component.")
                .create('v'));

        return options;
    }

    @Override
    protected void processOptions(final CommandLine cmd) throws Exception {
        super.processOptions(cmd);

        String scanPath = cmd.getOptionValue(SCAN_PATH);
        checkNotNull(scanPath, "scanpath must be provided");
        Set<File> scanFiles = new HashSet<File>();
        for (String path : scanPath.trim().split(File.pathSeparator)) {
            scanFiles.add(new File(path));
        }
        checkArgument(!scanFiles.isEmpty(), "scanpath must contain at least one element");
        scanner = new ProjectInfoScanner(scanFiles);

        if (cmd.hasOption('v')) {
            format = OutputFormat.LONG;
        } else if (cmd.hasOption('s')) {
            format = OutputFormat.SHORT;
        } else {
            format = OutputFormat.NORMAL;
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        Map<String, String[]> rows = new TreeMap<String, String[]>();
        int colCount = 0;
        for (ProjectInfo info : scanner) {
            String[] row = format.format(info);
            colCount = row.length;
            rows.put(info.getName(), row);
        }
        if (!rows.isEmpty()) {
            int[] maxColWidths = new int[colCount];
            for (int col = 0; col < colCount; col++) {
                maxColWidths[col] = 0;
            }
            for (String[] row : rows.values()) {
                for (int col=0; col < colCount; col++) {
                    maxColWidths[col] = Math.max(maxColWidths[col], row[col].length());
                }
            }
            StringBuilder output = new StringBuilder();
            for (String[] row : rows.values()) {
                if (row.length == 1) {
                    output.append(row[0]).append('\n');
                } else {
                    for (int col=0; col < colCount; col++) {
                        output.append(String.format("%-" + maxColWidths[col] + "s", row[col])).append("  ");
                    }
                    output.append('\n');
                }
            }
            System.out.print(output.toString());
        } else {
            System.out.println("No Lumify Components Found");
        }
        return 0;
    }

    private static enum OutputFormat {
        SHORT,
        NORMAL,
        LONG;

        public String[] format(final ProjectInfo info) {
            switch (this) {
                case SHORT: return VersionInspector.shortFormat(info);
                case NORMAL: return VersionInspector.normalFormat(info);
                case LONG: return VersionInspector.longFormat(info);
                default: throw new IllegalStateException("Unknown OutputFormat: " + this);
            }
        }
    }

    private static String[] shortFormat(final ProjectInfo info) {
        // lumify-core 1.0-SNAPSHOT [53b87d] Wednesday, February 12, 2014 00:08:49.049 EST
        return new String[] {
            info.getArtifactId(),
            info.getVersion(),
            String.format("[%s]", info.getScmRevision()),
            info.getBuildInfo().getDate()
        };
    }

    private static String[] normalFormat(final ProjectInfo info) {
        // Lumify: Core (com.altamiracorp.lumify:lumify-core:1.0-SNAPSHOT) [53b87d] Wednesday, February 12, 2014 00:08:49.049 EST
        return new String[] {
            info.getName(),
            String.format("(%s)", info.getCoordinates()),
            String.format("[%s]", info.getScmRevision()),
            info.getBuildInfo().getDate()
        };
    }

    private static String[] longFormat(final ProjectInfo info) {
        // ---------------
        // Project: Lumify: Core
        // Group ID: com.altamiracorp.lumify
        // Artifact ID: lumify-core
        // Version: 1.0-SNAPSHOT
        // SCM Revision: 53b87d...
        // Location: lumify-web.war::/WEB-INF/lib/lumify-core.jar::/META-INF/lumify/lumify-core-build.properties
        // Built On: Wednesday, February 12, 2014 00:08:49.049 EST
        // Built By: gshankman
        // Build Platform: Mac OS X 10.9.1 (x86_64)
        // Build JVM: 1.7.0_51-b13 (Oracle Corporation)
        // Built By Maven: 3.1.1
        // ---------------
        BuildInfo build = info.getBuildInfo();
        return new String[] {
            new StringBuilder()
                    .append(SEP_LINE).append('\n')
                    .append("Project:            ").append(info.getName()).append('\n')
                    .append("Group ID:           ").append(info.getGroupId()).append('\n')
                    .append("Artifact ID:        ").append(info.getArtifactId()).append('\n')
                    .append("Version:            ").append(info.getVersion()).append('\n')
                    .append("SCM Revision:       ").append(info.getScmRevision()).append('\n')
                    .append("Location:           ").append(info.getSource()).append('\n')
                    .append("Built On:           ").append(build.getDate()).append('\n')
                    .append("Built By:           ").append(build.getUser()).append('\n')
                    .append("Build Platform:     ").append(build.osSpec()).append('\n')
                    .append("Build JVM:          ").append(build.jvmSpec()).append('\n')
                    .append("Built By Maven:     ").append(build.getMavenVersion()).append('\n')
                    .append(SEP_LINE).append('\n')
                    .toString()
        };
    }

    public static void main(String[] args) throws Exception {
        int res = new VersionInspector().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }
}
