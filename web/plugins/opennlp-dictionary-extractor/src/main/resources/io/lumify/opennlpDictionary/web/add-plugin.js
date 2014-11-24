require([
    'configuration/admin/plugin',
    'hbs!io/lumify/opennlpDictionary/web/templates/add',
    'util/formatters',
    'd3'
], function(
    defineLumifyAdminPlugin,
    template,
    F,
    d3
    ) {
    'use strict';

    return defineLumifyAdminPlugin(DictionaryAdd, {
        section: 'Dictionary',
        name: 'Add',
        subtitle: 'Create new dictionary entries'
    });

    function DictionaryAdd() {

        this.defaultAttrs({
            createSelector: '.btn-primary',
            tokenSelector: '.token'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                createSelector: this.onCreate
            });

            this.on('change keyup', {
                tokenSelector: this.onChangeToken
            });

            this.$node.html(template({}));

            ontologyService.concepts()
                .always(function() {
                    self.$node.find('.badge').remove();
                })
                .done(function(concepts) {
                    self.$node.find('select')
                        .append(
                            _.chain(concepts.byTitle)
                            .sortBy(function(concept) {
                                return concept.displayName.toLowerCase();
                            })
                            .map(function(concept) {
                                return $('<option>')
                                    .val(concept.title)
                                    .text(concept.displayName)
                            })
                            .value()
                        );
                })
        });

        this.onChangeToken = function(event) {
            var val = $.trim($(event.target).val());
            if (val.length > 0) {
                this.select('createSelector').removeAttr('disabled');
            } else {
                this.select('createSelector').attr('disabled', true);
            }
        };

        this.onCreate = function() {
            var self = this;

            this.handleSubmitButton(
                this.select('createSelector'),
                this.adminService.dictionaryAdd(
                    this.$node.find('select').val(),
                    this.$node.find('.token').val(),
                    this.$node.find('.resolved').val()
                ).fail(this.showError.bind(this))
                .done(function() {
                    self.showSuccess();
                })
                .done(function() {
                    self.$node.find('.token').val('');
                    self.$node.find('.resolved').val('');
                })
            )
        };

    }
});
