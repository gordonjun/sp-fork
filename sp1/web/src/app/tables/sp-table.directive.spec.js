/**
 * Created by daniel on 2015-08-05.
 */
/* jshint -W117, -W030 */
describe('spTable', function() {
    var compiledElement;

    beforeEach(function() {
        bard.appModule('app.tables');
        bard.inject('$compile', '$rootScope', '$httpBackend');

        $rootScope.disped = [];
        $rootScope.source = mockData.getMockModels();

        // Compile a piece of HTML containing the directive
        var templateElement = angular.element(
            '<table sp-table row-collection="source" displayed-collection="disped" '    +
            'header-template="app/models/model-table-header.html">'                     +
                '<tbody>'                                                               +
                    '<tr>'                                                              +
                        '<td>John Johnson</td>'                                         +
                        '<td>14:30</td>'                                                +
                        '<td>delete-button</td>'                                        +
                    '</tr>'                                                             +
                '</tbody>'                                                              +
            '</table>');

        var controllerMock = {
            tableState: function() {
                return {
                    sort: {},
                    search: {},
                    pagination: {
                        start: 0,
                        number: 2,
                        totalItemCount: 2
                    }
                };
            },
            slice: function (start, number) {
                tableState.pagination.start = start;
                tableState.pagination.number = number;
            }
        };

        templateElement.data('$stTableController', controllerMock);

        $httpBackend.whenGET(/sp-table.html/).respond(function() {
            var request = new XMLHttpRequest();
            request.open('GET', 'app/tables/sp-table.html', false);
            request.send(null);
            return [request.status, request.response, {}];
        });

        $httpBackend.whenGET(/model-table-header.html/).respond(function() {
            var request = new XMLHttpRequest();
            request.open('GET', 'app/models/model-table-header.html', false);
            request.send(null);
            return [request.status, request.response, {}];
        });

        compiledElement = $compile(templateElement)($rootScope);

        // Fire all the watches, so the scope expressions are evaluated
        $rootScope.$digest();
        $httpBackend.flush();
    });

    describe('header', function() {
        it('should have a page size selector', function() {
            expect(compiledElement.find('select').attr('ng-model')).to.equal('vm.itemsByPage');
        });

        it('should have a search field in the header', function() {
            expect(compiledElement.find('input').attr('type')).to.equal('search');
        });

        it('should have included the header template', function() {
            expect(compiledElement.find('.column-headers').find('th').first().text()).to.equal('Name');
            expect(compiledElement.find('.column-headers').find('th').last().text()).to.equal('Actions');
        });
    });

    it('should have transcluded the table body', function() {
        expect(compiledElement.find('tbody').find('td').first().text()).to.equal('John Johnson');
        expect(compiledElement.find('tbody').find('td').last().text()).to.equal('delete-button');
    });

    describe('pagination info', function() {
        it('should show correct number for the first entry on page', function() {
            expect(compiledElement.find('.number-of-first-entry-on-page').text()).to.equal('1');
        });

        it('should show correct number for the last entry on page', function() {
            expect(compiledElement.find('.number-of-last-entry-on-page').text()).to.equal('2');
        });

        it('should show correct number for total number of entries', function() {
            expect(compiledElement.find('.total-number-of-entries').text()).to.equal('2');
        });
    });
});
