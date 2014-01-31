
Visibility Plugin
=================

Plugin to configure the user interface for displaying and editing visibility authorization strings.

Attributes
----------

Accessible in plugin as `this.attr`

* `value`: Previous value to populate

Events
------

Visibility plugins event contract.

Must Respond to:

* `visibilityclear`: Clears the current value

        this.on('visibilityclear', function() {
            // Clear the value
            this.select('fieldSelector').val('');
        });


Must Fire:

* `visibilitychange`: When the value changes

        this.trigger('visibilitychange', {
            value: '[current value]',
            valid: [true|false] 
        })

