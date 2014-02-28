
define([
    'flight/lib/component',
    'tpl!./justification',
    'tpl!./justificationRef',
    'data'
], function (
    defineComponent,
    template,
    templateRef,
    appData
) {
    'use strict';

    return defineComponent(Justification);


    function Justification() {

        this.defaultAttrs({
            fieldSelector: 'input',
            removeReferenceSelector: '.remove'
        });

        this.after('initialize', function() {
            if (this.attr.sourceMetadata) {
                this.toggleView(true, this.attr.sourceMetadata);
            } else {
                this.toggleView(false, 
                    this.attr.justificationMetadata && 
                    this.attr.justificationMetadata.justificationText);
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
                    if (!self.setReferenceWithValue(val)) {
                        self.trigger('justificationchange', { justificationText:val, valid:$.trim(val).length > 0 });
                    }
                });
                return;
            }
            var val = this.select('fieldSelector').val();
            this.trigger('justificationchange', { justificationText:val, valid:$.trim(val).length > 0 });
        };

        this.onValuePasted = function(event, data) {
            this.fromPaste = true;
            this.animate = true;
            this.setReferenceWithValue(data.value);
        };

        this.setReferenceWithValue = function(val) {
            var clipboard = appData.copiedDocumentText();

            if (clipboard && clipboard.text === val) {
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

            var val = this.attr.value || '';
            this.trigger('justificationchange', { justificationText:val, valid:$.trim(val).length > 0 });
        };

        this.setValue = function(value) {
            var sourceInfo = _.pick(value, 'startOffset', 'endOffset', 'vertexId', 'snippet');
            this.toggleView(true, value);
            this.trigger('justificationchange', { sourceInfo:sourceInfo, valid:true });
        };

        this.toggleView = function(referenceDisplay, value) {
            var self = this;
            
            if (referenceDisplay) {
                this.getVertexTitle(value).done(function() {
                    self.transitionHeight(templateRef(value));
                });
            } else {
                this.transitionHeight(template({value:value || ''}));
                this.select('fieldSelector').tooltip({ 
                    container:'body' 
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
                node.on('transitionend webkitTransitionEnd oTransitionEnd otransitionend', function(e) {
                    var oe = e.originalEvent || e;
                    if (oe.propertyName === 'height') {
                        node.off('.justification');
                        node.css({
                            height:'auto',
                            overflow: 'inherit',
                            transition: 'none'
                        });
                    }
                    if (self.fromPaste) {
                        self.fromPaste = false;
                        self.trigger('justificationfrompaste');
                    }
                });
                node.css({
                    height: node.find('.animationwrap').outerHeight(true) + 'px'
                });
            });

        };

        this.getVertexTitle = function(value) {
            var deferredTitle = $.Deferred();

            if (value.vertexTitle) {
                return deferredTitle.resolve();
            }

            var v = appData.vertex(value.vertexId);
            if (v) {
                value.vertexTitle = v.properties.title.value;
                return deferredTitle.resolve();
            }

            appData.refresh(value.vertexId).done(function(vertex) {
                value.vertexTitle = v.properties.title.value;
                deferredTitle.resolve();
            });

            return deferredTitle;
        };
    }
});
