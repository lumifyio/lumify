
# Analyst's Notebook Export

This Lumify web plugin allows users to export the vertices and edges to an `.anx` file that can be imported into
Analyst's Notebook (ANB).

## Build

1. build the plugin uber-jar

        mvn package -pl web/plugins/analysts-notebook-export -am

## Install

1. copy the plugin uber-jar to `/opt/lumify/lib`

## Configure

### Ontology Annotations

1. optionally add annotations to the concepts in your `.owl` file(s) to specify what ANB `iconFile` images to use
for vertices, e.g.:

         <rdf:RDF xmlns="http://lumify.io/dev#"
              xml:base="http://lumify.io/dev"
              xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
              xmlns:lumify="http://lumify.io#"
              xmlns:analystsNotebook851="http://lumify.io/analystsNotebook/v8.5.1#"
              xmlns:owl="http://www.w3.org/2002/07/owl#"
              xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
              xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
             <owl:Ontology rdf:about="http://lumify.io/dev">
                 <owl:imports rdf:resource="http://lumify.io"/>
             </owl:Ontology>

         <owl:Class rdf:about="http://lumify.io/dev#document">
             <rdfs:label xml:lang="en">Document</rdfs:label>
             <rdfs:subClassOf rdf:resource="http://lumify.io/dev#raw"/>
             <lumify:displayType xml:lang="en">document</lumify:displayType>
             <lumify:color xml:lang="en">rgb(28, 137, 28)</lumify:color>
             <lumify:subtitleFormula xml:lang="en">prop(&apos;http://lumify.io#author&apos;) || &apos;&apos;</lumify:subtitleFormula>
             <analystsNotebook851:iconFile>Document</analystsNotebook851:iconFile>
         </owl:Class>

### Lumify Configuration Properties

1. configure which version(s) you want to enable with pairs of `menuOption` properties, e.g.:

         web.ui.analystsNotebookExport.menuOption.1.label=8.5.1
         web.ui.analystsNotebookExport.menuOption.1.value=VERSION_8.5.1

1. see [config/analysts-notebook-export.properties](config/analysts-notebook-export.properties) for other options
