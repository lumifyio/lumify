---
layout: post
title: Lumify Feature - Auditing
author: Susan Feng
excerpt:  Read a brief explanation of how Lumify tackles auditing and how we designed a data model in Accumulo to store audit records.
---

## Lumify Feature - Auditing

When many people hear the term audit, they immediately associate
auditing with someone examining a company's financial records. According
to [Merriam-Webster](http://www.merriam-webster.com/dictionary/auditing), they define audit as "a complete and careful examination of the
financial records of a business or person." In the Lumify world, auditing
is a collection of records of every C.R.U.D action performed on any entity. In the [What is Lumify?]({{site.url}}/assets/2014/01/21/what-is-lumify.html) blog post,
we defined an entity as "any 'thing' you want to represent in Lumify",
which could be a person, organization, location, etc., that has properties and relationship to other entities.

### What does Lumify currently audit?

Before we get into the backend of how Lumify actually stores audit records,
it is important to know what Lumify currently audits:

1. Creating and deleting entities
2. Creating, updating, and deleting properties
3. Creating and deleting relationships

### Data Model

Lumify utilizes [Accumulo](https://accumulo.apache.org/) as our backend database for storing audit records, along
with various other data which will be further explained in future blog
posts. Essentially every C.R.U.D action is a separate row in our
Accumulo table that is specifically for audit data. While designing the
data model for this table, we wanted to generate the row keys in such a way that would optimize retrieval of audit
records for a specific vertex. Our row key is a combination of the vertex
ids, which are unique to every vertex, and a timestamp of when the action
was performed. This allows for a fast prefix scan in Accumulo. Each row contains a column family that we named "*common*"
and either a "*property*" or "*relationship*" column family.

The "common" column family represents data that every audit record will have, e.g. user
information and type of action performed.

![common]({{site.url}}/assets/2014-02-26-lumify-feature-auditing/common-audit-model.png)

The "property" column family represents data that are specific to property manipulation, e.g. old
property and new property values.

![property]({{site.url}}/assets/2014-02-26-lumify-feature-auditing/property-audit-model.png)

Lastly, our "relationship" column family represents data that are specific to
relationship manipulation, e.g. source & target vertices.

![relationship]({{site.url}}/assets/2014-02-26-lumify-feature-auditing/relationship-audit-model.png)

These table views are generated using another open source project from
[Altamira Technologies Corporation](http://www.altamiracorp.com) named
[bigtable](https://github.com/altamiracorp/bigtable). Not only does it provide a simple
UI for BigTable data models, it also provide an framework implementation for BigTable models in Java.

### How does Lumify surface Audit Data?

Lumify allows for a slick way of viewing audit records specific to the
entity you are currently viewing. Lumify on the front
end puts together the audit data sent from the Server, which is a json object of all column qualifiers
 and column values specific to a vertex, into a coherent
way for people to understand. To allow for a more organized way of
viewing audit records, Lumify also groups property audit records in addition
to collapsing long lists of audit records.

![sergey-brin]({{site.url}}/assets/2014-02-26-lumify-feature-auditing/sergey-brin-audit.png)

Like everything else in Lumify, we allow the capability to drag entities
onto the graph and to see data about entities that are referenced directly from the audit message.
For instance clicking on "GooglePlex" will take you to the
detail pane containing data about the entity "GooglePlex"

### Digging Deeper

To dig deeper into the data model Lumify uses for audit records, please
feel free to explore our [back end code](https://github.com/altamiracorp/lumify/tree/master/lumify-core/src/main/java/com/altamiracorp/lumify/core/model/audit) and [front end code](https://github.com/altamiracorp/lumify/tree/master/lumify-web-war/src/main/webapp/js/detail) for auditing.
