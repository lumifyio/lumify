package io.lumify.zipcode;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkerPrepareData;
import io.lumify.core.ingest.graphProperty.RegexGraphPropertyWorker;
import io.lumify.core.model.ontology.Concept;

public class ZipCodeGraphPropertyWorker extends RegexGraphPropertyWorker {
    private static final String ZIPCODE_REG_EX = "\\b\\d{5}-\\d{4}\\b|\\b\\d{5}\\b";
    private Concept concept;

    public ZipCodeGraphPropertyWorker() {
        super(ZIPCODE_REG_EX);
    }

    @Override
    protected Concept getConcept() {
        return concept;
    }

    @Override
    public void prepare(GraphPropertyWorkerPrepareData workerPrepareData) throws Exception {
        this.concept = getOntologyRepository().getConceptByIntent("zipCode");
        if (this.concept == null) {
            throw new LumifyException("Could not find intent: zipCode");
        }
        super.prepare(workerPrepareData);
    }
}
