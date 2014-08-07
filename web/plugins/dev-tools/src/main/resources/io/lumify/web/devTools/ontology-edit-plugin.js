require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/ontology-edit',
    'service/ontology',
    'util/formatters',
    'd3'
], function(
    defineLumifyAdminPlugin,
    template,
    OntologyService,
    F,
    d3
    ) {
    'use strict';

    var ontologyService = new OntologyService();

    return defineLumifyAdminPlugin(OntologyEdit, {
        section: 'Ontology',
        name: 'Edit',
        subtitle: 'Modify the current ontology'
    });

    function componentToHex(c) {
        var hex = c.toString(16);
        return hex.length == 1 ? '0' + hex : hex;
    }

    function hexToRgb(hex) {
        var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result ? 'rgb(' +
            parseInt(result[1], 16) + ', ' +
            parseInt(result[2], 16) + ', ' +
            parseInt(result[3], 16) + ')' : '';
    }

    function rgbToHex(rgb) {
        var r = /^rgb\((.+?)\)$/,
            match = rgb.match(r);

        if (match) {
            var colors = match[1].split(/\s*,\s*/);
            if (colors.length === 3) {
                return '#' + colors.map(function(colorStr) {
                    return componentToHex(parseInt(colorStr, 10));
                }).join('');
            }
        }

        return '#000000';
    }

    function OntologyEdit() {

        this.defaultAttrs({
            conceptSelector: '.concept',
            buttonSelector: '.btn-primary'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('change', {
                conceptSelector: this.onChange
            });
            this.on('click', {
                buttonSelector: this.onSave
            });

            this.$node.html(template({}));

            ontologyService.concepts()
                .always(function() {
                    self.$node.find('.badge').remove();
                })
                .done(function(concepts) {
                    self.concepts = concepts;
                    self.select('conceptSelector')
                        .append(
                            _.chain(concepts.byId)
                            .values()
                            .sortBy(function(concept) {
                                return concept.displayName.toLowerCase();
                            })
                            .map(function(concept) {
                                return $('<option>')
                                    .val(concept.title)
                                    .text(concept.displayName || concept.title)
                            })
                            .value()
                        ).change();
                });
        });

        this.onSave = function() {
            var self = this;

            this.handleSubmitButton(
                this.select('buttonSelector'),
                this.adminService.ontologyEdit({
                    concept: this.select('conceptSelector').val(),
                    displayName: this.$node.find('.displayName').val(),
                    color: hexToRgb(this.$node.find('.color').val()),
                    titleFormula: this.$node.find('.titleFormula').val(),
                    subtitleFormula: this.$node.find('.subtitleFormula').val(),
                    timeFormula: this.$node.find('.timeFormula').val()
                })
                    .fail(function() {
                        self.showError();
                    })
                    .done(function() {
                        self.showSuccess('Saved, refresh to see changes');
                    })
            )
        };

        this.onChange = function() {
            var self = this,
                conceptId = this.select('conceptSelector').val(),
                concept = this.concepts.byId[conceptId];

            this.$node.find('.btn-primary').removeAttr('disabled');

            _.each(concept, function(value, key) {
                self.$node.find('.' + key).val(
                    key === 'color' ?
                    rgbToHex(value) :
                    value
                );
            });

        }

    }
});