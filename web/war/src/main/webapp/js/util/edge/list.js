define([
    'flight/lib/component',
    'd3',
    'util/formatters',
    'util/vertex/justification/viewer'
], function(
    defineComponent,
    d3,
    F,
    JustificationViewer) {
    'use strict';

    return defineComponent(EdgeList);

    function EdgeList() {

        this.defaultAttrs({
            edgeItemSelector: '.edge'
        });

        this.after('initialize', function() {
            this.on('click', {
                edgeItemSelector: this.onSelectEdge
            });

            this.$list = $('<ul>')
                .addClass('nav nav-list')
                .appendTo(this.$node.addClass('vertex-list vertices-list'))
                .get(0);
            this.render();
        });

        this.onSelectEdge = function(event) {
            this.trigger('selectObjects', {
                edgeIds: $(event.target).closest('li')
                    .addClass('active')
                    .siblings('.active')
                        .removeClass('active')
                        .end()
                    .data('edgeId')
            });
        };

        this.render = function() {
            d3.select(this.$list)
                .selectAll('li.edge')
                .data(this.attr.edges)
                .call(function() {
                    this.enter()
                        .append('li')
                            .attr('class', 'edge vertex-item')
                        .append('a')
                        .call(function() {
                            this.append('span');
                            this.append('div').attr('class', 'subtitle')
                        })

                    this.attr('data-edge-id', function(d) {
                        return d.id;
                    });

                    this.select('span').each(function() {
                        var $this = $(this),
                            d = d3.select(this).datum(),
                            justification = _.findWhere(d.properties, { name: '_justificationMetadata' }),
                            sourceInfo = _.findWhere(d.properties, { name: '_sourceMetadata' });

                        $this.teardownAllComponents();

                        if (justification || sourceInfo) {
                            JustificationViewer.attachTo($this, {
                                justificationMetadata: justification && justification.value,
                                sourceMetadata: sourceInfo && sourceInfo.value
                            });
                        } else {
                            $this.text(i18n('detail.multiple.edge.justification.novalue'));
                        }
                    });

                    this.select('.subtitle').text(function(d) {
                        var propertyDate = _.findWhere(d.properties, { name: 'http://lumify.io#createDate' }),
                            propertyBy = _.findWhere(d.properties, { name: 'http://lumify.io#modifiedBy' });

                        return i18n('detail.multiple.edge.created.display',
                            propertyBy ?
                            lumifyData.currentUser.displayName :
                                i18n('detail.multiple.edge.created.by.unknown'),
                            propertyDate ?
                                F.date.relativeToNow(F.date.utc(propertyDate.value)) :
                                i18n('detail.multiple.edge.created.date.unknown')
                        );
                    });

                    this.exit().remove();
                });
        }

    }
});
