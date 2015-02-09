
define([
    'flight/lib/component',
    'tpl!./justification',
    'tpl!./justificationRef',
    'util/withTeardown',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    templateRef,
    withTeardown,
    withDataRequest,
    F
) {
    'use strict';

    return defineComponent(Justification, withTeardown, withDataRequest);

    function Justification() {

        this.defaultAttrs({
            fieldSelector: 'input',
            removeReferenceSelector: '.remove'
        });

        this.after('teardown', function() {
            this.select('fieldSelector').tooltip('destroy');
        })

        this.after('initialize', function() {
            if (this.attr._sourceMetadata) {
                this.setValue(this.attr._sourceMetadata)
            } else {
                this.setValue(this.attr['http://lumify.io#justification'] && this.attr['http://lumify.io#justification'].justificationText);
            }

            this.on('valuepasted', this.onValuePasted);
            this.on('change keyup paste', {
                fieldSelector: this.onChangeJustification
            });
            this.on('click', {
                removeReferenceSelector: this.onRemoveReference
            });
        });

        this.onChangeJustification = function(event) {
            var self = this;

            // If it's paste check to see if it can be a reference
            if (event.type === 'paste') {
                _.defer(function() {
                    var val = self.select('fieldSelector').val();
                    self.animate = true;
                    if (!self.setReferenceWithValue(val)) {
                        self.trigger('justificationchange', {
                            justificationText: val,
                            valid: $.trim(val).length > 0
                        });
                    }
                });
                return;
            }
            var val = this.select('fieldSelector').val();
            this.trigger('justificationchange', {
                justificationText: val,
                valid: $.trim(val).length > 0
            });
        };

        this.onValuePasted = function(event, data) {
            this.fromPaste = true;
            this.animate = true;
            this.setReferenceWithValue(data.value);
        };

        this.setReferenceWithValue = function(val) {
            var clipboard = lumifyData.copiedDocumentText,
                normalizeWhiteSpace = function(str) {
                    return str.replace(/\s/g, ' ');
                };

            if (clipboard && normalizeWhiteSpace(clipboard.text) === normalizeWhiteSpace(val)) {
                this.setValue(Object.freeze(clipboard));
                return true;
            }

            return false;
        };

        this.onRemoveReference = function(e) {
            e.stopPropagation();

            this.animate = true;
            this.toggleView(false);
            this.select('fieldSelector').focus();
            this.trigger('justificationchange', {
                justificationText: '',
                valid: false
            });
        };

        this.setValue = function(value) {
            if (_.isUndefined(value)) value = '';

            if (_.isString(value)) {
                this.toggleView(false, value);
                this.trigger('justificationchange', {
                    justificationText: value,
                    valid: $.trim(value).length > 0
                });
            } else {
                var sourceInfo = _.pick(value, 'startOffset', 'endOffset', 'vertexId', 'snippet', 'textPropertyKey');
                this.toggleView(true, value);
                this.trigger('justificationchange', { sourceInfo: sourceInfo, valid: true });
            }
        };

        this.toggleView = function(referenceDisplay, value) {
            var self = this;

            if (referenceDisplay) {
                this.select('fieldSelector').tooltip('destroy');
                this.getVertexTitle(value).done(function(title) {
                    value.vertexTitle = title;
                    self.transitionHeight(templateRef(value));
                });
            } else {
                this.transitionHeight(template({value: value || ''}));
                this.select('fieldSelector').tooltip({
                    container: 'body'
                }).data('tooltip').tip().addClass('field-tooltip');
            }
        };

        this.transitionHeight = function(content) {
            var self = this,
                node = this.$node;

            if (!this.animate) {
                this.animate = false;
                return node.html(content);
            }

            node.css({
                height: node.height() + 'px',
                transition: 'height ease-in-out 0.3s',
                overflow: 'hidden'
            }).html(content);

            _.defer(function() {
                var toHeight = node.find('.animationwrap').outerHeight(true);
                node.on(TRANSITION_END, function(e) {
                    var oe = e.originalEvent || e;

                    node.off(TRANSITION_END);

                    if (oe.propertyName === 'height') {
                        node.off('.justification');
                        node.css({
                            height: 'auto',
                            overflow: 'inherit',
                            transition: 'none'
                        });
                    }
                    if (self.fromPaste) {
                        self.fromPaste = false;
                        self.trigger('justificationfrompaste');
                    }
                    self.trigger('justificationanimationend');
                });
                node.css({
                    height: node.find('.animationwrap').outerHeight(true) + 'px'
                });
            });

        };

        this.getVertexTitle = function(value) {
            var deferredTitle = $.Deferred();

            if (value.vertexTitle) {
                return deferredTitle.resolve(value.vertexTitle);
            }

            return this.dataRequest('vertex', 'store', { vertexIds: value.vertexId })
                .then(function(vertex) {
                    return F.vertex.title(vertex);
                });
        };
    }
});
