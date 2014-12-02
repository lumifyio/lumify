package io.lumify.gpw.yarn;

import org.apache.twill.api.ResourceSpecification;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillSpecification;

public class GraphPropertyWorkerApplication implements TwillApplication {
    public static final String NAME = "lumify-graph-property-worker";
    public static final String RUNNABLE_NAME = "lumify-graph-property-worker-runnable";
    private final int virtualCores;
    private final Size memory;
    private final int instances;

    public GraphPropertyWorkerApplication(int virtualCores, Size memory, int instances) {
        this.virtualCores = virtualCores;
        this.memory = memory;
        this.instances = instances;
    }

    @Override
    public TwillSpecification configure() {
        ResourceSpecification resourceSpecification = ResourceSpecification.Builder.with()
                .setVirtualCores(virtualCores)
                .setMemory(memory.getSize(), memory.getUnits())
                .setInstances(instances)
                .build();

        return TwillSpecification.Builder.with()
                .setName(NAME)
                .withRunnable()
                .add(RUNNABLE_NAME, new GraphPropertyWorkerRunnable(), resourceSpecification).noLocalFiles()
                .anyOrder()
                .build();
    }
}
