(function () {
    'use strict';

    angular
        .module('app.core')
        .factory('itemService', itemService);

    itemService.$inject = ['logger', 'restService', '$rootScope', 'eventService', 'modelService', '$timeout'];
    /* @ngInject */
    function itemService(logger, restService, $rootScope, eventService, modelService, $timeout) {
        var service = {
            items: [],
            itemMap: {},
            itemsFetched: false,
            selected: [],
            createItem: createItem,
            deleteItem: deleteItem,
            saveItem: saveItem,
            getItem: getItem,
            getChildren: getChildren,
            getIdAbleHierarchy: getIdAbleHierarchy
        };

        activate();

        return service;

        function activate() {
            listenToItemEvents();
            if(modelService.activeModel !== null) {
                getAllItems(modelService.activeModel.id);
            }
            $rootScope.$on('modelChanged', function(event, model) {
                if( model != null) {
                    getAllItems(model.id);
                } else {
                    service.items = [];
                    service.itemMap = {};
                    service.itemsFetched = false;
                    service.selected = [];
                }
            });
        }

        function getAllItems(modelID) {
            service.items.splice(0, service.items.length);
            service.itemMap = {};
            return restService.getItems(modelID).then(function(data) {
                service.items.push.apply(service.items, data);
                _.forEach(service.items, function(i){service.itemMap[i.id] = i});
                logger.info('Item Service: Loaded ' + service.items.length + ' items through REST.');
                service.itemsFetched = true;
                $rootScope.$broadcast('itemsFetch', service.items);
            });
        }

        function getItem(id) {
            return _.isUndefined(service.itemMap[id]) ? null : service.itemMap[id];
        }

        function createItem(name, isa) {
            var newItem = {
                name: name,
                isa: isa
            };
            if (modelService.activeModel === null) {
                logger.error('Item Service: Failed to create item, no active model set.');
            } else {
                restService.postItems(newItem, modelService.activeModel.id);
            }
        }

        function saveItem(item) {
            if (modelService.activeModel === null) {
                var msg = 'Item Service: Failed to save item, no active model set.';
                logger.error(msg);
                return $q.reject(msg);
            } else {
                return restService.postItem(item, modelService.activeModel.id);
            }
        }

        function deleteItem(itemID) {
            if (modelService.activeModel === null) {
                logger.error('Item Service: Failed to request item deletion, no active model set.')
            }
            restService.deleteItem(modelService.activeModel.id, itemID);
        }

        function getChildren(node){
            if (_.isUndefined(node) || _.isUndefined(node.children)) return [];
            return _.map(node.children, function(c){
                return service.getItem(c.item)
            });
        }

        function getIdAbleHierarchy(root){
            if (_.isUndefined(root)) return null;
            var rootItem = _.isUndefined(root.item) ? getItem(root.id) : getItem(root.item);
            if (!_.isObject(rootItem)) return null;
            var rootCopy = angular.copy(rootItem);
            rootCopy.children = _.map(root.children, function(c){
                return getIdAbleHierarchy(c);
            });
            return rootCopy;
        }


        function listenToItemEvents() {
            eventService.addListener('ModelDiff', onItemEvent);

            function onItemEvent(data) {
                logger.info('Item Service: Handler received an event.');

                if (data.model == modelService.activeModel.id) {
                    for (var i = 0; i < data.updatedItems.length; i++) {
                        var remoteItem = data.updatedItems[i];

                        var localItem = getItem(remoteItem.id);
                        if (localItem != null) {
                            _.remove(service.items, localItem);
                        }
                        service.itemMap[remoteItem.id] = remoteItem;
                        service.items.push(remoteItem);
                        //if (existingItem === null) { // item not found => create
                        //    service.items.push(remoteItem);
                        //} else { // item found => update
                        //    service.items = _.filter(service.reject, {id: remoteItem.id});
                        //    service.items.push(remoteItem);
                        //    logger.info('Item Service: Updated item ' + oldName + '.');
                        //}
                    }

                    for (var j = 0; j < data.deletedItems.length; j++) {
                        var remoteItem = data.deletedItems[j];
                        var localItem = getItem(remoteItem.id);
                        if (localItem === null) {
                            logger.error('Item Service: Could not find an item with id ' + localItem.id + ' to delete.');
                        } else {
                            _.remove(service.items, localItem);
                            delete service.itemMap[localItem.id];
                            logger.info('Item Service: Removed item ' + localItem.name + '.');
                        }
                    }
                    if (data.updatedItems.length > 0) {
                        $timeout(function() {$rootScope.$broadcast('itemUpdate', data.updatedItems);}, 0);

                    }
                    if (data.deletedItems.length > 0) {
                        $timeout(function() {$rootScope.$broadcast('itemDeletion', data.deletedItems);}, 0);
                    }
                }
            }
        }

    }
})();
