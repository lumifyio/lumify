define([], function() {
    'use strict';

    return formulaFunction;

    function formulaFunction(formula, vertex, ontologyConcept, V) {
        var prop = V.prop.bind(undefined, vertex);

        try {
            return eval(formula); // jshint ignore:line
        } catch(e) {
            console.warn('Unable to execute formula: ' + formula + ' Reason: ', e);
        }
    }
});
