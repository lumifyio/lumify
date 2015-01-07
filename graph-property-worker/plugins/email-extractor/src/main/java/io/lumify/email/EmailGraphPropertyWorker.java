package io.lumify.email;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.RegexGraphPropertyWorker;

public class EmailGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String EMAIL_REG_EX = "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";
    public static final String CONFIG_IRI = "ontology.iri.email";
    private String ontologyClassUri;

    public EmailGraphPropertyWorker() {
        super(EMAIL_REG_EX);
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
