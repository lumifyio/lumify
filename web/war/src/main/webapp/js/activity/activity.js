
define([
    'flight/lib/component',
    'hbs!./template',
    './handlers',
    'd3',
    'configuration/plugins/activity/plugin',
    'util/formatters',
    'util/withCollapsibleSections',
    'service/longRunningProcess'
], function(
    defineComponent,
    template,
    builtinHandlers,
    d3,
    ActivityHandlers,
    F,
    withCollapsibleSections,
    LongRunningProcessService) {
    'use strict';

    var AUTO_UPDATE_INTERVAL_SECONDS = 60,
        longRunningProcessService = new LongRunningProcessService();

    return defineComponent(Activity, withCollapsibleSections);

    function Activity() {

        this.defaultAttrs({
            typesSelector: '.types',
            deleteButtonSelector: 'button.delete',
            cancelButtonSelector: 'button.cancel',
            noActivitySelector: '.no-activity'
        })

        this.before('teardown', function() {
            clearInterval(this.autoUpdateTimer);
        });

        this.after('initialize', function() {
            ActivityHandlers.registerActivityHandlers(builtinHandlers);

            this.removedTasks = {};
            this.$node.html(template({}));

            this.tasks = window.currentUser && window.currentUser.longRunningProcesses || [];
            this.tasksById = _.indexBy(this.tasks, 'id');

            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on(document, 'socketMessage', this.onSocketMessage);

            this.on('click', {
                deleteButtonSelector: this.onDelete,
                cancelButtonSelector: this.onCancel
            })
        });

        this.onCancel = function(event) {
            this.callServiceMethodRemove(event, 'cancel');
        };

        this.onDelete = function(event) {
            this.callServiceMethodRemove(event, 'delete');
        };

        this.callServiceMethodRemove = function(event, name) {
            var self = this,
                $button = $(event.target),
                processId = $button.closest('li').data('processId');

            if (processId) {
                $button.addClass('loading');
                longRunningProcessService[name](processId)
                    .always(function() {
                        $button.removeClass('loading');
                    })
                    .done(function() {
                        self.removedTasks[processId] = true;
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

                if (this.removedTasks[task.id]) {
                    return;
                }

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
                this.startAutoUpdate();
            } else {
                this.pauseAutoUpdate();
            }
        };

        this.pauseAutoUpdate = function() {
            clearInterval(this.autoUpdateTimer);
        };

        this.startAutoUpdate = function() {
            this.autoUpdateTimer = setInterval(this.update.bind(this), AUTO_UPDATE_INTERVAL_SECONDS * 1000)
        };

        this.update = function() {
            if (!this.isOpen) {
                return;
            }

            console.log('updating', this.tasks)

            var data = _.chain(this.tasks)
                    .filter(function(p) {
                        return !p.canceled;
                    })
                    .groupBy('type')
                    .pairs()
                    .value(),
                uniqueTypes = _.chain(data)
                    .map(function(p) {
                        return p[0];
                    })
                    .unique()
                    .map(function(type) {
                        var byType = ActivityHandlers.activityHandlersByType;
                        if (type in byType) {
                            if (byType[type].finishedComponentPath) {
                                return byType[type].finishedComponentPath;
                            }

                            return console.warn('No finishedComponentPath property for activity handler', byType[type]);
                        }

                        console.warn('No activity handler registered for type:', type);
                    })
                    .compact()
                    .value();

            require(uniqueTypes, this.updateWithDependencies.bind(this, data, uniqueTypes));
        };

        this.updateWithDependencies = function(data, requirePaths) {
            var finishedComponents = _.object(requirePaths, Array.prototype.slice.call(arguments, 2))

            this.select('noActivitySelector').toggle(data.length === 0);

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
                                .call(function() {
                                    this.append('strong');
                                    this.append('span').attr('class', 'badge');
                                })
                            this.append('ul').attr('class', 'collapsible-section')
                        });
                    this.exit().remove();

                    this.select('.collapsible-header strong').text(function(pair) {
                        return i18n('activity.tasks.type.' + pair[0]);
                    })

                    this.select('.collapsible-header .badge').text(function(pair) {
                        return F.number.pretty(pair[1].length);
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
                                        .attr('class', 'actions-container')
                                        .call(function() {
                                            this.append('div')
                                                .attr('class', 'actions-plugin')
                                            this.append('button')
                                                .attr('class', 'btn btn-mini btn-danger delete')
                                                .text('Delete')
                                        })
                                    this.append('div')
                                        .attr('class', 'type-container')
                                    this.append('div')
                                        .attr('class', 'progress-container')
                                        .call(function() {
                                            this.append('button').attr('class', 'cancel')
                                            this.append('div').attr('class', 'progress')
                                                .append('div')
                                                    .attr('class', 'bar')
                                        })
                                    this.append('div')
                                        .attr('class', 'progress-description')
                                });
                            this.exit().remove();

                            this.attr('data-process-id', _.property('id'));
                            this.attr('class', function(process) {
                                if (process.cancelled || process.endTime) {
                                    return 'finished';
                                }
                            })

                            this.select('.actions-plugin').each(function() {
                                var datum = d3.select(this).datum(),
                                    handler = ActivityHandlers.activityHandlersByType[datum.type],
                                    Component = finishedComponents[handler.finishedComponentPath];

                                Component.attachTo(this, {
                                    process: datum
                                });
                            })

                            this.select('.type-container').each(function() {
                                var datum = d3.select(this).datum();

                                if (datum.type && datum.type in ActivityHandlers.activityHandlersByType) {
                                    ActivityHandlers.activityHandlersByType[datum.type].titleRenderer(this, datum);
                                } else if (datum.type) {
                                    console.warn('No activity formatter for ', datum.type);
                                } else {
                                    console.warn('No activity process doesn\'t contain a type property');
                                }
                            });

                            this.select('.bar').style('width', function(process) {
                                return ((process.progress || 0) * 100) + '%';
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
