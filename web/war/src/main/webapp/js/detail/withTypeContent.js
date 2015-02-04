
define([
    'util/vertex/formatters',
    'util/withDataRequest',
    'require'
], function(F, withDataRequest, require) {
    'use strict';

    return withTypeContent;

    function withTypeContent() {

        withDataRequest.call(this);

        this._promisesToCancel = [];

        this.defaultAttrs({
            auditSelector: '.audits',
            deleteFormSelector: '.delete-form'
        });

        this.after('teardown', function() {
            this.cancel();
            this.$node.empty();
        });

        this.before('initialize', function(node) {
            var self = this;

            $(node).removeClass('custom-entity-image')

            this.around('dataRequest', function(func) {
                var promise = func.apply(this, Array.prototype.slice.call(arguments, 1))

                if (promise.cancel) {
                    this._promisesToCancel.push(promise);
                }

                promise.then(function() {
                    self.trigger('finishedLoadingTypeContent');
                })

                return promise;
            })
        });

        this.after('initialize', function() {
            var self = this,
                previousConcept = F.vertex.concept(this.attr.data),
                previousConceptId = previousConcept && previousConcept.id;

            this.auditDisplayed = false;
            this.on('toggleAuditDisplay', this.onToggleAuditDisplay);
            this.on('addNewProperty', this.onAddNewProperty);
            this.on('addNewComment', this.onAddNewComment);
            this.on('deleteItem', this.onDeleteItem);
            this.on('openFullscreen', this.onOpenFullscreen);
            this.on('toggleAudit', this.onAuditToggle);
            this.on('openSourceUrl', this.onOpenSourceUrl);
            this.on('maskWithOverlay', this.onMaskWithOverlay);

            this.debouncedConceptTypeChange = _.debounce(this.debouncedConceptTypeChange.bind(this), 500);
            this.on(document, 'verticesUpdated', function(event, data) {
                if (data && data.vertices) {
                    var current = _.findWhere(data.vertices, { id: this.attr.data.id }),
                        concept = current && F.vertex.concept(current);

                    if (concept && concept.id !== previousConceptId) {
                        self.debouncedConceptTypeChange(current);
                    }
                }
            });
        });

        this.onOpenSourceUrl = function(event, data) {
            window.open(data.sourceUrl);
        }

        this.onAddNewProperty = function(event) {
            this.trigger(this.select('propertiesSelector'), 'editProperty');
        };

        this.onAddNewComment = function(event) {
            this.trigger(this.select('commentsSelector'), 'editComment');
        };

        this.onDeleteItem = function(event) {
            var self = this,
                $container = this.select('deleteFormSelector');

            if ($container.length === 0) {
                $container = $('<div class="delete-form"></div>').insertBefore(
                    this.select('propertiesSelector')
                );
            }

            require(['./dropdowns/deleteForm/deleteForm'], function(DeleteForm) {
                var node = $('<div class="underneath"></div>').appendTo($container);
                DeleteForm.attachTo(node, {
                    data: self.attr.data
                });
            });
        };

        this.onOpenFullscreen = function(event) {
            var data = this.attr.data;
                vertices = _.isObject(data) && data.vertices ?
                    data.vertices : [this.attr.data];

            var url = F.vertexUrl.url(
                vertices,
                lumifyData.currentWorkspaceId
            );
            window.open(url);
        };

        this.onMaskWithOverlay = function(event, data) {
            event.stopPropagation();

            if (data.done) {
                this.$node.find('.detail-overlay').remove();
            } else {
                $('<div>')
                    .addClass('detail-overlay')
                    .toggleClass('detail-overlay-loading', data.loading)
                    .append($('<h1>').text(data.text))
                    .appendTo(this.$node);
            }
        };

        this.debouncedConceptTypeChange = function(vertex) {
            this.trigger(document, 'selectObjects', {
                vertices: [vertex],
                options: {
                    forceSelectEvenIfSame: true
                }
            });
        };

        this.sourceUrlToolbarItem = function() {
            var sourceUrl = _.findWhere(this.attr.data.properties, { name: 'http://lumify.io#sourceUrl' });

            if (sourceUrl) {
                return {
                    title: i18n('detail.toolbar.open.source_url'),
                    subtitle: i18n('detail.toolbar.open.source_url.subtitle'),
                    event: 'openSourceUrl',
                    eventData: {
                        sourceUrl: sourceUrl.value
                    }
                };
            }
        };

        this.onToggleAuditDisplay = function(event, data) {
            this.auditDisplayed = data.displayed;
            this.$node.toggleClass('showAuditing', data.displayed);
            this.$node.find('.comp-toolbar .audits').toggleClass('active', data.displayed);
        };

        this.onAuditToggle = function(event) {
            event.stopPropagation();
            event.preventDefault();

            this.auditDisplayed = !this.auditDisplayed;
            this.trigger('toggleAuditDisplay', {
                displayed: this.auditDisplayed
            });
        };

        this.classesForVertex = function(vertex) {
            var cls = [],
                isEdge = F.vertex.isEdge(vertex),
                props = vertex.properties || vertex,
                concept = !isEdge && F.vertex.concept(vertex);

            if (concept &&
                (concept.displayType === 'document' ||
                concept.displayType === 'image' ||
                concept.displayType === 'video')) {
                cls.push('artifact entity resolved');
                if (props['http://lumify.io#conceptType']) cls.push(props['http://lumify.io#conceptType'].value);
            } else if (!isEdge) {
                cls.push('entity resolved');
                if (props['http://lumify.io#conceptType']) {
                    cls.push('conceptType-' + props['http://lumify.io#conceptType'].value);
                }
            } else {
                cls.push('edge');
            }
            // TODO:
            // cls.push('gId-' + (vertex.id || props.graphNodeId));

            return cls.join(' ');
        };

        this.cancel = function() {
            _.invoke(this._promisesToCancel, 'cancel');
            this._promisesToCancel.length = 0;
        };
    }
});
