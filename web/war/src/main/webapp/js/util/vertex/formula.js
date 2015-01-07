define([], function() {
    'use strict';

    return formulaFunction;

    function formulaFunction(formula, vertex, V) {

        try {

            // If the formula is an expression wrap and return it
            if (formula.indexOf('return') === -1) {
                formula = 'return (' + formula + ')';
            }

            return new Function( // jshint ignore:line
                // Get property value and converted to string displayValue
                'prop',
                // Get actual raw property value
                'propRaw',
                // Get the longest property value and converted to string displayValue
                'longestProp',
                // Vertex Json
                'vertex',
                // Inner function string
                formula)(
                    V.displayProp.bind(undefined, vertex),
                    V.prop.bind(undefined, vertex),
                    V.longestProp.bind(undefined, vertex),
                    vertex);

        } catch(e) {
            console.warn('Unable to execute formula: ' + formula + ' Reason: ', e);
        }
    }
});
