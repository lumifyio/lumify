define([
    'require',
    'flight/lib/component',
    '../withDropdown',
    'tpl!./propForm',
    'fields/selection/selection',
    'tpl!util/alert',
    'util/withTeardown',
    'util/vertex/vertexSelect',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    require,
    defineComponent,
    withDropdown,
    template,
    FieldSelection,
    alertTemplate,
    withTeardown,
    VertexSelector,
    F,
    withDataRequest
) {
    'use strict';

    return defineComponent(PropertyForm, withDropdown, withTeardown, withDataRequest);

    function PropertyForm() {

        this.defaultAttrs({
            propertyListSelector: '.property-list',
            saveButtonSelector: '.btn-primary',
            deleteButtonSelector: '.btn-danger',
            configurationSelector: '.configuration',
            configurationFieldSelector: '.configuration input',
            previousValuesSelector: '.previous-values',
            previousValuesDropdownSelector: '.previous-values-container .dropdown-menu',
            vertexContainerSelector: '.vertex-select-container',
            visibilitySelector: '.visibility',
            justificationSelector: '.justification',
            propertyInputSelector: '.input-row input',
            visibilityInputSelector: '.visibility input'
        });

        this.before('initialize', function(n, c) {
            c.manualOpen = true;
        })

        this.after('initialize', function() {
            var self = this,
                property = this.attr.property,
                vertex = this.attr.data;

            this.on('click', {
                saveButtonSelector: this.onSave,
                deleteButtonSelector: this.onDelete,
                previousValuesSelector: this.onPreviousValuesButtons
            });
            this.on('keyup', {
                propertyInputSelector: this.onKeyup,
                justificationSelector: this.onKeyup,
                visibilityInputSelector: this.onKeyup
            });

            this.on('propertyerror', this.onPropertyError);
            this.on('propertychange', this.onPropertyChange);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('propertyselected', this.onPropertySelected);
            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);
            this.on('paste', {
                configurationFieldSelector: _.debounce(this.onPaste.bind(this), 10)
            });
            this.on('click', {
                previousValuesDropdownSelector: this.onPreviousValuesDropdown
            });
            this.$node.html(template({
                property: property,
                vertex: vertex
            }));

            this.select('saveButtonSelector').attr('disabled', true);
            this.select('deleteButtonSelector').hide();
            this.select('saveButtonSelector').hide();

            if (this.attr.property) {
                this.trigger('propertyselected', {
                    disablePreviousValuePrompt: true,
                    property: _.chain(property)
                        .pick('displayName key name value visibility metadata'.split(' '))
                        .extend({
                            title: property.name
                        })
                        .value()
                });
            } else if (!vertex) {
                this.on('vertexSelected', this.onVertexSelected);
                VertexSelector.attachTo(this.select('vertexContainerSelector'), {
                    value: ''
                });
                this.manualOpen();
            } else if (F.vertex.isEdge(vertex)) {
                throw new Error('Property form not supported for edges');
            } else {
                this.setupPropertySelectionField();
            }
        });

        this.setupPropertySelectionField = function() {
            var self = this;

            this.dataRequest('ontology', 'propertiesByConceptId', F.vertex.prop(this.attr.data, 'conceptType'))
                .done(function(properties) {
                    FieldSelection.attachTo(self.select('propertyListSelector'), {
                        properties: properties.list,
                        placeholder: i18n('property.form.field.selection.placeholder')
                    });
                    self.manualOpen();
                });
        }

        this.onVertexSelected = function(event, data) {
            event.stopPropagation();

            if (data.item && data.item.properties) {
                this.attr.data = data.item;
                this.setupPropertySelectionField();
            }
        };

        this.after('teardown', function() {
            this.select('visibilitySelector').teardownAllComponents();

            if (this.$node.closest('.buttons').length === 0) {
                this.$node.closest('tr').remove();
            }
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

        this.onPreviousValuesButtons = function(event) {
            var self = this,
                dropdown = this.select('previousValuesDropdownSelector'),
                buttons = this.select('previousValuesSelector').find('.active').removeClass('active'),
                action = $(event.target).closest('button').addClass('active').data('action');

            event.stopPropagation();
            event.preventDefault();

            if (action === 'add') {
                dropdown.hide();
                this.trigger('propertyselected', {
                    fromPreviousValuePrompt: true,
                    property: _.omit(this.currentProperty, 'value', 'key')
                });
            } else if (this.previousValues.length > 1) {
                this.trigger('propertyselected', {
                    property: _.omit(this.currentProperty, 'value', 'key')
                });

                dropdown.html(
                        this.previousValues.map(function(p, i) {
                            var visibility = p.metadata && p.metadata['http://lumify.io#visibilityJson'];
                            return _.template(
                                '<li data-index="{i}">' +
                                    '<a href="#">{value}' +
                                        '<div data-visibility="{visibilityJson}" class="visibility"/>' +
                                    '</a>' +
                                '</li>')({
                                value: F.vertex.prop(self.attr.data, self.previousValuesPropertyName, p.key),
                                visibilityJson: JSON.stringify(visibility || {}),
                                i: i
                            });
                        }).join('')
                    ).show();

                require(['configuration/plugins/visibility/visibilityDisplay'], function(Visibility) {
                    dropdown.find('.visibility').each(function() {
                        var value = $(this).data('visibility');
                        Visibility.attachTo(this, {
                            value: value && value.source
                        });
                    });
                });

            } else {
                dropdown.hide();
                this.trigger('propertyselected', {
                    fromPreviousValuePrompt: true,
                    property: $.extend({}, this.currentProperty, this.previousValues[0])
                });
            }
        };

        this.onPreviousValuesDropdown = function(event) {
            var li = $(event.target).closest('li');
                index = li.data('index');

            this.$node.find('.previous-values .edit-previous').addClass('active');
            this.trigger('propertyselected', {
                fromPreviousValuePrompt: true,
                property: $.extend({}, this.currentProperty, this.previousValues[index])
            });
        };

        this.onPropertySelected = function(event, data) {
            var self = this,
                property = data.property,
                disablePreviousValuePrompt = data.disablePreviousValuePrompt,
                propertyName = property.title,
                config = self.select('configurationSelector'),
                visibility = self.select('visibilitySelector'),
                justification = self.select('justificationSelector');

            this.currentProperty = property;
            this.$node.find('.errors').hide();

            config.teardownAllComponents();
            visibility.teardownAllComponents();
            justification.teardownAllComponents();

            var vertexProperty = property.title === 'http://lumify.io#visibilityJson' ?
                    _.first(F.vertex.props(this.attr.data, property.title)) :
                    !_.isUndefined(property.key) ?
                    _.first(F.vertex.props(this.attr.data, property.title, property.key)) :
                    undefined,
                previousValue = vertexProperty && vertexProperty.value,
                visibilityValue = vertexProperty &&
                    vertexProperty.metadata &&
                    vertexProperty.metadata['http://lumify.io#visibilityJson'],
                sandboxStatus = vertexProperty && vertexProperty.sandboxStatus,
                isExistingProperty = typeof vertexProperty !== 'undefined',
                previousValues = disablePreviousValuePrompt !== true && F.vertex.props(this.attr.data, propertyName),
                previousValuesUniquedByKey = previousValues && _.unique(previousValues, _.property('key'));

            this.currentValue = this.attr.attemptToCoerceValue || previousValue;
            if (this.currentValue && _.isObject(this.currentValue) && ('latitude' in this.currentValue)) {
                this.currentValue = 'point(' + this.currentValue.latitude + ',' + this.currentValue.longitude + ')';
            }

            if (visibilityValue) {
                visibilityValue = visibilityValue.source;
                this.visibilitySource = { value: visibilityValue, valid: true };
            }

            if (property.name === 'http://lumify.io#visibilityJson') {
                vertexProperty = property;
                isExistingProperty = true;
                previousValues = null;
                previousValuesUniquedByKey = null;
            }

            if (data.fromPreviousValuePrompt !== true) {
                if (previousValuesUniquedByKey && previousValuesUniquedByKey.length) {
                    this.previousValues = previousValuesUniquedByKey;
                    this.previousValuesPropertyName = propertyName;
                    this.select('previousValuesSelector')
                        .show()
                        .find('.active').removeClass('active')
                        .addBack()
                        .find('.edit-previous span').text(previousValuesUniquedByKey.length)
                        .addBack()
                        .find('.edit-previous small').toggle(previousValuesUniquedByKey.length > 1);

                    this.select('justificationSelector').hide();
                    this.select('visibilitySelector').hide();
                    this.select('saveButtonSelector').hide();
                    this.select('previousValuesDropdownSelector').hide();

                    return;
                } else {
                    this.select('previousValuesSelector').hide();
                }
            }

            this.select('previousValuesDropdownSelector').hide();
            this.select('justificationSelector').show();
            this.select('visibilitySelector').show();
            this.select('saveButtonSelector').show();

            this.select('deleteButtonSelector')
                .toggle(
                    !!isExistingProperty &&
                    propertyName !== 'http://lumify.io#visibilityJson'
                );

            var button = this.select('saveButtonSelector')
                .text(isExistingProperty ? i18n('property.form.button.update') : i18n('property.form.button.add'));
            if (isExistingProperty) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }

            this.dataRequest('ontology', 'properties').done(function(properties) {
                var propertyDetails = properties.byTitle[propertyName];
                self.currentPropertyDetails = propertyDetails;
                if (propertyName === 'http://lumify.io#visibilityJson') {
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
                } else if (propertyDetails) {
                    var isCompoundField = propertyDetails.dependentPropertyIris &&
                        propertyDetails.dependentPropertyIris.length;

                    require([
                        (
                            isCompoundField ?
                            'fields/compound/compound' :
                            propertyDetails.possibleValues ?
                                'fields/restrictValues' :
                                'fields/' + propertyDetails.dataType
                        ),
                        'detail/dropdowns/propertyForm/justification',
                        'configuration/plugins/visibility/visibilityEditor'
                    ], function(PropertyField, Justification, Visibility) {

                        if (isCompoundField) {
                            PropertyField.attachTo(config, {
                                property: propertyDetails,
                                vertex: self.attr.data,
                                predicates: false,
                                focus: true,
                                values: property.key ?
                                    F.vertex.props(self.attr.data, propertyDetails.title, property.key) :
                                    null
                            });
                        } else {
                            PropertyField.attachTo(config, {
                                property: propertyDetails,
                                vertexProperty: vertexProperty,
                                value: self.attr.attemptToCoerceValue || previousValue,
                                predicates: false,
                                tooltip: (!self.attr.sourceInfo && !self.attr.justificationText) ? {
                                    html: true,
                                    title:
                                        '<strong>' +
                                        i18n('justification.field.tooltip.title') +
                                        '</strong><br>' +
                                        i18n('justification.field.tooltip.subtitle'),
                                    placement: 'left',
                                    trigger: 'focus'
                                } : null
                            });
                        }

                        Justification.attachTo(justification, {
                            justificationText: self.attr.justificationText,
                            sourceInfo: self.attr.sourceInfo
                        });

                        Visibility.attachTo(visibility, {
                            value: visibilityValue || ''
                        });

                        self.settingVisibility = false;
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

            var isCompoundField = this.currentPropertyDetails.dependentPropertyIris,
                transformValue = function(valueArray) {
                    if (valueArray.length === 1) {
                        return valueArray[0];
                    } else if (valueArray.length === 2) {
                        // Must be geoLocation
                        return 'point(' + valueArray.join(',') + ')';
                    } else if (valueArray.length === 3) {
                        return JSON.stringify({
                            description: valueArray[0],
                            latitude: valueArray[1],
                            longitude: valueArray[2]
                        });
                    }
                }

            if (isCompoundField) {
                this.currentValue = _.map(data.values, transformValue);
            } else {
                this.currentValue = transformValue(data.values);
            }

            this.currentMetadata = data.metadata;
        };

        this.onPropertyError = function(event, data) {
            var messages = this.markFieldErrors(data.error);

            this.$node.find('.errors').html(
                alertTemplate({
                    error: messages
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
                vertexId: this.attr.data.id,
                property: _.pick(this.currentProperty, 'key', 'name')
            });
        };

        this.onSave = function(evt) {
            if (!this.valid) return;

            var vertexId = this.attr.data.id,
                propertyKey = this.currentProperty.key,
                propertyName = this.currentProperty.title,
                value = this.currentValue,
                justification = _.pick(this.justification || {}, 'sourceInfo', 'justificationText');

            _.defer(this.buttonLoading.bind(this, this.attr.saveButtonSelector));

            this.$node.find('input').tooltip('hide')

            this.$node.find('.errors').hide();
            if (propertyName.length &&
                (
                    this.settingVisibility ||
                    (
                        (_.isString(value) && value.length) ||
                        _.isNumber(value) ||
                        value
                    )
                )) {

                this.trigger('addProperty', {
                    isEdge: F.vertex.isEdge (this.attr.data),
                    vertexId: this.attr.data.id,
                    property: $.extend({
                            key: propertyKey,
                            name: propertyName,
                            value: value,
                            visibilitySource: this.visibilitySource.value,
                            metadata: this.currentMetadata
                        }, justification)
                });
            }
        };
    }
});
