

define([], function() {

        
    var // keypaths to vertices objects in ajax responses
        VERTICES_RESPONSE_KEYPATHS = ['vertices', 'data.vertices'],
        
        IGNORE_NO_CONVERTER_FOUND_REGEXS = [
            /^user/,
            /^configuration$/,
            /^workspaces?$/,
            /^workspace\?/,
            /^workspace\/diff/,
            /^workspace\/relationships/,
            /^workspace\/update/,
            /^vertex\/relationships/,
        ],

        // Custom converters for routes that are more complicated than above,
        // call updateCacheWithVertex and append to updated, return
        // true if handled
        JSON_CONVERTERS = [

            function vertexProperties(json, updated) {
                var cache;
                if (!json.sourceVertexId &&_.isString(json.id) && _.isObject(json.properties) && _.keys(json.properties).length) {
                    cache = this.updateCacheWithVertex(json);
                    $.extend(true, json, cache);
                    updated.push(cache);

                    updated = true;
                }
                return updated;
            },

            function vertexRelationships(json, updated) {
                var self = this;

                if (json.relationships) {
                    json.relationships.forEach(function(relationship) {
                        if (relationship.vertex) {
                            var cache = self.updateCacheWithVertex(relationship.vertex);
                            $.extend(true, relationship.vertex, cache);
                            updated.push(cache);
                        }
                    });
                }
            },

            function findPath(json, updated) {
                var self = this;

                if (json.paths) {
                    json.paths.forEach(function(path) {
                        path.forEach(function(vertex) {
                            var cache = self.updateCacheWithVertex(vertex);
                            $.extend(true, vertex, cache);
                            updated.push(cache);
                        });
                    });
                    return true;
                }
            },

            function vertexProperties(json, updated) {
                var cache;
                if (json.vertex && json.vertex.id && json.properties) {
                    cache = this.updateCacheWithVertex(json.vertex, {
                        deletedProperty: json.deletedProperty
                    });
                    updated.push(cache);
                    return true;
                } else if (json.vertex && json.vertex.graphVertexId && json.properties) {
                    cache = this.updateCacheWithVertex({
                        id: json.vertex.graphVertexId,
                        properties: json.properties
                    }, {
                        deletedProperty: json.deletedProperty
                    });
                    updated.push(cache);
                    return true;
                }
            },

            function verticesRoot(json, updated) {
                var self = this;

                if (_.isArray(json) && json.length && json[0].id && json[0].properties) {
                    json.forEach(function(vertex) {
                        var cache = self.updateCacheWithVertex(vertex);
                        cache.properties._refreshedFromServer = true;
                        $.extend(true, vertex, cache);
                        updated.push(cache);
                    });
                    return true;
                }
            }


        ];

    return withCacheUpdatingAjaxFilters;


    function withCacheUpdatingAjaxFilters() {

        this.after('initialize', function() {
            this.setupAjaxPrefilter();
        });

        this.setupAjaxPrefilter = function() {
            var self = this;

            // Attach converter to ajax requests to merge with cachedVertices,
            // sending updateVertices events
            $.ajaxPrefilter(function(options, originalOptions, xhr) {
                var jsonConverter = options.converters['text json'];
                options.converters['text json'] = function(data) {

                    var json = jsonConverter(data);

                    try {
                        var updated = [],
                            converterFound = _.any(JSON_CONVERTERS, function(c) {
                                if (!json) return;

                                var result = c.call(self, json, updated);
                                return result === true;
                            });

                        if (!converterFound && _.keys(json).length) {
                            var keypathFound = false;

                            VERTICES_RESPONSE_KEYPATHS.forEach(function(paths) {
                                var val = json,
                                    components = paths.indexOf('.') === -1 ? [paths] : paths.split('.');

                                while (val && components.length) {
                                    val = val[components.shift()];
                                }

                                if (val && _.isArray(val) && val.length === 0) {
                                    keypathFound = true;
                                }

                                // Found vertices
                                if (val && self.resemblesVertices(val)) {
                                    keypathFound = true;
                                    val.forEach(function(v) {
                                        updated.push( $.extend(true, v, self.updateCacheWithVertex(v)) );
                                    });
                                } else if (!keypathFound) {
                                    keypathFound = true;

                                    // Might be an error if we didn't match and
                                    // getting vertices without updating cache
                                    // and applying patches
                                    if (
                                        !_.some(IGNORE_NO_CONVERTER_FOUND_REGEXS, function(regex) {
                                            return regex.test(options.url);
                                        })
                                    ) {
                                        console.warn('No converter applied for url:', options.url);
                                    }
                                }
                            });
                        }

                        if (updated.length) {
                            _.defer(function() {
                                self.trigger('verticesUpdated', { 
                                    vertices:updated.map(function(v) {
                                        var vertex = $.extend(true, {}, v);
                                        vertex.workspace = {};
                                        return Object.freeze(vertex);
                                    })
                                });
                            });
                        }
                    } catch(e) {
                        console.error('Request failed in prefilter cache phase', e.stack || e.message);
                    }
                    return json;
                };
            });
        };
    }
});
