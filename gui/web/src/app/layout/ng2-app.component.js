"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var core_1 = require('@angular/core');
var ng2_shell_component_1 = require('./ng2-shell.component');
var Ng2AppComponent = (function () {
    function Ng2AppComponent() {
    }
    Ng2AppComponent = __decorate([
        core_1.Component({
            selector: 'ng2-app',
            template: '<ng2-shell></ng2-shell>',
            styleUrls: [],
            directives: [ng2_shell_component_1.Ng2ShellComponent],
            providers: []
        }), 
        __metadata('design:paramtypes', [])
    ], Ng2AppComponent);
    return Ng2AppComponent;
}());
exports.Ng2AppComponent = Ng2AppComponent;
//# sourceMappingURL=ng2-app.component.js.map