
define(['sf'], function() {
    'use strict';

    return {
        date: {
            relativeToNow: function(date) {
                date = _.isDate(date) ? date : new Date(date);

                var span = new sf.TimeSpan(Date.now() - date),
                    time = '';

                if (span.seconds < 30 && span.minutes < 1) {
                    time = 'moments';
                } else if (span.minutes < 2) {
                    time = 'a minute';
                } else if (span.hours < 1) {
                    time = sf("{0:^m 'minutes'}", span);
                } else if (span.hours === 1 && span.days < 1) {
                    time = 'an hour';
                } else if (span.days < 1) {
                    time = sf("{0:^h 'hours'}", span);
                } else if (span.days === 1 && span.months < 1) {
                    time = 'a day';
                } else if (span.months < 1) {
                    time = sf("{0:^d 'days'}", span);
                } else if (span.months === 1 && span.years < 1) {
                    time = 'a month';
                } else if (span.years < 1) {
                    time = sf("{0:^M 'months'}", span);
                } else if (span.years === 1) {
                    time = 'a year';
                } else {
                    time = sf("{0:^y 'years'}", span);
                }

                return time + ' ago';
            }
        }
    }
});
