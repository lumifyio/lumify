package io.lumify.flightTrack;

import com.google.inject.Inject;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.json.JSONObject;
import org.securegraph.Visibility;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

public class FlightAware extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FlightAware.class);
    private static final String CONFIG_API_KEY = "flightaware.apikey";
    private static final String CONFIG_USERNAME = "flightaware.username";
    private static final String CMD_OPT_QUERY = "query";
    private static final String CMD_OPT_OUTDIR = "out";

    private FlightRepository flightRepository;

    public static void main(String[] args) throws Exception {
        int res = new FlightAware().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }


    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_QUERY)
                        .withDescription("Flight Aware query (eg \"-idents VRD*\")")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_OUTDIR)
                        .withDescription("Output directory")
                        .hasArg()
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File outDir = null;
        if (cmd.hasOption(CMD_OPT_OUTDIR)) {
            outDir = new File(cmd.getOptionValue(CMD_OPT_OUTDIR));
            if (!outDir.exists() || !outDir.isDirectory()) {
                System.err.println("Could not find output directory " + outDir);
                return 1;
            }
        }
        String query = cmd.getOptionValue(CMD_OPT_QUERY);
        String apiKey = getConfiguration().get(CONFIG_API_KEY, null);
        if (apiKey == null) {
            System.err.println("Could not find configuration " + CONFIG_API_KEY);
            return 1;
        }
        String userName = getConfiguration().get(CONFIG_USERNAME, null);
        if (userName == null) {
            System.err.println("Could not find configuration " + CONFIG_USERNAME);
            return 1;
        }
        FlightAwareClient client = new FlightAwareClient(apiKey, userName);

        Visibility visibility = new Visibility("");
        while (true) {
            LOGGER.info("Performing search");
            try {
                JSONObject json = client.search(query);

                if (cmd.hasOption("out")) {
                    String fileName = FlightRepository.ISO8601DATEFORMAT.format(new Date()) + ".json";
                    FileOutputStream out = new FileOutputStream(new File(cmd.getOptionValue("out"), fileName));
                    try {
                        out.write(json.toString(2).getBytes());
                    } finally {
                        out.close();
                    }
                }

                flightRepository.save(json, visibility, getAuthorizations());
            } catch (Exception ex) {
                LOGGER.error("Problem doing search", ex);
            }

            Thread.sleep(15 * 60 * 1000);
        }
    }

    @Inject
    public void setFlightRepository(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }
}
