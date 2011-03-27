/**
 * Copyright (C) 2005 Itensil, Inc.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * Author: ggongaware (at) itensil.com
 *
 */

/*@cc_on
  	@if (@_jscript_version >= 7)
var navigator;
var window;
var Debug;
var document;
var opera;
var java;
var WScript;
var defineClass;
/*@end
@*/

function ScriptHost() {
    this.debug = false;
    this.test = false;
/*@cc_on
  	@if (@_jscript_version >= 7)
  	this.is_browser = null;
    this.agent = "JScript.NET";
    this.is_dotNet = true;
    this.is_wsh = false;
    this.is_ie = false;
    this.is_rhino = false;
    this.is_gecko = false;
    this.is_opera = false;
    this.is_safari = false;
    this.newline = "\r\n";
  	@elif (@_jscript)
  	this.is_browser = typeof(navigator) == "undefined" ? false : true;
  	this.agent = this.is_browser ?
  	        navigator.userAgent.toLowerCase() : "WindowsScriptHost";
  	this.is_dotNet = false;
  	this.is_wsh = !this.is_browser;
  	this.is_ie = this.is_browser;
  	this.is_ie7 = this.is_browser && (window.XMLHttpRequest != null);
  	this.is_rhino = false;
  	this.is_gecko = false;
  	this.is_opera = false;
  	this.is_safari = false;
  	this.newline = "\r\n";
  	@else */
  	this.is_browser = typeof(navigator) == "undefined" ? false : true;
    if (this.is_browser) {
  	    this.agent = navigator.userAgent.toLowerCase();
  	} else {
  	    this.agent = "Rhino";
  	}
    this.is_dotNet = false;
    this.is_wsh = false;
    this.is_ie = false;
    this.newline = "\n";
/*@end
@*/
    if (this.is_browser && !this.is_ie) {
        var agent = this.agent;
        this.is_gecko = ((agent.indexOf("gecko") >= 0)
            && (agent.indexOf("spoofer") < 0) && (agent.indexOf("khtml") < 0)
            && (agent.indexOf("netscape/7.0") < 0));
        this.is_safari = ((agent.indexOf("applewebkit") >= 0)
                && (agent.indexOf("spoofer") < 0));
        this.is_khtml = (navigator.vendor == "KDE" ||
            (document.childNodes && !document.all && !navigator.taintEnabled));
        this.is_opera = agent.indexOf("opera") >= 0;
        this.is_rhino = false;
        this.is_gecko1_9 = this.is_gecko && (agent.indexOf("rv:1.9") >= 0);
    } else if (!this.is_ie) {
        this.is_gecko = false;
        this.is_opera = false;
  	    this.is_safari = false;
        this.is_rhino = typeof(defineClass) == "function";
    }
    if (this.is_browser) {
        this.complyMode = document.compatMode
            && (document.compatMode == "CSS1Compat");
    }
}

ScriptHost.prototype.println = function(s) {
    this.print(s + this.newline);
}

ScriptHost.prototype.print = function(s) {
    if (this.is_browser) {
        if (this.is_gecko) {
        	 if (window.console) window.console.log(s);
        	 else window.dump(s);
        }
        else if (this.is_ie) Debug.write(s);
        else if (this.is_safari && window.console) window.console.log(s);
        else if (this.is_opera) opera.postError(s);
    } else if (this.is_dotNet) {
        print(s);
    } else if (this.is_rhino) {
        java.lang.System.out.print(s);
    } else if (this.is_wsh) {
        WScript.echo(s);
    }
};

ScriptHost.prototype.dump = function(obj) {
    var d = [];
    var a;
    for (var i in obj) {
        a = obj[i];
        if (typeof(a) == "function") {
            d.push("  " + i + "(" + a.length + ")");
        } else { d.push("  " + i + " : " + a); }
    }
    this.println(typeof(obj) + " {" + this.newline + d.join("," + this.newline)
            + this.newline + "}");
};

var SH = new ScriptHost();
