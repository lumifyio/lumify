
define([
    'flight/lib/component',
    'tpl!./diff',
    'service/ontology',
    'util/formatters'
], function(defineComponent, template, OntologyService, formatters) {
    'use strict';

    return defineComponent(Diff);

    function Diff() {

        var ontologyService = new OntologyService();

        this.defaultAttrs({
            publishSelector: 'button'
        })

        this.after('initialize', function() {
            var self = this;

            ontologyService.properties()
                .done(function(properties) {
                    var formatValue = function(name, change) {
                        var value = change.value;
                        switch (properties.byTitle[name].dataType) {
                            case 'geoLocation':
                                value = [change.latitude, change.longitude].join(', ')
                                break;
                            case 'date':
                                value = formatters.date.dateString(value);
                                break;
                        }

                        return value;
                    };

                    self.$node.html(template({
                        diffs:self.attr.diffs,
                        formatValue: formatValue
                    }));

                    self.on('click', {
                        publishSelector: self.onPublish
                    })
                    self.on('diffsChanged', function(event, data) {
                        var scroll = self.$node.find('.diffs-list'),
                            previousScroll = scroll.scrollTop();

                        self.$node.html(template({
                            diffs: data.diffs,
                            formatValue: formatValue
                        }));

                        self.$node.find('.diffs-list').scrollTop(previousScroll);
                    })
                });
        });

        this.onPublish = function() {
            var $target = $(event.target),
                cls = 'btn-success';

            if ($target.closest('thead').length) {
                var allButtons = $target.closest('table').find('td button');
                if ($target.hasClass(cls)) {
                    allButtons.removeClass(cls);
                } else {
                    allButtons.addClass(cls);
                }
            }
            $target.toggleClass(cls)
        };
    }
});

