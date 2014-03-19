
define([
    'tpl!./toolbar/fullscreen',
    'tpl!./toolbar/fullscreen-item',
    'tpl!./toolbar/audits',
    'data'
], function(fullscreenButtonTemplate, fullscreenItemTemplate, auditsButtonTemplate, appData) {
    'use strict';

    var intercomInstance;

    return withTypeContent;

    function withTypeContent() {

        this._xhrs = [];

        this.defaultAttrs({
            fullscreenSingleSelector: '.fullscreen-single',
            fullscreenMultiSelector: '.fullscreen-multi',
            fullscreenDropdownButtonSelector: '.fullscreen-multi .dropdown-toggle',
            fullscreenDropdownItemSelector: '.fullscreen-multi .existing a',
            auditSelector: '.audits'
        });

        this.after('teardown', function() {
            this.cancel();
            this.$node.empty();
        });

        this.after('initialize', function() {

            if (!window.isFullscreenDetails) {
                this.on('clearAvailableFullscreenDetails', this.onFullscreenClear);
                this.on('fullscreenDetailVerticesAvailable', this.onFullscreenAdd);
                this.on('click', {
                    fullscreenDropdownButtonSelector: this.onFullscreenDropdownClicked,
                    fullscreenDropdownItemSelector: this.onFullscreenWindowClicked
                });

                this.setupTabCommunication();
            }

            this.auditDisplayed = false;
            this.on('toggleAuditDisplay', this.onToggleAuditDisplay);

            this.on('click', {
                auditSelector: this.onAuditToggle
            });
        });

        this.fullscreenButton = function(vertexIds) {
            return fullscreenButtonTemplate({
                vertexIds: vertexIds
            });
        };

        this.auditsButton = function() {
            return auditsButtonTemplate({});
        };

        this.onToggleAuditDisplay = function(event, data) {
            this.auditDisplayed = data.displayed;
            this.$node.toggleClass('showAuditing', data.displayed);
            this.$node.find('.btn-toolbar .audits').toggleClass('active', data.displayed);
        };

        this.onAuditToggle = function(event) {
            event.stopPropagation();
            event.preventDefault();

            this.auditDisplayed = !this.auditDisplayed;
            this.trigger('toggleAuditDisplay', {
                displayed: this.auditDisplayed
            });
        };

        this.onFullscreenDropdownClicked = function(event) {
            if (intercomInstance) {
                var multi = this.select('fullscreenMultiSelector');
                multi.find('.existing').remove();
                multi.find('.divider').hide();
                intercomInstance.emit('ping');
            }
        };

        this.onFullscreenWindowClicked = function(event) {
            var info = $(event.target).closest('li').data('info');

            if (info) {
                var ids;
                if ($.isArray(this.attr.data)) {
                    ids = _.map(
                            _.reject(this.attr.data, function(v) { 
                                return v['http://lumify.io#conceptType'] === 'relationship';
                            }), function(v) {
                                return v.id || v.graphVertexId;
                            });
                } else {
                    ids = [this.attr.data.id || this.attr.data.graphVertexId];
                }
                intercomInstance.emit('addVertices', {
                    message: JSON.stringify({
                        targetIdentifier: info.identifier,
                        vertices: ids,
                        workspaceId: appData.workspaceId
                    })
                });
            }
        };

        this.onFullscreenClear = function(event, data) {
            this.select('fullscreenMultiSelector')
                .hide()
                .find('.existing').remove();

            this.select('fullscreenSingleSelector').show();
        };

        this.onFullscreenAdd = function(event, data) {
            $.when.apply(null, this._xhrs)
             .done(function() {
                var multi = this.select('fullscreenMultiSelector');
                multi.show()
                    .find('ul')
                    .append(fullscreenItemTemplate({
                        data: data,
                        text: data.title
                    }));

                multi.find('.divider').show();

                this.select('fullscreenSingleSelector').hide();
             }.bind(this));
        };

        this.setupTabCommunication = function() {
            var self = this;

            require(['intercom'], function(Intercom) {
                if (!intercomInstance) {
                    intercomInstance = Intercom.getInstance();

                    intercomInstance.on('fullscreenDetailsWithVertices', function(data) {
                        var info = JSON.parse(data.message);
                        self.trigger('fullscreenDetailVerticesAvailable', info);
                    });
                }
                self.trigger('clearAvailableFullscreenDetails');
                intercomInstance.emit('ping');
            });
        };

        this.classesForVertex = function(vertex) {
            var cls = [],
                props = vertex.properties || vertex;

            if (vertex.concept.displayType === 'document' || vertex.concept.displayType === 'image' || vertex.concept.displayType === 'video') {
                cls.push('artifact entity resolved');
                if (props['http://lumify.io#conceptType']) cls.push(props['http://lumify.io#conceptType'].value);
            } else {
                cls.push('entity resolved');
                if (props['http://lumify.io#conceptType']) cls.push('conceptType-' + props['http://lumify.io#conceptType'].value);
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
            this._xhrs.push(xhr);
            return xhr;
        };
    }
});
