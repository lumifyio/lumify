require([
    'jquery',
    'util/messages'
], function($, i18n) {
    'use strict';

    var terms;

    $.get("/terms")
        .done(function(json) {
            terms = json.terms;

            if (json.status.current != true) {
                console.log("terms.tile", terms.title);
                console.log("terms.html", terms.html);
                console.log("terms.date", terms.date);
                console.log("termsOfUse.checkbox", i18n("termsOfUse.checkbox"));
                console.log("termsOfUse.button.accept", i18n("termsOfUse.button.accept"));
                console.log("termsOfUse.button.decline", i18n("termsOfUse.button.decline"));
            } else {
                console.log("terms of use status", json.status);
            }
        })
        .fail(function() {
            console.log("error getting the terms of use and current user's acceptance status");
        });

    window.acceptTOU = function() {
        $.post("/terms", {"hash": terms.hash})
            .done(function() {
                console.log("terms of use acceptance posted");
            })
            .fail(function() {
               console.log("error posting terms of use acceptance");
            });
    }

});