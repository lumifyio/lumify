package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.Property;
import org.securegraph.Vertex;

public class Requeue extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Requeue.class);
    public static final String OPT_PROPERTYNAME = "propertyname";

    public static void main(String[] args) throws Exception {
        int res = new Requeue().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt(OPT_PROPERTYNAME)
                        .withDescription("The name of the property to requeue")
                        .create("pn")
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String propertyName = cmd.getOptionValue(OPT_PROPERTYNAME);

        LOGGER.info("requeue all vertices (property: %s)", propertyName);
        int count = 0;
        int pushedCount = 0;
        Iterable<Vertex> vertices = getGraph().getVertices(getAuthorizations());
        for (Vertex vertex : vertices) {
            if (propertyName == null) {
                getWorkQueueRepository().pushElement(vertex);
                pushedCount++;
            } else {
                Iterable<Property> properties = vertex.getProperties(propertyName);
                for (Property property : properties) {
                    getWorkQueueRepository().pushGraphPropertyQueue(vertex, property);
                    pushedCount++;
                }
            }
            count++;
            if ((count % 10000) == 0) {
                LOGGER.debug("requeue status. vertices looked at %d. items pushed %d. last vertex id: %s", count, pushedCount, vertex.getId());
            }
        }
        getWorkQueueRepository().flush();
        LOGGER.info("requeue all vertices complete. vertices looked at %d. items pushed %d.", count, pushedCount);

        return 0;
    }
}
