define([
    'flight/lib/component',
    'hbs!./offlineOverlayTpl',
    'util/formatters'
], function(
    defineComponent,
    template,
    F) {
    'use strict';

    return defineComponent(Overlay);

    function Overlay() {

        this.after('teardown', function() {
            this.overlay.remove();
        });

        this.after('initialize', function() {
            var self = this;
            $(function() {
                self.overlay = $(template())
                    .appendTo(document.body);

                self.$lastCheck = self.overlay.find('.last-checked');
                setInterval(self.updateLastCheck.bind(self), 30000)
                self.poll(1);
                self.updateLastCheck();

            });
        });

        this.updateLastCheck = function() {
            if (this.lastCheck) {
                this.$lastCheck.text(
                    i18n('lumify.offline_overlay.last_check', F.date.relativeToNow(this.lastCheck))
                ).show();
            } else {
                this.$lastCheck.hide();
            }
        };

        this.poll = function(checkNumber) {
            var self = this;

            this.lastCheck = F.date.utc(new Date()).getTime();
            $.get('ping.html')
                .done(function() {
                    window.location.reload();
                })
                .fail(function() {
                    var waitTimeSeconds = Math.min(120, Math.pow(2, checkNumber) - 1);
                    setTimeout(self.poll.bind(self, checkNumber + 0.5), waitTimeSeconds * 1000);
                });
        }

    }
});
