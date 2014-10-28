
Activity Plugin
=====================

Plugin to configure the user interface for activity items

To register an activity item:

        require(['configuration/plugins/activity/plugin'], function(ActivityPlugin) {
            ActivityPlugin.registerActivityHandler({

                // Required Attributes
                type: (string)
                kind: (string)
                titleRenderer: function(el, task)

                // Optional
                autoDismiss: (boolean)
                allowCancel: (boolean)
            })
        })

# Properties

* `type`: (String) Name of activity type. 

    Define `activity.tasks.type.[MY_ACTIVITY_TYPE]` message bundle string for localized display.
    
        // Handler definition
        { type: 'saveWorkspace', ... }
        
        // MessageBundle.properties
        activity.tasks.type.saveWorkspace=Save Workspace

* `kind`: (String) Kind of activity. Currently allows `eventWatcher`, `longRunningProcess`
* `titleRenderer`: (function) Responsible for rendering the rows title. (Can be async)

        function myRenderer(el, task) {
            el.textContent = task.id;
        }
        
* `autoDismiss`: (boolean) True to auto remove row on complete
* `allowCancel`: (boolean) True to show cancel button (default false for eventWatcher, true for longRunningProcess)


# Activity Kind Specific Properties

1. `eventWatcher`

    Required properties:
    * `eventNames`: (Array) 2 event names `[startEventName, endEventName]`
    
    Note: Task object in `titleRenderer` contains `eventData` property containing the triggered events data.

1. `longRunningProcess`

    Optional properties:
    * `finishedComponentPath`: (String) Path to flight component for finished button



