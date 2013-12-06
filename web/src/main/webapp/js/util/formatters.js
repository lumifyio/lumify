
define(['sf'], function() {
    'use strict';

    return {
        string: {
            plural: function(count, plural, singular) {
                switch(count) {
                    case 0: return 'No ' + plural;
                    case 1: return '1 ' + singular;
                    default: return count + ' ' + plural;
                }
            }
        },
        date: {
            relativeToNow: function(date) {
                date = _.isDate(date) ? date : new Date(date);

                var span = new sf.TimeSpan(Date.now() - date),
                    time = '';

                if (span.years > 1) {
                    time = sf("{0:^y 'years'}", span);
                } else if (span.years === 1) {
                    time = 'a year';
                } else if (span.months > 1) {
                    time = sf("{0:^M 'months'}", span);
                } else if (span.months === 1) {
                    time = 'a month';
                } else if (span.days > 1) {
                    time = sf("{0:^d 'days'}", span);
                } else if (span.days === 1) {
                    time = 'a day';
                } else if (span.hours > 1) {
                    time = sf("{0:^h 'hours'}", span);
                } else if (span.hours === 1) {
                    time = 'an hour';
                } else if (span.minutes > 1) {
                    time = sf("{0:^m 'minutes'}", span);
                } else if (span.minutes === 1) {
                    time = 'a minute';
                } else {
                    time = 'moments';
                }

                return time + ' ago';
            }
        }
    }
});
