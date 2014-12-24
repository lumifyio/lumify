package io.lumify.zipcode;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.RegexGraphPropertyWorker;

public class ZipCodeGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String ZIPCODE_REG_EX = "\\b\\d{5}-\\d{4}\\b|\\b\\d{5}\\b";
    public static final String CONFIG_IRI = "ontology.iri.zipCode";
    private String ontologyClassUri;

    public ZipCodeGraphPropertyWorker() {
        super(ZIPCODE_REG_EX);
    }

    @Override
    protected String getOntologyClassUri() {
        return ontologyClassUri;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.ontologyClassUri = (String) workerPrepareData.getConfiguration().get(CONFIG_IRI);
        if (this.ontologyClassUri == null || this.ontologyClassUri.length() == 0) {
            throw new LumifyException("Could not find configuration property: " + CONFIG_IRI);
        }
        super.prepare(workerPrepareData);
    }
}
