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
                commentText: this.attr.comment && this.attr.comment.value || '',
                buttonText: i18n('detail.comment.form.button')
            }));

            this.on('opened', function() {
                this.select('inputSelector').focus();
            });

            require([
                'configuration/plugins/visibility/visibilityEditor'
            ], function(Visibility) {
                Visibility.attachTo(self.$node.find('.visibility'), {
                    value: self.attr.comment &&
                        self.attr.comment.metadata &&
                        self.attr.comment.metadata['http://lumify.io#visibilityJson'] &&
                        self.attr.comment.metadata['http://lumify.io#visibilityJson'].source
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
                key: this.attr.comment && this.attr.comment.key,
                value: this.getValue(),
                metadata: this.attr.path && {
                    'http://lumify.io/comment#path': this.attr.path
                },
                visibilitySource: this.visibilitySource && this.visibilitySource.value || '',
                sourceInfo: this.attr.sourceInfo
            })
                .then(function() {
                    self.teardown();
                })
                .catch(function(error) {
                    self.markFieldErrors(error && error.statusText || error);
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
