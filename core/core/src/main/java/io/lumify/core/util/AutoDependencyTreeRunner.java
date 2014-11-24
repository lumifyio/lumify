package io.lumify.core.util;

import java.util.ArrayList;
import java.util.List;

public class AutoDependencyTreeRunner {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AutoDependencyTreeRunner.class);
    private List<DependencyNode> dependencyNodes = new ArrayList<DependencyNode>();

    public void add(Runnable... newRunnables) {
        for (int i = 0; i < newRunnables.length; i++) {
            if (i == 0) {
                addDependencyNode(newRunnables[i], null);
            } else {
                addDependencyNode(newRunnables[i], newRunnables[i - 1]);
            }
        }
    }

    private void addDependencyNode(Runnable runnable, Runnable dependency) {
        DependencyNode dependencyNode = findOrAddDependencyNode(runnable);
        if (dependency != null) {
            dependencyNode.addDependency(findOrAddDependencyNode(dependency));
        }
    }

    private DependencyNode findOrAddDependencyNode(Runnable runnable) {
        DependencyNode dependencyNode = findDependencyNode(runnable);
        if (dependencyNode == null) {
            dependencyNode = new DependencyNode(runnable);
            dependencyNodes.add(dependencyNode);
        }
        return dependencyNode;
    }

    private DependencyNode findDependencyNode(Runnable runnable) {
        for (DependencyNode dependencyNode : dependencyNodes) {
            if (dependencyNode.equals(runnable)) {
                return dependencyNode;
            }
        }
        return null;
    }

    public void dryRun() {
        run(true);
    }

    public void run() {
        run(false);
    }

    private void run(boolean dryRun) {
        List<DependencyNode> ranNodes = new ArrayList<DependencyNode>();
        for (DependencyNode dependencyNode : dependencyNodes) {
            run(dependencyNode, ranNodes, dryRun);
        }
    }

    private void run(DependencyNode dependencyNode, List<DependencyNode> ranNodes, boolean dryRun) {
        for (DependencyNode dependent : dependencyNode.getDependents()) {
            if (ranNodes.contains(dependent)) {
                continue;
            }

            run(dependent, ranNodes, dryRun);
            ranNodes.add(dependent);
        }
        if (!ranNodes.contains(dependencyNode)) {
            LOGGER.debug("Running " + dependencyNode);
            if (!dryRun) {
                dependencyNode.getRunnable().run();
            }
            ranNodes.add(dependencyNode);
        }
    }

    private static class DependencyNode {
        private final Runnable runnable;
        private final List<DependencyNode> dependents = new ArrayList<DependencyNode>();

        public DependencyNode(Runnable runnable) {
            this.runnable = runnable;
        }

        public void addDependency(DependencyNode dependentNode) {
            dependents.add(dependentNode);
        }

        public List<DependencyNode> getDependents() {
            return dependents;
        }

        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public String toString() {
            return getRunnable().toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Runnable) {
                return obj == this.runnable;
            } else if (obj instanceof DependencyNode) {
                return ((DependencyNode) obj).getRunnable() == this.runnable;
            } else {
                throw new RuntimeException("Not supported");
            }
        }
    }
}
