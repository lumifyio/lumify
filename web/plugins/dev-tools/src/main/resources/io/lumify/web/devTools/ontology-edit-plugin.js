require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/ontology-edit',
    'util/formatters',
    'util/ontology/conceptSelect',
    'util/withDataRequest',
    'd3'
], function(
    defineLumifyAdminPlugin,
    template,
    F,
    ConceptSelector,
    withDataRequest,
    d3
    ) {
    'use strict';

    return defineLumifyAdminPlugin(OntologyEdit, {
        mixins: [withDataRequest],
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
            conceptSelector: '.concept-container',
            buttonSelector: '.btn-primary'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('conceptSelected', this.onConceptSelected);
            this.on('click', {
                buttonSelector: this.onSave
            });

            this.$node.html(template({}));

            ConceptSelector.attachTo(this.select('conceptSelector'), {
                showAdminConcepts: true
            });
        });

        this.onSave = function() {
            var self = this;

            this.handleSubmitButton(
                this.select('buttonSelector'),
                this.dataRequest('admin', 'ontologyEdit', {
                    concept: this.currentConcept,
                    displayName: this.$node.find('.displayName').val(),
                    color: hexToRgb(this.$node.find('.color').val()),
                    titleFormula: this.$node.find('.titleFormula').val(),
                    subtitleFormula: this.$node.find('.subtitleFormula').val(),
                    timeFormula: this.$node.find('.timeFormula').val()
                })
                    .then(function() {
                        self.showSuccess('Saved, refresh to see changes');
                    })
                    .catch(function() {
                        self.showError();
                    })

            )
        };

        this.onConceptSelected = function(event, data) {
            var self = this;

            if (data.concept) {
                this.currentConcept = data.concept.id;
                this.$node.find('.btn-primary').removeAttr('disabled');

                _.each(data.concept, function(value, key) {
                    self.$node.find('.' + key).val(
                        key === 'color' ?
                        rgbToHex(value) :
                        value
                    );
                });
            } else {
                this.$node.find('.btn-primary').attr('disabled', true);
            }
        };

    }
});
