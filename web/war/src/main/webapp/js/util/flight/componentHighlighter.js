
define(['flight/lib/registry'], function(registry) {
    var highlighter = $('<div>')
        .addClass('component-highlighter')
        .css({
        })
        .appendTo(document.body)

    return {
        highlightComponents: function(enable) {
            var $doc = $(document).off('.highlightcomps');
            highlighter.hide();
            if (enable) {
                $doc.on('mouseover.highlightcomps', _.debounce(mouseover, 250))
            }
        }
    };

    function eventsFromComponentInstances(instances) {
        var events = [];
        Object.keys(instances).forEach(function(identifier) {
            var instanceInfo = instances[identifier];
            instanceInfo.events.forEach(function(event) {
                events.push(event.type);
            })
        })
        events = _.chain(events)
           .unique()
           .sortBy(function(e) {
               return e;
           })
           .value();

        return events;
    }

    function nameFromComponent(component) {
        var name = component.toString(),
            nameMatch = name.match(/^([^,]+)/)

        if (nameMatch) {
            name = $.trim(nameMatch[1]);
        }

        return name;
    }

    function logComponent(name, closestComponent) {
        var events = eventsFromComponentInstances(closestComponent.info.instances);

        console.groupCollapsed('Found Component: ' + name);
        {
            console.groupCollapsed('Listens for events');
            {
                console.log(events.join(', '))
            }
            console.groupEnd();

            console.groupCollapsed('Bound to Node');
            {
                console.log(closestComponent.node)
            }
            console.groupEnd();

            console.groupCollapsed('Flight Component Info');
            {
                console.log(closestComponent.info)
            }
            console.groupEnd();
        }
        console.groupEnd();
    }

    function highlightNode(node, name) {
        var $node = $(node),
            position = $node.offset();

        if (position) {
            var height = $node.outerHeight() || $node.parent().outerHeight();

            highlighter.attr('data-name', name);
            highlighter.css({
                left: Math.round(position.left) + 'px',
                top: Math.round(position.top) + 'px',
                width: $node.outerWidth(),
                height: $node.outerHeight() || $node.parent().outerHeight()
            }).show();
        }
    }

    function mouseover(event) {
        var closestComponentInfo,
            shortestSteps = Number.MAX_VALUE;

        highlighter.hide();

        registry.components.forEach(function(componentInfo) {
            componentInfo.attachedTo.forEach(function(node) {
                Object.keys(componentInfo.instances).forEach(function(identifier) {
                    var instanceInfo = componentInfo.instances[identifier];
                    if (instanceInfo.instance.popover) {
                        node = instanceInfo.instance.popover[0];
                        console.log('replacing node with ', node)
                    }
                })
                var walkNode = event.target,
                    found = false,
                    steps = 0;

                while (walkNode) {
                    if (walkNode === node) {
                        found = true;
                        break;
                    }
                    walkNode = walkNode.parentNode;
                    steps++;
                }

                if (found && steps < shortestSteps) {
                    shortestSteps = steps;
                    closestComponent = {
                        info: componentInfo,
                        node: node,
                        steps: steps,
                    };
                }
            })
        })

        if (closestComponent) {
            var name = nameFromComponent(closestComponent.info.component);

            logComponent(name, closestComponent);

            highlightNode(closestComponent.node, name);
        }
    }
});
