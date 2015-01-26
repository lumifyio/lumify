
define([
    'sf',
    'jstz',
    'timezone-js',
    'util/messages',
    'jquery',
    'underscore'
], function(sf, jstz, timezoneJS, i18n) {
    'use strict';

    var BITS_FOR_INDEX = 12,
        BITS_FOR_OFFSET = 32 - BITS_FOR_INDEX,
        classNameIndex = 0,
        toClassNameMap = {},
        fromClassNameMap = {},
        isMac = checkIfMac(),
        isFirefox = ~navigator.userAgent.indexOf('Firefox'),
        keyboardMappings = {
            metaIcons: {
                shift: isMac ? '⇧' : i18n('keyboard.shift'),
                meta: isMac ? '⌘' : i18n('keyboard.ctrl'),
                ctrl: isMac ? '⌃' : i18n('keyboard.ctrl'),
                alt: isMac ? '⌥' : i18n('keyboard.alt')
            },
            charIcons: {
                esc: isMac ? '⎋' : null,
                escape: isMac ? '⎋' : i18n('keyboard.escape'),
                'delete': isMac ? '⌫' : null,
                backspace: isMac ? '⌦' : null,
                up: '↑',
                down: '↓',
                left: '←',
                right: '→',
                drag: isMac ? (isFirefox ? null : '') : null
            }
        };

    timezoneJS.timezone.zoneFileBasePath = 'tz';
    timezoneJS.timezone.defaultZoneFile = ['northamerica'];
    timezoneJS.timezone.init({ async: true });

    function checkIfMac() {
        return ~navigator.userAgent.indexOf('Mac OS X');
    }

    function codeForCharacter(character) {
        var special = {
          backspace: 8, tab: 9, clear: 12,
          enter: 13, 'return': 13,
          esc: 27, escape: 27, space: 32,
          left: 37, up: 38,
          right: 39, down: 40,
          del: 46, 'delete': 46,
          home: 36, end: 35,
          pageup: 33, pagedown: 34,
          ',': 188, '.': 190, '/': 191,
          '`': 192, '-': 189, '=': 187,
          ';': 186, '\'': 222,
          '[': 219, ']': 221, '\\': 220
        };

        // Set delete to equal backspace on mac
        if (isMac) {
            special.del = special['delete'] = special.backspace;
        }

        return special[character.toLowerCase()] || character.toUpperCase().charCodeAt(0);
    }

    function decimalAdjust(type, value, exp) {
		// If the exp is undefined or zero...
		if (typeof exp === 'undefined' || parseFloat(exp) === 0) {
			return Math[type](value);
		}
		value = parseFloat(value);
		exp = parseFloat(exp);
		// If the value is not a number or the exp is not an integer...
		if (isNaN(value) || !(typeof exp === 'number' && exp % 1 === 0)) {
			return NaN;
		}
		// Shift
		value = value.toString().split('e');
		value = Math[type](parseFloat(value[0] + 'e' + (value[1] ? (parseFloat(value[1]) - exp) : -exp)));
		// Shift back
		value = value.toString().split('e');
		return parseFloat(value[0] + 'e' + (value[1] ? (parseFloat(value[1]) + exp) : exp));
	}

    var FORMATTERS = {

        number: {
            pretty: function(number) {
                if (_.isString(number)) {
                    number = parseFloat(number);
                }
                if (_.isNumber(number)) {
                    return sf('{0:#.##}', number)
                        .split('').reverse().join('')
                        .replace(/(\d{3}(?=\d))/g, '$1,')
                        .split('').reverse().join('');
                }

                return '';
            },
            prettyApproximate: function(number) {
                if (number >= 1000000000000) {
                    return (decimalAdjust('round', number / 1000000000000, -1) + i18n('numbers.trillion_suffix'));
                } else if (number >= 1000000000) {
                    return (decimalAdjust('round', number / 1000000000, -1) + i18n('numbers.billion_suffix'));
                } else if (number >= 1000000) {
                    return (decimalAdjust('round', number / 1000000, -1) + i18n('numbers.million_suffix'));
                } else if (number >= 1000) {
                    return (decimalAdjust('round', number / 1000, -1) + i18n('numbers.thousand_suffix'));
                } else return FORMATTERS.number.pretty(number);
            },
            percent: function(number) {
                if (_.isString(number)) {
                    number = parseFloat(number);
                }
                return Math.round(number * 100) + '%';
            },
            /**
             * Split 32-bit integers into 12-bit index, 20-bit offset
             */
            offsetValues: function(value) {
                var offsetMask = (1 << BITS_FOR_INDEX) - 1;

                return {
                    index: value >> BITS_FOR_OFFSET,
                    offset: value & offsetMask
                };
            },
            /**
             * Combine 12-bit index, 20-bit offset into 32-bit integer
             */
            compactOffsetValues: function(index, offset) {
                return (index << BITS_FOR_OFFSET) | offset;
            },
            heading: function(value) {
                if (_.isUndefined(value)) {
                    return;
                }

                var inRange = value % 360;
                return i18n('field.heading.' + [
                    'north',
                    'northeast',
                    'east',
                    'southeast',
                    'south',
                    'southwest',
                    'west',
                    'northwest',
                ][Math.round(inRange / 45) % 8]) + ' ' + FORMATTERS.number.pretty(inRange) + '°';
            }
        },

        'boolean': {
            pretty: function(bool) {
                return bool ? i18n('boolean.true') : i18n('boolean.false');
            }
        },

        bytes: {
            pretty: function(bytes, precision) {
                var k = 1024,
                    m = k * 1024,
                    g = m * 1024,
                    t = g * 1024;

                precision = _.isUndefined(precision) ? 1 : precision;

                if ((bytes >= 0) && (bytes < k)) {
                    return bytes + ' ' + i18n('bytes.suffix');

                } else if ((bytes >= k) && (bytes < m)) {
                    return (bytes / k).toFixed(precision) + ' ' + i18n('bytes.kilo');

                } else if ((bytes >= m) && (bytes < g)) {
                    return (bytes / m).toFixed(precision) + ' ' + i18n('bytes.mega');

                } else if ((bytes >= g) && (bytes < t)) {
                    return (bytes / g).toFixed(precision) + ' ' + i18n('bytes.giga');

                } else if (bytes >= t) {
                    return (bytes / t).toFixed(precision) + ' ' + i18n('bytes.tera');

                } else {
                    return bytes + ' ' + i18n('bytes.suffix');
                }
            }
        },
        className: {
            from: function(className) {
                var original = fromClassNameMap[className];
                if (!original) {
                    console.error('Never created a class for ', original);
                }
                return original;
            },
            to: function(string) {
                var className = toClassNameMap[string];
                if (!className) {
                    className = toClassNameMap[string] = 'id' + (classNameIndex++);
                }
                fromClassNameMap[className] = string;
                return className;
            }
        },
        geoLocation: {
            parse: function(str) {
                var m = str && str.match(/\s*point(?:\[|\()(.*?),(.*?)(?:\]|\))\s*/i);
                if (m) {
                    var latitude = m[1], longitude = m[2];
                    return {
                        latitude: latitude,
                        longitude: longitude
                    };
                }
            },
            pretty: function(geo, withholdDescription) {

                if (_.isString(geo)) {

                    var parsed = FORMATTERS.geoLocation.parse(geo);
                    if (parsed) {
                        return FORMATTERS.geoLocation.pretty(parsed);
                    } else {
                        return geo;
                    }
                }

                if (geo && geo.latitude && geo.longitude) {
                    var latlon = (
                            _.isNumber(geo.latitude) ? geo.latitude : parseFloat(geo.latitude)
                        ).toFixed(3) + ', ' +
                        (
                            _.isNumber(geo.longitude) ? geo.longitude : parseFloat(geo.longitude)
                        ).toFixed(3);

                    if (withholdDescription !== true && geo.description) {
                        return geo.description + ' ' + latlon;
                    }

                    return latlon;
                }
            }
        },
        object: {
            shortcut: function(key) {
                var normalized = key.replace(/\+/g, '-').toUpperCase(),
                    forLookup = normalized,
                    parts = normalized !== '-' ? normalized.split('-') : ['-'],
                    shortcut = {normalized: normalized, forEventLookup: normalized};

                if (parts.length === 1) {
                    shortcut.keyCode = codeForCharacter(parts[0]);
                    shortcut.character = parts[0];
                    shortcut.forEventLookup = shortcut.keyCode;
                } else if (parts.length === 2) {
                    shortcut.keyCode = codeForCharacter(parts[1]);
                    shortcut.character = parts[1];
                    shortcut[parts[0].toLowerCase() + 'Key'] = true;
                    shortcut.forEventLookup = parts[0] + '-' + shortcut.keyCode;
                } else return console.warn('Unable to add shortcut ', key);

                return shortcut;
            }
        },
        string: {
            normalizeAccents: function(str) {
                return str
                    .replace(/[áàãâä]/gi,'a')
                    .replace(/[éè¨ê]/gi,'e')
                    .replace(/[íìïî]/gi,'i')
                    .replace(/[óòöôõ]/gi,'o')
                    .replace(/[úùüû]/gi, 'u')
                    .replace(/[ç]/gi, 'c')
                    .replace(/[ñ]/gi, 'n');
            },
            shortcut: function(character, metaKeys) {
                if (!metaKeys) {
                    metaKeys = FORMATTERS.object.shortcut(character);
                    character = metaKeys.character;
                }

                var metaIcon;

                character = keyboardMappings.charIcons[character.toLowerCase()] ||
                    (character.length > 1 ? character.toLowerCase() : character);

                if (metaKeys.metaKey) {
                    metaIcon = keyboardMappings.metaIcons.meta;
                } else if (metaKeys.ctrlKey) {
                    metaIcon = keyboardMappings.metaIcons.ctrl;
                } else if (metaKeys.altKey) {
                    metaIcon = keyboardMappings.metaIcons.alt;
                } else if (metaKeys.shiftKey) {
                    metaIcon = keyboardMappings.metaIcons.shift;
                }

                if (metaIcon) {
                    return metaIcon + (isMac ? '' : '+') + character;
                }

                return character;
            },
            plural: function(count, singular, plural) {
                plural = plural || (singular + 's');

                switch (count) {
                    case 0: return 'No ' + plural;
                    case 1: return '1 ' + singular;
                    default: return count + ' ' + plural;
                }
            },
            truncate: function(str, words) {
                var maxChars = 7 * words,
                    string = $.trim(str),
                    wordsArray = string.split(/\s+/),
                    truncated = wordsArray.slice(0, words).join(' '),
                    ellipsis = '…';

                if (truncated.length > maxChars) {
                    // Use standard truncation (set amount of characters)
                    truncated = string.substring(0, maxChars) + ellipsis;
                } else if (truncated !== string) {
                    truncated = truncated + ellipsis;
                }

                return truncated
            }
        },
        date: {
            local: function(str) {
                if (_.isUndefined(str)) return '';
                var millis = _.isString(str) && !isNaN(Number(str)) ? Number(str) : str,
                    dateInLocale = _.isDate(millis) ? millis : new Date(millis);

                return dateInLocale;
            },
            utc: function(str) {
                if (_.isUndefined(str)) return '';
                var dateInLocale = FORMATTERS.date.local(str),
                    millisInMinutes = 1000 * 60,
                    millisFromLocaleToUTC = dateInLocale.getTimezoneOffset() * millisInMinutes,
                    dateInUTC = new Date(dateInLocale.getTime() + millisFromLocaleToUTC);
                return dateInUTC;
            },
            dateString: function(millisStr) {
                if (_.isUndefined(millisStr)) return '';
                return sf('{0:yyyy-MM-dd}', FORMATTERS.date.local(millisStr));
            },
            dateTimeString: function(millisStr, overrideTzInfo) {
                if (_.isUndefined(millisStr)) return '';
                var timezoneAbbreviation = overrideTzInfo;
                if (!timezoneAbbreviation) {
                    var tzInfo = FORMATTERS.timezone.currentTimezone(FORMATTERS.date.local(millisStr));
                    if (tzInfo) {
                        timezoneAbbreviation = tzInfo.tzAbbr;
                    }
                }
                return sf('{0:yyyy-MM-dd HH:mm}{1}',
                    FORMATTERS.date.local(millisStr),
                    timezoneAbbreviation ? (' ' + timezoneAbbreviation) : ''
                );
            },
            dateStringUtc: function(millisStr) {
                if (_.isUndefined(millisStr)) return '';
                return FORMATTERS.date.dateString(FORMATTERS.date.utc(millisStr));
            },
            dateTimeStringUtc: function(millisStr) {
                if (_.isUndefined(millisStr)) return '';
                return FORMATTERS.date.dateTimeString(FORMATTERS.date.utc(millisStr), 'UTC');
            },
            timeString: function(millisStr) {
                if (_.isUndefined(millisStr)) return '';
                return sf('{0:HH:mm}', FORMATTERS.date.local(millisStr));
            },
            timeStringUtc: function(millisStr) {
                if (_.isUndefined(millisStr)) return '';
                return FORMATTERS.date.timeString(FORMATTERS.date.utc(millisStr));
            },
            relativeToNow: function(date) {
                if (_.isUndefined(date)) return '';
                var span = new sf.TimeSpan(FORMATTERS.date.utc(Date.now()) - date),
                    time = '';

                if (span.years > 1) {
                    time = sf("{0:^y '" + i18n('time.years') + "'}", span);
                } else if (span.years === 1) {
                    time = i18n('time.year');
                } else if (span.months > 1) {
                    time = sf("{0:^M '" + i18n('time.months') + "'}", span);
                } else if (span.months === 1) {
                    time = i18n('time.month');
                } else if (span.days > 1) {
                    time = sf("{0:^d '" + i18n('time.days') + "'}", span);
                } else if (span.days === 1) {
                    time = i18n('time.day');
                } else if (span.hours > 1) {
                    time = sf("{0:^h '" + i18n('time.hours') + "'}", span);
                } else if (span.hours === 1) {
                    time = i18n('time.hour');
                } else if (span.minutes > 1) {
                    time = sf("{0:^m '" + i18n('time.minutes') + "'}", span);
                } else if (span.minutes === 1) {
                    time = i18n('time.minute');
                } else {
                    time = i18n('time.moments');
                }

                return time + ' ' + i18n('time.ago');
            }
        },
        timezone: {
            dateStringToUtc: function(dateStr, timezone) {
                if (/^\s*$/.test(dateStr)) {
                    return dateStr;
                }
                if (isNaN(new Date(dateStr).getTime())) {
                    return dateStr;
                }
                var date = new timezoneJS.Date(dateStr, timezone);
                return date.toString('yyyy-MM-dd', 'Etc/UTC');
            },
            dateTimeStringToUtc: function(dateStr, timezone) {
                if (/^\s*$/.test(dateStr)) {
                    return dateStr;
                }
                if (isNaN(new Date(dateStr).getTime())) {
                    return dateStr;
                }
                var date = new timezoneJS.Date(dateStr, timezone);
                return date.toString('yyyy-MM-dd HH:mm', 'Etc/UTC');
            },
            dateTimeStringToTimezone: function(millis, timezone) {
                var date = new timezoneJS.Date(millis, timezone);
                return date.toString('yyyy-MM-dd HH:mm Z');
            },
            date: function(dateStr, timezone) {
                if (/^\s*$/.test(dateStr)) {
                    return dateStr;
                }
                if (isNaN(new Date(dateStr).getTime())) {
                    return dateStr;
                }
                return new timezoneJS.Date(dateStr, timezone);
            },
            offsetDisplay: function(offsetMinutes) {
                var negative = offsetMinutes < 0,
                    offsetMinutesAbs = Math.abs(offsetMinutes),
                    hours = Math.floor(offsetMinutesAbs / 60),
                    minutes = Math.floor(offsetMinutesAbs % 60);

                if (('' + hours).length === 1) {
                    hours = '0' + hours;
                }
                if (('' + minutes).length === 1) {
                    minutes = '0' + minutes;
                }

                return (negative ? '-' : '+') + hours + ':' + minutes;
            },
            list: function() {
                return _.chain(jstz.olson.timezones)
                    .map(function(name, key) {
                        var components = key.split(','),
                            offsetMinutes = parseInt(components[0], 10),
                            dst = components.length > 1 && components[1] === '1';

                        return [
                            name,
                            {
                                offset: offsetMinutes,
                                offsetDisplay: FORMATTERS.timezone.offsetDisplay(offsetMinutes),
                                name: name,
                                dst: dst
                            }
                        ];
                    })
                    .object()
                    .value();
            },

            lookupTimezone: function(name, withOffsetForDate) {
                var list = FORMATTERS.timezone.list(),
                    tz = list[name],
                    region = timezoneJS.timezone.getRegionForTimezone(name);

                timezoneJS.timezone.loadZoneFile(region, { async: false });

                if (withOffsetForDate && withOffsetForDate.getTime) {
                    withOffsetForDate = withOffsetForDate.getTime();
                }

                if (!withOffsetForDate || isNaN(withOffsetForDate)) {
                    withOffsetForDate = Date.now();
                }

                var tzInfo = timezoneJS.timezone.getTzInfo(withOffsetForDate, name)

                // Returns opposite of what we want
                tzInfo.tzOffset *= -1;
                tzInfo.tzOffsetDisplay = FORMATTERS.timezone.offsetDisplay(tzInfo.tzOffset);

                return $.extend({}, tz, tzInfo);
            },

            currentTimezone: function() {
                return FORMATTERS.timezone.lookupTimezone(jstz.determine().name());
            }
        }
    };

    return FORMATTERS;
});
