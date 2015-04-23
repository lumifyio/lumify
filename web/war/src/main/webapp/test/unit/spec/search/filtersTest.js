define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('search/filters/filters', function(Filters) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('Filters', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

            })

            describe('on propertychange events', function(){
                it('should work when isScrubbing is false', function(){
                    var c = this.component,
                        data = {
                            id: 1,
                            options: {
                                isScrubbing: false
                            }
                        };

                    c.trigger('propertychange', data);
                })

                xit('should work when isScrubbing is true', function(){
                    var c = this.component,
                        data = {
                            id: 2,
                            options: {
                                isScrubbing: true
                            }
                        };

                    c.trigger('propertychange', data);
                })
            })

            describe('on propertyselected events', function(){

            })

            describe('on propertyinvalid events', function(){

            })

            describe('on clearfilters events', function(){

            })

            describe('on click events', function(){

            })

            describe('on conceptSelected events', function(){

            })

            describe('on searchByRelatedEntity events', function(){

            })

        })
    })
});
