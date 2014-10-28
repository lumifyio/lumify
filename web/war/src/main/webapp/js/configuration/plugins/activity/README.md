
Activity Plugin
=====================

Plugin to configure the user interface for activity items

To register an activity item:

        require(['configuration/plugins/activity/plugin'], function(ActivityPlugin) {
            ActivityPlugin.registerActivityHandler({

                // Required Attributes
                type: [name of activity (unique)]
                kind: [kind of activity (eventWatcher|longRunningProcess)]
                titleRenderer: function(el, task)

                // Optional
                autoDismiss: boolean (remove on finish)
                allowCancel: boolean (allow cancel button to show)
            })
        })

# Kinds of Activities

1. eventWatcher

    Required properties:
    
        1. `eventNames`: Array of 2 event names [startEventName, endEventName]

1. longRunningProcess

    Optional properties:
        
        1. `finishedComponentPath`: Path to flight component for finished button



