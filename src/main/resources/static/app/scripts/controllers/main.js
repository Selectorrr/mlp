'use strict';

/**
 * @ngdoc function
 * @name staticApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the staticApp
 */
angular.module('staticApp')
.controller('MainCtrl', function ($scope, $http, $q) {
  var inputs, outputs;
  $scope.onSelect = function (selected) {
    $http.post('http://localhost:8080/train', {
      inputs: inputs,
      outputs: outputs,
      selected: selected
    });
  };
  $scope.getLocation = function (val) {
    var deferred = $q.defer();
    inputs = val.split('');
    $http.get('//maps.googleapis.com/maps/api/geocode/json', {
      params: {
        address: val,
        sensor: false
      }
    }).then(function (response) {
      var result = response.data.results.map(function (item) {
        return item.formatted_address;
      });
      outputs = result;
      var sortedResult = $http.post('http://localhost:8080/sort', {
        inputs: inputs,
        outputs: result
      }).then(function(resp){
        deferred.resolve(resp.data);
      });
    });
    return deferred.promise;
  };

});
