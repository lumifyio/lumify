

define([
    'data',
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./appFullscreenDetails',
    'tpl!./appFullscreenDetailsError',
    'service/vertex',
    'detail/detail',
    'util/jquery.removePrefixedClasses'
], function(appData, defineComponent, registry, template, errorTemplate, VertexService, Detail) {
    'use strict';

    return defineComponent(FullscreenDetails);

    function filterEntity(v) {
        return !filterArtifacts(v);
    }
    function filterArtifacts(v) {
        return (/^(document|image|video)$/).test(v.concept.displayType);
    }

    function FullscreenDetails() {
        this.vertexService = new VertexService();

        this.defaultAttrs({
            detailSelector: '.detail-pane .content',
            closeSelector: '.close-detail-pane',
            noResultsSelector: '.no-results'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.updateTitle();

            this._windowIsHidden = false;
            this.on(document, 'window-visibility-change', this.onVisibilityChange);
            this.on('click', { closeSelector: this.onClose });
            this.vertices = [];

            this.fullscreenIdentifier = Math.floor((1 + Math.random()) * 0xFFFFFF).toString(16).substring(1);

            this.$node.addClass('fullscreen-details');

            var self = this;
            appData.cachedConceptsDeferred.done(function() {
                self.vertexService
                    .getMultiple(self.attr.graphVertexIds)
                    .done(self.handleVerticesLoaded.bind(self));
            });
        });

        this.onClose = function(event) {
            event.preventDefault();

            var self = this,
                pane = $(event.target).closest('.detail-pane'),
                node = pane.find('.content'),
                instanceInfos = registry.findInstanceInfoByNode(node[0]);

            if (instanceInfos.length) {
                instanceInfos.forEach(function(info) {
                    self.vertices = _.reject(self.vertices, function(v) {
                        return v.id === info.instance.attr.loadGraphVertexData.id;
                    });
                    info.instance.teardown();
                });
            }
            pane.remove();

            this.updateLocationHash();
            this.updateLayout();
            this.updateTitle();
        };

        this.updateLocationHash = function() {
            location.hash = '#v=' + _.pluck(this.vertices, 'id').sort().join(',');
        };

        this.updateLayout = function() {

            this.$node.toggleClass('onlyone', this.vertices.length === 1);

            var verts = this.vertices.length,
                entities = _.filter(this.vertices, filterEntity).length,
                artifacts = _.filter(this.vertices, filterArtifacts).length;
            
            this.$node
                .removePrefixedClasses('vertices- entities- has- entity-cols-')
                .addClass([
                    this.vertices.length <= 4 ? 'vertices-' + this.vertices.length : 'vertices-many',
                    'entities-' + entities,
                    'entity-cols-' + _.find([4,3,2,1], function(i) { return entities % i === 0; }),
                    entities ? 'has-entities' : '',
                    'artifacts-' + artifacts,
                    artifacts ? 'has-artifacts' : ''
                ].join(' '));
        };

        this.updateTitle = function() {
            document.title = this.titleForVertices();
        };

        this.handleNoVertices = function() {
            document.title = "No vertices found";
            this.select('noResultsSelector')
                .html(errorTemplate({ vertices: this.attr.graphVertexIds }))
                .addClass('visible');
        };

        this.handleVerticesLoaded = function(vertices) {

            Detail.teardownAll();
            this.$node.find('.detail-pane').remove();

            if (vertices.length === 0) {
                return this.handleNoVertices();
            }

            this.vertices = _.sortBy(vertices, function(v) {
                var descriptors = [];

                // TODO: Image/Video before documents

                // Sort by title
                descriptors.push(v.properties.title);
                return descriptors.join(''); 
            });
            
            this.vertices.forEach(function(v) {
                var node = filterEntity(v) ?  this.$node.find('.entities-container') : this.$node.find('.artifacts-container');

                node.append('<div class="detail-pane visible highlight-none"><div class="content"/></div>');
                // TODO: add classes that determine displayType
                Detail.attachTo(this.$node.find('.detail-pane').last().find('.content'), {
                    loadGraphVertexData: v,
                    highlightStyle: 2
                });
            }.bind(this));

            this.updateLocationHash();
            this.updateLayout();
            this.updateTitle();

            if (!this._commSetup) {
                this.setupTabCommunications();
                this._commSetup = true;
            }
        };

        this.onVisibilityChange = function(event, data) {
            this._windowIsHidden = data.hidden;
            if (data.visible) {
                clearTimeout(this.timer);
                this.updateTitle();
            }
        };

        this.onAddGraphVertices = function(data) {
            var self = this,
                vertices = data.vertices,
                targetIdentifier = data.targetIdentifier;

            if (targetIdentifier !== this.fullscreenIdentifier) {
                return;
            }

            var existingVertexIds = _.pluck(this.vertices, 'id');
            var newVertices = _.reject(vertices, function(v) {
                return existingVertexIds.indexOf(v) >= 0;
            });

            if (newVertices.length === 0) {
                return;
            }

            if (this._windowIsHidden) {
                this.flashTitle(vertices);
            }

            this.vertexService
                .getMultiple(existingVertexIds.concat(newVertices))
                .done(this.handleVerticesLoaded.bind(this))
                .done(this.flashTitle.bind(this, newVertices));
        };

        this.flashTitle = function(newVertexIds, newVertices) {
            var self = this,
                i = 0;

            if (!newVertices || newVertices.length === 0) return;

            clearTimeout(this.timer);

            if (this._windowIsHidden) {
                this.timer = setTimeout(function f() {
                    if (self._windowIsHidden && i++ % 2 === 0) {
                        document.title = newVertices.length === 1 ? 
                            ('"' + newVertices[0].properties.title + '" added') :
                            newVertices.length + ' items added';
                    } else {
                        self.updateTitle();
                    }

                    if (self._windowIsHidden) {
                        self.timer = setTimeout(f, 500);
                    }
                }, 500);
            }
        };

        this.titleForVertices = function() {
            if (!this.vertices || this.vertices.length === 0) {
                return 'Loading...';
            } else if (this.vertices.length === 1) {
                return this.vertices[0].properties.title;
            } else {
                var first = '"' + this.vertices[0].properties.title + '"',
                    l = this.vertices.length - 1;

                return first + ' and ' + l + ' other' + (l > 1 ? 's' : '');
            }
        };

        this.setupTabCommunications = function() {
            var self = this;

            require(['intercom'], function(Intercom) {
                var intercom = Intercom.getInstance(),
                    broadcast = function() {
                        intercom.emit('fullscreenDetailsWithVertices', {
                            message: JSON.stringify({
                                vertices: self.vertices,
                                title: self.titleForVertices(),
                                identifier: self.fullscreenIdentifier
                            })
                        }); 
                    };

                intercom.on('addVertices', function(data) {
                    self.onAddGraphVertices(JSON.parse(data.message));
                });
                intercom.on('ping', broadcast);
                broadcast();
            });
        };
    }
});

