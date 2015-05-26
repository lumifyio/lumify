define(
		[ 'jquery', 'underscore' ],
		function($, _) {
			'use strict';

			var PREFIX = 'VERTEX_MENU_PLUGIN', identifier = 0, menuItems = [], byId = {};

			return {
				menuItems:menuItems,

				menuItemsById:byId,

				registerMenuItem:function(menuItem) {
					menuItem.identifier = PREFIX + identifier;
					menuItem.event = 'vertexMenuPluginClicked:' + menuItem.identifier;

					byId[PREFIX + identifier] = menuItem;
					identifier++;
					menuItems.push(menuItem);
				}
			};
		});
