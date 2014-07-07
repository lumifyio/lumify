package io.lumify.flightTrack;

import com.google.inject.Inject;
import io.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.Visibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class Replay extends CommandLineBase {
    public static final String CMD_OPT_INDIR = "in";
    public static final String CMD_OPT_SPEED = "speed";
    public static final double DEFAULT_REPLAY_SPEED = 1.0;

    private FlightRepository flightRepository;

    public static void main(String[] args) throws Exception {
        int res = new Replay().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_INDIR)
                        .withDescription("Input directory")
                        .hasArg()
                        .isRequired()
                        .create("i")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_SPEED)
                        .withDescription("The speed to replay (default: " + DEFAULT_REPLAY_SPEED + ")")
                        .hasArg()
                        .create("s")
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File inDir = new File(cmd.getOptionValue(CMD_OPT_INDIR));
        if (!inDir.exists()) {
            System.err.println(cmd.getOptionValue(CMD_OPT_INDIR) + " does not exist");
            return 1;
        }

        double replaySpeed = DEFAULT_REPLAY_SPEED;
        if (cmd.hasOption(CMD_OPT_SPEED)) {
            replaySpeed = Double.parseDouble(cmd.getOptionValue(CMD_OPT_SPEED));
        }

        ArrayList<File> files = toOrdered(inDir.listFiles());
        if (files.size() == 0) {
            return 2;
        }

        Date lastFileTime = null;
        Date startTime = new Date();
        for (int i = 0; i < files.size(); i++) {
            File f = files.get(i);
            Date t = parseDateFromFileName(f);
            if (i > 0) {
                long sleepTime = calculateSleepTime(startTime, lastFileTime, replaySpeed, t);
                LOGGER.debug("Sleeping for " + (((double) sleepTime) / 1000.0) + "s");
                Thread.sleep(sleepTime);
            }
            try {
                replayFile(f);
            } catch (Exception ex) {
                LOGGER.error("Could not replay file %s", f.getAbsolutePath(), ex);
            }
            lastFileTime = parseDateFromFileName(f);
        }

        return 0;
    }

    private void replayFile(File file) throws Exception {
        LOGGER.debug("Replaying file: " + file.getName());
        JSONObject json = readFile(file);
        Visibility visibility = new Visibility("");
        this.flightRepository.save(json, visibility, getAuthorizations());
    }

    private static JSONObject readFile(File file) throws IOException, JSONException {
        FileInputStream in = new FileInputStream(file);
        try {
            String s = IOUtils.toString(in);
            return new JSONObject(s);
        } finally {
            in.close();
        }
    }

    private static long calculateSleepTime(Date startTime, Date lastFileTime, double replaySpeed, Date fileTime) {
        double timeSinceStart = new Date().getTime() - startTime.getTime();
        double fileTimeDiff = fileTime.getTime() - lastFileTime.getTime();
        double t = (fileTimeDiff - timeSinceStart) / replaySpeed;
        return (long) Math.max(t, 0);
    }

    private static Date parseDateFromFileName(File file) throws java.text.ParseException {
        String dateStr = file.getName();
        int extIndex = dateStr.lastIndexOf('.');
        dateStr = dateStr.substring(0, extIndex);
        return FlightRepository.ISO8601DATEFORMAT.parse(dateStr);
    }

    private static ArrayList<File> toOrdered(File[] files) {
        ArrayList<File> results = new ArrayList<File>();
        for (File f : files) {
            if (f.getName().endsWith(".json")) {
                results.add(f);
            }
        }
        Collections.sort(results, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return results;
    }

    @Inject
    public void setFlightRepository(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }
}
