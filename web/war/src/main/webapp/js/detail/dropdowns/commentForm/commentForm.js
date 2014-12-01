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

            this.update();
        });

        this.onChange = function(event) {
            this.update();
        };

        this.onSave = function(event) {
            var self = this;

            this.select('primarySelector').addClass('loading').attr('disabled', true);

            this.dataRequest('vertex', 'setProperty', this.attr.data.id, {
                name: 'http://lumify.io/comment#entry',
                value: this.getValue(),
                visibilitySource: ''
            })
                .catch(function() {
                    self.select('primarySelector').removeClass('loading').removeAttr('disabled');
                })
                .done(function() {
                    self.teardown();
                })
        };

        this.getValue = function() {
            return $.trim(this.select('inputSelector').val());
        };

        this.update = function() {
            var val = this.getValue();

            if (val.length) {
                this.select('primarySelector').removeAttr('disabled');
            } else {
                this.select('primarySelector').attr('disabled', true);
            }
        }
    }
});
