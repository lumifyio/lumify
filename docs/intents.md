# Intents

The ontology defines concepts, relationships, and properties. During data processing Lumify needs to know
 what type of concept, relationship, and property to assign when it finds them. For example if Lumify is scanning a
 document and finds a phone number, Lumify will need to assign a concept to that phone number. This is where
 intents come in.

Intents can be defined in the ontology and overridden in the configuration. To assign an intent you add the
 intent attribute to an OWL element.

    <owl:Class rdf:about="http://lumify.io/dev#phoneNumber">
      <rdfs:label xml:lang="en">Phone Number</rdfs:label>
      <lumify:intent>phoneNumber</lumify:intent>
      ...
    </owl:Class>

To override an intent you can add the following to your configuration.

    ontology.intent.concept.phoneNumber=http://lumify.io/dev#phoneNumber
    ontology.intent.relationship.phoneNumber=http://lumify.io/dev#phoneNumber
    ontology.intent.property.phoneNumber=http://lumify.io/dev#phoneNumber

## Concepts

| audio        | Audio file                                |
| city         | Geographic city                           |
| country      | Geographic country                        |
| csv          | Comma separated file                      |
| document     | Document                                  |
| email        | E-Mail address                            |
| entityImage  | Image assigned to an entity               |
| image        | Image file                                |
| location     | Geographic location                       |
| organization | Organization                              |
| person       | Person                                    |
| phoneNumber  | Phone number                              |
| rdf          | Resource description framework (RDF) file |
| state        | Geographic state                          |
| video        | Video file                                |
| zipCode      | Zip code                                  |

## Relationships

| artifactContainsImageOfEntity | Artifact has image of entity         |
| artifactHasEntity             | Artifact has entity                  |
| entityHasImage                | Entity has image                     |
| hasMedia                      | Thing has media                      |

## Properties

| geoLocation             | geoLocation | Geo-location                                |
| media.clockwiseRotation | integer     | Image clockwise rotation                    |
| media.dateTaken         | date        | Date/time image was taken                   |
| media.deviceMake        | string      | The device make                             |
| media.deviceModel       | string      | The device model                            |
| media.duration          | double      | The length in seconds of the media file     |
| media.fileSize          | long        | The filesize of the media file              |
| media.height            | integer     | The height in pixels of the media           |
| media.imageHeading      | double      | The compass direction the camera was facing |
| media.metadata          | json        | Additional metadata found in the media      |
| media.width             | integer     | The width in pixels of the media            |
| media.yAxisFlipped      | boolean     | Is image Y-axis flipped                     |
| pageCount               | long        | Number of pages in the artifact             |
