(function () {
    'use strict';

    angular
        .module('app.dashboard')
        .factory('dashboardService', dashboardService);

    dashboardService.$inject = ['$sessionStorage', 'logger', '$ocLazyLoad', '$http'];
    /* @ngInject */
    function dashboardService($sessionStorage, logger, $ocLazyLoad, $http) {
        var service = {
            addDashboard: addDashboard,
            getDashboard: getDashboard,
            removeDashboard: removeDashboard,
            addWidget: addWidget,
            getWidget: getWidget,
            closeWidget: closeWidget,
            storage: $sessionStorage.$default({
                dashboards: [{
                    id: 1,
                    name: 'My Board',
                    widgets: []
                }],
                widgetID: 1,
                dashboardID: 2
            })//,
            //widgetKinds: getWidgetList()
        };

        $http.get('/widgetList.json', 'json').
            then(function(response) {
                console.log(response.data.widgetList);
                // bad idea??
                service.widgetKinds = response.data.widgetList;
        });
             
        activate();

        return service;

        function activate() {

        }

        function addDashboard(name) {
            var dashboard = {
                id: service.storage.dashboardID++,
                name: name,
                widgets: []
            };
            logger.info('Dashboard Controller: Added a dashboard with name ' + dashboard.title + ' and index '
                + dashboard.id + '.');
        }

        function getDashboard(id) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            if (index === -1) {
                return null
            } else {
                return service.storage.dashboards[index];
            }
        }

        function removeDashboard(id) {
            var index = _.findIndex(service.storage.dashboards, {id: id});
            service.storage.dashboards.splice(index, 1);
        }

        function addWidget(dashboard, widgetKind, additionalData) {
               
            $ocLazyLoad.load(widgetKind.jsfiles).then(function() {
                var widget = angular.copy(widgetKind, {});
                widget.id = service.storage.widgetID++;
                if (additionalData !== undefined) {
                    widget.storage = additionalData;
                }
                dashboard.widgets.push(widget);
                logger.log('Dashboard Controller: Added a ' + widget.title + ' widget with index '
                    + widget.id + ' to dashboard ' + dashboard.name + '.');
            });
        }

        function getWidget(id) {
            var widget = null;
            for(var i = 0; i < service.storage.dashboards.length; i++) {
                var dashboard = service.storage.dashboards[i];
                var index = _.findIndex(dashboard.widgets, {id: id});
                if (index > -1) {
                    widget = dashboard.widgets[index];
                    break;
                }
            }
            return widget;
        }

        function closeWidget(id) {
            for(var i = 0; i < service.storage.dashboards.length; i++) {
                var dashboard = service.storage.dashboards[i];
                var index = _.findIndex(dashboard.widgets, {id: id});
                if (index > -1) {
                    dashboard.widgets.splice(index, 1);
                    break;

                }
            }
        }
    }
})();
