package io.lumify.assignimagemr;

import org.apache.hadoop.conf.Configuration;
import org.securegraph.Visibility;
import org.securegraph.accumulo.AccumuloAuthorizations;
import org.securegraph.accumulo.mapreduce.SecureGraphMRUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AssignImageConfiguration {
    public static final String HAS_IMAGE_LABELS = "assignImageMR.hasImageLabel";
    private final AccumuloAuthorizations authorizations;
    private final String[] hasImageLabels;
    private final Visibility visibility;

    public AssignImageConfiguration(Configuration configuration) {
        this.authorizations = new AccumuloAuthorizations(configuration.getStrings(SecureGraphMRUtils.CONFIG_AUTHORIZATIONS));
        List<String> hasImageLabelsList = new ArrayList<String>();
        for (int i = 0; i < 10000; i++) {
            hasImageLabelsList.addAll(Arrays.asList(configuration.get(HAS_IMAGE_LABELS + "." + i)));
        }
        this.hasImageLabels = hasImageLabelsList.toArray(new String[hasImageLabelsList.size()]);
        this.visibility = new Visibility("");
    }

    public AccumuloAuthorizations getAuthorizations() {
        return authorizations;
    }

    public String[] getHasImageLabels() {
        return hasImageLabels;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
