
define([
    'flight/lib/component',
    'hbs!./template',
    './handlers',
    'd3',
    'configuration/plugins/activity/plugin',
    'util/formatters',
    'util/withCollapsibleSections',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    builtinHandlers,
    d3,
    ActivityHandlers,
    F,
    withCollapsibleSections,
    withDataRequest) {
    'use strict';

    var AUTO_UPDATE_INTERVAL_SECONDS = 60; // For updating relative times

    return defineComponent(Activity, withCollapsibleSections, withDataRequest);

    function processIsIndeterminate(process) {
        var handler = ActivityHandlers.activityHandlersByType[process.type];
        return (handler.kind === 'eventWatcher' || handler.indeterminateProgress);
    }

    function processIsFinished(process) {
        return process.cancelled || process.endTime;
    }

    function processShouldAutoDismiss(process) {
        var handler = ActivityHandlers.activityHandlersByType[process.type];
        return handler.autoDismiss === true;
    }

    function processAllowCancel(process) {
        var handler = ActivityHandlers.activityHandlersByType[process.type];
        return (handler.kind === 'longRunningProcess' || handler.allowCancel === true);
    }

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

            this.tasks = lumifyData.currentUser && lumifyData.currentUser.longRunningProcesses || [];
            this.tasksById = _.indexBy(this.tasks, 'id');

            this.throttledUpdate = _.debounce(this.update.bind(this), 100);
            this.updateEventWatchers();

            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on(document, 'longRunningProcessChanged', this.onLongRunningProcessChanged);
            this.on(document, 'showActivityDisplay', this.onShowActivityDisplay);
            this.on(document, 'activityHandlersUpdated', this.onActivityHandlersUpdated);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.on('click', {
                deleteButtonSelector: this.onDelete,
                cancelButtonSelector: this.onCancel
            })
        });

        this.onShowActivityDisplay = function(event) {
            var visible = this.$node.closest('.visible').length > 0;

            if (!visible) {
                this.trigger('menubarToggleDisplay', { name: 'activity' });
            }
        };

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
                this.dataRequest('longRunningProcess', name, processId)
                    .finally(function() {
                        $button.removeClass('loading');
                    })
                    .done(function() {
                        self.removedTasks[processId] = true;
                        var task = self.tasksById[processId];
                        delete self.tasksById[processId];
                        self.tasks.splice(self.tasks.indexOf(task), 1);
                        self.update().then(function() {
                            if (self.currentTaskCount === 0 && name === 'delete') {
                                _.delay(function() {
                                    var visible = self.$node.closest('.visible').length > 0;
                                    if (visible) {
                                        self.trigger('menubarToggleDisplay', { name: 'activity' });
                                    }
                                }, 250)
                            }
                        })
                    });
            }
        };

        this.onLongRunningProcessChanged = function(event, data) {
            var task = data.process;

            this.addOrUpdateTask(task);
            this.update();
        };

        this.onVerticesUpdated = function(event, data) {
            this.throttledUpdate();
        };

        this.onActivityHandlersUpdated = function(event) {
            this.updateEventWatchers();
            this.update();
        };

        this.updateEventWatchers = function() {
            var self = this,
                namespace = '.ACTIVITY_HANDLER',
                eventWatchers = ActivityHandlers.activityHandlersByKind.eventWatcher;

            this.off(document, namespace);
            eventWatchers.forEach(function(activityHandler) {
                self.on(
                    document,
                    activityHandler.eventNames[0] + namespace,
                    self.onActivityHandlerStart.bind(self, activityHandler)
                );
                self.on(
                    document,
                    activityHandler.eventNames[1] + namespace,
                    self.onActivityHandlerEnd.bind(self, activityHandler)
                );
            });
        };

        this.addOrUpdateTask = function(task) {
            var existingTask = this.tasksById[task.id];

            if (this.removedTasks[task.id]) {
                return;
            }

            if (existingTask) {
                this.tasks.splice(this.tasks.indexOf(existingTask), 1, task);
            } else {
                this.tasks.splice(0, 0, task);
            }

            this.tasksById[task.id] = task;
        };

        this.onActivityHandlerStart = function(handler, event, data) {
            this.addOrUpdateTask({
                id: handler.type,
                type: handler.type,
                enqueueTime: Date.now(),
                startTime: Date.now(),
                eventData: data
            });
            this.update();
        };

        this.onActivityHandlerEnd = function(handler, event, data) {
            this.addOrUpdateTask({
                id: handler.type,
                type: handler.type,
                endTime: Date.now(),
                eventData: data
            });
            this.update();
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

        this.notifyActivityMonitors = function(tasks) {
            var count = _.reduce(tasks, function(count, task) {
                if (processIsFinished(task)) {
                    return count;
                }

                return count + 1;
            }, 0);

            if (!this.previousCountForNotify || count !== this.previousCountForNotify) {
                this.trigger('activityUpdated', { count: count });
            }
        }

        this.update = function() {
            var self = this,
                tasks = _.chain(this.tasks)
                    .filter(function(p) {
                        if (p.canceled) {
                            return false;
                        }

                        if (processIsFinished(p) && processShouldAutoDismiss(p)) {
                            return false;
                        }

                        return true;
                    })
                    .sortBy(function(p) {
                        return p.enqueueTime * -1;
                    })
                    .value(),
                data = _.chain(tasks)
                    .groupBy('type')
                    .pairs()
                    .sortBy(function(pair) {
                        return pair[0].toLowerCase();
                    })
                    .value();

            this.currentTaskCount = tasks.length;
            this.notifyActivityMonitors(tasks);

            if (!this.isOpen) {
                return;
            }

            var uniqueTypes = _.chain(data)
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
                        return;
                    }

                    console.warn('No activity handler registered for type:', type);
                })
                .compact()
                .value();

            return Promise.require.apply(Promise, uniqueTypes)
                .then(function(deps) {
                    self.updateWithDependencies.apply(self, [data, uniqueTypes].concat(deps))
                })
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
                    this.exit()
                        .each(function() {
                            $('.actions-plugin', this).teardownAllComponents();
                        })
                        .remove();

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
                                                .text(i18n('activity.process.button.dismiss'))
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
                            this.exit()
                                .each(function() {
                                    $('.actions-plugin', this).teardownAllComponents();
                                })
                                .remove();

                            this.attr('data-process-id', _.property('id'));
                            // TODO: add transition to delay this?
                            this.attr('class', function(process) {
                                if (processIsFinished(process)) {
                                    return 'finished';
                                }
                            });

                            this.select('.actions-plugin').each(function() {
                                var datum = d3.select(this).datum(),
                                    handler = ActivityHandlers.activityHandlersByType[datum.type],
                                    componentPath = handler.finishedComponentPath
                                    Component = componentPath && finishedComponents[componentPath];

                                if (Component && datum.endTime) {
                                    Component.attachTo(this, {
                                        process: datum
                                    });
                                } else {
                                    $(this).teardownAllComponents();
                                }
                            });

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
                                if (processIsIndeterminate(process)) {
                                    return '100%';
                                }
                                return ((process.progress || 0) * 100) + '%';
                            });

                            this.select('.progress-container').attr('class', function(process) {
                                var cls = 'progress-container ';
                                if (processAllowCancel(process)) {
                                    return cls;
                                }

                                return cls + 'no-cancel';
                            });

                            this.select('.progress').attr('class', function(process) {
                                var cls = 'progress ';

                                if (processIsIndeterminate(process)) {
                                    return cls + 'active progress-striped';
                                }
                                return cls + ' determinate';
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
