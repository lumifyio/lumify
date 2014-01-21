---
layout: post
title:  "What is Lumify?"
author: Jeff Kunkle
excerpt: It seems fitting that the first post to the Lumify blog spend some time explaining what Lumify is. What problem does Lumify address? What are the key concepts? What can you do with Lumify? Read the full post for answers to these questions and more.
---
It seems fitting that the first post to the Lumify blog spend some additional time explaining what Lumify is. Summarized in one sentence,

> Lumify is an open source big data analysis and visualization platform.

That high-level nebulous sentence doesn't do the project justice or provide great clarity on just what you can do with Lumify. So let's start from the top.

## What problem does Lumify address?

It's no secret the world is producing a lot of information. [According to IBM](http://www.ibm.com/smarterplanet/us/en/business_analytics/article/it_business_intelligence.html), 90% of the data in the world today has been created in the last two years alone. The ability to deal with the volume and variety of data in organizations today is a serious challenge that's only going to get harder as the rate of data creation accelerates. Lumify was created to help organizations tackle this problem in an open, non-proprietary way.

There are countless vendors in today's marketplace offering a variety of solutions to solve your big data woes. Each takes a slightly different angle on the problem in an effort to differentiate their product. But at the end of the day they all have the same goal, to help you get value and insight form the growing mounds of available data. We built Lumify to do just that.

## What are the key Lumify concepts?

Understanding a couple key concepts will greatly help in making sense of what Lumify does and how it does it.

* **Ontology** - An ontology is the structure for organizing information you care to analyze in Lumify. Basically, it's your data model. The entities, relationships, and properties that make up Lumify's graph are entirely specified by you using [OWL](http://www.w3.org/TR/owl-features/).
* **Entity** - An entity is any "thing" you want to represent in Lumify. It could be a person, place, event, etc. Entities have properties and relationships to other entities.
* **Relationship** - A relationship is a link between two entities. For example, Joe (entity) works with (relationship) Susan (entity). Lumify can be configured to understand and enforce valid relationships between entities.
* **Properties** - Properties are data about an entity. For example, a person entity may have properties for first name, last name, date of birth, height, weight, etc. Lumify will track whatever properties you define in your ontology.
* **Graph** - The data model within Lumify is stored as a graph of entities, or things, and the relationships between those entities. For the technologists out there, these are usually referred to as vertices (entities) and edges (relationships). Entities and relationships can have one or more associated properties.

## What can you do with Lumify?

Lumify is specifically designed for investigative work, where you don't always know exactly what you're looking for and need to explore the data, searching for relationships and patterns within to draw new insights. All of this is done though a clean, modern, purpose-built web interface. Some of the specific features of Lumify include:

### Search

Lumify provides a full-text search over everything in your graph. You can also use custom filters built from properties defined in your ontology.

![searching]({{site.url}}/assets/2014-01-21-what-is-lumify/search.png)

### Graph Visualization

The primary feature of Lumify is the graph visualization. Lumify provides both 2D and 3D graph visualizations with a variety of automatic layouts to help you tame a crowded graph.

![graph visualization]({{site.url}}/assets/2014-01-21-what-is-lumify/graph.png)

### Link Analysis

Lumify provides a variety of options for analyzing the links between entities on the graph. A right click menu on any entity allows you to display all related entities, find paths to another entity, and establish a new relationship (i.e. connect) to another entity.

![link analysis]({{site.url}}/assets/2014-01-21-what-is-lumify/link.png)

### Geospatial Analysis

Understanding how your data relates to the physical world is important for investigative workflows. Lumify provides the ability to integrate any [Open Layers](http://openlayers.org/)-compatible mapping system, such as Google Maps or ESRI, for geospatial analysis. Any data tagged with location information can be aggregated and viewed on a map. You can also search from the map by drawing a shape within which you want to find information.

![map integration]({{site.url}}/assets/2014-01-21-what-is-lumify/map.png)

### Multimedia Analysis

Data comes in all shapes and sizes. Out-of-the-box Lumify comes with specific ingest processing and interface elements for textual content, images, and videos. For example, Lumify will run an OCR process on images in an effort to obtain searchable text and extract closed captioning from videos for indexing. On the interface side, Lumify allows users to tag entities within images, watch videos with synchronized transcripts, and create new entities and relationships from any extracted text. Below is a screenshot of entities extracted from a Wikipedia article.

![text analysis]({{site.url}}/assets/2014-01-21-what-is-lumify/text.png)

### Histogram Analysis

Sometimes it's difficult to understand your data when looking at it one item at a time. Using the histogram feature in Lumify, users can select any number of items on their graph and quickly see a histogram of all property values for the selected entities. 

### Knowledge Building

While graph visualization is Lumify's bread and butter, knowledge creation is a close second. Lumify provides many different ways to resolve new entities, establish relationships, and assign properties from the details view, graph, or map.

![knowledge building]({{site.url}}/assets/2014-01-21-what-is-lumify/build.png)

### Live, Collaborative Spaces

Few of us work alone anymore, especially on difficult investigative-type tasks. Lumify's spaces feature allows you to organize work into a set of projects, or workspaces. Each space can be individually shared in read-only or edit mode with other Lumify users. Changes to the space are immediately propagated to everyone sharing the workspace without needing to refresh browser windows.

![sharing]({{site.url}}/assets/2014-01-21-what-is-lumify/sharing.png)

### Analytics Platform

There's a lot more to Lumify than what you see on the interface. Behind the scenes is an open platform for building custom analytics. Lumify currently integrates [Apache OpenNLP](http://opennlp.apache.org/) for natural language processing of text and [CLAVIN](http://clavin.bericotechnologies.com/) for text geo-tagging/geo-parsing. But all of that is pluggable. In fact, you could replace OpenNLP and CLAVIN with your own implementation or even a proprietary third-party solution. Since Lumify is completely open source, you're free to build any kind of analytic magic you can dream up.

## Who would use Lumify?

Lumify was built to be a general purpose data analysis platform, so it can be used in a lot of situations. However, it really shines in investigative-type workflows, where you may not know exactly what you're looking for and need to explore the data and analyze relationships to gain insight into your problem. Lumify is perfectly suited for:

* Intelligence Analysts
* Detectives
* Fraud Investigators
* Lenders
* Lawyers
* Researchers

## Who develops and supports Lumify?

At it's core, Lumify is an integration of popular open source, big data technologies like Hadoop and Accumulo, overlaid with a graph datastore and web-based analysis and visualization interface. A small team from [Altamira Technologies](http://altamiracorp.com) oversees the development, but contributions from all are welcome and encouraged. 

## Why choose Lumify?

The challenge of making sense of the ever-increasing volumes of data isn't going away any time soon. The ability to effectively deal with this data-deluge will be a key differentiator for businesses moving forward. Eric Schmidt, executive chairman of Google, was recently quoted as saying that "the biggest disrupter (of 2014) that we're sure about is the arrival of big data and machine intelligence everywhere". Lumify can serve as that platform. It is

* **Free** - Lumify is completely free. There are no licensing or usage fees whatsoever for the software.
* **Permissively Licensed** - All Lumify code is licensed under the [Apache 2 license](http://www.apache.org/licenses/LICENSE-2.0.html), giving you the freedom to use it commercially, modify, and redistribute.
* **Open Source** - All Lumify source code is open and available. You can go [see it right now](https://github.com/altamiracorp/lumify). With that openness comes the ability to change and adapt it to your needs.
* **Scalable** - We didn't attempt to reinvent the wheel with Lumify. It's built on proven, scalable, open source big data technologies including [Apache Hadoop](http://hadoop.apache.org/) and [Apache Accumulo](http://accumulo.apache.org/).
* **Supported** - [Altamira](http://altamiracorp.com) is committed to building an open source big data analysis and visualization platform for its customers and the wider open source community. We've dedicated a full-time development team to making this a success. In the near future we'll be announcing some professional support options for customers looking for help with Lumify.

## How can you get started with Lumify?

We've just released Lumify into the wild and it's still a young project. We're looking for people interested in kicking the tires, providing feedback, and helping to improve the project. Here are some ways to get started.

1. **Watch the videos** - Check out the videos on the [Lumify home page](http://lumify.io) to get a feel for some of Lumify's features.
1. **Try the VM** - We've built a virtual machine for you to play with that has all the Lumify components pre-installed. Learn more about it [here](https://github.com/altamiracorp/lumify/blob/master/docs/PREBUILT_VM.md)
2. **Follow the example** - There's an [example Twitter integration](https://github.com/altamiracorp/lumify-twitter) you can look at to see how you can get your own data into Lumify.
3. **Check out the source** - The Lumify source code is available [on Github](https://github.com/altamiracorp/lumify) right now.
