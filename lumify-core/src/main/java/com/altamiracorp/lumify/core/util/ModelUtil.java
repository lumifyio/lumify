package com.altamiracorp.lumify.core.util;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.resources.Resource;
import com.altamiracorp.lumify.core.model.termMention.TermMentionModel;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.videoFrames.VideoFrame;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.user.User;

import java.util.Arrays;
import java.util.List;

public class ModelUtil {

    private static final List<String> tables = Arrays.asList(
            ArtifactThumbnail.TABLE_NAME,
            Workspace.TABLE_NAME,
            TermMentionModel.TABLE_NAME,
            VideoFrame.TABLE_NAME,
            Resource.TABLE_NAME,
            UserRow.TABLE_NAME,
            DictionaryEntry.TABLE_NAME,
            Audit.TABLE_NAME,
            "atc_securegraph"); // TODO refactor to config file info. But since this is only for development this is low priority

    public static void initializeTables(ModelSession modelSession, User user) {
        for (String table : tables) {
            modelSession.initializeTable(table, user.getModelUserContext());
        }
    }

    public static void deleteTables(ModelSession modelSession, User user) {
        for (String table : tables) {
            modelSession.deleteTable(table, user.getModelUserContext());
        }
        for (String table : modelSession.getTableList(user.getModelUserContext())) {
            if (table.startsWith("atc_")) {
                modelSession.deleteTable(table, user.getModelUserContext());
            }
        }
    }
}
