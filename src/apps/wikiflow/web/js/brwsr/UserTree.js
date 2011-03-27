/**
 * (c) 2006 Itensil, Inc.
 * ggongaware (at) itensil.com
 *
 *  User Accounts
 * 
 */

var UserTree = {

    userSvcUri : "../uspace/",
    roleSvcUri : "../act/",

    __selfId : null,
    __users : null,
    __groups : null,
    __usrLoadCnt : 0,
    xb : new XMLBuilder(),

    getUserNode : function(id) {
        if (this.__users == null) {
            this.loadUsers();
        }
        return this.__users[id];
    },

    getGroupNode : function(id) {
        if (this.__groups == null) {
            this.loadUsers();
        }
        return this.__groups[id];
    },

    loadUsers : function() {
        var doc = this.xb.loadURI(this.userSvcUri + "list?cnt=" + UserTree.__usrLoadCnt);
        if (App.checkError(doc)) return;
        this.__users = new Object();
        this.__groups = new Object();
        UserTree.__usrLoadCnt++;
        this.__selfId = doc.documentElement.getAttribute("self");
        this.__selfRoles = doc.documentElement.getAttribute("self-roles").split(" ");
        var usrElems = Xml.match(doc.documentElement, "user");
        var ii;
        for (ii = 0; ii < usrElems.length; ii++) {
            this.__users[usrElems[ii].getAttribute("id")] = usrElems[ii];
        }
        var grpElems = Xml.match(doc.documentElement, "group");
        for (ii = 0; ii < grpElems.length; ii++) {
            this.__groups[grpElems[ii].getAttribute("id")] = grpElems[ii];
        }
    },

    getSelfId : function() {
        if (this.__selfId == null) {
            this.loadUsers();
        }
        return this.__selfId;
    },
    
    selfInRole : function(role) {
    	UserTree.getSelfId();
    	return arrayIndex(this.__selfRoles, role) >= 0;
    },

    getUserName : function(id) {
        var usr = this.getUserNode(id);
        return usr ? usr.getAttribute("name") : "???";
    },
    
   	getGroupName : function(id) {
    	var gNode = this.getGroupNode(id);
		return gNode ? gNode.getAttribute("name") : "???";
   	},

    getUserMenu : function(menuB) {
        if ((menuB ? this.__userMenu : this.__userMenuB) == null) {
            
            this[(menuB ? "__userMenu" : "__userMenuB")] = 
            	new Menu(new MenuModel(UserTree.__getUserMenuItems));
        }
        return (menuB ? this.__userMenu : this.__userMenuB);
    },
    
    __getUserMenuItems : function() {
		if (UserTree.__users == null) {
            UserTree.loadUsers();
        }
        var menItms = [];
        for (var uid in UserTree.__users) {
            var usr = UserTree.__users[uid];
            menItms.push({label:usr.getAttribute("name") + " <" + usr.getAttribute("email") + ">",
                name:usr.getAttribute("name"), uid:uid});
        }
        menItms.push({isSep:true});
        menItms.push({label:"Invite...", icon:"mb_usrIco", act:UserTree.invite});
        return menItms;
    },

    getGroupMenu : function(skipEveryone) {
        if (this.__groupMenu == null || this.__groupMenu.model.skipEveryone != skipEveryone) {
            var mod = new MenuModel(UserTree.__getGroupMenuItems);
            mod.skipEveryone = skipEveryone;
            this.__groupMenu = new Menu(mod);
        }
        return this.__groupMenu;
    },
    
    __getGroupMenuItems : function() {
    	if (UserTree.__groups == null) {
            UserTree.loadUsers();
        }
        var menItms = [];
        var evo;
        for (var gid in UserTree.__groups) {
            var grp = UserTree.__groups[gid];
            if (grp.getAttribute("everyone") == "1") {
                evo = grp;
            } else {
                menItms.push({label:grp.getAttribute("name"),
                    name:grp.getAttribute("name"), gid:gid});
            }
        }
        if (!this.skipEveryone) {
            menItms.push({isSep:true});
            menItms.push({label:evo.getAttribute("name"),
                        name:evo.getAttribute("name"),
                        everyone:true,
                        gid:evo.getAttribute("id")});
        }
        return menItms;
    },

    invite : function() {
        var diag = xfDialog("Invite", true, document.body, "../view-usr/invite.xfrm.jsp", UserTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.addEventListener("xforms-close", { handleEvent : function() {
                // reset user list
                UserTree.refresh();
            }});
        diag.show(200, 200);
    },
    
    addStudent : function() {
        var diag = xfDialog("Add Student", true, document.body, "../view-usr/add-student.xfrm", UserTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        xfMod.addEventListener("xforms-close", { handleEvent : function() {
                // reset user list
                UserTree.refresh();
                if (ActivityTree.__tree) {
        			ActivityTree.__tree.redrawAll();
        		}
            }});
        diag.show(200, 200);
    },
    
    
   	addOrg : function(evt, context) {
        var diag = xfDialog("Add Organization", true, document.body, "../view-usr/org.xfrm", UserTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        	
        var xfMod = diag.xform.getDefaultModel();
        if (context.constructor === OrgTreeItem) {
        	Tree.selectItem(context);
        	xfMod.setValue("parentId", context.getId());
        }
        
        xfMod.addEventListener("xforms-close", { handleEvent : function() {
                if (context.constructor === OrgTreeItem) {
                	//context._tree.redraw(context, Tree.HINT_INSERT);
                	context._tree.redrawAll();
                } else {
                	context.redrawAll();
                }
                
            }});
        diag.show(200, 200);
    },
    

    selfSet : function() {
        var diag = xfDialog("Settings", true, document.body, "../view-usr/self.xfrm", UserTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        diag.showModal(200, 200);
    },

    selfPass : function() {
        var diag = xfDialog("Password", true, document.body, "../view-usr/pass.xfrm", UserTree.xb,
        	null, null, null, false, App ? App.chromeHelp : null);
        var xfMod = diag.xform.getDefaultModel();
        diag.showModal(200, 200);
    },

    usrManProps : function(evt, item) {
        if (UserTree.__selfId == item.node.getAttribute("id")) return;
        var diag = xfDialog("Properties: " + item.node.getAttribute("name"),
                true, document.body, "../view-usr/user.xfrm", UserTree.xb, null,
                UserTree.userSvcUri + "getUserMan?id=" + item.node.getAttribute("id"),
                null, false, App ? App.chromeHelp : null);
        diag.xform.getDefaultModel().addEventListener("xforms-close", { handleEvent : function() {
                item._tree.redrawAll();
            }});
        diag.show(getMouseX(evt), getMouseY(evt));
    },

    usrManRemove : function(evt, item) {
        if (UserTree.__selfId == item.node.getAttribute("id")) return;
        Tree.selectItem(item);
        if (confirm("Are you sure?")) {
            if (!App.checkError(UserTree.xb.loadURI(
                    UserTree.userSvcUri + "removeUser?id=" + item.node.getAttribute("id")))) {
                item.node.parentNode.removeChild(item.node);
                item.removeItem();
                UserTree.refresh();
            }
        }
    },

    usrManGroupRemove : function(evt, item) {
        Tree.selectItem(item);
        if (confirm("Are you sure?")) {
            if (!App.checkError(UserTree.xb.loadURI(
                    UserTree.userSvcUri + "leaveGroup?group=" + item.node.getAttribute("id") +
                    "&user=" + item.node.parentNode.getAttribute("id")))) {
                item.node.parentNode.removeChild(item.node);
                item.removeItem();
            }
        }
    },

    usrManGroupAdd  : function(evt, item, menuItem) {
         if (!App.checkError(UserTree.xb.loadURI(
                UserTree.userSvcUri + "joinGroup?group=" + menuItem.gid +
                "&user=" + item.node.getAttribute("id")))) {
            var gElem = Xml.element(item.node, "group");
            gElem.setAttribute("id", menuItem.gid);
            gElem.setAttribute("name", menuItem.name);
            item._tree.redraw(item, Tree.HINT_INSERT);
         }
    },

    manUsers : function() {
        var diag = new Dialog("Manage Users", true);
        diag.initHelp(App.chromeHelp);
        diag.render(document.body);
        var hElem = makeElement(makeElement(diag.contentElement, "div", "userDiag"), "div", "userSubDiag");
        var modl = new UserManTreeModel();
        var grid = new Grid(modl);
        grid.addHeader(noBreakString("User / Group"));
        grid.addHeader("Email");
        grid.addHeader(noBreakString("Community Roles"));
        var tree = new Tree(modl);
        var grpMenu = UserTree.getGroupMenu(true);
        tree.menu = new Menu(new MenuModel(
         [
                 {label : "Properties", icon : "mb_optIco", act : UserTree.usrManProps},
                 {label : "Add Group", icon : "mb_addIco", sub : grpMenu},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : UserTree.usrManRemove}
         ]));
        tree.menu2 = new Menu(new MenuModel(
         [
                 {label : "Remove Membership", icon : "mb_remIco", act : UserTree.usrManGroupRemove}
         ]));
        grpMenu.defaultAct = function(evt, context) {
                UserTree.usrManGroupAdd(evt, context, this);
            };

        grid.render(hElem, "userGrid");
        tree.render(hElem, grid.getTreeStyle(), "userTree");
        diag.addDisposable(grid);
        diag.addDisposable(tree);
        App.addDispose(diag);
        diag.showModal(200, 200);
        grid.onresize = function(rect) {
            rect.h += 24;
            rect.w += 24;
            diag.contentResized(rect);
        };
        
        hElem = null; // IE enclosure clean-up
    },
    
    renderUserActs : function(uiParent, src) {
      
        var modl = new UserActTreeModel(src);
        var grid = new Grid(modl);
        grid.addHeader(noBreakString(App.edu ? "Student / Course" : "User / Activity"));
        grid.addHeader(App.edu ? "Student ID / Status" : "Email / Task");
        grid.addHeader(noBreakString("Due"));
        var tree = new Tree(modl);
        
        var mnOpts = [
        		 {label : "Properties", icon : "mb_optIco", act : UserTree.usrManProps},
                 {isSep : true },
                 {label : "Remove " + (App.edu ? "Student" : "User"), icon : "mb_remIco", act : UserTree.usrManRemove}];
                 
        if (App.edu) 
        	arrayInsert(mnOpts, 0, {label : "Add Course", icon : "mb_addIco", act : ActivityTree.addCourse});
        	
        tree.menu = new Menu(new MenuModel(mnOpts));
         
        tree.menu2 = new Menu(new MenuModel(
             [
                 {label : "Workzone", icon : "mb_workIco", act : ActivityTree.workzone},
                 {label : "Properties", icon : "mb_optIco", act : ActivityTree.properties},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : ActivityTree.remove}
             ]));

        grid.render(uiParent, "activityGrid");
        tree.render(uiParent, grid.getTreeStyle(), "fileTree activityTree");
        App.addDispose(grid);
        App.addDispose(tree);
        
        ActivityTree.__tree = tree;
        
        addEventHandler(window, "resize",
            function(evt) {
                grid.resize(getBounds(grid.__hParent));
            });
        
        uiParent = null; // IE enclosure clean-up
    },
    
    renderOrgMemInfo : function(uiParent, memNode) {
    	if (UserTree._lastMemXfrm) {
    		UserTree._lastMemXfrm.dispose();
    		UserTree._lastMemXfrm = null;
    	}
    	var grpNode = memNode.parentNode;
    	makeElementNbSpd(uiParent, "div");
    	makeElement(uiParent, "div", "minorHead", grpNode.getAttribute("name") + " - Member Info");
    	var frmUri = "../view-usr/member.xfrm.jsp";
    	var xf = new XForm(UserTree.xb.loadURI(frmUri), "member_xfrm", UserTree.xb, frmUri);
        xf.setDefaultUris(UserTree.userSvcUri + "getMember?user=" +
        	 memNode.getAttribute("id") + "&group=" + grpNode.getAttribute("id"));
    	xf.render(makeElement(uiParent, "div", "wikiView"));
    	UserTree._lastMemXfrm = xf;
    	App.addDispose(xf);
    },
    
    renderOrgActs : function(uiParent, userId) {
    	
    	if (!userId) {
			uiParent.innerHTML = "<div style='padding:16px;font-size:12px'>&lt; Select a member from the organization.</div>";
			return;
		} else {
			uiParent.innerHTML = "<div class='minorHead'>" +  UserTree.getUserName(userId) + "</div>";
		}
      
    	var hElem = makeElement(uiParent, "div", "minorTreeBox");
      
        var modl = new OrgActTreeModel(userId);
        var grid = new Grid(modl);
        grid.addHeader("Activity");
        grid.addHeader("Task");
        grid.addHeader("Due");
        var tree = new Tree(modl);

        tree.menu = new Menu(new MenuModel(
             [
                 {label : "Workzone", icon : "mb_workIco", act : ActivityTree.workzone},
                 {label : "Properties", icon : "mb_optIco", act : ActivityTree.properties},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : ActivityTree.remove}
             ]));

        grid.render(hElem, "activityGrid");
        tree.render(hElem, grid.getTreeStyle(), "fileTree activityTree");
        App.addDispose(grid);
        App.addDispose(tree);
        
        ActivityTree.__tree = tree;
        
        uiParent = null; // IE enclosure clean-up
    },
    
    
    renderOrgs : function(uiParent, infoUi) {
    	UserTree.renderOrgActs(infoUi);
    	
    	makeElement(uiParent, "div", "minorHead", "Organizations and Members");
    	
    	
      	var hElem = makeElement(uiParent, "div", "minorTreeBox");
        var modl = new OrgTreeModel();
        var grid = new Grid(modl);
        grid.addHeader("Name");
        grid.addHeader("Description / Position");
        grid.addHeader("ID");
        
        var tree = new Tree(modl);
        
        var userMenu = UserTree.getUserMenu();
        tree.menu = new Menu(new MenuModel(
         [
         		 {label : "Properties", icon : "mb_optIco", act : null },
                 {label : "Add Member", icon : "mb_usrIco", sub : userMenu},
                 {isSep : true },
                 {label : "Add Sub Organization", icon : "mb_addIco", act : function(evt, context) {
                 		UserTree.addOrg(evt, context);
                 	}},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : UserTree.grpManRemove}
         ]));
        tree.menu2 = new Menu(new MenuModel(
         [
         		{label : "Member Info", icon : "mb_viewIco", act : function(evt, context) {
         				UserTree.renderOrgActs(infoUi, context.node.getAttribute("id"));
         				UserTree.renderOrgMemInfo(infoUi, context.node);
         			} },
         		{isSep : true },
                {label : "Remove Membership", icon : "mb_remIco", act : UserTree.grpManUserRemove}
         ]));
        userMenu.defaultAct = function(evt, context) {
                UserTree.grpManUserAdd(evt, context, this);
       	};
		
        grid.render(hElem, "activityGrid");
        tree.render(hElem, grid.getTreeStyle(), "fileTree activityTree");
        
        var addLink = makeElement(hElem, "div", "minorBtn", "Add Top Organization...");
		makeElementNbSpd(addLink, "div", "mbIco mb_addIco");
		setEventHandler(addLink, "onclick", 
				function(evt) {
					UserTree.addOrg(evt, tree);
				});
				
        App.addDispose(grid);
        App.addDispose(tree);
                
        uiParent = null; // IE enclosure clean-up
    },

    grpManRemove : function(evt, item) {
        Tree.selectItem(item);
        if (confirm("Are you sure?")) {
            if (!App.checkError(UserTree.xb.loadURI(
                    UserTree.userSvcUri + "removeGroup?id=" + item.node.getAttribute("id")))) {
                item.node.parentNode.removeChild(item.node);
                item.removeItem();
                UserTree.refresh();
            }
        }
    },

    grpManUserRemove : function(evt, item) {
        Tree.selectItem(item);
        if (confirm("Are you sure?")) {
            if (!App.checkError(UserTree.xb.loadURI(
                    UserTree.userSvcUri + "leaveGroup?user=" + item.node.getAttribute("id") +
                    "&group=" + item.node.parentNode.getAttribute("id")))) {
                item.node.parentNode.removeChild(item.node);
                item.removeItem();
            }
        }
    },

    grpManUserAdd  : function(evt, item, menuItem) {
         if (!App.checkError(UserTree.xb.loadURI(
                UserTree.userSvcUri + "joinGroup?user=" + menuItem.uid +
                "&group=" + item.node.getAttribute("id")))) {
            var gElem = Xml.element(item.node, "user");
            gElem.setAttribute("id", menuItem.uid);
            gElem.setAttribute("name", menuItem.name);
            if (item.constructor === OrgTreeItem) {
            	if (!item.expanded) item._tree.toggle(item);
            	item._tree.redraw(item.kids[0], Tree.HINT_INSERT);
            } else {
            	item._tree.redraw(item, Tree.HINT_INSERT);
            }
         }
    },

    grpManAdd : function() {
        var name = prompt("Group name:", "New Group");
        if (name != null && /^\w+/.test(name)) {
            if (!App.checkError(UserTree.xb.loadURI(UserTree.userSvcUri + "addGroup?name=" + Uri.escape(name)))) {
                // reset user list
                UserTree.refresh();
                this._tree.redrawAll();
            }
        }
    },

    manGroups : function() {
        var diag = new Dialog("Manage Groups", true);
        diag.initHelp(App.chromeHelp);
        diag.render(document.body);
        var modl = new GroupTreeModel();
        modl._diag = diag;
        var tree = new Tree(modl);
        var hElem = makeElement(diag.contentElement, "div", "userDiag");
        hElem = makeElement(makeElement(hElem, "div", "addGroup"), "span", "addGroup", "Add Group");
        hElem.onclick = UserTree.grpManAdd;
        hElem._tree = tree;
        hElem = makeElement(hElem.parentNode.parentNode, "div", "userSubDiag");

        var userMenu = UserTree.getUserMenu();
        tree.menu = new Menu(new MenuModel(
         [
                 {label : "Add User", icon : "mb_usrIco", sub : userMenu},
                 {isSep : true },
                 {label : "Remove", icon : "mb_remIco", act : UserTree.grpManRemove}
         ]));
        tree.menu2 = new Menu(new MenuModel(
         [
                 {label : "Remove Membership", icon : "mb_remIco", act : UserTree.grpManUserRemove}
         ]));
        userMenu.defaultAct = function(evt, context) {
                UserTree.grpManUserAdd(evt, context, this);
            };
        tree.render(hElem, "background:none", "userTree");
        diag.addDisposable(tree);
        App.addDispose(diag);
        diag.showModal(200, 200);
        hElem = null; // IE enclosure clean-up
    },
    
    refresh : function() {
    	if (this.__users != null)
            for (var uid in this.__users)
                delete this.__users[uid];
        if (this.__groups != null)
            for (var gid in this.__groups)
                delete this.__groups[gid];
        this.__users = null;
        this.__groups = null;
    	if (this.__userMenu != null) this.__userMenu.asyncRefresh();
        if (this.__userMenuB != null) this.__userMenuB.asyncRefresh();
        if (this.__groupMenu != null) this.__groupMenu.asyncRefresh();
    },

    dispose : function() {
        if (this.__userMenu != null) this.__userMenu.dispose();
        if (this.__userMenuB != null) this.__userMenuB.dispose();
        if (this.__groupMenu != null) this.__groupMenu.dispose();
        if (this.__users != null)
            for (var uid in this.__users)
                delete this.__users[uid];
        if (this.__groups != null)
            for (var gid in this.__groups)
                delete this.__groups[gid];
        this.__users = null;
        this.__groups = null;
        this.__userMenu = null;
        this.__userMenuB = null;
        this.__groupMenu = null;
    }
    
    
};

UserManTreeModel.prototype = new TreeGridModel();
UserManTreeModel.prototype.constructor = UserManTreeModel;

function UserManTreeModel() {
    TreeGridModel.apply(this, []);
    this.nCols = 3;
}

UserManTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    if (itemParent === this) { // at root
        var holdThis = this;
        UserTree.xb.loadURIAsync(UserTree.userSvcUri + "userManList",
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    } else { // child items = groups
        this.digest(itemParent.node, callback, tree, itemParent);
        this.resize();
    }
};

UserManTreeModel.prototype.digest = function(node, callback, tree, itemParent) {
    var ii, list;
    if (itemParent === this) {
        list = Xml.match(node, "user");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new TreeGridItem(this, nod.getAttribute("name"), Xml.matchOne(nod, "group") != null, "userIco");
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenuAction;
            tItm.cells.push(new GridCell(nod.getAttribute("email")));
            tItm.cells[0].__edit = false;
            var rols = nod.getAttribute("roles").split(" ");
            tItm.cells.push(new GridCell(rols.join(", ")));
            tItm.cells[1].__edit = false;
            itemParent.add(tItm);
        }
    } else {
        list = Xml.match(node, "group");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new TreeGridItem(this, nod.getAttribute("name"), false, "groupIco");
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenu2Action;
            tItm.cells.push(new GridCell(""));
            tItm.cells[0].__edit = false;
            tItm.cells.push(new GridCell(""));
            tItm.cells[1].__edit = false;
            itemParent.add(tItm);
        }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
};


GroupTreeModel.prototype = new TreeModel();
GroupTreeModel.prototype.constructor = GroupTreeModel;

function GroupTreeModel() {
    TreeModel.apply(this, []);
}


GroupTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    if (itemParent === this) { // at root
        var holdThis = this;
        UserTree.xb.loadURIAsync(UserTree.userSvcUri + "groupManList",
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    } else { // child items = users
        this.digest(itemParent.node, callback, tree, itemParent);
        this.resize();
    }
};


GroupTreeModel.prototype.resize = function() {
    var rect = this._tree.getBounds();
    rect.h += 50;
    rect.w += 50;
    this._diag.contentResized(rect);
};

GroupTreeModel.prototype.digest = function(node, callback, tree, itemParent) {
    var ii, list;
    if (itemParent === this) {
        list = Xml.match(node, "group");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new TreeItem(this, nod.getAttribute("name"), Xml.matchOne(nod, "user") != null, "groupIco");
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenuAction;
            itemParent.add(tItm);
        }
    } else {
        list = Xml.match(node, "user");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new TreeItem(this, nod.getAttribute("name"), false, "userIco");
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenu2Action;
            itemParent.add(tItm);
        }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
};


/**
 * 
 */


OrgTreeModel.prototype = new TreeGridModel();
OrgTreeModel.prototype.constructor = OrgTreeModel;

function OrgTreeModel() {
    TreeGridModel.apply(this, []);
    this.nCols = 3;
    this.itemIndex = new Object();
}


OrgTreeModel.prototype.onReady = function(callback, tree, itemParent) {
    if (itemParent === this) { // at root
        var holdThis = this;
        UserTree.xb.loadURIAsync(UserTree.userSvcUri + "orgList",
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                	holdThis.itemIndex = new Object();
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    } else { // child items
        if (!itemParent.userParent) {
            callback.apply(tree, [itemParent.kids, itemParent]);
            this.addItemRows(itemParent.kids, itemParent);
        } else {
        	var holdThis = this;
        	UserTree.xb.loadURIAsync(UserTree.userSvcUri + "groupUsers?groupId=" + itemParent.groupId,
	            function(doc, arg, xmlHttp) {
	                if (doc == null || App.checkError(doc)) {
	                    // TODO error message
	                } else {
	                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
	                    holdThis.resize();
	                }
	            });
        }
    }
};

OrgTreeModel.prototype.digest = function(node, callback, tree, itemParent) {
    var ii, list;
    var toggles = [];
    if (itemParent === this) {
        list = Xml.match(node, "group");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new OrgTreeItem(this, nod, true);
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenuAction;
            list[ii] = tItm;
            
            this.itemIndex[tItm.getId()] = tItm;
            if (!nod.getAttribute("parentId")) {
           		itemParent.add(tItm);
           		toggles.push(tItm);
            }
            
            // Top member sub item
            var uItem = new TreeGridItem(this, "Members", true, "groupIco");
            uItem.userParent = true;
            uItem.act = uItem.optAct = uItem.editAct = null;
            uItem.groupId = tItm.getId();
            uItem.cells.push(new GridCell(H.nbsp,null,true));
            uItem.cells.push(new GridCell(H.nbsp,null,true));
            tItm.add(uItem);
        }
        // pass-2 nest
        for (ii = 0; ii < list.length; ii++) {
            var tItm = list[ii];
            var pid = tItm.node.getAttribute("parentId");
            if (pid) {
            	var par = this.itemIndex[pid];
                if (par) {
                	par.add(tItm);
               		if (arrayFindStrict(toggles, par) < 0) toggles.push(par);
                }
            }
        }
        
        
    } else {
        list = Xml.match(node, "user");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new OrgTreeItem(this, nod, false);
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenu2Action;
            itemParent.add(tItm);
        }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
    toggles.sort(ActivityTreeModel.depthSort);
    
    for (ii = 0; ii < toggles.length; ii++) {
        tree.toggle(toggles[ii]);
    }
};

OrgTreeItem.prototype = new TreeGridItem();
OrgTreeItem.prototype.constructor = OrgTreeItem;

function OrgTreeItem(model, node, allowsKids) {
	if (arguments.length == 0) return;
	this.isGroup = Xml.getLocalName(node) == "group";
	if (model) TreeGridItem.apply(this, [model, node.getAttribute("name"), allowsKids, this.isGroup ? "orgIco" : "userIco"]);
	this.node = node;
	if (this.isGroup) {
		this.cells.push(new GridCell(node.getAttribute("fullName"), null, true));
		this.cells.push(new GridCell(node.getAttribute("remoteKey"), null, true));
	} else {
		this.cells.push(new GridCell(node.getAttribute("positions").replace(' ', ', '), null, true));
	}
}

OrgTreeItem.prototype.getId = function() {
	return this.node.getAttribute("id");
};

/**
 * 
 */
UserActTreeModel.prototype = new TreeGridModel();
UserActTreeModel.prototype.constructor = UserActTreeModel;

function UserActTreeModel(src) {
    TreeGridModel.apply(this, []);
    this.src = src;
    this.nCols = 3;
    this.itemIndex = {};
}

UserActTreeModel.prototype.onReady = function(callback, tree, itemParent) {
	var holdThis = this;
    if (itemParent === this) { // at root
        UserTree.xb.loadURIAsync(UserTree.userSvcUri + this.src,
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    } else { // child items = activities
        UserTree.xb.loadURIAsync(ActivityTree.serviceUri + "submittedList?ends=1&userId=" + itemParent.node.getAttribute("id"),
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    }
};

UserActTreeModel.prototype.digest = function(node, callback, tree, itemParent) {
    var ii, list;
    if (itemParent === this) {
        list = Xml.match(node, "user");
        for (ii = 0; ii < list.length; ii++) {
            var nod = list[ii];
            var tItm = new TreeGridItem(this, nod.getAttribute("name"), true, "userIco");
            tItm.node = nod;
            tItm.editAct = null;
            tItm.act = tItm.optAct = treeMenuAction;
            tItm.cells.push(new GridCell(nod.getAttribute("email"), null, true));
            tItm.cells.push(new GridCell("-", null, true));
            itemParent.add(tItm);
        }
    } else {
       list = Xml.match(node, "activity");     

	    for (ii = 0; ii < list.length; ii++) {
	        var nod = list[ii];
	        var tItm = new ProjectTreeItem(this, nod, false, nod.getAttribute("icon") + "Ico");
	        tItm.act = tItm.optAct = treeMenu2Action;
	        itemParent.add(tItm);
	    }
    }
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
};



/**
 * 
 */
OrgActTreeModel.prototype = new TreeGridModel();
OrgActTreeModel.prototype.constructor = OrgActTreeModel;

function OrgActTreeModel(userId) {
    TreeGridModel.apply(this, []);
    this.userId = userId;
    this.nCols = 3;
    this.itemIndex = {};
}

OrgActTreeModel.prototype.onReady = function(callback, tree, itemParent) {
	var holdThis = this;
    if (itemParent === this) { // at root
	   	var tItm
	   	tItm = new TreeGridItem(this, "Owned Activities", true, "task2Ico");
	   	tItm.svc = "submittedList";
        tItm.editAct = tItm.act = tItm.optAct = null;
        tItm.cells.push(new GridCell("-", null, true));
        tItm.cells.push(new GridCell("-", null, true));
        itemParent.add(tItm);
        
        tItm = new TreeGridItem(this, "Assigned Activities", true, "task3Ico");
        tItm.svc = "assignedList";
        tItm.editAct = tItm.act = tItm.optAct = null;
        tItm.cells.push(new GridCell("-", null, true));
        tItm.cells.push(new GridCell("-", null, true));
        itemParent.add(tItm);
        
        
        callback.apply(tree, [itemParent.kids, itemParent]);
    	this.addItemRows(itemParent.kids, itemParent);
        this.resize();
        
    } else { // child items = activities
        UserTree.xb.loadURIAsync(ActivityTree.serviceUri + itemParent.svc + "?ends=1&userId=" + this.userId,
            function(doc, arg, xmlHttp) {
                if (doc == null || App.checkError(doc)) {
                    // TODO error message
                } else {
                    holdThis.digest(doc.documentElement, callback, tree, itemParent);
                    holdThis.resize();
                }
            });
    }
};

OrgActTreeModel.prototype.digest = function(node, callback, tree, itemParent) {
    var ii, list;
   
   	list = Xml.match(node, "activity");  
   	if (list.length == 0) {
   		var uItem = new TreeGridItem(this, "<No activities>", false, "blankIco");
        uItem.act = uItem.optAct = uItem.editAct = null;
        uItem.cells.push(new GridCell(H.nbsp,null,true));
        uItem.cells.push(new GridCell(H.nbsp,null,true));
        itemParent.add(uItem);
   	} else {
	    for (ii = 0; ii < list.length; ii++) {
	        var nod = list[ii];
	        var tItm = new ProjectTreeItem(this, nod, false, nod.getAttribute("icon") + "Ico");
	        tItm.act = tItm.optAct = treeMenuAction;
	        itemParent.add(tItm);
	    }
   	}
    callback.apply(tree, [itemParent.kids, itemParent]);
    this.addItemRows(itemParent.kids, itemParent);
};




/**
 * Team drag and drop
 */
TeamDNDHand.prototype = new DNDTypeDummy();
TeamDNDHand.prototype.constructor = TeamDNDHand;

function TeamDNDHand() {
    this.type = "dndTeam";
}

TeamDNDHand.prototype.dropTest = function(dropElem, dragElem) {
    // team drop
    var type = dragElem._dndType.type;
    if (type == "dndTeam") {
    	if (dropElem._xfctrl && dropElem._xfctrl.__hWidget.readOnly) return false;
        return true;
    }
    return false;
};

TeamDNDHand.prototype.dropExec = function(dropElem, dragElem) {
    // activity drop
    var dragItem = dragElem._actElem.__item;
    dropElem.className = "assign";
    dropElem.innerHTML = dragElem.innerHTML;
    if (dropElem.assignObj) {
    	dropElem.assignObj.setAssign(dragElem._actElem.getAttribute("uid"));
    }
};

/**
 * The team member
 */
function TeamMember(roster, usrId) {
	this.usrId = usrId;
	this.index = roster.count;
	this.roster = roster;
	this.name = UserTree.getUserName(usrId);
}

TeamMember.prototype = {
	
	render : function(uiParent, role) {
		if (role) makeElement(uiParent, "div", "roleName", role + ":");
		var nm = this.name;
		var elem = makeElement(uiParent, "div", "meetMem mtMem" + (this.index % 9));
		elementNoSelect(elem);
		elem.setAttribute("uid", this.usrId);
		if (this.roster.canvas)
			this.roster.canvas.makeDraggable(elem, TeamRoster.dndType.type);
		makeElement(elem, "b", null, nm.charAt(0));
		makeElement(elem, "span", null, nm.substring(1));
		return elem;
	},
	
	dispose : function() {
		
	}
};



/**
 * Info and methods for the team
 */
function TeamRoster(uiElement, canvas) {
	this.canvas = canvas;
	this.uiElement = uiElement;
	if (canvas) canvas.addDNDType(TeamRoster.dndType);
}

TeamRoster.prototype = {
	
	count : 0,
	
	members : new Object(),
	rolesAssigned : new Object(),
		
	addTeamMember : function(usrId) {
		var tm = this.getMember(usrId, true);
		tm.render(this.uiElement);
	},
	
	// get a member object, maybe null, if not create == true
	getMember : function(usrId, create) {
		var tm = this.members[usrId];
		if (!tm && create) {
			tm = new TeamMember(this, usrId);
			this.members[usrId] = tm;
			this.count++;
		}
		return tm;
	},
	
	// render an "Add member" button
	initAddButton : function(uiParent, label) {
		var holdThis = this;
	    var teamLink = makeElement(uiParent, "div", "minorBtn palOut", label);
	    makeElement(teamLink, "div", "mbIco mb_usrIco");
	    setEventHandler(teamLink, "onclick", function(evt) {
	    		var usrMenu = UserTree.getUserMenu();
	    		usrMenu.defaultAct = TeamRoster.addTeamClick;
	    		usrMenu.popUp(evt, holdThis);
	    	});
	    teamLink = null; // IE enclosure clean-up
	},
	
	renderRoles : function(uiParent, roles, assignents, onclick, isLaunch) {
        var roleTable = makeLayoutTable(uiParent, "roles");
        var hrow = makeElement(roleTable, "tr");
        makeElement(hrow, "th", null, "Role Name");
        makeElement(hrow, "th", null, "Assignment");
        var ii = 0;
        for (var rol in roles) {
            var row = makeElement(roleTable, "tr", (ii++ % 2) ? "" : "alt");
            makeElement(row, "td", "role first", rol + ":");
            var who = assignents[rol];
            var td;
            if (!who) {
            	who = isLaunch ? "<default>" : "<unassigned>";
            	td = makeElement(row, "td", "assign", who);
            } else {
            	this.rolesAssigned[rol] = who;
            	td = makeElement(row, "td", "assign");
            	var tm = this.getMember(who, true);
				tm.render(td);
            }
            td._role = rol;
            setEventHandler(td, "onclick", onclick);
            setEventHandler(td, "oncontextmenu", onclick);
        }
    },
    
    __teamMenu : null,
    __teamFlowMenu : null,

    getTeamMenu : function(userAct, clearRole) {
        var usrMenu = UserTree.getUserMenu();
        if (this.__teamMenu == null) {
            this.__teamMenu = new Menu(new MenuModel([
                {label : "Assign", icon : "mb_usrIco", sub : usrMenu },
                {isSep : true},
                {label : "Clear Assignment", icon : "mb_remIco", act : clearRole}]
                ));
            App.addDispose(this.__teamMenu);
        } else {
            this.__teamMenu.model.items[0].sub = usrMenu;
            this.__teamMenu.model.items[2].act = clearRole;
        }
        usrMenu.defaultAct = userAct;
        return this.__teamMenu;
    },
    
    getTeamFlowMenu : function(userAct, clearRole, userAct2) {
        var usrMenu = UserTree.getUserMenu();
        var usrMenu2 = UserTree.getUserMenu(true);
        if (this.__teamFlowMenu == null) {
            this.__teamFlowMenu = new Menu(new MenuModel([
                {label : "Assign", icon : "mb_usrIco", sub : usrMenu },
                {label : "Assign for Activity Only", icon : "mb_copyIco", sub : usrMenu2 },
                {isSep : true},
                {label : "Clear Assignment", icon : "mb_remIco", act : clearRole}]
                ));
            App.addDispose(this.__teamFlowMenu);
        } else {
            this.__teamFlowMenu.model.items[0].sub = usrMenu;
            this.__teamFlowMenu.model.items[1].sub = usrMenu2;
            this.__teamFlowMenu.model.items[3].act = clearRole;
        }
        usrMenu.defaultAct = userAct;
        usrMenu2.defaultAct = userAct2;
        return this.__teamFlowMenu;
    },

    clearFlowRole : function(evt, context) {
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "clearRole?role=" +
                                      Uri.escape(context._role) + "&flowUri=" +
                                      Uri.escape(App.activeFlow));
        if (App.checkError(doc)) return;
        setElementText(context, "<unassigned>");
    },

    setFlowRole : function(evt, context) {
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "setRole?role=" +
                           Uri.escape(context._role) + "&flowUri="+
                           Uri.escape(App.activeFlow) + "&assignId=" + this.uid);
        if (App.checkError(doc)) return;
        setElementText(context, this.name);
        ActivityTree.refresh();
    },

    clearActivityRole : function(evt, context) {
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "clearRole?role=" +
                                      Uri.escape(context._role) + "&activity=" +
                                      App.activeActivityNode.getAttribute("id"));
        if (App.checkError(doc)) return;
        setElementText(context, "<unassigned>");
    },

    setActivityRole : function(evt, context) {
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "setRole?role=" +
                           Uri.escape(context._role) + "&activity=" +
                           App.activeActivityNode.getAttribute("id")
                           + "&assignId=" + this.uid);
        if (App.checkError(doc)) return;
        setElementText(context, this.name);
        ActivityTree.refresh();
    },
    
    setActivityRole2 : function(evt, context) {
    	if (!App.activeActivityNode) {
    		alert("Sorry, No current activity");
    		return
    	}
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "setRole?role=" +
                           Uri.escape(context._role) + "&activity=" +
                           App.activeActivityNode.getAttribute("id")
                           + "&assignId=" + this.uid);
        if (App.checkError(doc)) return;
        ActivityTree.refresh();
    },
	
    getFlowRoles : function(uri) {
        var flowRoles = new Object();

        // load flow roles
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "roleList?flowUri=" + Uri.escape(uri));
        if (App.checkError(doc)) return null;
        var frolElems = Xml.match(doc.documentElement, "role");
        for (var ii = 0; ii < frolElems.length; ii++){
            flowRoles[frolElems[ii].getAttribute("role")] = frolElems[ii].getAttribute("assignId");
        }
        return flowRoles;
    },

    getActivityRoles : function(id) {
        var activityRoles = new Object();

        // load Activity roles
        var doc = ActivityTree.xb.loadURI(ActivityTree.serviceUri + "roleList?activity=" + id);
        if (App.checkError(doc)) return null;
        var frolElems = Xml.match(doc.documentElement, "role");
        for (var ii = 0; ii < frolElems.length; ii++){
            activityRoles[frolElems[ii].getAttribute("role")] = frolElems[ii].getAttribute("assignId");
        }
        return frolElems.length > 0 ? activityRoles : null;
    },
    
    clearTeam : function() {
    	this.count = 0;
    	this.members = new Object();
    	this.rolesAssigned = new Object(); 
    },
    
    addFromFile : function(uri) {
    	var permDoc = FileTree.xb.loadURI(FileTree.serviceUri + "getPerms?uri=" + Uri.escape(uri));
    	if (App.checkError(permDoc)) return;
    	var perms = Xml.match(permDoc.documentElement, "perm");
    	for (var ii=0; ii < perms.length; ii++) {
    		this.addTeamMember(perms[ii].getAttribute("user"));
    	}
    },
     
	dispose : function() {
		this.uiElement = null;
	}
};

TeamRoster.addTeamClick = function(evt, context /* TeamRoster */) {
	context.addTeamMember(this.uid);
};

TeamRoster.getLaunchRoleNode = function(role, root) {
    return Xml.matchOne(root, "role", "role", role);
};

TeamRoster.setLaunchRole = function(evt, context) {
    var nod = TeamRoster.getLaunchRoleNode(context.hElem._role, context.roleRoot);
    if (nod) {
        nod.setAttribute("assignId", this.uid);
    }
    setElementText(context.hElem, this.name);
};

TeamRoster.clearLaunchRole = function(evt, context) {
    var nod = TeamRoster.getLaunchRoleNode(context.hElem._role, context.roleRoot);
    if (nod) {
        nod.setAttribute("assignId", "");
    }
    setElementText(context.hElem, "<unassigned>");
};

TeamRoster.dndType = new TeamDNDHand();


/**
 * XForm plugin
 */
function UserXFTypeFmt(){ 
	this.count = 0;
}

UserXFTypeFmt.prototype = new XFTypeFormat();
UserXFTypeFmt.prototype.constructor = UserXFTypeFmt;


UserXFTypeFmt.prototype.format = function(str, ctrl) {
	if (ctrl._actElem && ctrl._actElem.parentNode) 
		ctrl._actElem.parentNode.removeChild(ctrl._actElem);
		
	if (str == "") {
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "filEmpty",  "<Set a user here>");
	} else {
		var tmem = new TeamMember(this, str);
		ctrl._actElem = tmem.render(ctrl.__hElem);
	}
	
	ctrl._actElem._xfctrl = ctrl;

	if (!this.__dnd) {
		this.__dnd = dndGetCanvas(document.body);
		this.__dnd.addDNDType(TeamRoster.dndType);
	}
	
	this.canvas = this.__dnd;
	
	this.__dnd.makeDropTarget(ctrl._actElem, TeamRoster.dndType.type);
	ctrl._actElem.assignObj = {
		setAssign : function(uid) {
			ctrl.setValue(uid);
		}
	};
	
	// add context menu
	var usrMenu = UserTree.getUserMenu();
	var menu = new Menu(new MenuModel(
         	[
             {label : "Select User", icon : "mb_usrIco", sub : usrMenu },
             {isSep : true },
             {label : "Clear User", icon : "mb_remIco", act : function() { ctrl.setValue(""); }}
     		]));
	App.addDispose(menu);
	if (!App.guest) {
		setEventHandler(ctrl._actElem, "oncontextmenu", function(evt) {
				if (!ctrl.__hWidget.readOnly) {
					usrMenu.defaultAct = function() { ctrl.setValue(this.uid); }
				 	menu.popUp(evt, ctrl);
				}
				return false;
			});
	}
    return str;
};

UserXFTypeFmt.prototype.parse = function(str, ctrl) {
    return str;
};

UserXFTypeFmt.prototype.decorate = function(uiElem, ctrl) {
	if (ctrl.constructor == XFControlInput) {
		ctrl.__hWidget.style.display = 'none';
	}
};

UserXFTypeFmt.prototype.disposeDecor = function(uiElem, ctrl) {
	if (this.__dnd && ctrl._actElem) {
		ctrl._actElem._xfctrl = null;
		this.__dnd.disposeDropTarget(ctrl._actElem);
	}
	ctrl._actElem = null;
};

xsdTypes.itensilXF["user"] = { mapped : XSD_MAPPED_TYPE.STRING };

XFTypeFormat.addFormat(XFORM_ITENSIL_NAMESPACE, "user", new UserXFTypeFmt());



function GroupXFTypeFmt(){ 
	this.count = 0;
}

GroupXFTypeFmt.prototype = new XFTypeFormat();
GroupXFTypeFmt.prototype.constructor = GroupXFTypeFmt;


GroupXFTypeFmt.prototype.format = function(str, ctrl) {
	if (ctrl._actElem && ctrl._actElem.parentNode) 
		ctrl._actElem.parentNode.removeChild(ctrl._actElem);
		
	if (str == "") {
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "filEmpty",  "<Set a group here>");
	} else {
		ctrl._actElem = makeElement(ctrl.__hElem, "div", "grpVal", UserTree.getGroupName(str));
	}
	
	// add context menu
	var grpMenu = UserTree.getGroupMenu(ctrl.getAttribute("everyone") != "1");
	var menu = new Menu(new MenuModel(
         	[
             {label : "Select Group", icon : "mb_usrIco", sub : grpMenu },
             {isSep : true },
             {label : "Clear Group", icon : "mb_remIco", act : function() { ctrl.setValue(""); }}
     		]));
	App.addDispose(menu);
	
	var mnEvtFn = function(evt) {
			if (!ctrl.__hWidget.readOnly) {
				grpMenu.defaultAct = function() { ctrl.setValue(this.gid); }
				menu.popUp(evt, ctrl); 
			}
			return false;
		};
	
	setEventHandler(ctrl._actElem, "oncontextmenu", mnEvtFn);
	setEventHandler(ctrl._actElem, "onclick", mnEvtFn);
	
    return str;
};

GroupXFTypeFmt.prototype.parse = function(str, ctrl) {
    return str;
};

GroupXFTypeFmt.prototype.decorate = function(uiElem, ctrl) {
	if (ctrl.constructor == XFControlInput) {
		ctrl.__hWidget.style.display = 'none';
	}
};

GroupXFTypeFmt.prototype.disposeDecor = function(uiElem, ctrl) {
	ctrl._actElem = null;
};

xsdTypes.itensilXF["userGroup"] = { mapped : XSD_MAPPED_TYPE.STRING };

XFTypeFormat.addFormat(XFORM_ITENSIL_NAMESPACE, "userGroup", new GroupXFTypeFmt());

