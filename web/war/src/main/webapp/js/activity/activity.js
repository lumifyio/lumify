
define([
    'flight/lib/component',
    'hbs!./template',
    'd3',
    './formatters',
    'util/formatters'
], function(
    defineComponent,
    template,
    d3,
    activityFormatters,
    F) {
    'use strict';

    return defineComponent(Activity);

    function Activity() {

        this.defaultAttrs({
            typesSelector: '.types'
        })

        this.after('initialize', function() {
            this.user = window.currentUser;
            this.$node.html(template({}));
            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on(document, 'socketMessage', this.onSocketMessage);
        });

        this.onSocketMessage = function(event, message) {
            if (message && message.type === 'longRunningProcessChange') {
                console.log(message.data)
            }
        };

        this.onToggleDisplay = function(event, data) {
            if (data.name === 'activity' && this.$node.closest('.visible').length) {
                this.update(this.user.longRunningProcesses);
            }
        };

        this.update = function(processes) {
            console.log('Updating', processes)

            var data = _.chain(processes)
                .groupBy('type')
                .pairs()
                .value();

            d3.select(this.select('typesSelector').get(0))
                .selectAll('section')
                .data(data)
                .call(function() {
                    this.enter()
                        .append('section')
                        .call(function() {
                            this.attr('class', 'collapsible expanded')
                            this.append('h1')
                                .attr('class', 'collapsible-header')
                                .append('strong')
                            this.append('ul')
                        });

                    this.select('.collapsible-header strong').text(function(pair) {
                        return i18n('activity.tasks.type.' + pair[0]);
                    })

                    this.select('ul')
                        .selectAll('li')
                        .data(function(pair) {
                            return pair[1];
                        }, function(process) {
                            return process.id;
                        })
                        .call(function() {
                            this.enter()
                                .append('li')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'type-container')
                                    this.append('div')
                                        .attr('class', 'progress-container')
                                        .call(function() {
                                            this.append('button')
                                            this.append('div').attr('class', 'progress')
                                                .append('div')
                                                    .attr('class', 'bar')
                                        })
                                    this.append('div')
                                        .attr('class', 'progress-description')
                                })

                            this.select('.type-container').each(function() {
                                var datum = d3.select(this).datum();

                                if (datum.type && datum.type in activityFormatters) {
                                    activityFormatters[datum.type](this, datum);
                                } else if (datum.type) {
                                    console.warn('No activity formatter for ', datum.type);
                                } else {
                                    console.warn('No activity process doesn\'t contain a type property');
                                }
                            });

                            this.select('.bar').style('width', function(process) {
                                return (process.progress * 100) + '%';
                            });

                            this.select('.progress-description').text(function(process) {
                                if (process.endTime) {
                                    return 'Finished ' + F.date.relativeToNow(process.endTime);
                                } else if (process.startTime) {
                                    return 'Started ' + F.date.relativeToNow(process.startedTime);
                                } else {
                                    return 'Enqueued ' + F.date.relativeToNow(process.enqueueTime);
                                }
                            });
                        })
                });
        }

    }
});
