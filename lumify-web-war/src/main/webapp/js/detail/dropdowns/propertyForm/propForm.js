define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./propForm',
    'service/ontology',
    'fields/selection/selection'
], function (
    defineComponent,
    withDropdown,
    template,
    OntologyService,
    FieldSelection) {
    'use strict';

    return defineComponent(PropertyForm, withDropdown);

    function PropertyForm() {

        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            propertyListSelector: '.property-list',
            saveButtonSelector: '.btn-primary',
            deleteButtonSelector: '.btn-danger',
            configurationSelector: '.configuration',
            visibilitySelector: '.visibility',
            propertyInputSelector: '.input-row input'
        });

        this.after('initialize', function () {
            var self = this,
                vertex = this.attr.data;

            this.on('click', {
                saveButtonSelector: this.onSave,
                deleteButtonSelector: this.onDelete
            });
            this.on('keyup', {
                propertyInputSelector: this.onKeyup
            });

            this.on('addPropertyError', this.onAddPropertyError);
            this.on('propertychange', this.onPropertyChange);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('propertyselected', this.onPropertySelected);
            this.on('visibilitychange', this.onVisibilityChange);

            this.$node.html(template({}));

            self.select('saveButtonSelector').attr('disabled', true);
            self.select('deleteButtonSelector').hide();

            (vertex.properties._conceptType ?
                self.attr.service.propertiesByConceptId(vertex.properties._conceptType) :
                self.attr.service.propertiesByRelationshipLabel(vertex.properties.relationshipLabel)
            ).done(function (properties) {
                var propertiesList = [];

                properties.list.forEach(function (property) {
                    if (property.title.charAt(0) !== '_' && property.title !== 'boundingBox') {
                        var data = {
                            title: property.title,
                            displayName: property.displayName
                        };
                        propertiesList.push(data);
                    }
                });
                
                propertiesList.sort(function(pa, pb) {
                    var a = pa.title, b = pb.title;
                    if (a === 'startDate' && b === 'endDate') return -1;
                    if (b === 'startDate' && a === 'endDate') return 1;
                    if (a === b) return 0;
                    return a < b ? -1 : 1;
                });

                FieldSelection.attachTo(self.select('propertyListSelector'), {
                    properties: propertiesList,
                    placeholder: 'Select Property'
                });
            });
        });

        this.onPropertySelected = function(event, data) {
            var self = this,
                property = data.property,
                propertyName = property.title,
                config = self.select('configurationSelector'),
                visibility = self.select('visibilitySelector');

            this.currentProperty = property;

            config.teardownAllComponents();
            visibility.teardownAllComponents();

            var previousValue = this.attr.data.properties[propertyName],
                isExistingProperty = (typeof this.attr.data.properties[propertyName]) !== 'undefined';

            this.currentValue = previousValue;
            if (this.currentValue && this.currentValue.latitude) {
                this.currentValue = 'point(' + this.currentValue.latitude + ',' + this.currentValue.longitude + ')';
            }

            this.select('deleteButtonSelector').toggle(!!isExistingProperty);

            var button = this.select('saveButtonSelector').text(isExistingProperty ? 'Update' : 'Add');
            if (isExistingProperty) button.removeAttr('disabled');
            else button.attr('disabled', true);

            this.ontologyService.properties().done(function(properties) {
                var propertyDetails = properties.byTitle[propertyName];
                if (propertyDetails) {
                    require(['fields/' + propertyDetails.dataType], function(PropertyField) {
                        PropertyField.attachTo(config, {
                            property: propertyDetails,
                            value: previousValue,
                            predicates: false
                        });
                    });
                    require(['configuration/plugins/visibility/visibility'], function(Visibility) {
                        Visibility.attachTo(visibility, {
                            // TODO
                            value: ''
                        });
                    });
                } else console.warn('Property ' + propertyName + ' not found in ontology');
            });
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            console.log(data);
        };

        this.onPropertyInvalid = function (event, data) {
            event.stopPropagation();

            this.invalid = true;
            this.select('saveButtonSelector').attr('disabled', true);
        };

        this.onPropertyChange = function (event, data) {
            this.invalid = false;
            this.select('saveButtonSelector').removeAttr('disabled');

            event.stopPropagation();

            if (data.values.length === 1) {
                this.currentValue = data.values[0];
            } else if (data.values.length > 1) {
                // Must be geoLocation
                this.currentValue = 'point(' + data.values.join(',') + ')';
            }
        };

        this.onAddPropertyError = function (event) {
            this.$node.find('input').addClass('validation-error');
            _.defer(this.clearLoading.bind(this));
        };

        this.onKeyup = function(evt) {
            if (evt.which === $.ui.keyCode.ENTER) {
                this.onSave();
            }
        };

        this.onDelete = function() {
            _.defer(this.buttonLoading.bind(this, this.attr.deleteButtonSelector));
            this.trigger('deleteProperty', {
                property: this.currentProperty.title
            });
        };

        this.onSave = function (evt) {
            if (this.invalid || !this.visibilitySource || !this.visibilitySource.valid) return;

            var vertexId = this.attr.data.id,
                propertyName = this.currentProperty.title,
                value = this.currentValue;

            _.defer(this.buttonLoading.bind(this, this.attr.saveButtonSelector));

            this.$node.find('input').removeClass('validation-error');
            if (propertyName.length && ((_.isString(value) && value.length) || value)) {
                this.trigger('addProperty', {
                    property: {
                        name: propertyName,
                        value: value,
                        visibilitySource: this.visibilitySource.value
                    }
                });
            }
        };
    }
});
