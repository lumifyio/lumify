
define([
    'flight/lib/component',
    './image/image',
    '../properties/properties',
    '../relationships/relationships',
    '../comments/comments',
    '../withTypeContent',
    '../withHighlighting',
    '../toolbar/toolbar',
    'tpl!./entity',
    'util/vertex/formatters',
    'util/withDataRequest',
    'detail/dropdowns/propertyForm/propForm'
], function(defineComponent,
    Image,
    Properties,
    Relationships,
    Comments,
    withTypeContent,
    withHighlighting,
    Toolbar,
    template,
    F,
    withDataRequest,
    PropertyForm) {
    'use strict';

    var MAX_RELATIONS_TO_DISPLAY; // Loaded with configuration parameters

    return defineComponent(Entity, withTypeContent, withHighlighting, withDataRequest);

    function defaultSort(x,y) {
        return x === y ? 0 : x < y ? -1 : 1;
    }

    function Entity(withDropdown) {

        this.defaultAttrs({
            glyphIconSelector: '.entity-glyphIcon',
            propertiesSelector: '.properties',
            relationshipsSelector: '.relationships',
            commentsSelector: '.comments',
            titleSelector: '.entity-title',
            toolbarSelector: '.comp-toolbar',
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on('addImage', this.onAddImage);

            this.loadEntity();
        });

        this.onAddImage = function(event, data) {
            this.select('glyphIconSelector').trigger('setImage', data);
        };

        this.onVerticesUpdated = function(event, data) {
            var matching = _.findWhere(data.vertices, { id: this.attr.data.id });

            if (matching) {
                this.select('titleSelector').text(F.vertex.title(matching))
                    .next('.subtitle')
                    .text(F.vertex.concept(matching).displayName);

                this.attr.data = matching;
            }
        };

        this.loadEntity = function() {
            var vertex = this.attr.data;

            this.trigger('finishedLoadingTypeContent');

            this.vertex = vertex;
            this.attr.data = vertex;
            this.$node.html(template({
                vertex: vertex,
                F: F
            }));

            Toolbar.attachTo(this.select('toolbarSelector'), {
                toolbar: [
                    {
                        title: i18n('detail.toolbar.open'),
                        submenu: [
                            Toolbar.ITEMS.FULLSCREEN,
                            this.sourceUrlToolbarItem()
                        ]
                    },
                    {
                        title: i18n('detail.toolbar.add'),
                        submenu: [
                            Toolbar.ITEMS.ADD_PROPERTY,
                            Toolbar.ITEMS.ADD_IMAGE,
                            Toolbar.ITEMS.ADD_COMMENT
                        ]
                    },
                    {
                        icon: 'img/glyphicons/white/glyphicons_157_show_lines@2x.png',
                        right: true,
                        submenu: [
                            Toolbar.ITEMS.AUDIT,
                            _.extend(Toolbar.ITEMS.DELETE_ITEM, {
                                title: i18n('detail.toolbar.delete.entity'),
                                subtitle: i18n('detail.toolbar.delete.entity.subtitle')
                            })
                        ]
                    }
                ]
            });

            Image.attachTo(this.select('glyphIconSelector'), {
                data: vertex
            });

            Properties.attachTo(this.select('propertiesSelector'), {
                data: vertex
            });

            Relationships.attachTo(this.select('relationshipsSelector'), {
                data: vertex
            });

            Comments.attachTo(this.select('commentsSelector'), {
                vertex: vertex
            });

            this.updateEntityAndArtifactDraggables();
            this.updateText();
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if (!$target.is('.add-new-properties,button') &&
                $target.parents('.underneath').length === 0) {
                PropertyForm.teardownAll();
            }

            if ($target.is('.vertex, .artifact')) {
                var id = $target.data('vertexId');
                this.trigger('selectObjects', { vertexIds: [id] });
                evt.stopPropagation();
            }
        };

    }
});
