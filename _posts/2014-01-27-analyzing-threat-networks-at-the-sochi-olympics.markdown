---
layout: post
title: "Analyzing Threat Networks at the Sochi Olympics"
excerpt: One of the demo scenarios we've been developing to showcase the capabilities of Lumify is based on the various terrorist networks threatening the upcoming Winter Olympic Games in Sochi, Russia. See how Lumify can help stop potential attacks.
---
One of the demo scenarios we've been developing to showcase the capabilities of **Lumify** is based on the various terrorist networks threatening the upcoming **[Winter Olympic Games](http://en.wikipedia.org/wiki/2014_Winter_Olympics)** in Sochi, Russia.

![Olympic Rings]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Olympic_rings.png "Olympic Rings - license: public domain - source: http://commons.wikimedia.org/wiki/File:Olympic_rings_without_rims.svg")

## Background

**[Sochi](http://en.wikipedia.org/wiki/Sochi)** is a resort town in southwest Russia, on the coast of the Black Sea. Located within the **[North Caucasus](http://en.wikipedia.org/wiki/North_Caucasus)** region, Sochi is only a few hundred miles from the restive Russian republics of **Chechnya**, **Dagestan**, and **Ingushetia**. This area was the main focal point of the **[Chechen-Russian conflict](http://en.wikipedia.org/wiki/Chechen-Russian_conflict)**, as well as the ongoing low-level insurgency that followed. Several terrorist organizations are active in the region, having claimed responsibility for attacks throughout Russia. Additionally, the **[Tsarnaev brothers](http://en.wikipedia.org/wiki/Dzhokhar_and_Tamerlan_Tsarnaev)**, suspected of bombing the 2013 Boston Marathon, are ethnic Chechens who spent time in Dagestan.

![North Caucasus map]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Sochi_in_North_Caucasus.png "North Caucasus map - license: CC-BY-SA - derived from: http://commons.wikimedia.org/wiki/File:Chechnya_and_Caucasus.png")

There are growing concerns among security experts and the Russian government that Chechen terrorists may intend to launch attacks against the Winter Olympics in February. In recent weeks, multiple groups have **[publicly threatened to strike Sochi](http://www.voanews.com/content/reu-militant-islamist-video-threatens-winter-olympics/1833742.html)**. In this demo scenario, a **hypothetical analyst** has been tasked with **investigating connections** between terrorist organizations and individuals active in the region.

## Collecting Data

The data we obtained for this scenario was either licensed explicitly for redistribution (including for commercial purposes) through [Creative Commons](http://creativecommons.org/), or it was in the public domain by law. This allows us to use the data freely in our demo, redistribute it via our website, and include it as "sample data" along with the Lumify source code in our [GitHub repository](https://github.com/altamiracorp/lumify). We collected 91 online news articles from [Wikinews](http://www.wikinews.org/), [Global Voices](http://globalvoicesonline.org/), and [Voice of America](http://www.voanews.com/) &ndash; all of which contained various keywords related to Chechen terrorist organizations. We also gathered a few images from [Wikimedia Commons](http://commons.wikimedia.org/wiki/Main_Page) and a CC-licensed video from [YouTube](https://www.youtube.com/creativecommons).

## Ingesting Data

These files were then uploaded into Lumify via the enterprise ingestion pipeline currently under development. The news articles were processed by **[Apache OpenNLP NameFinder](http://opennlp.apache.org/)**, a [named entity recognition](http://en.wikipedia.org/wiki/Named-entity_recognition) tool that automatically extracts names of person, organization, and location entities from plain text, enabling Lumify to highlight those extracted names when displaying the original document. We also ran the articles through **[CLAVIN](http://clavin.io/)**, an open source [geoparser](http://en.wikipedia.org/wiki/Geoparsing) that resolves extracted location names with the [actual geospatial entities](http://www.geonames.org/) to which they refer. The image files were analyzed by a facial detection tool, and we applied [OCR](http://en.wikipedia.org/wiki/Optical_character_recognition) software to the video extracting any text shown in individual frames.

At the end of the ingestion process, we see that this instance of Lumify currently contains 92 documents (91 collected from real-world sources, plus one contrived news article written specifically for this demo), as well as 127 location entities automatically resolved by CLAVIN.

![Data loaded into Lumify]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Data_loaded_into_Lumify.png "Data loaded into Lumify")

## Investigating Leads

From this starting point, our hypothetical analyst begins by entering "**[Doku Umarov](http://en.wikipedia.org/wiki/Dokka_Umarov)**" &ndash; the name of a well-known **Chechen terrorist leader** &ndash; into the Lumify Search Box. This search returns 9 results &ndash; that is, 9 documents which contain "Doku Umarov." The analyst clicks on one of the results to open it in the **Detail Pane** where the text of the source document is then displayed. Within that document are highlighted names of persons, organizations, and locations extracted by OpenNLP (indicated by a dotted underline), as well as resolved location entities identified by CLAVIN (in bold and underlined with purple).

Among the extracted names highlighted in the document is "Doku Umarov," the subject of the initial search in this scenario. The analyst decides to **resolve** this name as a new **Person** entity, and then drags it onto the **Graph**. A brief Internet search turns up a photo of Umarov on the [RewardsForJustice.net](http://www.rewardsforjustice.net/umarov) website operated by the U.S. State Department. The user quickly downloads the photo, and uploads it into Lumify as Umarov's profile image.

![Doku Umarov in Lumify]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Doku_Umarov_in_Lumify.png "Doku Umarov in Lumify")

## Building Connections

Our hypothetical user continues to review the articles returned by the search, resolving additional entities of interest, adding them to the Graph, and **creating relationships** between them. Some discoveries lead to further search queries, and the Graph grows larger and more complex as the analyst adds new entities and connections between them.

Eventually, the analyst reaches a point where he or she is satisfied that the Graph created in Lumify adequately represents all of the available information that is relevant to the investigation. By selecting all of the items on the Graph, the Detail Pane changes to a **Histogram View** showing summary statistics about the distribution of values across variables for items in the Graph. Each entry in the Histogram View is **clickable** and results in the corresponding items in the Graph being selected.

![Graph with Histogram View in Lumify]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Graph_with_Histogram_View_in_Lumify.png "Graph with Histogram View in Lumify")

## Mapping it Out

Since these documents have been analyzed by CLAVIN, they are all tagged with **geospatial** data for any resolved location names extracted from the text. Some of these locations have subsequently been linked to other entities like persons, organizations, and events by creating relationships between them. At any time, the analyst can quickly switch to the **Map View** and zoom into the North Caucasus region to **visualize** the geographic context of the information stored in the Graph.

![Map View of North Caucasus in Lumify]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Map_View_of_North_Caucasus_in_Lumify.png "Map View of North Caucasus in Lumify")

## The Missing Link

At this stage, our hypothetical analyst could write a report describing the network of known terrorists and terrorist organizations operating in the region around Sochi. Such a report would be informative and useful, but it would not provide any new insights for decision makers or security personnel.

_**Suddenly**_, a news article is published that provides a crucial piece of information. Russian security forces announced that a suspected militant had been killed in an overnight shootout. The individual was associated with **[Vilayat Dagestan](http://en.wikipedia.org/wiki/Shariat_Jamaat)**, a jihadist group supported by Doku Umarov and formerly known as **Shariat Jamaat**. This group had recently threatened to attack the Sochi Olympics. Most significantly, among the evidence recovered from the scene of the shootout was a martyrdom video recorded by **Ruzanna Ibragimova**, the so-called "black widow" believed to be hiding somewhere in Sochi in preparation for a suicide bombing.

_(**Please note**: this last bit of information &ndash; that a martyrdom video by Ibragimova was recovered by Russian police &ndash; is entirely fictional and was created for the purposes of this demo scenario.)_

Before this critical piece of the puzzle was obtained, the only known connection between Ibragimova and mastermind Doku Umarov was that they both resided in Dagestan &ndash; far too tenuous a link to imply any direct relationship. However, now that Ibragimova has been definitively linked to Vilayat Dagestan, the analyst can use the "**Find Path**" feature in Lumify to **discover** that this "black widow" is connected to Umarov by way of their shared association with Vilayat Dagestan.

![Find Path in Lumify]({{site.url}}/assets/2014-01-27-analyzing-threat-networks-at-the-sochi-olympics/Find_Path_in_Lumify.png "Find Path in Lumify")

## Pulling it all Together

The analyst can **share** this **Lumify Workspace** with his or her colleagues, enabling them to investigate the entities, trace every connection, and read through the original source documents. They can also modify and expand the existing Graph with new information as it becomes available.

Finally, our hypothetical user decides to **publish** an analysis describing the connection between Ibragimova and Umarov, highlighting the seriousness of the "black widow" suicide bomber threat at the upcoming Winter Olympics in Sochi. This analysis can be **substantiated** by including images of the Graph exported from Lumify, as well as references to documents and other items supporting the claims made in the final analytic product.

Throughout this scenario, the analyst used Lumify as an intelligent **[evidence board](http://www.amctv.com/shows/breaking-bad/dea-evidence-board)** to help "connect the dots" within the available data. The link between Ibragimova and Umarov was _not evident in any one source document_ &ndash; only by putting together multiple pieces of the puzzle could the connection be found. Rather than having to keep track of written notes or some kind of whiteboard drawing, the analyst recorded and maintained this knowledge in Lumify, and then used the **powerful** features of the platform to **discover critical new insights**. Lumify enabled the analyst to quickly search for new information, analyze relationships between entities, explore the geographic context on a map, and instantly share this rich knowledge with collaborators around the world.
