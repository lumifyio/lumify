require(['configuration/plugins/graphVertexMenu/plugin'],function(graphVertexMenu) {
	var componentPath = '/vertexMenu/vertex-menu-plugin';
	
	
	define(componentPath, ['flight/lib/component'],
		function(defineComponent) {
			defineComponent(GraphVertexMenuPlugin);

			function GraphVertexMenuPlugin() {
				this.after('initialize',function(){
					console.log('after initialize;');					
				});					
			}
	});
	
	graphVertexMenu.registerMenuItem({
		label : 'vertex menu plugin',
		clicked:function(vertexId){
			alert(vertexId);
		},
		componentPath : componentPath
	});
	

});
