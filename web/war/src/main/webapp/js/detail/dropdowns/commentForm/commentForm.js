define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./commentForm',
    'tpl!util/alert',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    withDropdown,
    commentTemplate,
    alertTemplate,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(CommentForm, withDropdown, withDataRequest);

    function CommentForm() {

        this.defaultAttrs({
            inputSelector: 'textarea',
            primarySelector: '.btn-primary'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('change keyup paste', {
                inputSelector: this.onChange
            });
            this.on('click', {
                primarySelector: this.onSave
            });
            this.on('visibilitychange', this.onVisibilityChange);

            this.$node.html(commentTemplate({
                graphVertexId: this.attr.data.id,
                buttonText: i18n('detail.comment.form.button')
            }));

            this.on('opened', function() {
                this.select('inputSelector').focus();
            });

            require([
                'configuration/plugins/visibility/visibilityEditor'
            ], function(Visibility) {
                Visibility.attachTo(self.$node.find('.visibility'), {
                    value: ''
                });
            });

            this.checkValid();
        });

        this.onChange = function(event) {
            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onSave = function(event) {
            var self = this;

            this.buttonLoading();

            this.dataRequest(this.attr.type, 'setProperty', this.attr.data.id, {
                name: 'http://lumify.io/comment#entry',
                value: this.getValue(),
                visibilitySource: this.visibilitySource && this.visibilitySource.value || '',
                sourceInfo: this.attr.sourceInfo
            })
                .then(function() {
                    self.teardown();
                })
                .catch(function(error) {
                    self.markFieldErrors(error && error.statusText);
                    self.clearLoading();
                })
        };

        this.getValue = function() {
            return $.trim(this.select('inputSelector').val());
        };

        this.checkValid = function() {
            var val = this.getValue();

            if (val.length && this.visibilitySource && this.visibilitySource.valid) {
                this.select('primarySelector').removeAttr('disabled');
            } else {
                this.select('primarySelector').attr('disabled', true);
            }
        }
    }
});
