
Visibility Plugin
=================

Plugin to configure the user interface for displaying and editing visibility authorization strings.

The visibility component requires two flightjs components:

    visibilityEditor.js
    visibilityDisplay.js


## Visibility Editor

Describes the form for editing visibility values.

### Attributes

Accessible in plugin as `this.attr`

* `value`: Previous value to populate

### Events

Visibility plugins event contract.

#### Must Respond to:

* `visibilityclear`: Clears the current value

        this.on('visibilityclear', function() {
            // Clear the value
            this.select('fieldSelector').val('');
        });


#### Must Fire:

* `visibilitychange`: When the value changes. Send `valid` boolean if the current value is valid for submitting.

        this.trigger("visibilitychange", {
            value: "[current value]",
            valid: [ true | false ] 
        })

## Visibility Display

Describes the display of visibility values.

### Attributes

* `value`: Current visibility value
