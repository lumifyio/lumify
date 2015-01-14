
define([
    'flight/lib/component',
    'tpl!./controls'
], function(defineComponent, template) {
    'use strict';

    var PAN_INACTIVE_AREA = 8,
        PAN_AREA_DRAG_SIZE = 75,
        PAN_SPEED = 10,
		PAN_MIN_PERCENT_SPEED = 0.25,
		PAN_DISTANCE = 10;

    return defineComponent(Controls);

    function Controls() {

        this.defaultAttrs({
            pannerSelector: '.panner',
            buttonSelector: 'button',
            toggleOptionsSelector: '.options button',
            optionsContainerSelector: '.options-container'
        })

        this.after('initialize', function() {
            var self = this,
                hasOptions = !!this.attr.optionsComponentPath;

            this.$node.html(template({
                options: hasOptions
            }));

            if (hasOptions) {
                require([this.attr.optionsComponentPath], function(Options) {
                    Options.attachTo(self.$node.find('.options-container'), self.attr.optionsAttributes);
                });
            }

            this.attachEvents();
        });

        this.attachEvents = function() {
            this.on('mousedown', {
                pannerSelector: this.onPannerMouseDown
            });
            this.on('mousedown mouseup', {
                buttonSelector: this.onButton
            });
            this.on('click', {
                toggleOptionsSelector: this.onToggleOptions
            });
            this.visible = false;
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'click', this.hideOptions);

            $(window).on('mouseup blur', this.handleUp.bind(this));
        };

        this.hideOptions = function(event) {
            if (this.visible && $(event.target).closest(this.$node).length === 0) {
                this.select('optionsContainerSelector').hide();
                this.select('toggleOptionsSelector').removeClass('active');
                this.visible = false;
            }
        };

        this.onToggleOptions = function(event) {
            this.visible = !this.visible;
            event.stopPropagation();
            event.target.blur();
            this.select('optionsContainerSelector').toggle();
            $(event.target).toggleClass('active');
        };

        this.onGraphPaddingUpdated = function(e, data) {
            var rightPadding = data.padding.r || 0;
            this.$node.css({ right: rightPadding + 'px' });
        };

        this.onButton = function(e) {
            clearInterval(this.buttonRepeat);

            if (e.type === 'mouseup') return;

            var $target = $(e.target),
                eventName = $target.data('event'),
                repeat = $target.data('repeat');

            if (!eventName) {
                return;
            }

            this.trigger(eventName);

            if (repeat) {
                this.buttonRepeat = setInterval(function() {
                    this.trigger(eventName);
                }.bind(this), PAN_SPEED);
            }
        };

        this.onPannerMouseDown = function(e) {
            this.trigger('startPan');
            this.handleMove(e);
            $(window).on('mousemove.panningControls', this.handleMove.bind(this))
        };

        this.handleMove = function(e) {
            e.preventDefault();
            e.stopPropagation();
            clearInterval(this.panInterval);

            if (!this.panner) {
                this.panner = this.select('pannerSelector');
            }

            var pan = eventToPan(this.panner, e);
            if (isNaN(pan.x) || isNaN(pan.y)){
                return;
            }

            var self = this;
            this.panInterval = setInterval(function() {
                self.trigger('pan', { pan: pan });
            }, PAN_SPEED);
        };

        this.handleUp = function(e) {
            this.trigger('endPan');
            clearInterval(this.panInterval);
            $(window).off('mousemove.panningControls');
        };
    }

    // Ported from jquery.cytoscape-panzoom plugin
    function eventToPan(panner, e) {
        var v = {
                x: e.originalEvent.pageX - panner.offset().left - panner.width() / 2,
                y: e.originalEvent.pageY - panner.offset().top - panner.height() / 2
            },
            r = PAN_AREA_DRAG_SIZE,
            d = Math.sqrt(v.x * v.x + v.y * v.y),
            percent = Math.min(d / r, 1);

        if (d < PAN_INACTIVE_AREA) {
            return {
                x: NaN,
                y: NaN
            };
        }

        v = {
            x: v.x / d,
            y: v.y / d
        };

        percent = Math.max(PAN_MIN_PERCENT_SPEED, percent);

        var vnorm = {
            x: -1 * v.x * (percent * PAN_DISTANCE),
            y: -1 * v.y * (percent * PAN_DISTANCE)
        };

        return vnorm;
    }
});
