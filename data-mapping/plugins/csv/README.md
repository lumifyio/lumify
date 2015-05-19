# Structured Data Mapping in Lumify
---

Lumify provides an interface, `io.lumify.mapping.DocumentMapping`, for ingesting structured data such as CSV, JSON and XML files.  These files are processed by the enterprise Storm topology and the `DocumentMapping` is applied to generate Lumify entities and relationships.

The following data types are currently supported by Lumify structured data ingest:

*	[Comma Separated Value (CSV)](#csvMapping)


## [Mapping Format](id:mappingFormat)
---

Structured data mappings are provided as JSON documents and should be stored in a `tar` archive with the file(s) they apply to.  They must be named with the extension `.mapping.json` so Lumify can identify the mapping file within the archive.

### Common `DocumentMapping` Properties

*	**`type`** _String **(required)**_

	The type of structured data being mapped.  Legal values are:
	
	*	`csv`


## [Mapping Comma Separated Value Files](id:csvMapping)
---

`type`: `csv`

The reference implementation of the `DocumentMapping` interface is the `io.lumify.mapping.csv.CsvDocumentMapping`.  It supports extraction of entities, properties and relationships from a columnar data file.  Each row of the data file is processed individually and the mapping is applied to extract and transform entities and their properties from the columns.  Once all entities have been created, the relationships between them are established.  Relationships are only created if both the source and target entities were successfully extracted.

### `CsvDocumentMapping` Properties

*	`skipRows` _Integer (optional)_

	The number of rows in the input file to skip before extracting data.
	
	**Default: `0`**
	
*	**`entities`** _Object.\<String, [EntityMapping](#entityMapping)\> **(required)**_

	A mapping of entity keys to mapping definitions for each entity found in a row of the CSV. The keys
	of this map are used to configure the relationship mappings.
	
*	`relationships` _Array.<[RelationshipMapping](#relationshipMapping)> (optional)_

	An array of mappings defining the relationships between the entities extracted from each row.
	
#### Example

##### _Sample Data_

	Name, Zip Code, Birth Date, Unused
	John Smith, 20147, 10/30/1977, blah
	Jane Smith, 20147, 10/15/1983, baz
	Bob Jones, 20176, 5/5/1977, blech
	
##### _Mapping_

	{
		"type": "csv",
		"skipRows": 1,
		"entities": {
			"person": {
				"signColumn": { "index": 0 },
				"conceptURI": "person",
				"properties": {
					"birthDate": {
						"index": 2,
						"xform": {
							"dataType": "date",
							"format": "MM/dd/yyyy"
						}
					}
				}
			},
			"zip": {
				"signColumn": { "index": 1 },
				"conceptURI": "location",
				"useExisting": true
			}
		},
		"relationships": [
			{
				"label": "personLivesAtLocation",
				"source": "person",
				"target": "zip"
			}
		]
	}
		
## [Entity Mapping](id:entityMapping)
---

Lumify can extract one or more entities from a CSV file, optionally setting properties on those entities from the provided data as well.  Each entity is mapped to a configured key so it can be identified for relationship creation.  Entities must be mapped to a known ontological concept.  The IRI for this concept can be specified in one of two ways, a known IRI for all entities in the document or derived from one or more columns found in each row.

### Entity Mapping Properties

*	`conceptIRIType` _String (optional)_

	The method of determining the concept URI.  The supported values are:
	
	*	`constant` _(default)_
	
		The concept URI is provided as a known _String_ value.  When using this method of concept URI resoltuion, the
		following property is also required.
		
		*	**`conceptIRI`** _String **(required)**_
		
			The constant concept URI for the mapped entity.
		
	*	`columnLookup`
	
		The concept URI is provided as a column value mapping, derived for each entity during ingest.  When using
		this method of concept URI resolution, the following property is also required.
		
		*	**`concept`** _[ColumnMapping](#columnMapping) **(required)**_
		
			The mapping definition used to resolve the concept URI for the mapped entity during ingest.
			
*	`properties` _Object.\<String, [ColumnMapping](#columnMapping)\> (optional)_

	A map of property keys to the mappings that will be evaluated to determine the property value for each entity.
	
*	[`required` _Boolean (optional)_](id:entityRequired)

	If `true`, Lumify will only add entities and relationships from the current row if this entity can be resolved.
	Required entities must have a valid sign and values for all required properties.  **Default: `false`**

	
## [Column Mapping](id:columnMapping)
---

Column mappings allow you to extract the values from one or more columns in a row, optionally converting them to various data types.

### Column Mapping Properties

*	`type` _String (optional)_`

	The type of column mapping.  The allowed values are:
	
	*	[`single`](#singleColumn) **(default)**

		Uses the value found in a single column.
		
	*	[`constant`](#constantColumn)
	
		Specifies the value of a "pseudo-column" that is not found in the incoming data.
		
	*	[`formattedMultiColumn`](#formattedColumn)
	
		This column applies a Java format string to the values resolved from any number of specified columns.
		
	*	[`geocircle`](#geocircleColumn)
	
		This column resolves a GeoCircle from the columns of the current row.
		
	*	[`geopoint`](#geopointColumn)
	
		This column resolves a GeoPoint from the columns of the current row.
		
	*	[`mappedMultiColumn`](#mappedColumn)
	
		This column generates a key from a number of provided columns and uses that key to lookup the resolved
		value from a provided mapping.
		
	*	[`required`](#requiredColumn)
	
		Used to wrap another Column Mapping to indicate it is required.  This is typically used to
		mark entity properties as required and, if no value is provided for a required property, the
		entity will not be resolved for that row.  See [`required`](#entityRequired) Entity Mappings
		for more details.
		
	*	[`fallback`](#fallbackColumn)
	
		Fallback column mappings attempt to resolve a property value from one column, falling back
		to a second column if no value is found in the first.

		
### [Single Column Mapping](id:singleColumn)

**`"type": "single"`**

This column mapping evaluates to the value found in the specified column, optionally applying a data tranformation to
convert it to a different type.  If no data transformation is provided, the column value will be a `String`.

#### Properties

*	**`index`** _Integer **(required)**_

	The 0-based index of the target column.
	
*	`xform` _[DataTransformation](#dataXform) (optional)_

	A data transformation that will be applied to the `String` value of the column.  If not provided, the `String`
	found in the specified column will be returned.
	

### [Constant Column Mapping](id:constantColumn)

**`"type": "constant"`**

This mapping always evaluates to the provided value, regardless of the values found in the CSV.

#### Properties

*	**`value`** _* **(required)**_

	The value returned by this column.  It may be of any simple type (e.g. String, Number, Boolean, etc.).
	

### [Formatted Column Mapping](id:formattedColumn)

**`"type": "formattedMultiColumn"`**

This mapping formats the values resolved from a set of provided columns using a Java-style format string, optionally
applying a data transformer to the formatted value to convert it to a different type.

#### Properties

*	**`columns`** _Array.<[ColumnMapping](#columnMapping)> **(required)**_

	The column mappings that resolve to the values provided to the
	[`String.format()`](http://docs.oracle.com/javase/7/docs/api/java/lang/String.html#format(java.lang.String,%20java.lang.Object...))
	method as the arguments for the specified format string.  Values are provided to `String.format()` in the order
	they appear in this array.
	
*	**`format`** _String **(required)**_

	The Java-style format string used to create the resolved value for this column.
	
*	`xform` _[DataTransformation](#dataXform) (optional)_

	A data transformation that will be applied to the formatted value.  If not provided,
	the formatted value will be returned as a `String`.


### [GeoCircle Column Mapping](id:geocircleColumn)

**`"type": "geocircle"`**

This mapping resolves a `GeoCircle` property value from multiple input columns.  If any of the
provided columns do not resolve to a legal value, this mapping will return `null`.

#### Properties

*	**`latitudeColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	A column mapping that resolves to a `Double` value indicating the latitude of the center-point
	of this circle.

*	**`longitudeColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	A column mapping that resolves to a `Double` value indicating the longitude of the center-point
	of this circle.

*	**`radiusColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	A column mapping that resolves to a `Double` value indicating the radius of this circle.
	

### [GeoPoint Column Mapping](id:geopointColumn)

**`"type": "geopoint"`**

This mapping resolves a `GeoPoint` property value from multiple input columns.  If any of the
provided columns do not resolve to a legal value, this mapping will return `null`.

#### Properties

*	**`latitudeColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	A column mapping that resolves to a `Double` value indicating the latitude of this point.

*	**`longitudeColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	A column mapping that resolves to a `Double` value indicating the longitude of this point.

*	`altitudeColumn` _[ColumnMapping](#columnMapping) (optional)_

	A column mapping that resolves to a `Double` value indicating the altitude of this point.  If
	this column is not provided, the resulting point will not contain an altitude value.
	

### [Mapped Column Mapping](id:mappedColumn)

**`"type": "mappedMultiColumn"`**

This mapping joins the `String` values of one or more columns to create a key for mapping to
the desired value.  If a mapping for the full key is not found, columns will be removed from
the end of the key until either a match is discovered or all columns are exhausted. If no match
is found, `null` will be returned. A default value can be configured by providing a value for
the empty key (`""`).  Keys are formed by joining the values of the specified columns with a
separator character.

#### Properties

*	**`keyColumns`** _Array.<[ColumnMapping](#columnMapping)> **(required)**_

	The `String` valued column mappings that will be resolved to create the lookup key.

*	**`valueMap`** _Object.<String, String> **(required)**_

	A map of joined lookup keys to the value that should be resolved for that key. A default
	value may be specified by providing a value for the empty key, `""`.  If no default is
	specified, `null` will be returned when no match is found.

*	`separator` _String (optional)_

	The separator that will be included between each portion of the lookup key.
	**Default: `:`**


### [Required Column Mapping](id:requiredColumn)

**`"type": "required"`**

This mapping wraps another column mapping, generating an exception if the value returned by the underlying
mapping is `null`.

#### Properties

*	**`column`** _[ColumnMapping](#columnMapping) **(required)**_

	The required column mapping.
	
### [Fallback Column Mapping](id:fallbackColumn)

**`"type": "fallback"`**

This mapping examines the contents of a primary column mapping, using a secondary mapping if
the primary value is `null` or meets other configured criteria.

#### Properties

*	**`primaryColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	The mapping that resolves to the primary value.  If this value is `null` or meets
	the criteria specified by `fallbackIf`, the `fallbackColumn` will be used as the
	value of this mapping.
	
*	**`fallbackColumn`** _[ColumnMapping](#columnMapping) **(required)**_

	The mapping that will be used to resolve this column if the primary column
	triggers the fallback condition.
	
*	`fallbackIf` _[MappingPredicate](#mappingPredicate) (optional)_

	The conditions, in addition to a simple `null` check, under which the fallback column
	will be used instead of the primary column.
	


## [Data Transformations](id:dataXform)
---

The following data transformations may be applied to [Column Mappings](#columnMapping) that support them.  Each
transform converts an input `String` to another type of value.

#### Data Transformation Properties

*	**`dataType`** _String **(required)**_

	The data transformation that will be applied to the input string.  The following data types are supported:
	
	*	[`bigDecimal`](#simpleXform) - converts values to a Java `BigDecimal`
	
	*	[`bigInteger`](#simpleXform) - converts values to a Java `BigInteger`
	
	*	[`boolean`](#simpleXform) - converts values to a `Boolean`
	
	*	[`date`](#dateXform) - converts values to a `Long` representing milliseconds from the Java epoch
	
	*	[`double`](#simpleXform) - converts values to a `Double`
	
	*	[`integer`](#simpleXform) - converts values to an `Integer`
	
	*	[`long`](#simpleXform) - converts values to a `Long`
	
	*	[`mappedBoolean`](#mappedBooleanXform) - maps specific input values to `Boolean` values
	
	*	[`mappedString`](#mappedStringXform) - maps specific input values to `String` values
	
	*	[`replace`](#replaceXform) - replaces input values matching certain criteria with a configured value
	
	*	[`string`](#simpleXform) - identify transformation for non-empty input values
	

### [Simple Transformations](id:simpleXform)

*	**`"dataType": "bigDecimal"`**
*	**`"dataType": "bigInteger"`**
*	**`"dataType": "boolean"`**
*	**`"dataType": "double"`**
*	**`"dataType": "integer"`**
*	**`"dataType": "long"`**
*	**`"dataType": "string"`**

Simple transformations require no additional parameters and attempt to convert the input value to the target data type.
If the input string is `null`, empty or whitespace only or the value cannot be converted to the target type, the
transformation returns `null`.


### [Mapped Boolean Transformation](id:mappedBooleanXform)

**`"dataType": "mappedBoolean"`**

This transformation performs a lookup on the input value, returning the `Boolean` value specified in the provided
map or the default value if no matching key is found.

#### Properties

*	**`valueMap`** _Object.<String, Boolean> **(required)**_

	The map of input strings to Boolean values.
	
*	`defaultValue` _Boolean (optional)_

	The default value to return if no matching key is found.  **Default: `null`**
	

### [Mapped String Transformation](id:mappedStringXform)

**`"dataType": "mappedString"`**

This transformation performs a lookup on the input value, returning the `String` value specified in the provided
map or the default value if no matching key is found.

#### Properties

*	**`valueMap`** _Object.<String, String> **(required)**_

	The map of input strings to target `String` values.
	
*	`defaultValue` _String (optional)_

	The default value to return if no matching key is found.  **Default: `null`**
	

### [Replace Transformation](id:replaceXform)

**`"dataType": "replace"`**

This transformation first applies a configured [Data Transformation](#dataXform) to the input String, then tests
the resulting value against a set of provided criteria.  This value is used as the transformed value unless it
matches the configured replacement criteria, in which case the replacement value is returned instead.

#### Properties

*	**`xform`** _[DataTransformation](#dataXform) **(required)**_

	The data transformation that will be applied to the input value before matching it against the replacement
	criteria.
	
*	**`targetCriteria`** _[MappingPredicate](#mappingPredicate) **(required)**_

	Values matching this criteria will be replaced with the configured `replacementValue`.
	
*	**`replacementValue`** _* **(required)**_

	The value that will be substituted for input values meeting the target criteria.  This must be the same
	type of value as that returned by the configured data transformation.
	

## [Mapping Predicates](id:mappingPredicate)
---

A predicate evaluates an input value, returning `true` or `false` to indicate whether the value meets the
criteria described by the predicate.

#### Mapping Predicate Properties

*	**`op`** _String **(required)**_

	The operation performed by this predicate.  Lumify supports the following operations:
	
	*	[`and`](#andPredicate) - matches an input value if all configured predicates match the input value
	
	*	[`emptyStr`](#emptyStringPredicate) - matches an input string that is empty or whitespace-only
	
	*	[`eq`](#equalsPredicate) - matches an input value equal to another value
	
	*	[`not`](#notPredicate) - negates the result of a match against a target predicate
	
	*	[`null`](#nullPredicate) - matches `null` input values
	
	*	[`or`](#orPredicate) - matches an input value if any configured predicates match the input value
	
	*	[`stringEq`](#stringEqualsPredicate) - matches an input value whose trimmed `.toString()` matches a provided String
	

### [And Predicate](id:andPredicate)

**`"op": "and"`**

This predicate matches an input value if all configured sub-predicates match the input value.

#### Properties

*	**`predicates`** _Array.<[MappingPredicate](#mappingPredicate)> **(required)**_

	The predicates that will be and-ed together.
	

### [Empty String Predicate](id:emptyStringPredicate)

**`"op": "emptyStr"`**

This predicate matches an input String value if it is empty (`""`) or whitespace-only.


### [Equals Predicate](id:equalsPredicate)

**`"op": "eq"`**

This predicate matches if the input value is equal to the target value (via `.equals()`).  The
equals predicate may not be used to match `null` values; use the [Null Predicate](#nullPredicate)
instead.

#### Properties

*	**`value`** _* **(required)**_

	The target value.


### [Not Predicate](id:notPredicate)

**`"op": "not"`**

This predicate negates the match of a target predicate.

#### Properties

*	**`predicate`** _[MappingPredicate](#mappingPredicate) **(required)**_

	The predicate to negate.


### [Null Predicate](id:nullPredicate)

**`"op": "null"`**

This predicate matches if the input value is `null`.


### [Or Predicate](id:orPredicate)

**`"op": "or"`**

This predicate matches an input value if any configured sub-predicates match the input value.

#### Properties

*	**`predicates`** _Array.<[MappingPredicate](#mappingPredicate)> **(required)**_

	The predicates that will be or-ed together.
	
### [String Equals Predicate](#stringEqualsPredicate)

**`"op": "stringEq"`**

This predicate matches values whose trimmed `.toString()` method matches a
target String value. It can operate in case-sensitive or case-insensitive
mode.

#### Properties

*	**`value`** _String **(required)**_

	The string value to match.
	
*	`caseSensitive` _Boolean (optional)_

	`true` to perform case-sensitive matches. **Default: `false`**


## [Relationship Mapping](id:relationshipMapping)
---

Once entities have been extracted from a row in the CSV file, Lumify can establish relationships between them.  Relationships must contain a label
that is defined in the configured Lumify ontology.

### Relationship Mapping Properties

*	**`source`** _String **(required)**_

	The key (from the [CsvDocumentMapping.entities](#csvMapping) map) of the entity that is the source of this relationship.
	
*	**`target`** _String **(required)**_

	The key (from the [CsvDocumentMapping.entities](#csvMapping) map) of the entity that is the target of this relationship.

*	`labelType` _String (optional)_

	The method of determining the relationship label (IRI).  The supported values are:
	
	*	`constant` _(default)_
	
		The relationship label is provided as a known _String_ value.  When using this method of label resoltuion, the
		following property is also required.
		
		*	**`label`** _String **(required)**_
		
			The constant label (IRI) for this relationship.
		
	*	`conceptMapped`
	
		The relationship label is determined by the concept IRIs of the source and/or target entities.  When using
		this method, Lumify will traverse the ontological hierarchy to find a match for the provided source/target
		entities.  For example, if the source entity is a `Terrorist` and you have a relationship defined for `Person`,
		Lumify will match `Terrorist` against `Person` to identify the relationship label.  When using this method of
		label resolution, the following property is also required.
		
		*	**`labelMappings`** _Array.<Object> **(required)**_
		
			An array of label mapping definitions that describe how to resolve relationship labels based on the
			ontological types of the source and/or target entities.  At least one of the `source` or `target`
			concepts must be provided.  If only one is provided, the mapped label will be used for all relationships
			between entities matching the provided concept.  If both are provided, both the concepts of the source
			and target entities must match.  Mapping definitions have the following properties:
			
			*	`source` _String (optional)_
			
				The ontological concept of the source entity for this relationship.
				
			*	`target` _String (optional)_
			
				The ontological concept of the target entity for this relationship.
				
			*	**`label`** _String **(required)**_
			
				The relationship label (IRI) that will be used when the source and/or target entities are
				of the specified ontological types.
			
I