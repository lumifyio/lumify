package com.altamiracorp.lumify.core.util;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.core.model.artifact.Artifact;
import com.altamiracorp.lumify.core.model.artifactThumbnails.ArtifactThumbnail;
import com.altamiracorp.lumify.core.model.audit.Audit;
import com.altamiracorp.lumify.core.model.dictionary.DictionaryEntry;
import com.altamiracorp.lumify.core.model.geoNames.GeoName;
import com.altamiracorp.lumify.core.model.geoNames.GeoNameAdmin1Code;
import com.altamiracorp.lumify.core.model.geoNames.GeoNameCountryInfo;
import com.altamiracorp.lumify.core.model.geoNames.GeoNamePostalCode;
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
            Artifact.TABLE_NAME,
            ArtifactThumbnail.TABLE_NAME,
            Workspace.TABLE_NAME,
            TermMentionModel.TABLE_NAME,
            VideoFrame.TABLE_NAME,
            Resource.TABLE_NAME,
            UserRow.TABLE_NAME,
            GeoName.TABLE_NAME,
            GeoNameAdmin1Code.TABLE_NAME,
            GeoNameCountryInfo.TABLE_NAME,
            GeoNamePostalCode.TABLE_NAME,
            DictionaryEntry.TABLE_NAME,
            Audit.TABLE_NAME,
            "atc_titan", // TODO refactor see com.altamiracorp.lumify.model.TitanGraphSession
            "atc_securegraph"); // TODO refactor add delete to securegraph?

    public static void initializeTables(ModelSession modelSession, User user) {
        for (String table : tables) {
            modelSession.initializeTable(table, user.getModelUserContext());
        }
    }

    public static void deleteTables(ModelSession modelSession, User user) {
        for (String table : tables) {
            modelSession.deleteTable(table, user.getModelUserContext());
        }
    }
}
