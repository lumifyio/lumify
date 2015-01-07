require([
    'jquery',
    'util/messages'
], function($, i18n) {
    'use strict';

    var template;

    $(document).on('applicationReady', function(event) {
        $.get('terms')
            .done(function(json) {
                var terms = json.terms;
                if (terms.date) {
                    terms.date = new Date(terms.date);
                }

                if (json.status.current != true) {
                    require([
                        'flight/lib/component',
                        'hbs!io/lumify/termsOfUse/terms-of-use'
                    ], function(defineComponent, tpl) {
                        template = tpl;

                        var Terms = defineComponent(TermsOfUse);
                        Terms.attachTo($('#app'), {
                            terms: terms
                        });
                    });
                }
            })
            .fail(function() {
                console.error("error getting the terms of use and current user's acceptance status");
            });
    });

    function TermsOfUse() {

        this.defaultAttrs({
            termsSelector: '.terms-modal',
            acceptSelector: '.terms-modal .btn-primary',
            declineSelector: '.terms-modal .btn-danger'
        });

        this.after('initialize', function() {
            this.$node.append(template(this.attr));

            this.on('click', {
                acceptSelector: this.onAccept,
                declineSelector: this.onDecline
            });

            this.showModal();
        });

        this.onAccept = function(event) {
            event.stopPropagation();
            event.preventDefault();

            var button = $(event.target)
                .addClass('loading')
                .attr('disabled', true);

            $.post('terms', { hash: this.attr.terms.hash })
                .done(function() {
                    var modal = button.closest('.modal');
                    modal.modal('hide');
                    _.delay(function() {
                        modal.remove();
                    }, 1000);
                })
                .fail(this.showButtonError.bind(
                    this,
                    button,
                    i18n('termsOfUse.button.accept.error'))
                );
        };

        this.onDecline = function(event) {
            event.stopPropagation();
            event.preventDefault();

            var button = $(event.target)
                .addClass('loading')
                .attr('disabled', true);

            $.post('logout')
                .fail(this.showButtonError.bind(
                    this,
                    button,
                    i18n('termsOfUse.button.decline.error'))
                )
                .done(function() {
                    location.reload();
                });
        };

        this.showButtonError = function(button, errorText) {
            button.removeClass('loading')
                .text(errorText);

            _.delay(function() {
                button.text('Accept').removeAttr('disabled');
            }, 3000);
        }

        this.showModal = function() {
            var modal = this.select('termsSelector');
            modal
                .find('.modal-body').css({
                    padding: 0,
                    maxHeight: Math.max(50, $(window).height() * 0.5) + 'px'
                })
                .find('.term-body').css({
                    padding: '0 1.2em',
                    fontSize: '10pt'
                });

            _.defer(function() {
                modal.modal('show');
            });
        }

    }
});
