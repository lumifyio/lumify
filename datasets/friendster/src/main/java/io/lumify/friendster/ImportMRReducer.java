package io.lumify.friendster;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.apache.accumulo.core.data.Mutation;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

class ImportMRReducer extends Reducer<Text, Mutation, Text, Mutation> {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ImportMRReducer.class);

    @Override
    protected void reduce(Text keyText, Iterable<Mutation> values, Context context) throws IOException, InterruptedException {
        try {
            safeReduce(keyText, values, context);
        } catch (Exception ex) {
            LOGGER.error("failed reduce", ex);
        }
    }

    private void safeReduce(Text keyText, Iterable<Mutation> values, Context context) throws IOException, InterruptedException {
        String key = keyText.toString();
        context.setStatus(key);
        int keySplitLocation = key.indexOf(ImportMR.KEY_SPLIT);
        if (keySplitLocation < 0) {
            throw new IOException("Invalid key: " + keyText);
        }
        String tableNameString = key.substring(0, keySplitLocation);
        Text tableName = new Text(tableNameString);
        writeAccumuloMutations(context, key, tableName, values);
    }

    private void writeAccumuloMutations(Context context, String key, Text tableName, Iterable<Mutation> values) throws IOException, InterruptedException {
        int totalItemCount = 0;
        for (Mutation m : values) {
            if (totalItemCount % 1000 == 0) {
                context.setStatus(String.format("%s (count: %d)", key, totalItemCount));
            }
            context.write(tableName, m);
            totalItemCount++;
        }
    }
}
