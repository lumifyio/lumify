define([
    'flight/lib/component',
    'hbs!./template',
    '../dropdowns/commentForm/commentForm',
    'util/withCollapsibleSections',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    CommentForm,
    withCollapsibleSections,
    F) {
    'use strict';

    return defineComponent(Comments, withCollapsibleSections);

    function Comments() {

        this.after('initialize', function() {
            this.on('editComment', this.onEditComment);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node.html(template({}));
            this.update();
        });

        this.onVerticesUpdated = function(event, data) {
            var vertex = data && data.vertices && _.findWhere(data.vertices, { id: this.attr.data.id });
            if (vertex) {
                this.attr.data = vertex;
                this.update();
            }
        };

        this.update = function() {
            var comments = _.chain(this.attr.data.properties)
                    .where({ name: 'http://lumify.io/comment#entry' })
                    .sortBy(function(p) {
                        return p.metadata['http://lumify.io#modifiedDate'];
                    })
                    .value()
                selection = d3.select(this.$node.find('.comment-content ul').get(0))
                    .selectAll('.comment')
                    .data(comments)
                    .order();

            this.$node.find('.collapsible .badge').text(
                F.number.pretty(comments.length)
            );

            selection.enter()
                .append('li').attr('class', 'comment')
                .call(function() {
                    this.append('div').attr('class', 'comment-text')
                    this.append('span').attr('class', 'user')
                    this.append('span').attr('class', 'date')
                })

            selection.select('.comment-text').text(function(p) {
                return p.value;
            });
            selection.select('.user').each(function(p) {
                $(this).text('Loading...');
                F.vertex.metadata.userAsync(this, p.metadata['http://lumify.io#modifiedBy']);
            });
            selection.select('.date').text(function(p) {
                return F.date.dateTimeString(p.metadata['http://lumify.io#modifiedDate']);
            });

            // TODO: visibility

            selection.exit().remove();
        };

        this.onEditComment = function(evt, data) {
            var root = $('<div class="underneath">'),
                comment = data && data.comment,
                commentRow = comment && $(evt.target).closest('tr');

            this.$node.find('button.info').popover('hide');

            if (commentRow && commentRow.length) {
                root.appendTo(
                    $('<tr><td colspan=3></td></tr>')
                        .insertAfter(commentRow)
                        .find('td')
                );
            } else {
                root.appendTo(this.$node.find('.comment-content'));
            }

            this.$node.find('.collapsible').addClass('expanded');

            root.on(TRANSITION_END, function handler(e) {
                var $this = $(this);
                if (e && e.originalEvent && e.originalEvent.propertyName === 'height') {
                    var sp = $this.scrollParent()
                    sp.animate({
                        scrollTop: $this.position().top
                    });
                    root.off(TRANSITION_END, handler);
                }
            })
            CommentForm.teardownAll();
            CommentForm.attachTo(root, {
                data: this.attr.data,
                comment: comment
            });
        };

    }
});
