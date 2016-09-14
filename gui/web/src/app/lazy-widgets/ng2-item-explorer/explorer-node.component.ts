import { Component, Inject, Input } from '@angular/core';
import { Ng2ItemExplorerService } from './explorer.service';
import { Subscription } from 'rxjs/Subscription';
import { HierarchyNode } from '../../spTypes';
import { EventBusService } from "../../core/event-bus.service";
import { Item } from '../../spTypes';
 
@Component({
    selector: 'explorer-node',
    templateUrl: 'app/lazy-widgets/ng2-item-explorer/explorer-node.component.html',
    directives: [ItemExplorerNodeComponent]
})

export class ItemExplorerNodeComponent {
    @Input() node: HierarchyNode;

    name: string = "";
    getName: (id: string) => string;
    sendSelected: () => void;
    sendUnselected: () => void;

    toggleExpanded: () => void;
    toggleSelected: () => void;
    expanded: boolean = false;
    selected: boolean = false;
    childless: boolean = true;
    
    constructor(
	@Inject('itemService') itemService,
	evBus: EventBusService

    ){
	this.getName = (id: string) => {
	    //TODO null check
	    return itemService.getItem(id).name
	}

	this.sendSelected = () => {
	    console.log("VI klickar");
	    console.log(this.node);
	    console.log(this.getName(this.node.item));
            evBus.tweetToTopic<string[]>("itemSelected", [this.node.item]); 
	}
	this.sendUnselected = () => {
            evBus.tweetToTopic<string[]>("itemUnselected", [this.node.item]); 	
	}

	this.toggleExpanded = () => {
	    this.expanded = !this.expanded;	    
	}
	this.toggleSelected = () => {
	    this.selected = !this.selected;
	    if(this.selected){
		this.sendSelected();
	    } else {
		this.sendUnselected();
	    }
	}
    }

    ngOnInit(){
	this.name = this.getName(this.node.item);
	this.childless = this.node.children.length == 0;
    }
}





