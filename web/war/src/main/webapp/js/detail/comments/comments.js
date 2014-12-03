define([
    'flight/lib/component',
    'hbs!./template',
    '../dropdowns/commentForm/commentForm',
    'util/withCollapsibleSections',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    CommentForm,
    withCollapsibleSections,
    F,
    withDataRequest) {
    'use strict';

    var VISIBILITY_NAME = 'http://lumify.io#visibilityJson';

    return defineComponent(Comments, withCollapsibleSections, withDataRequest);

    function Comments() {

        this.after('initialize', function() {
            this.on('editComment', this.onEditComment);
            if (this.attr.vertex) {
                this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            } else if (this.attr.edge) {
                this.on(document, 'edgesUpdated', this.onEdgesUpdated);
            }

            this.on('commentOnSelection', this.onCommentOnSelection);

            this.attr.data = this.attr.vertex || this.attr.edge;
            this.$node.html(template({}));
            this.update();
        });

        this.onCommentOnSelection = function(event, data) {
            this.trigger('editComment', {
                sourceInfo: data
            });
        };

        this.onVerticesUpdated = function(event, data) {
            var vertex = data && data.vertices && _.findWhere(data.vertices, { id: this.attr.data.id });
            if (vertex) {
                this.attr.data = vertex;
                this.update();
            }
        };

        this.onEdgesUpdated = function(event, data) {
            var edge = data && data.edges && _.findWhere(data.edges, { id: this.attr.data.id });
            if (edge) {
                this.attr.data = edge;
                this.update();
            }
        };

        this.update = function() {
            var self = this,
                comments = _.chain(this.attr.data.properties)
                    .where({ name: 'http://lumify.io/comment#entry' })
                    .sortBy(function(p) {
                        return p.metadata['http://lumify.io#createDate'];
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
                    this.append('span').attr('class', 'visibility')
                    this.append('span').attr('class', 'user')
                    this.append('span').attr('class', 'date')
                })

            selection.select('.comment-text').text(function(p) {
                return p.value;
            });
            selection.select('.visibility').each(function(p) {
                this.textContent = '';
                F.vertex.properties.visibility(
                    this,
                    { value: p.metadata && p.metadata[VISIBILITY_NAME] },
                    self.attr.data.id
                );
            })
            var users = this.dataRequest('user', 'getUserNames', _.map(comments, function(p) {
                return p.metadata['http://lumify.io#modifiedBy'];
            }));
            selection.select('.user').each(function(p, i) {
                var $this = $(this).text('Loading...');
                users.done(function(users) {
                    $this.text(users[i]);
                })
            });
            selection.select('.date')
                .text(function(p) {
                    var created = p.metadata['http://lumify.io#createDate'],
                        modified = p.metadata['http://lumify.io#modifiedDate'],
                        edited = created !== modified,
                        relativeString = F.date.relativeToNow(F.date.utc(created));

                    if (edited) {
                        return i18n('detail.comments.date.edited', relativeString);
                    }
                    return relativeString;
                })
                .attr('title', function(p) {
                    var created = p.metadata['http://lumify.io#createDate'],
                        modified = p.metadata['http://lumify.io#modifiedDate'],
                        edited = created !== modified;
                    if (edited) {
                        return i18n(
                            'detail.comments.date.hover.edited',
                            F.date.dateTimeString(created),
                            F.date.dateTimeString(modified)
                        );
                    }
                    return F.date.dateTimeString(created);
                });

            selection.exit().remove();

            this.$node.find('.collapsible-header').toggle(comments.length > 0);
        };

        this.onEditComment = function(evt, data) {
            var root = $('<div class="underneath">'),
                comment = data && data.comment,
                sourceInfo = data && data.sourceInfo,
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
                type: this.attr.vertex ? 'vertex' : 'edge',
                sourceInfo: sourceInfo,
                comment: comment
            });
        };

    }
});
