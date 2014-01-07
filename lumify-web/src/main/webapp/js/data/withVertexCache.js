
define([
    'service/vertex'
], function(VertexService) {

    return withVertexCache;

    function withVertexCache() {

        this.cachedVertices = {};
        this.workspaceVertices = {};
        if (!this.vertexService) {
            this.vertexService = new VertexService();
        }


        this.resemblesVertices = function(val) {
            return val && val.length && val[0].id && val[0].properties;
        };

        this.verticesInWorkspace = function() {
            return _.values(_.pick(this.cachedVertices, _.keys(this.workspaceVertices)));
        };

        this.copy = function(obj) {
            return JSON.parse(JSON.stringify(obj));
        };

        this.workspaceOnlyVertexCopy = function(vertex) {
            return {
                id: vertex.id,
                workspace: this.copy(vertex.workspace || this.workspaceVertices[vertex.id] || {})
            };
        };

        this.workspaceOnlyVertex = function(id) {
            return {
                id: id,
                workspace: this.workspaceVertices[id] || {}
            };
        };

        this.inWorkspace = function(vertex) {
            return !!this.workspaceVertices[_.isString(vertex) ? vertex : vertex.id];
        };

        this.vertex = function(id) {
            return this.cachedVertices['' + id];
        };

        this.vertices = function(ids) {
            if (ids.toArray) {
                ids = ids.toArray();
            }

            ids = ids.map(function(id) {
                return id.id ? id.id : '' + id;
            });

            return _.values(_.pick(this.cachedVertices, ids));
        };

        this.refresh = function(vertex) {
            var self = this,
                deferred = null;

            if (_.isString(vertex) || _.isNumber(vertex)) {
                deferred = this.vertexService.getVertexProperties(vertex);
            } else if (vertex.properties._type === 'artifact' && vertex.properties._rowKey) { 
                deferred = $.when(
                    this.vertexService.getArtifactById(vertex.properties._rowKey),
                    this.vertexService.getVertexProperties(vertex.id)
                );
            } else {
                deferred = this.vertexService.getVertexProperties(vertex.id);
            }

            return deferred.then(function(v) {
                return self.vertex((v && v.id) || vertex.id);
            });
        };

        this.updateCacheWithArtifact = function(artifact, subType) {
            // Determine differences between artifact search and artifact get requests
            var id = artifact.graphVertexId || artifact.Generic_Metadata.graphVertexId,
                rowKey = artifact._rowKey || artifact.key.value,
                content = artifact.Generic_Metadata;

            // Fix characters
            if (rowKey) {
                artifact._rowKey = encodeURIComponent((rowKey || '').replace(/\\[x](1f)/ig, '\u001f'));
            }
            if (content) {
                // Format Html
                artifact.contentHtml = (content.highlightedText || content.text || '').replace(/[\n]+/g, "<br><br>\n");
            }
            if (artifact.Generic_Metadata.videoTranscript){ 
                artifact.videoTranscript = JSON.parse(artifact.Generic_Metadata.videoTranscript);
                artifact.videoDuration = artifact.Generic_Metadata.videoDuration;
            }

            var cache = this.updateCacheWithVertex({
                id: id,
                properties: {
                    _rowKey: rowKey,
                    _type: 'artifact',
                    _subType: subType || artifact.type,
                    source: artifact.source
                }
            });

            // Properties from artifacts that don't override vertex
            if (!cache.properties.title) {
                cache.properties.title = artifact.subject || artifact.Generic_Metadata.subject || 'No Title Available';
            }
            if (!cache.properties.geoLocation) {
                if (artifact.Dynamic_Metadata && 
                    artifact.Dynamic_Metadata['atc:geoLocationTitle'] &&
                    artifact.Dynamic_Metadata.latitude &&
                    artifact.Dynamic_Metadata.longitude) {

                    cache.properties.geoLocation = {
                        latitude: artifact.Dynamic_Metadata.latitude,
                        longitude: artifact.Dynamic_Metadata.longitude,
                        title: artifact.Dynamic_Metadata['atc:geoLocationTitle']
                    };
                }
            }

            $.extend(true, cache.artifact || (cache.artifact = {}), artifact);
            return cache;
        };

        this.updateCacheWithVertex = function(vertex, options) {
            var id = vertex.id;
            var cache = this.cachedVertices[id] || (this.cachedVertices[id] = { id: id });

            if (options && options.deletedProperty && cache.properties) {
                delete cache.properties[options.deletedProperty]
            }

            $.extend(true, cache.properties || (cache.properties = {}), vertex.properties);
            $.extend(true, cache.workspace ||  (cache.workspace = {}),  vertex.workspace || {});

            if (this.workspaceVertices[id]) {
                this.workspaceVertices[id] = cache.workspace;
            }

            if (_.isString(cache.properties.geoLocation)) {
                var m = cache.properties.geoLocation.match(/point\[(.*?),(.*?)\]/);
                if (m) {
                    var latitude = m[1];
                    var longitude = m[2];
                    cache.properties.geoLocation = {
                        latitude: latitude,
                        longitude: longitude,
                        title: cache.properties._geoLocationDescription
                    };
                }
            } 

            if (cache.properties.latitude || cache.properties.longitude) {
                $.extend(cache.properties.geoLocation || (cache.properties.geoLocation = {}), {
                    latitude: cache.properties.latitude,
                    longitude: cache.properties.longitude,
                    title: cache.properties._geoLocationDescription
                });
                delete cache.properties.latitude;
                delete cache.properties.longitude;
                delete cache.properties._geoLocationDescription;
            }

            return cache;
        };

    }
});
