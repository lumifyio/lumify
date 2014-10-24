
define([
    'flight/lib/component',
    'hbs!./template',
    './formatters',
    'd3',
    'util/formatters',
    'service/longRunningProcess'
], function(
    defineComponent,
    template,
    activityFormatters,
    d3,
    F,
    LongRunningProcessService) {
    'use strict';

    var longRunningProcessService = new LongRunningProcessService();

    return defineComponent(Activity);

    function Activity() {

        this.defaultAttrs({
            typesSelector: '.types',
            deleteButtonSelector: '.progress-container button'
        })

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.tasks = window.currentUser && window.currentUser.longRunningProcesses || [];
            this.tasksById = _.indexBy(this.tasks, 'id');

            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on(document, 'socketMessage', this.onSocketMessage);

            this.on('click', {
                deleteButtonSelector: this.onDelete
            })
        });

        this.onDelete = function(event) {
            var self = this,
                processId = $(event.target).closest('li').data('processId');
            if (processId) {
                longRunningProcessService.delete(processId)
                    .done(function() {
                        var task = self.tasksById[processId];
                        delete self.tasksById[processId];
                        self.tasks.splice(self.tasks.indexOf(task), 1);
                        self.update();
                    });
            }
        };

        this.onSocketMessage = function(event, message) {
            if (message && message.type === 'longRunningProcessChange') {
                var task = message.data,
                    existingTask = this.tasksById[task.id];

                if (existingTask) {
                    this.tasks.splice(this.tasks.indexOf(existingTask), 1, task);
                } else {
                    this.tasks.splice(0, 0, task);
                }

                this.tasksById[task.id] = task;

                this.update();
            }
        };

        this.onToggleDisplay = function(event, data) {
            var openingActivity = data.name === 'activity' && this.$node.closest('.visible').length;

            this.isOpen = openingActivity;

            if (openingActivity) {
                this.update();
            }
        };

        this.update = function() {
            if (!this.isOpen) {
                return;
            }

            var processes = this.tasks,
                data = _.chain(processes)
                    .groupBy('type')
                    .pairs()
                    .value();

            d3.select(this.select('typesSelector').get(0))
                .selectAll('section')
                .data(data)
                .order()
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
                    this.exit().remove();

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
                        .order()
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
                                });
                            this.exit().remove();

                            this.attr('data-process-id', _.property('id'));

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
                                var timePrefix = '';

                                if (process.endTime) {
                                    timePrefix = 'Finished ' + F.date.relativeToNow(F.date.utc(process.endTime));
                                } else if (process.startTime) {
                                    timePrefix = 'Started ' + F.date.relativeToNow(F.date.utc(process.startTime));
                                } else {
                                    timePrefix = 'Enqueued ' + F.date.relativeToNow(F.date.utc(process.enqueueTime));
                                }

                                if (process.progressMessage) {
                                    return timePrefix + ' – ' + process.progressMessage;
                                }

                                return timePrefix;
                            });
                        })
                });
        }

    }
});
