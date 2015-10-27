(function() {
    'use strict';

    angular
        .module('app.layout')
        .directive('htTopNav', htTopNav);

    /* @ngInject */
    function htTopNav () {
        var directive = {
            scope: {},
            bindToController: {
                'navline': '='
            },
            controller: TopNavController,
            controllerAs: 'vm',
            restrict: 'EA',
            templateUrl: 'app/layout/sp-top-nav.html'
        };

        return directive;
    }

    /* @ngInject */
    TopNavController.$inject = ['modelService', 'dashboardService', '$state', '$modal'];

    function TopNavController(modelService, dashboardService, $state, $modal) {
        var vm = this;
        vm.modelService = modelService;
        vm.dashboardService = dashboardService;
        vm.$state = $state;
        vm.createModel = createModel;

        function createModel() {
            var modalInstance = $modal.open({
                templateUrl: '/app/models/createmodel.html',
                controller: 'CreateModelController',
                controllerAs: 'vm'
            });

            modalInstance.result.then(function(chosenName) {
                modelService.createModel(chosenName);
            });
        }
    }

})();
