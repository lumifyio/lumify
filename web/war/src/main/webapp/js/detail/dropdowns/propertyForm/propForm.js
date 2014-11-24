define([
    'require',
    'flight/lib/component',
    '../withDropdown',
    'tpl!./propForm',
    'fields/selection/selection',
    'data',
    'tpl!util/alert',
    'util/withTeardown',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    require,
    defineComponent,
    withDropdown,
    template,
    FieldSelection,
    appData,
    alertTemplate,
    withTeardown,
    F,
    withDataRequest
) {
    'use strict';

        // Animates the property value to the justification reference on paste if false
    var SKIP_SELECTION_ANIMATION = true;

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
            this.on('justificationfrompaste', this.onJustificationFromPaste);
            this.on('paste', {
                configurationFieldSelector: _.debounce(this.onPaste.bind(this), 10)
            });
            this.on('click', {
                previousValuesDropdownSelector: this.onPreviousValuesDropdown
            });
            this.$node.html(template({
                property: this.attr.property
            }));

            self.select('saveButtonSelector').attr('disabled', true);
            self.select('deleteButtonSelector').hide();
            self.select('saveButtonSelector').hide();

            if (this.attr.property) {
                this.trigger('propertyselected', {
                    disablePreviousValuePrompt: true,
                    property: _.chain(this.attr.property)
                        .pick('displayName key name value visibility metadata'.split(' '))
                        .extend({
                            title: this.attr.property.name
                        })
                        .value()
                });
            } else if (F.vertex.isEdge(this.attr.data)) {
                throw new Error('Property form not supported for edges');
            } else {
                this.dataRequest('ontology', 'propertiesByConceptId', F.vertex.prop(vertex, 'conceptType'))
                    .done(function(properties) {
                        var propertiesList = [];

                        properties.list.forEach(function(property) {
                            if (property.userVisible) {
                                propertiesList.push(_.pick(property, 'displayName', 'title', 'userVisible'));
                            }
                        });

                        FieldSelection.attachTo(self.select('propertyListSelector'), {
                            properties: propertiesList,
                            placeholder: i18n('property.form.field.selection.placeholder')
                        });
                    });
            }
        });

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
            var dropdown = this.select('previousValuesDropdownSelector'),
                buttons = this.select('previousValuesSelector').find('.active').removeClass('active'),
                action = $(event.target).closest('button').addClass('active').data('action');

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
                            var visibility = p['http://lumify.io#visibilityJson'];
                            return _.template(
                                '<li data-index="{i}">' +
                                    '<a href="#">{value}' +
                                        '<div data-visibility="{visibilityJson}" class="visibility"/>' +
                                    '</a>' +
                                '</li>')({
                                value: F.vertex.displayProp(p),
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

            var vertexProperty = property.key || property.key === '' ?
                    F.vertex.propForNameAndKey(this.attr.data, property.name, property.key) : undefined,
                previousValue = vertexProperty && (vertexProperty.latitude ? vertexProperty : vertexProperty.value),
                visibilityValue = vertexProperty && vertexProperty['http://lumify.io#visibilityJson'],
                sandboxStatus = vertexProperty && vertexProperty.sandboxStatus,
                isExistingProperty = typeof vertexProperty !== 'undefined',
                previousValues = disablePreviousValuePrompt !== true && F.vertex.props(this.attr.data, propertyName);

            this.currentValue = previousValue;
            if (this.currentValue && this.currentValue.latitude) {
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
            }

            if (data.fromPreviousValuePrompt !== true) {
                previousValues = _.reject(previousValues, function(prevVal) {
                    return prevVal.sandboxStatus === 'PUBLIC';
                });

                if (previousValues && previousValues.length) {
                    this.previousValues = previousValues;
                    this.select('previousValuesSelector')
                        .show()
                        .find('.active').removeClass('active')
                        .addBack()
                        .find('.edit-previous span').text(previousValues.length)
                        .addBack()
                        .find('.edit-previous small').toggle(previousValues.length > 1);

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
                .text(
                    sandboxStatus === 'PRIVATE' ?  i18n('property.form.button.delete') :
                    sandboxStatus === 'PUBLIC_CHANGED' ?  i18n('property.form.button.undo') : ''
                )
                .toggle(
                    (!!isExistingProperty) &&
                    sandboxStatus !== 'PUBLIC' &&
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
                    require([
                        (
                            propertyDetails.possibleValues ?
                                'fields/restrictValues' :
                                'fields/' + propertyDetails.dataType
                        ),
                        'detail/dropdowns/propertyForm/justification',
                        'configuration/plugins/visibility/visibilityEditor'
                    ], function(PropertyField, Justification, Visibility) {

                        PropertyField.attachTo(config, {
                            property: propertyDetails,
                            vertexProperty: vertexProperty,
                            value: previousValue,
                            predicates: false,
                            tooltip: {
                                html: true,
                                title:
                                    '<strong>' +
                                    i18n('justification.field.tooltip.title') +
                                    '</strong><br>' +
                                    i18n('justification.field.tooltip.subtitle'),
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

            clonedSelection.one(TRANSITION_END, function() {
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
            } else if (data.values.length === 2) {
                // Must be geoLocation
                this.currentValue = 'point(' + data.values.join(',') + ')';
            } else if (data.values.length === 3) {
                this.currentValue = JSON.stringify({
                    description: data.values[0],
                    latitude: data.values[1],
                    longitude: data.values[2]
                });
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
                (this.settingVisibility ||
                 (((_.isString(value) && value.length) || value)))) {

                this.trigger('addProperty', {
                    isEdge: F.vertex.isEdge (this.attr.data),
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
