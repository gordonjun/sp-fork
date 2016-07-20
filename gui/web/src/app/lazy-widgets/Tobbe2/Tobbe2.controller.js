/**
 * Created by Martin on 2015-11-19.
 */
(function () {
    'use strict';

    angular
        .module('app.Tobbe2')
        .controller('Tobbe2Controller', Tobbe2Controller);

    Tobbe2Controller.$inject = ['$scope', 'dashboardService', 'eventService', 'spServicesService', 'operationControlService'];
    /* @ngInject */
    function Tobbe2Controller($scope, dashboardService, eventService, spServicesService, operationControlService) {
        var vm = this;
        vm.widget = $scope.$parent.$parent.$parent.vm.widget; //lol what
        var service = {
            connected: false,
            controlServiceID: '',
            state: [],
            resourceTree: [],
            latestMess: {},
            connect: connect,
            sendRawDB: sendRawDB
        };
        vm.hej = 'null'
        vm.control = operationControlService;

        //functions¨
        vm.adressToRaw = adressToRaw;
        vm.parseColour = parseColour;
        vm.sendOrder = sendOrder;
        vm.sendRawDB = sendRawDB;
        activate();
        vm.placeyplaceholder = 'Chose operation'
        vm.myFunction = myFunction;
        vm.changeColor = changeColor;
        vm.activate2 = activate2;
        vm.value = 1;
        vm.debug14 = 0;
        vm.parseColourTrueFalse = parseColourTrueFalse;
        vm.searchObj = searchObj;
        vm.reset = reset;
        vm.data = {
            resMult: [
                {
                    name: 'Robot 2',
                    resource: [
                        {id: '126 18 0', set: '1', action: 'Set at position 1', value: '#FFFFFF'},
                        {id: '126 18 0', set: '2', action: 'Set at position 2', value: '#FFFFFF'},
                        {id: '126 18 0', set: '3', action: 'Set at position 3', value: '#FFFFFF'},
                        {id: '126 18 0', set: '4', action: 'Set at position 4', value: '#FFFFFF'},
                        {id: '126 18 0', set: '5', action: 'Set at position 5', value: '#FFFFFF'}
                    ],
                    currVal: 'Choose place',
                    currID: 'null',
                    image: 'images/Industry-Robot-icon.png'
                }
            ],
            resSel: [
                {
                    name: 'Elevator 2',
                    resource: [
                        {id: '135 0 0', action: 'Up', value: '#FFFFFF'},
                        {id: '135 0 1', action: 'Down', value: '#FFFFFF'}
                    ],
                    image: 'images/elevator-icon.png'
                },
                {
                    name: 'Elevator 3',
                    resource: [
                        {id: '140 0 0', action: 'Up', value: '#FFFFFF'},
                        {id: '140 0 1', action: 'Down', value: '#FFFFFF'}
                    ],
                    image: 'images/elevator-icon.png'

                },
                {
                    name: 'Flexlink',
                    resource: [
                        {id: '138 16 0', action: 'Start', value: '#FFFFFF'},
                        {id: '138 16 1', action: 'Stop', value: '#FFFFFF'}
                    ],
                    image: 'images/conveyor-icon2.png'
                },
                {
                    name: 'Robot 4',
                    resource: [
                        {id: '128 0 2', action: 'Home', value: '#FFFFFF'},
                        {id: '128 0 3', action: 'Dodge', value: '#FFFFFF'}
                    ],
                    image: 'images/Industry-Robot-icon.png'

                },
                {
                    name: 'Robot 5',
                    resource: [
                        {id: '132 0 2', action: 'Home', value: '#FFFFFF'},
                        {id: '132 0 3', action: 'Dodge', value: '#FFFFFF'}
                    ],
        image: 'images/Industry-Robot-icon.png'
                },
                {
                    name: 'Reset PLC',
                    resource: [
                        {id: '141 0 0', action: 'Set', value: '#FFFFFF'},
                        {id: '141 0 1', action: 'Mode', value: '#FFFFFF'},
                        {id: '141 0 2', action: 'Force palett 0', value: '#FFFFFF'},
                        {id: '141 0 3', action: 'Force palett 1', value: '#FFFFFF'}
                    ],
        image: 'images/plc-icon2.png'
                },
                {
                    name: 'Robot 2',
                    resource: [
                        {id: '126 0 5', action: 'Pick at position', value: '#FFFFFF'},
                        {id: '126 0 4', action: 'Place at position', value: '#FFFFFF'},
                        {id: '126 0 2', action: 'Place at elevator 3', value: '#FFFFFF'},
                        {id: '126 0 6', action: 'Place at table', value: '#FFFFFF'}
                    ],
        image: 'images/Industry-Robot-icon.png'

                }
                        
            ]/*,
            singleShow: [
                {
                    name: 'Sensor 1',
                    id: 'db hej hej',
                    value: 'false',
                    image: 'images/elevator-icon.png'
                }
            ]*/
        };

        function changeColor(circleID, colour) {
            var property = document.getElementById(circleID);
            switch (colour) {
                case 'green':
                    property.style.background = "radial-gradient(circle at 15px 15px, #5cd65c, #000)";//green
                    break;
                case 'red':
                    property.style.background = "radial-gradient(circle at 15px 15px, #ff3333, #000)";//red
                    break;
                default:
                    break;
            }
        }

        /* When the user clicks on the button,
         toggle between hiding and showing the dropdown content */
        function myFunction(something) {
            document.getElementById(something).classList.toggle("show");
        }

        // Close the dropdown menu if the user clicks outside of it
        window.onclick = function (event) {
            if (!event.target.matches('.dropbtn')) {

                var dropdowns = document.getElementsByClassName("dropdown-content");
                var i;
                for (i = 0; i < dropdowns.length; i++) {
                    var openDropdown = dropdowns[i];
                    if (openDropdown.classList.contains('show')) {
                        openDropdown.classList.remove('show');
                    }
                }
            }
        }

        function activate2(int) {
            if (int == 1)
                vm.debug14++;
            else
                vm.debug14--;
        }

        function activate() {
            $scope.$on('closeRequest', function () {
                dashboardService.closeWidget(vm.widget.id);
            });
            eventService.addListener('ServiceError', onEvent);
            eventService.addListener('Progress', onEvent);
            eventService.addListener('Response', onEvent);
        }

    function updateItems(){
      var its = _.filter(itemService.items, function(o){
        return (angular.isDefined(o.id) && angular.isDefined(service.state[o.id]))
      });
      service.itemState = [];
      _.foreach(its, function(o){
        service.itemState.push({'item': o, 'state': service.state[o.id]})
      })
    }

    function onEvent(ev){
      //console.log("control service");
      //console.log(ev);

      if (ev.isa == "Response" && ev.service == "OperationControl" && !(_.isUndefined(ev.attributes.dbs)) ) {
        ev.attributes.dbs.forEach(vm.parseColour);

      }

      if (!_.has(ev, 'reqID') || ev.reqID !== service.controlServiceID) return;

      if (_.has(ev, 'attributes.theBus')){
        if (ev.attributes.theBus === 'Connected' && ! service.connected){
          sendTo(service.latestMess, 'subscribe');
        }

        service.connected = ev.attributes.theBus === 'Connected'
      }

      if (_.has(ev, 'attributes.state')){
        service.state = ev.attributes.state;
      }
      if (_.has(ev, 'attributes.resourceTree')){
        service.resourceTree = ev.attributes.resourceTree;
      }
    }

        function sendOrder() {

            var mess = {"data": {getNext: false, "buildOrder": vm.ButtonColour.kub}};
            spServicesService.callService(spServicesService.getService("runnerService"),
                mess,
                function (resp) {
                    if (_.has(resp, 'attributes.result')) {
                        console.log("Hej" + vm.result);
                    }
                }
            )
        }

        function connect(bus, connectionSpecID, resourcesID){
            var mess = {
                'setup': {
                    'busIP': bus.ip,
                    'publishTopic': bus.publish,
                    'subscribeTopic': bus.subscribe
                }
            };
            var conn = {};
            if (angular.isDefined(connectionSpecID)){
                conn.connectionDetails = connectionSpecID
            }
            if (angular.isDefined(resourcesID)){
                conn.resources = resourcesID
            }
            mess.connection = conn;

            sendTo(mess, 'connect').then(function(repl){
                console.log("inside first connection");
                console.log(repl);
                if (messageOK(repl) && _.has(repl, 'reqID')){
                    service.controlServiceID = repl.reqID;
                }
            });
            service.latestMess = mess;
        }

        function parseColour(item, index) {
            if (!(_.isUndefined(item.address))) {
                var theKey = adressToRaw(item.address);
                var theValue = item.value;
                if (theKey === '138 16 0' || theKey === '138 16 1') return;
                if (theKey === '138 16 2') {
                    theKey = '138 16 0';
                    searchObj( vm.data, theKey, theValue);
                    theKey = '138 16 1';
                    searchObj( vm.data, theKey, !theValue);
                }
                
                else searchObj( vm.data, theKey, theValue );
                //var oki = _.find(vm.data.resSel, hejhej);

            }
        }
        //function hejhej(param1){
        //    return !_.isUndefined(_.find(param1.resource, function(r) {return r.id == '135 0 1 true';}));
        //}

        function adressToRaw(params) {
            return params.db + ' ' + params.byte + ' ' + params.bit;
        }

        function searchObj( obj, keyToFind, data ){

            for( var key in obj ) {

                if( typeof obj[key] === 'object' ){
                    searchObj( obj[key], keyToFind, data );
                }

                if( obj[key] === keyToFind ){
                    obj.value = parseColourTrueFalse(data);
                }

            }

        }

        function reset() {
            vm.control.reset();
        }

        function parseColourTrueFalse(value) {
            if (value === true) return '#99e699';
            else if (value === false) return '#ff6666';
            else return '#FFFFFF';
        }

        function sendRawDB(params) {
            var mess = service.latestMess;
            console.log(params);
            mess.command = {
                'commandType': 'raw',
                'raw': params
            };
            spServicesService.callService('OperationControl',{'data':mess});
        }
    }
})();










