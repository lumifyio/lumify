package io.lumify.twitter;

import io.lumify.core.cmdline.CommandLineBase;

import org.apache.commons.cli.CommandLine;

public class TwitterDataIngestRunner extends CommandLineBase {

    public static void main(String[] args) throws Exception {
        int res = new TwitterDataIngestRunner().run(args);
        if( res != 0 ) {
            System.exit(res);
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        LOGGER.info("Twitter data ingestion complete");
        return 0;
    }
}
