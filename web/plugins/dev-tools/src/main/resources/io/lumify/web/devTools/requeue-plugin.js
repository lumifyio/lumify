require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/requeue'
], function(
    defineLumifyAdminPlugin,
    template
    ) {
    'use strict';

    return defineLumifyAdminPlugin(Requeue, {
        section: 'Vertex',
        name: 'Requeue',
        subtitle: 'Requeue vertices and edges'
    });

    function Requeue() {

        this.defaultAttrs({
            parameterSelector: 'input',
            parameterButton: 'button.param',
            vertexButton: 'button.vertex',
            edgeButton: 'button.edge'
        })

        this.after('initialize', function() {
            this.on('click', {
                parameterButton: this.onParameterRequeue,
                vertexButton: this.onVertexRequeue,
                edgeButton: this.onEdgeRequeue
            });

            this.$node.html(template({}));
        });

        this.onParameterRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.adminService.queueVertices(
                        this.select('parameterSelector').val()
                    )
                )
            );
        };

        this.onVertexRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.adminService.queueVertices()
                )
            );
        };

        this.onEdgeRequeue = function(event) {
            this.handleSubmitButton(event.target,
                this.showResult(
                    this.adminService.queueEdges()
                )
            );
        };

        this.showResult = function(promise) {
            var self = this;

            return promise
                .done(this.showSuccess.bind(this))
                .fail(this.showError.bind(this))
                .always(function() {
                    _.delay(function() {
                        self.$node.find('.alert').remove();
                    }, 3000)
                });
        }
    }
});
