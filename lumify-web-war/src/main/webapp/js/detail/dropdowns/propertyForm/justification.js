
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
            this.setReferenceWithValue(data.value);
        };

        this.setReferenceWithValue = function(val) {
            var clipboard = appData.copiedDocumentText;

            if (clipboard && clipboard.text === val) {
                this.setValue(Object.freeze(clipboard));
                return true;
            }

            return false;
        };

        this.onRemoveReference = function(e) {
            e.stopPropagation();

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
            if (referenceDisplay) {

                var self = this,
                    update = function() {
                        self.$node.html(templateRef(value));
                    };

                if (!value.vertexTitle) {
                    var v = appData.vertex(value.vertexId);
                    if (!v) {
                        return appData.refresh(value.vertexId)
                            .done(function(vertex) {
                                value.vertexTitle = v.properties.title.value;
                                update();
                            });
                    } else {
                        value.vertexTitle = v.properties.title.value;
                    }
                }

                update();
            } else {
                this.$node.html(template({value:value || ''}));
                this.select('fieldSelector').tooltip();
            }
        };
    }
});
