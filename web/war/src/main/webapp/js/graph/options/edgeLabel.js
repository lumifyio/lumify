define([
    'flight/lib/component',
    'util/withDataRequest'
], function(
    defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(EdgeLabelToggle, withDataRequest);

    function EdgeLabelToggle() {

        this.after('initialize', function() {
            var self = this,
                cy = this.attr.cy,
                preferenceName = 'edgeLabels',
                preferenceValue = lumifyData.currentUser.uiPreferences[preferenceName],
                showEdges = preferenceValue !== 'false';

            $('<label>').text(i18n('controls.options.edgeLabels.toggle') + ' ')
                .css({
                    'white-space': 'nowrap'
                })
                .append('<input type="checkbox"' + (showEdges ? ' checked' : '') + '>')
                .appendTo(this.$node.empty())

            this.$node
                .find('input').on('change', function() {
                    var checked = $(this).is(':checked');
                    lumifyData.currentUser.uiPreferences[preferenceName] = '' + checked;
                    self.trigger('reapplyGraphStylesheet');
                    self.dataRequest('user', 'preference', preferenceName, checked);
                });
        });

    }
});
