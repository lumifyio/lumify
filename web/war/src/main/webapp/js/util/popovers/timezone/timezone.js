
define([
    'flight/lib/component',
    '../withPopover',
    'util/formatters',
    'jstz'
], function(
    defineComponent,
    withPopover,
    F,
    jstz) {
    'use strict';

    return defineComponent(Timezone, withPopover);

    function Timezone() {

        this.defaultAttrs({
            acceptSelector: '.btn-primary',
            cancelSelector: '.btn-default',
        });

        this.before('initialize', function(node, config) {
            var timezone = config.timezone || jstz.determine().name();
            config.template = 'timezone/template';

            this.after('setupWithTemplate', function() {
                var self = this;

                this.on(this.popover, 'click', {
                    acceptSelector: this.onAccept,
                    cancelSelector: this.onCancel
                });

                var timezoneToInfo = {};
                this.popover.find('input').val(timezone).typeahead({
                    items: 10000,
                    source: _.chain(jstz.olson.timezones)
                            .keys()
                            .map(function(p) {
                                var components = p.split(','),
                                    offsetMinutes = parseInt(components[0], 10),
                                    name = jstz.olson.timezones[p];

                                timezoneToInfo[name] = {
                                    selected: timezone === name,
                                    name: name,
                                    offset: F.timezone.offsetDisplay(offsetMinutes),
                                    dst: components.length >= 2 && components[1] === '1',
                                };
                                return name;
                            })
                            .value(),
                    matcher: function(name) {
                        if (!this.query) {
                            return true;
                        }
                        if (this.query === timezone) {
                            return true;
                        }

                        return Object.getPrototypeOf(this).matcher.apply(this, arguments);
                    },
                    highlighter: function(name) {
                        var info = timezoneToInfo[name];
                        return '<span class="offset">' + info.offset + '</span>' +
                            '<header title="' + info.name + '">' +
                                name + (info.dst ? ' *' : '') +
                            '</header>';
                    },
                    updater: function(name) {
                        self.trigger('selectTimezone', {
                            name: name
                        });
                        self.teardown();
                    }
                });
                var input = this.popover.find('input'),
                    typeahead = input.data('typeahead');

                typeahead.lookup = function(event) {
                        var items;

                        this.query = this.$element.val();

                        items = $.isFunction(this.source) ?
                            this.source(this.query, $.proxy(this.process, this)) :
                            this.source;

                        return items ? this.process(items) : this
                    };
                typeahead.process = function(items) {
                        var self = this;

                        items = $.grep(items, function(item) {
                            return self.matcher(item)
                        });

                        //items = this.sorter(items)

                        this.render(items.slice(0)).show();

                        var selected = _.find(this.$menu.find('li').toArray(), function(i) {
                            return $(i).data('value') === timezone;
                        });
                        if (selected) {
                            _.defer(function() {
                                $(selected).addClass('active').get(0).scrollIntoView();
                            });
                        }

                        return this;
                    };

                typeahead.lookup().show();
                typeahead.$element[0].select();
                this.positionDialog();
                /*
                var active = this.popover.find('.active');
                if (active.length) {
                    active.get(0).scrollIntoView();
                }
                */
            });
        });

        this.onAccept = function(e) {
        };

        this.onCancel = function() {
            this.teardown();
        };
    }
});
