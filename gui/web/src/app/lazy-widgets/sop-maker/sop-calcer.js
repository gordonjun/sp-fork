(function () {
    'use strict';

    angular
        .module('app.sopMaker')
        .service('sopCalcer', sopCalcer);

    sopCalcer.$inject = ['itemService'];
    /* @ngInject */
    function sopCalcer(itemService) {
        var service = {
            calcStructMeasures: calcStructMeasures,
            makeIt: makeIt,
            createSOP: createSOP,
            fillResult: fillResult,
            getWidth: getWidth,
            calcOpWidth: calcOpWidth,
            calcOpHeight: calcOpHeight
        };

        activate();

        return service;

        function activate() {

        }

        function calcStructMeasures(sop, measures, para, dirScope) {
            var structMeasures = [];

            if(sop.isa === 'Sequence') {
                structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height;
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
                }
            } else if (sop.isa === 'Other') {
                structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = sop.clientSideAdditions.height-measures.margin;
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
                }
            } else if (sop.isa === 'Parallel' || sop.isa === 'Arbitrary') {
                structMeasures.x21 = 0; structMeasures.y21 = para; structMeasures.x31 = 0; structMeasures.y31 = sop.clientSideAdditions.height-measures.margin-para;
                structMeasures.x41 = 0; structMeasures.y41 = sop.clientSideAdditions.height-measures.margin; structMeasures.width = sop.clientSideAdditions.width; structMeasures.height = 0;
                structMeasures.x51 = 0; structMeasures.y51 = para/2; structMeasures.x61 = 0; structMeasures.y61 = sop.clientSideAdditions.height-measures.margin-para/2;
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
                    structMeasures.x31 = [structMeasures.y31, structMeasures.y31 = structMeasures.x31][0];
                    structMeasures.x41 = [structMeasures.y41, structMeasures.y41 = structMeasures.x41][0];
                    structMeasures.x51 = [structMeasures.y51, structMeasures.y51 = structMeasures.x51][0];
                    structMeasures.x61 = [structMeasures.y61, structMeasures.y61 = structMeasures.x61][0];
                    structMeasures.width = [structMeasures.height, structMeasures.height = structMeasures.width][0];
                }
            } else if (sop.isa === 'Alternative') {
                structMeasures.x11 = sop.clientSideAdditions.width/2 - sop.clientSideAdditions.lineL; structMeasures.y11 = 0; structMeasures.x12 = sop.clientSideAdditions.lineL + sop.clientSideAdditions.lineR; structMeasures.y12 = 0;
                structMeasures.x21 = sop.clientSideAdditions.width/2 - sop.clientSideAdditions.lineL; structMeasures.y21 = sop.clientSideAdditions.height - measures.margin; structMeasures.x22 = sop.clientSideAdditions.lineL + sop.clientSideAdditions.lineR; structMeasures.y22 = 0;
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    structMeasures.x11 = [structMeasures.y11, structMeasures.y11 = structMeasures.x11][0];
                    structMeasures.x12 = [structMeasures.y12, structMeasures.y12 = structMeasures.x12][0];
                    structMeasures.x21 = [structMeasures.y21, structMeasures.y21 = structMeasures.x21][0];
                    structMeasures.x22 = [structMeasures.y22, structMeasures.y22 = structMeasures.x22][0];
                }
            }
            return structMeasures;
        }

        function makeIt(dirScope, measures) {
            var mC = {
                'margin' : measures.margin,
                'opH' : measures.opH,
                'opW' : measures.opW,
                'para' : measures.para,
                'arrow' : measures.arrow,
                'textScale': measures.textScale,
                'condLineHeight': measures.condLineHeight,
                'nameLineHeight': measures.nameLineHeight
            };

            if (!dirScope.vm.sopSpecCopy.vertDir) { // Swap ops minimum width and height if horizontal dir
                var tempw = mC.opW;
                mC.opW = mC.opH;
                mC.opH = tempw;
            }

            var sopWidth = 0,
                sopHeight = 0;

            for(var i = 0; i < dirScope.vm.sopSpecCopy.sop.length; i++) {
                var j;
                if(dirScope.vm.sopSpecCopy.vertDir) {
                    j = i;
                } else {
                    j = dirScope.vm.sopSpecCopy.sop.length - 1 - i; // To avoid flip on SOP direction animation
                }
                var w = service.getWidth(dirScope.vm.sopSpecCopy.sop[j], mC, dirScope, true);
                if(w < mC.opW) {
                    w = mC.opW;
                }
                var result = service.createSOP(dirScope.vm.sopSpecCopy.sop[j], mC, sopWidth + w / 2, 0, false, 0, dirScope, true);
                /*var rawCopy = {};
                 angular.copy(dirScope.vm.sopSpecCopy, rawCopy);
                 console.log(rawCopy);*/
                if(result.height > sopHeight) {
                    sopHeight = result.height;
                }
                sopWidth += (w + mC.margin);
            }

            sopHeight = sopHeight + 20; // To contain the ending line
            sopWidth = sopWidth + 20;

            if (!dirScope.vm.sopSpecCopy.vertDir) { // Swap SOP width and height if horizontal dir
                var tempw2 = sopWidth;
                sopWidth = sopHeight;
                sopHeight = tempw2;
            }

            dirScope.vm.sopSpecCopy.width = sopWidth;
            dirScope.vm.sopSpecCopy.height = sopHeight;

        }

        function createSOP(sequence, measures, middle, start, parentObject, parentObjectIndex, dirScope, firstLoop) {
            // Save of parent reference and array index into the JSON tree to enable localization on add/remove
            sequence.clientSideAdditions.parentObject = parentObject;
            sequence.clientSideAdditions.parentObjectIndex = parentObjectIndex;

            var result = {
                'operations' : [],
                'structs' : [],
                'lines' : [],
                'width' : 0,
                'height' : 0,
                'scale' : 100,
                'dir' : dirScope.vm.sopSpecCopy.vertDir,
                'x' : 0,
                'y' : 0
            }, drawHere, sub;

            if(typeof sequence.clientSideAdditions.lines === 'undefined') {
                sequence.clientSideAdditions.lines = [];
            }

            if(firstLoop === true) { // to make room for first line
                start = start + measures.margin;
            }

            if (sequence.isa === 'Hierarchy') {
                var op = itemService.getItem(sequence.operation);
                if (sequence.sop.length > 0){
                    console.log('hierarchy children not supported yet');
                    /*sub = this.createSOP(sequence.sop, measures, middle, start, sequence, 0, dirScope, false);
                     result = this.fillResult(result, sub);

                     result.height = (result.height + sub.height);
                     if (result.width < sub.width) {result.width = sub.width;}*/

                } else {

                    result.width = this.calcOpWidth(op, measures, dirScope, sequence);
                    result.height = this.calcOpHeight(op, measures, dirScope, sequence) + measures.margin;
                    var arrow = measures.arrow;
                    if (start < measures.arrow) {arrow = 0;}

                    // Save of Op measures straight into the SOP
                    sequence.clientSideAdditions.width = result.width;
                    sequence.clientSideAdditions.height = result.height - measures.margin;
                    sequence.clientSideAdditions.x = (middle - (result.width / 2));
                    sequence.clientSideAdditions.y = start;
                    sequence.clientSideAdditions.arrow = 'M ' + (result.width/2 - arrow) + ' ' + (-arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + arrow + ' ' + -arrow + ' Z';

                    // Swap width, height and x, y if Horizontal direction
                    if(!dirScope.vm.sopSpecCopy.vertDir) {
                        sequence.clientSideAdditions.height = [sequence.clientSideAdditions.width, sequence.clientSideAdditions.width = sequence.clientSideAdditions.height][0];
                        sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
                        sequence.clientSideAdditions.arrow = 'M ' + (-arrow) + ' ' + (result.width/2 - arrow) + ' l ' + arrow + ' ' + arrow + ' l ' + -arrow + ' ' + arrow + ' Z';
                    }

                }
            } else if (sequence.isa === 'Sequence') {
                drawHere = start;

                for (var m = 0; m < sequence.sop.length; m++) {
                    sub = this.createSOP(sequence.sop[m], measures, middle, drawHere, sequence, m, dirScope, false);
                    result = this.fillResult(result, sub);

                    sequence.clientSideAdditions.lines.push({
                        x1 : middle,
                        y1 : start + result.height + sub.height - 1,
                        x2 : 0,
                        y2 : -measures.margin + 1
                    });

                    result.height = (result.height + sub.height);
                    if (result.width < sub.width) {result.width = sub.width;}

                    drawHere = start + result.height;

                }

            } else {  // Parallel, Alternative, Other or Arbitrary

                result.width = this.getWidth(sequence, measures, dirScope, false);
                drawHere = middle - (result.width / 2) + measures.margin;
                var linepos = [];
                var lineMinusL = measures.margin;
                var lineMinusR = measures.margin;
                var para;
                if (sequence.isa === 'Alternative') {
                    para = 0;
                } else {
                    para = measures.para;
                }
                var sopLength = sequence.sop.length;
                for (var n = 0; n < sopLength; n++) {
                    var o = n;
                    if(!dirScope.vm.sopSpecCopy.vertDir) {
                        o = sopLength - 1 - n;
                    }
                    var subW = this.getWidth(sequence.sop[o], measures, dirScope, false);
                    drawHere = drawHere + subW / 2;
                    sub = this.createSOP(sequence.sop[o], measures, drawHere, start + para + measures.margin, sequence, o, dirScope, false);

                    sequence.clientSideAdditions.lines.push({ // The lines above the structs
                        x1 : drawHere,
                        y1 : start + para - 1,
                        x2 : 0,
                        y2 : measures.margin + 1
                    });

                    result = this.fillResult(result, sub);
                    linepos.push({
                        'x' : drawHere,
                        'startY' : start + para + sub.height,
                        'subSopIndex' : o
                    });
                    if (result.height < (sub.height + para + measures.margin)) {
                        result.height = sub.height + para + measures.margin;
                    }
                    drawHere = drawHere + subW / 2 + measures.margin;

                    if(sequence.isa === 'Alternative' && sopLength > 1) { // shorten the horizontal lines if more than one op in an alternative struct
                        if(o === 0) lineMinusL = lineMinusL + subW/2;
                        else if(o === sopLength - 1) lineMinusR = lineMinusR + subW/2;
                    }

                }

                // Swap if horizontal direction
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    lineMinusL = [lineMinusR, lineMinusR = lineMinusL][0];
                }

                if(typeof sequence.clientSideAdditions.lines2 === 'undefined') {
                    sequence.clientSideAdditions.lines2 = [];
                } else {
                    sequence.clientSideAdditions.lines2.forEach( function(line) {
                        line.drawnLine.remove();
                        line.drawnShadow.remove();
                    });
                    sequence.clientSideAdditions.lines2 = [];
                }

                for (var p = 0; p < linepos.length; p++) {

                    // The lines below the structs
                    sequence.clientSideAdditions.lines2.push({
                        x1 : linepos[p].x,
                        y1 : linepos[p].startY,
                        x2 : 0,
                        y2 : (result.height - (linepos[p].startY - start)),
                        subSopIndex : linepos[p].subSopIndex
                    });
                }

                if(result.height === 0) { // Increasing width and height of empty struct
                    result.height = measures.margin;
                    result.width = measures.opW + lineMinusR + lineMinusL;
                }

                result.height = result.height + para + measures.margin;

                // Save of struct attributes straight into the SOP
                sequence.clientSideAdditions.width = result.width;
                sequence.clientSideAdditions.height = result.height;
                sequence.clientSideAdditions.lineL = result.width / 2 - lineMinusL;
                sequence.clientSideAdditions.lineR = result.width / 2 - lineMinusR;
                sequence.clientSideAdditions.x = middle - (result.width / 2);
                sequence.clientSideAdditions.y = start;

                // Swap if horizontal direction
                if(!dirScope.vm.sopSpecCopy.vertDir) {
                    sequence.clientSideAdditions.x = [sequence.clientSideAdditions.y, sequence.clientSideAdditions.y = sequence.clientSideAdditions.x][0];
                }

                sequence.clientSideAdditions.structMeasures = service.calcStructMeasures(sequence, measures, para, dirScope);

            }

            if(firstLoop === true) { // first launch only
                sequence.clientSideAdditions.lines.push({ // the starting line above the whole SOP
                    x1 : middle,
                    y1 : start,
                    x2 : 0,
                    y2 : -(measures.margin+1)
                });
            }

            result.x = middle - result.width / 2;
            result.y = start;
            return result;
        }

        function fillResult(result, fill) {
            for(var q = 0; q < fill.operations.length; q++) {
                result.operations.push(fill.operations[q]);
            }
            for(var r = 0; r < fill.structs.length; r++) {
                result.structs.push(fill.structs[r]);
            }
            for(var s = 0; s < fill.lines.length; s++) {
                result.lines.push(fill.lines[s]);
            }
            return result;
        }

        function getWidth(sop, measures, dirScope, firstLaunch) {
            var w, nW;

            // Creating an empty object in each node to gather all additions made by sopCalcer and sopDrawer
            if(typeof sop.clientSideAdditions === 'undefined') {
                sop.clientSideAdditions = {};
            }

            if (sop.isa === 'Hierarchy') {
                var op = itemService.getItem(sop.operation);

                if(firstLaunch) {
                    //handleHierarchy(op, sop);
                    addConditions(op, sop, dirScope);
                }

                /*if(sop.sop.length > 0) {
                 return this.getWidth(sop.sop, measures, dirScope, firstLaunch);
                 } else {*/
                return this.calcOpWidth(op, measures, dirScope, sop);
                //}

            }

            if (sop.isa === 'Sequence') {
                w = 0;
                for (var n = 0; n < sop.sop.length; n++) {
                    nW = this.getWidth(sop.sop[n], measures, dirScope, firstLaunch);
                    if (nW > w) {
                        w = nW;
                    }
                }
                return w;
            } else {
                w = 0;
                for (var o = 0; o < sop.sop.length; o++) {
                    nW = this.getWidth(sop.sop[o], measures, dirScope, firstLaunch);
                    w = w + nW + measures.margin;
                }
                if(w === 0) {
                    w = measures.opW + 2 * measures.margin;
                } else {
                    w = w + measures.margin;
                }
                return w;
            }
        }

        function handleHierarchy(op, sop) {
            if(typeof sop.sop === 'String') {
                console.log('trying to get sopSpec by id');
                var sopSpec = itemService.getItem(sop.sop);
                if(sopSpec) {
                    sop.sop = sopSpec.sop;
                }
            } else if(sop.sop.length === 0) {
                console.log('adding fake sop');
                sop.sop = {
                    isa: "Sequence",
                    sop: [
                        {
                            isa: "Hierarchy",
                            sop: [],
                            operation: "24285cc2-c608-47bf-8883-1ee8404d6ae1",
                            conditions: []
                        },
                        {
                            isa: "Hierarchy",
                            sop: [],
                            operation: "24285cc2-c608-47bf-8883-1ee8404d6ae1",
                            conditions: []
                        }
                    ]
                };

                /*var children = itemService.getItemsByIds(op.attributes.children);
                 var childOps = $filter('with')(children, {isa: 'Operation'});
                 var childOpIds = Object.keys(childOps);
                 console.log(childOpIds);
                 if(childOpIds.length > 0) {
                 var result = itemService.getSOP(childOpIds);
                 result.success(function(data) {
                 console.log(data);
                 console.log(sop);
                 sop.sop = data.sop;
                 });
                 }*/
            }
        }

        function calcOpWidth(op, measures, dirScope, struct) {
            var longestString = longestConditionString(struct, op);
            var summedTextHeight = sumTextHeight(struct, measures);
            if (dirScope.vm.sopSpecCopy.vertDir && (measures.opW / longestString.length) < measures.textScale) {
                return measures.textScale * longestString.length;
            } else if(!dirScope.vm.sopSpecCopy.vertDir && summedTextHeight > measures.opW) {
                return summedTextHeight;
            } else {
                return measures.opW;
            }
        }

        function calcOpHeight(op, measures, dirScope, struct) {
            var longestString = longestConditionString(struct, op, measures);
            var summedTextHeight = sumTextHeight(struct, measures);
            if (dirScope.vm.sopSpecCopy.vertDir && summedTextHeight > measures.opH) {
                return summedTextHeight;
            } else if(!dirScope.vm.sopSpecCopy.vertDir && (measures.opH / longestString.length) < measures.textScale) {
                return measures.textScale * longestString.length;
            } else {
                return measures.opH;
            }
        }

        function addConditions(op, sop, dirScope) {
            // pick the specially prepared conditions in sop if present
            var conditions = [];
            if(dirScope.vm.widget.storage.viewAllConditions && op.conditions) {
                op.conditions.forEach(function(opCond) {
                    conditions.push(opCond);
                });
            } else if(sop.conditions) {
                sop.conditions.forEach(function(sopCond) {
                    conditions.push(sopCond);
                });
            }

            // convert proposition format to text and put it in the sop

            var kinds = ['preGuards', 'postGuards', 'preActions', 'postActions'];

            kinds.forEach(function(kind) {
                sop.clientSideAdditions[kind] = [];
            });

            for(var i = 0; i < conditions.length; i++) {
                if (conditions[i].attributes.kind === 'precondition') {
                    var preGuardAsText = guardAsText(conditions[i].guard);
                    if (preGuardAsText !== '') {
                        sop.clientSideAdditions[kinds[0]].push(preGuardAsText);
                    }
                    var preActionAsText = actionAsText(conditions[i].action);
                    if (preActionAsText !== '') {
                        sop.clientSideAdditions[kinds[2]].push(preActionAsText);
                    }
                } else if(conditions[i].attributes.kind === 'postcondition') {
                    var postGuardAsText = guardAsText(conditions[i].guard);
                    if (postGuardAsText !== '') {
                        sop.clientSideAdditions[kinds[1]].push(postGuardAsText);
                    }
                    var postActionAsText = actionAsText(conditions[i].action);
                    if (postActionAsText !== '') {
                        sop.clientSideAdditions[kinds[3]].push(postActionAsText);
                    }
                }
            }
            // Place out and-operators if multiple guards or actions
            kinds.forEach(function(kind) {
                for(var j = 0; j < sop.clientSideAdditions[kind].length-1; j++) {
                    sop.clientSideAdditions[kind][j] = sop.clientSideAdditions[kind][j] + ' ^';
                }
            });
            // Place out guard-action separating slash on correct place if needed
            for(var k = 0; k <= 1; k++) {
                var noOfGuards = sop.clientSideAdditions[kinds[k]].length;
                var noOfActions = sop.clientSideAdditions[kinds[k+2]].length;
                if(noOfGuards > 0) {
                    sop.clientSideAdditions[kinds[k]][noOfGuards-1] = sop.clientSideAdditions[kinds[k]][noOfGuards-1] + ' /';
                } else if(noOfActions > 0) {
                    sop.clientSideAdditions[kinds[k+2]][noOfActions-1] = '/ ' + sop.clientSideAdditions[kinds[k+2]][noOfActions-1];
                }
            }

            function getNameFromId(id) {
                var item = itemService.getItem(id);
                if(item === null) {
                    return '';
                } else {
                    return item.name;
                }
            }

            function handleProp(prop) {
                if(prop.hasOwnProperty('id')) {
                    return getNameFromId(prop.id);
                } else if(prop.hasOwnProperty('v')) {
                    return prop.v;
                } else {
                    return prop;
                }
            }

            function guardAsText(prop) {
                var operator;
                if(prop.isa === 'EQ' || prop.isa === 'NEQ' || prop.isa === 'GREQ' || prop.isa === 'LEEQ' || prop.isa === 'GR' || prop.isa === 'LE') {
                    var left = handleProp(prop.left),
                        right = handleProp(prop.right);
                    if(prop.isa === 'EQ') {
                        operator = ' == ';
                    } else if(prop.isa === 'NEQ') {
                        operator = ' != ';
                    } else if(prop.isa === 'GREQ') {
                        operator = ' >= ';
                    } else if(prop.isa === 'LEEQ') {
                        operator = ' <= ';
                    } else if(prop.isa === 'GR') {
                        operator = ' > ';
                    } else { //prop.isa === 'LE')
                        operator = ' < ';
                    }
                    if(left === right) {
                        return '';
                    } else {
                        return left + operator + right;
                    }
                } else if(prop.isa === 'AND' || prop.isa === 'OR') {
                    var operatorType = '\&';
                        if (prop.isa === 'OR') {
                            operatorType = '\|';
                        }
                    operator = ' ' + operatorType + ' ';
                    var line = '';
                    for(var i = 0; i < prop.props.length; i++) {
                        if(i > 0) {
                            line = line + operator;
                        }
                        var nextPropText = guardAsText(prop.props[i]);
                        if (prop.props[i].hasOwnProperty('props')) {
                            if(prop.props[i].props.length>1) {
                                nextPropText = '(' + nextPropText + ')';
                            }
                        }
                        line = line + nextPropText;
                    }
                    return line;
                } else if(prop.isa === 'NOT') {
                    return '!' + handleProp(prop.p);
                } else {
                    return '';
                }
            }

            function actionAsText(action) {
                var textLine = '';

                for(var i = 0; i < action.length; i++) {
                    if(i > 0) {
                        textLine = textLine + '; ';
                    }
                    var actionValue = false;
                    if (action[i].value.isa == "SVIDEval") {
                        actionValue = action[i].value.isa.id;
                    } else {
                        actionValue = action[i].value.v;
                    }

                    textLine = textLine + getNameFromId(action[i].id) + ' = ' + actionValue;
                }
                return textLine;
            }

        }

        function longestConditionString(struct, op) {
            var textStrings = struct.clientSideAdditions.preGuards.concat(struct.clientSideAdditions.preActions, struct.clientSideAdditions.postGuards, struct.clientSideAdditions.postActions);
            textStrings.push(op.name);
            return textStrings.reduce(function (a, b) { return a.length > b.length ? a : b; });
        }

        function sumTextHeight(struct, measures) {
            var noOfTextStrings = 1 + struct.clientSideAdditions.preGuards.length + struct.clientSideAdditions.preActions.length + struct.clientSideAdditions.postGuards.length + struct.clientSideAdditions.postActions.length;
            return noOfTextStrings * measures.condLineHeight + measures.nameLineHeight;
        }

    }
})();
