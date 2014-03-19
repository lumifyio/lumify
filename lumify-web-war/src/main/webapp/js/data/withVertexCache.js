
define([
    'service/vertex',
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

            if (_.isArray(vertex)) {
                var vertices = [], toRequest = [];
                vertex.forEach(function(v) {
                    var cachedVertex = self.vertex(v);
                    if (cachedVertex) {
                        vertices.push(cachedVertex);
                    } else {
                        toRequest.push(v);
                    }
                })
                deferred = $.Deferred();

                if (toRequest.length) {
                    this.vertexService.getMultiple(toRequest)
                        .done(function(requestedVertices) {
                            deferred.resolve(vertices.concat(requestedVertices));
                        })
                } else {
                    deferred.resolve(vertices);
                }

                return deferred;
            } else if (_.isString(vertex) || _.isNumber(vertex)) {
                deferred = this.vertexService.getVertexProperties(vertex);
            } else {
                deferred = this.vertexService.getVertexProperties(vertex.id);
            }

            return deferred.then(function(v) {
                return self.vertex((v && v.id) || vertex.id);
            });
        };

        this.updateCacheWithVertex = function(vertex, options) {
            var id = vertex.id;
            var cache = this.cachedVertices[id] || (this.cachedVertices[id] = { id: id });

            if (options && options.deletedProperty && cache.properties) {
                delete cache.properties[options.deletedProperty]
            }

            if (!cache.properties) cache.properties = {};
            if (!cache.workspace) cache.workspace = {};

            cache.properties = _.isUndefined(vertex.properties) ? cache.properties : vertex.properties;
            cache.workspace = $.extend(true, {}, cache.workspace, vertex.workspace || {});
            
            $.extend(cache, _.pick(vertex, ['_visibility', '_visibilityJson', 'sandboxStatus']));

            if (!cache.properties.source || !cache.properties.source.value) {
                if (cache.properties._source && cache.properties._source.value) {
                    cache.properties.source = cache.properties._source;
                }
            }

            cache.detectedObjects = vertex.detectedObjects;

            if (this.workspaceVertices[id]) {
                this.workspaceVertices[id] = cache.workspace;
            }

            cache.concept = this.cachedConcepts.byId[cache.properties['http://lumify.io#conceptType'].value || cache.properties['http://lumify.io#conceptType']]
            if (cache.concept) {
                setPreviewsForVertex(cache, this.workspaceId);
            } else console.error('Unable to attach concept to vertex', cache.properties['http://lumify.io#conceptType']);

            cache.resolvedSource = this.resolvedSourceForProperties(cache.properties);
            return cache;
        };


        function setPreviewsForVertex(vertex, currentWorkspace) {
            var workspaceParameter = '?workspaceId=' + encodeURIComponent(currentWorkspace),
                vId = encodeURIComponent(vertex.id),
                artifactUrl = _.template("/artifact/" + vId + "/{ type }" + workspaceParameter);

            vertex.imageSrcIsFromConcept = false;

            if (vertex.properties._glyphIcon) {
                vertex.imageSrc = vertex.properties._glyphIcon.value + workspaceParameter;
            } else {
                switch (vertex.concept.displayType) {

                    case 'image': 
                        vertex.imageSrc = artifactUrl({ type: 'thumbnail' });
                        vertex.imageRawSrc = artifactUrl({ type: 'raw' });
                        break;

                    case 'video': 
                        vertex.imageSrc = artifactUrl({ type: 'poster-frame' });
                        vertex.imageRawSrc = artifactUrl({ type: 'raw' });
                        vertex.imageFramesSrc = artifactUrl({ type: 'video-preview' });
                        break;

                    default:
                        vertex.imageSrc = vertex.concept.glyphIconHref;
                        vertex.imageRawSrc = artifactUrl({ type: 'raw' });
                        vertex.imageSrcIsFromConcept = true;
                }
            }
        }

        this.resolvedSourceForProperties = function(p) {
            var source = p.source && p.source.value,
                author = p.author && p.author.value;
            
            return source ? 
                author ? ([source,author].join(' / ')) : source : 
                author ? author : '';
        }
    }
});
