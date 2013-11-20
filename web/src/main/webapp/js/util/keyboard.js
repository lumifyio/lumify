

define(['flight/lib/component'],
function(defineComponent) {
    'use strict';

    return defineComponent(Keyboard);


    function Keyboard() {
        this.after('initialize', function() {

            function shouldFilter(e) {
                return $(e.target).is('input,select,textarea');
            }

            this.on('keyup', function(e) {
                if (shouldFilter(e)) return;
                
                if (e.which === 191) this.onForwardSlash();
            });
        });

        this.onForwardSlash = function() {
            this.trigger(document, 'menubarToggleDisplay', { name:'search' });
        };
    }
});
