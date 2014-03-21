define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./propForm',
    'service/ontology',
    'fields/selection/selection',
    'data',
    'tpl!util/alert'
], function(
    defineComponent,
    withDropdown,
    template,
    OntologyService,
    FieldSelection,
    appData,
    alertTemplate
) {
    'use strict';

        // Animates the property value to the justification reference on paste if false
    var SKIP_SELECTION_ANIMATION = true;

    return defineComponent(PropertyForm, withDropdown);

    function PropertyForm() {

        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            propertyListSelector: '.property-list',
            saveButtonSelector: '.btn-primary',
            deleteButtonSelector: '.btn-danger',
            configurationSelector: '.configuration',
            configurationFieldSelector: '.configuration input',
            visibilitySelector: '.visibility',
            justificationSelector: '.justification',
            propertyInputSelector: '.input-row input',
            visibilityInputSelector: '.visibility input'
        });

        this.before('initialize', function(n, c) {
            if (c.property) {
                c.manualOpen = true;
            }
        })

        this.after('initialize', function() {
            var self = this,
                vertex = this.attr.data;

            this.on('click', {
                saveButtonSelector: this.onSave,
                deleteButtonSelector: this.onDelete
            });
            this.on('keyup', {
                propertyInputSelector: this.onKeyup,
                visibilityInputSelector: this.onKeyup
            });

            this.on('addPropertyError', this.onAddPropertyError);
            this.on('propertychange', this.onPropertyChange);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('propertyselected', this.onPropertySelected);
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);
            this.on('justificationfrompaste', this.onJustificationFromPaste);
            this.on('paste', {
                configurationFieldSelector: _.debounce(this.onPaste.bind(this), 10)
            });
            this.$node.html(template({
                property: this.attr.property
            }));

            self.select('saveButtonSelector').attr('disabled', true);
            self.select('deleteButtonSelector').hide();


            if (this.attr.property) {
                this.trigger('propertyselected', {
                    property: {
                        displayName: this.attr.property.displayName,
                        title: this.attr.property.key
                    }
                });
            } else {
                (vertex.properties._conceptType.value != 'relationship' ?
                    self.attr.service.propertiesByConceptId(vertex.properties._conceptType.value) :
                    self.attr.service.propertiesByRelationshipLabel(vertex.properties.relationshipType.value)
                ).done(function(properties) {
                    var propertiesList = [{
                        title: '_visibilityJson',
                        displayName: 'Visibility'
                    }];

                    properties.list.forEach(function(property) {
                        if (/^[^_]/.test(property.title) && property.title !== 'boundingBox') {
                            var data = {
                                title: property.title,
                                displayName: property.displayName
                            };
                            propertiesList.push(data);
                        }
                    });
                    
                    propertiesList.sort(function(pa, pb) {
                        var a = pa.title, b = pb.title;
                        if (a === '_visibilityJson') return -1;
                        if (b === '_visibilityJson') return 1;
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
            }
        });

        this.after('teardown', function() {
            this.select('configurationSelector').teardownAllComponents();
            this.select('visibilitySelector').teardownAllComponents();
            this.select('justificationSelector').teardownAllComponents();
        });

        this.onPaste = function(event) {
            var self = this,
                value = $(event.target).val();

            _.defer(function() {
                self.trigger(
                    self.select('justificationSelector'),
                    'valuepasted',
                    { value: value }
                );
            });
        };

        this.onPropertySelected = function(event, data) {
            var self = this,
                property = data.property,
                propertyName = property.title,
                config = self.select('configurationSelector'),
                visibility = self.select('visibilitySelector'),
                justification = self.select('justificationSelector');

            this.currentProperty = property;
            this.$node.find('.errors').hide();

            config.teardownAllComponents();
            visibility.teardownAllComponents();
            justification.teardownAllComponents();

            var vertexProperty = this.attr.data.properties[propertyName],
                previousValue = vertexProperty && (vertexProperty.latitude ? vertexProperty : vertexProperty.value),
                visibilityValue = vertexProperty && vertexProperty._visibilityJson,
                sandboxStatus = vertexProperty && vertexProperty.sandboxStatus,
                isExistingProperty = (typeof this.attr.data.properties[propertyName]) !== 'undefined';

            this.currentValue = previousValue;
            if (this.currentValue && this.currentValue.latitude) {
                this.currentValue = 'point(' + this.currentValue.latitude + ',' + this.currentValue.longitude + ')';
            }

            if (visibilityValue) {
                visibilityValue = visibilityValue.source;
                this.visibilitySource = { value: visibilityValue, valid: true };
            }

            this.select('deleteButtonSelector')
                .text(
                    sandboxStatus === 'PRIVATE' ?  'Delete' :
                    sandboxStatus === 'PUBLIC_CHANGED' ?  'Undo' : ''
                )
                .toggle(
                    (!!isExistingProperty) && 
                    sandboxStatus !== 'PUBLIC' &&
                    propertyName !== '_visibilityJson'
                );

            var button = this.select('saveButtonSelector').text(isExistingProperty ? 'Update' : 'Add');
            if (isExistingProperty) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }

            this.ontologyService.properties().done(function(properties) {
                var propertyDetails = properties.byTitle[propertyName];
                if (propertyDetails) {
                    require([
                        'fields/' + propertyDetails.dataType,
                        'detail/dropdowns/propertyForm/justification',
                        'configuration/plugins/visibility/visibilityEditor'
                    ], function(PropertyField, Justification, Visibility) {

                        PropertyField.attachTo(config, {
                            property: propertyDetails,
                            value: previousValue,
                            predicates: false,
                            tooltip: {
                                html: true,
                                title: '<strong>Include a Reference</strong><br>Paste value from document text',
                                placement: 'left',
                                trigger: 'focus'
                            }
                        });

                        Justification.attachTo(justification, vertexProperty);

                        Visibility.attachTo(visibility, {
                            value: visibilityValue || ''
                        });

                        self.settingVisibility = false;
                        self.checkValid();
                        self.manualOpen();
                    });
                } else if (propertyName === '_visibilityJson') {
                    require([
                        'configuration/plugins/visibility/visibilityEditor'
                    ], function(Visibility) {
                        var val = vertexProperty && vertexProperty.value,
                            source = (val && val.source) || (val && val.value && val.value.source);

                        Visibility.attachTo(visibility, {
                            value: source || ''
                        });
                        visibility.find('input').focus();
                        self.settingVisibility = true;
                        self.visibilitySource = { value: source, valid: true };

                        self.checkValid();
                        self.manualOpen();
                    });
                } else console.warn('Property ' + propertyName + ' not found in ontology');
            });
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onJustificationChange = function(event, data) {
            this.justification = data;
            this.checkValid();
        };

        this.onJustificationFromPaste = function(event, data) {
            var justification = this.select('justificationSelector'),
                configuration = this.select('configurationSelector'),
                selection = justification.find('.selection'),
                clonedSelection = selection.clone(),
                popSnippet = function() {
                    selection.closest('.animationwrap').removeClass('pop-fast').addClass('pop-fast');
                };

            if (!clonedSelection.length) return;

            // More than number of words shouldn't animate, just pop text
            if (SKIP_SELECTION_ANIMATION || selection.text().split(/\s+/).length > 3) {
                return popSnippet();
            }

            configuration.find('.input-row input').after(clonedSelection);

            var position = selection.position(),
                clonedPosition = clonedSelection.position(),
                clonedMarginLeft = parseInt(clonedSelection.css('left'), 10);

            clonedSelection
                .one('transitionend webkitTransitionEnd ' +
                     'oTransitionEnd otransitionend',
                function() {
                    clonedSelection.remove();
                    popSnippet();
                }).css({
                    textIndent: (selection.get(0).offsetLeft - clonedMarginLeft) + 'px',
                    marginTop: -1 * (clonedPosition.top - position.top) + 'px'
                });
        };

        this.onPropertyInvalid = function(event, data) {
            event.stopPropagation();

            this.propertyInvalid = true;
            this.checkValid();
        };

        this.checkValid = function() {
            if (this.settingVisibility) {
                this.valid = this.visibilitySource && this.visibilitySource.valid;
            } else {
                this.valid = !this.propertyInvalid && 
                    (this.visibilitySource && this.visibilitySource.valid) &&
                    (this.justification && this.justification.valid);
            }

            if (this.valid) {
                this.select('saveButtonSelector').removeAttr('disabled');
            } else {
                this.select('saveButtonSelector').attr('disabled', true);
            }
        }

        this.onPropertyChange = function(event, data) {
            this.propertyInvalid = false;
            this.checkValid();

            event.stopPropagation();

            if (data.values.length === 1) {
                this.currentValue = data.values[0];
            } else if (data.values.length > 1) {
                // Must be geoLocation
                this.currentValue = 'point(' + data.values.join(',') + ')';
            }
        };

        this.onAddPropertyError = function(event, data) {
            this.$node.find('.errors').html(
                alertTemplate({
                    error: (data.error || 'Unknown error') 
                })
            ).show();
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

        this.onSave = function(evt) {
            if (!this.valid) return;

            var vertexId = this.attr.data.id,
                propertyName = this.currentProperty.title,
                value = this.currentValue,
                justification = _.pick(this.justification || {}, 'sourceInfo', 'justificationText');

            _.defer(this.buttonLoading.bind(this, this.attr.saveButtonSelector));

            this.$node.find('.errors').hide();
            if (propertyName.length && 
                (this.settingVisibility || 
                 (((_.isString(value) && value.length) || value)))) {

                this.trigger('addProperty', {
                    property: $.extend({
                            name: propertyName,
                            value: value,
                            visibilitySource: this.visibilitySource.value
                        }, justification)
                });
            }
        };
    }
});
