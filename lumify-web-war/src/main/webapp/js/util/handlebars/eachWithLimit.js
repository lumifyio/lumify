define(['hbs/handlebars'], function(Handlebars) {
    'use strict';

    // Modified version of handlebars `each`

    return Handlebars.registerHelper('eachWithLimit', function(context, options) {
        var fn = options.fn, 
            inverse = options.inverse,
            limit = options.hash.limit,
            i = 0,
            ret = '',
            data;

        if (isNaN(limit)) {
            throw new Error('limit option is required');
        }

        if (Handlebars.Utils.isFunction(context)) {
            context = context.call(this);
        }

        data = Handlebars.createFrame(options.data || {});

        if (context && typeof context === 'object') {
            if (Handlebars.Utils.isArray(context)) {
                for(var j = context.length; i < j; i++) {
                    if (data) {
                        data.index = i;
                        data.first = (i === 0);
                        data.last  = (i === (context.length - 1));
                        data.hidden = i >= limit;
                        data.showButton = data.last && data.hidden;
                    }
                    ret = ret + fn(context[i], { data: data });
                }
            } else {
                throw new Error('Array required for eachWithLimit');
            }
        }

        if (i === 0) {
            ret = inverse(this);
        }

        return ret;
    });

});
