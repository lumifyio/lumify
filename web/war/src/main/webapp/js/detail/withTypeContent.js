
define([
    'data',
    'util/vertex/formatters'
], function(appData, F) {
    'use strict';

    return withTypeContent;

    function withTypeContent() {

        this._xhrs = [];

        this.defaultAttrs({
            auditSelector: '.audits'
        });

        this.after('teardown', function() {
            this.cancel();
            this.$node.empty();
        });

        this.before('initialize', function(node) {
            $(node).removeClass('custom-entity-image')
        });

        this.after('initialize', function() {
            var self = this,
                previousConcept = this.attr.data.concept && this.attr.data.concept.id;

            this.auditDisplayed = false;
            this.on('toggleAuditDisplay', this.onToggleAuditDisplay);
            this.on('addNewProperty', this.onAddNewProperty);
            this.on('openFullscreen', this.onOpenFullscreen);
            this.on('toggleAudit', this.onAuditToggle);
            this.on('openSourceUrl', this.onOpenSourceUrl);

            this.debouncedConceptTypeChange = _.debounce(this.debouncedConceptTypeChange.bind(this), 500);
            this.on(document, 'verticesUpdated', function(event, data) {
                if (data && data.vertices) {
                    var current = _.findWhere(data.vertices, { id: this.attr.data.id });
                    if (current && current.concept && current.concept.id !== previousConcept) {
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

        this.onOpenFullscreen = function(event) {
            var url = F.vertexUrl.url(
                _.isArray(this.attr.data) ? this.attr.data : [this.attr.data.id],
                appData.workspaceId
            );
            window.open(url);
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
                props = vertex.properties || vertex;

            if (vertex.concept.displayType === 'document' ||
                vertex.concept.displayType === 'image' ||
                vertex.concept.displayType === 'video') {
                cls.push('artifact entity resolved');
                if (props['http://lumify.io#conceptType']) cls.push(props['http://lumify.io#conceptType'].value);
            } else {
                cls.push('entity resolved');
                if (props['http://lumify.io#conceptType']) {
                    cls.push('conceptType-' + props['http://lumify.io#conceptType'].value);
                }
            }
            cls.push('gId-' + (vertex.id || props.graphNodeId));

            return cls.join(' ');
        };

        this.cancel = function() {
            this._xhrs.forEach(function(xhr) {
                if (xhr.state() !== 'complete' && typeof xhr.abort === 'function') {
                    xhr.abort();
                }
            });
            this._xhrs.length = 0;
        };

        // Pass a started XHR request to automatically cancel if detail pane
        // changes
        this.handleCancelling = function(xhr) {

            // If this is an ajax request notify the detail pane to stop
            // showing loading icon
            var self = this;
            if (_.isFunction(xhr.abort)) {
                xhr.always(function() {
                    self.trigger('finishedLoadingTypeContent');
                });
            }

            this._xhrs.push(xhr);
            return xhr;
        };
    }
});
