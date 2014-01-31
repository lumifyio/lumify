
define([], function() {

    return init;

    function init(cy, cell) {
        var container = $(cy.container()),
            w = container.width(),
            h = container.height(),
            c = container.find('.path-debug-grid');

        if (!c.length) {
            c = $('<canvas class="path-debug"/>').appendTo(container);
        }

        c.attr('width', w)
         .attr('height', h)
         .css({ position: 'absolute', top: 0 });

        var canvas = c[0], 
            ctx = canvas.getContext('2d');

        function draw() {
            ctx.clearRect(0,0,w,h);
            ctx.strokeStyle = 'rgba(0,0,0,0.4)';
            ctx.beginPath();
            var xinc = cell.x * cy.zoom(),
            yinc = cell.y * cy.zoom();

            for (var x = xinc; x < w; x+= xinc) {
                ctx.moveTo(x,0);
                ctx.lineTo(x,h);
            }
            for (var y = yinc; y < h; y+= yinc) {
                ctx.moveTo(0,y);
                ctx.lineTo(w,y);
            }
            ctx.stroke();
        }
        cy.off('zoom', draw);
        cy.on('zoom', draw);

        draw();
    }
});
