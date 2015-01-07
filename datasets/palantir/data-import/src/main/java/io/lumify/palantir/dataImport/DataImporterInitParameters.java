package io.lumify.palantir.dataImport;

import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Visibility;

public class DataImporterInitParameters {
    private String connectionString;
    private String username;
    private String password;
    private String tableNamespace;
    private String idPrefix;
    private String owlPrefix;
    private String outputDirectory;
    private Graph graph;
    private Visibility visibility;
    private Authorizations authorizations;
    private boolean ontologyExport;
    private String hasMediaConceptTypeIri;

    public String getConnectionString() {
        return connectionString;
    }

    public DataImporterInitParameters setConnectionString(String connectionString) {
        this.connectionString = connectionString;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DataImporterInitParameters setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DataImporterInitParameters setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getTableNamespace() {
        return tableNamespace;
    }

    public DataImporterInitParameters setTableNamespace(String tableNamespace) {
        this.tableNamespace = tableNamespace;
        return this;
    }

    public String getIdPrefix() {
        return idPrefix;
    }

    public DataImporterInitParameters setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
        return this;
    }

    public String getOwlPrefix() {
        return owlPrefix;
    }

    public DataImporterInitParameters setOwlPrefix(String owlPrefix) {
        this.owlPrefix = owlPrefix;
        return this;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public DataImporterInitParameters setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        return this;
    }

    public Graph getGraph() {
        return graph;
    }

    public DataImporterInitParameters setGraph(Graph graph) {
        this.graph = graph;
        return this;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public DataImporterInitParameters setVisibility(Visibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public DataImporterInitParameters setAuthorizations(Authorizations authorizations) {
        this.authorizations = authorizations;
        return this;
    }

    public DataImporterInitParameters setOntologyExport(boolean ontologyExport) {
        this.ontologyExport = ontologyExport;
        return this;
    }

    public boolean isOntologyExport() {
        return ontologyExport;
    }

    public String getHasMediaConceptTypeIri() {
        return hasMediaConceptTypeIri;
    }

    public DataImporterInitParameters setHasMediaConceptTypeIri(String hasMediaConceptTypeIri) {
        this.hasMediaConceptTypeIri = hasMediaConceptTypeIri;
        return this;
    }
}
