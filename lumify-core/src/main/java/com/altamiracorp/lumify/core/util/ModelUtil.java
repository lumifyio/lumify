package com.altamiracorp.lumify.core.util;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.altamiracorp.lumify.core.model.detectedObjects.DetectedObjectModel;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.user.User;

import java.util.Arrays;
import java.util.List;

public class ModelUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ModelUtil.class);

    private static final List<String> tables = Arrays.asList(
            ArtifactThumbnail.TABLE_NAME,
            TermMentionModel.TABLE_NAME,
            VideoFrame.TABLE_NAME,
            DictionaryEntry.TABLE_NAME,
            Audit.TABLE_NAME,
            DetectedObjectModel.TABLE_NAME,
            // TODO refactor to config file info. But since this is only for development this is low priority
            "atc_securegraph_d",
            "atc_securegraph_v",
            "atc_securegraph_e");

    public static void initializeTables(ModelSession modelSession, User user) {
        for (String table : tables) {
            modelSession.initializeTable(table, user.getModelUserContext());
        }
    }

    public static void deleteTables(ModelSession modelSession, User user) {
        LOGGER.debug("BEGIN deleting tables");
        for (String table : tables) {
            modelSession.deleteTable(table, user.getModelUserContext());
        }
        for (String table : modelSession.getTableList(user.getModelUserContext())) {
            if (table.startsWith("atc_")) {
                modelSession.deleteTable(table, user.getModelUserContext());
            }
        }
        LOGGER.debug("END deleting tables");
    }
}
