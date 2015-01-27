package io.lumify.email;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.RegexGraphPropertyWorker;
import io.lumify.core.model.ontology.Concept;

public class EmailGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String EMAIL_REG_EX = "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b";
    private Concept concept;

    public EmailGraphPropertyWorker() {
        super(EMAIL_REG_EX);
    }

    @Override
    protected Concept getConcept() {
        return concept;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.concept = getOntologyRepository().getConceptByIntent("email");
        if (this.concept == null) {
            throw new LumifyException("Could not find intent: email");
        }
        super.prepare(workerPrepareData);
    }
}
