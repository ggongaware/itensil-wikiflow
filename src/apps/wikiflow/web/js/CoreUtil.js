/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: CoreUtil
 * Common Classes and Functions
 */

/**
 * Rectangle Object
 */
function Rectangle(x, y, w, h) {
    if (arguments.length == 0) {
        this.x = 0; this.y = 0;
        this.w = 0; this.h = 0;
    } else {
        this.x = x; this.y = y;
        this.w = w; this.h = h;
    }
}

Rectangle.prototype.clone = function () {
    return new Rectangle(this.x, this.y, this.w, this.h);
};

Rectangle.prototype.toString = function() {
    return "Rectangle [" + this.x + ", "
            + this.y + ", "
            + this.w + ", "
            + this.h + "]";
};

Rectangle.prototype.union = function(r) {
    var x1 = Math.min(this.x, r.x);
	var x2 = Math.max(this.x + this.w, r.x + r.w);
	var y1 = Math.min(this.y, r.y);
	var y2 = Math.max(this.y + this.h, r.y + r.h);
	return new Rectangle(x1, y1, x2 - x1, y2 - y1);
};

Rectangle.prototype.inflate = function(f) {
	this.x -= f;
	this.y -= f;
	f*=2;
	this.w += f; if (this.w < 0) this.w = 0;
	this.h += f; if (this.h < 0) this.h = 0;
	return this;
};

// Rectangle intersection test
Rectangle.intersects = function(rect1, rect2) {
    var w1 = rect1.w;
	var h1 = rect1.h;
	var w2 = rect2.w;
	var h2 = rect2.h;
	if (w1 <= 0 || h1 <= 0 || w2 <= 0 || h2 <= 0) {
	    return false;
	}
	var x1 = rect1.x;
	var y1 = rect1.y;
	var x2 = rect2.x;
	var y2 = rect2.y;
	w1 += x1;
	h1 += y1;
	w2 += x2;
	h2 += y2;
	//      overflow || intersect
	return ((w2 < x2 || w2 > x1) &&
		    (h2 < y2 || h2 > y1) &&
		    (w1 < x1 || w1 > x2) &&
		    (h1 < y1 || h1 > y2));
};

Rectangle.fromPoints = function(p1, p2) {
	var x = Math.min(p1.x, p2.x);
	var y = Math.min(p1.y, p2.y);
	var w = Math.abs(p1.x - p2.x);
	var h = Math.abs(p1.y - p2.y);
	return new Rectangle(x, y, w, h);
};

function Point(x, y) {
    this.x = x;
    this.y = y;
}

Point.NORTH = 0;
Point.EAST = 1;
Point.SOUTH = 2;
Point.WEST = 3;
Point.directions = ["N", "E", "S", "W"];
Point.dirCodes = {N:0, E:1, S:2, W:3};
Point.reverse = [Point.SOUTH, Point.WEST, Point.NORTH, Point.EAST];
Point.rightturn = [Point.EAST, Point.SOUTH, Point.WEST, Point.NORTH];

Point.prototype.clone = function() {
    return new Point(this.x, this.y);
};

Point.prototype.toString = function() {
    return  this.x + "," + this.y;
};

Point.decode = function(str) {
    var nums = str.split(",");
    if (nums.length < 2) return null;
    return new Point(parseInt(nums[0]), parseInt(nums[1]));
};

Point.prototype.alignVertical = function(p) {
    this.x = p.x;
};

Point.prototype.alignHorizontal = function(p) {
    this.y = p.y;
};

Point.prototype.align = function(p, dir) {
    if (dir == Point.NORTH || dir == Point.SOUTH) {
        this.x = p.x;
    } else {
        this.y = p.y;
    }
};

Point.prototype.distance = function(p) {
    var dy = p.y - this.y;
    if (dy == 0) {
        return p.x - this.x;
    }
    return dy;
};

Point.prototype.equals = function(p) {
    return this.x == p.x && this.y == p.y;
};

Point.prototype.move = function (dist, dir) {
    var pnt = new Point(this.x, this.y);
    switch (dir) {
        case Point.NORTH:
            pnt.y -= dist;
            break;
        case Point.EAST:
            pnt.x += dist;
            break;
        case Point.SOUTH:
            pnt.y += dist;
            break;
        case Point.WEST:
            pnt.x -= dist;
            break;
    }
    return pnt;
};

Point.prototype.direction = function(p) {
    var dx = p.x - this.x;
    var dy = p.y - this.y;
    if (Math.abs(dx) > Math.abs(dy)) {
        return dx < 0 ? Point.WEST : Point.EAST;
    } else {
        return dy < 0 ? Point.NORTH : Point.SOUTH;
    }
};

Point.prototype.toRect = function() {
	return new Rectangle(this.x, this.y, 1, 1);
};


function arrayAddAll(arr, arr2) {
    if (arr2 == null) return;
    for (var i =0; i < arr2.length; i++) {
        arr.push(arr2[i]);
    }
}

function arrayAdd(arr, obj) {
    arr.push(obj);
}

function arrayInsert(arr, idx, obj) {
    arr.splice(idx, 0, obj);
}

function arrayRemove(arr, rmObj) {
    for (var i = 0; i < arr.length; i++) {
        if (arr[i] == rmObj) {
            arr.splice(i, 1);
            return arr;
        }
    }
    return arr;
}

function arrayRemoveStrict(arr, rmObj) {
    for (var i = 0; i < arr.length; i++) {
        if (arr[i] === rmObj) {
            arr.splice(i, 1);
            return arr;
        }
    }
    return arr;
}

function arrayReplace(arr, oldObj, newObj) {
    var idx = arrayFind(arr, oldObj);
    if (idx < 0) {
        return false;
    }
    arr.splice(idx, 1, newObj);
    return true;
}

function arrayFindStrict(arr, obj) {
    for (var i = 0; i < arr.length; i++) {
        if(obj === arr[i]) return i;
    }
    return -1;
}

function arrayFind(arr, obj) {
    for (var i =0; i < arr.length; i++) {
        if(obj == arr[i]) return i;
    }
    return -1;
}

function stringTr(s1, s2, s3) {
	var map = [];
	var i;
	for (i = 0; i < s2.length; i++) {
		var j = s2.charCodeAt(i);
		if (map[j] == undefined) {
			var k = i > s3.length ? "" : s3.charAt(i);
			map[j] = k;
		}
	}
	var t = "";
	for (i = 0; i < s1.length; i++) {
		var ch = s1.charCodeAt(i);
		var r = map[ch];
		if (r == undefined) {
			t += s1.charAt(i);
		} else {
			t += r;
		}
	}
	return t;
}


function objectExtend(objDst, objSrc) {
    var prop;
    for (prop in objSrc) {
        if (!objDst[prop]) objDst[prop] = objSrc[prop];
    }
    return objDst;
}

function LinkListNode() {
    this.prevNode = null;
    this.nextNode = null;
}

LinkListNode.prototype.unlink = function() {
    if (this.prevNode != null) {
        this.prevNode.nextNode = this.nextNode;
    }
    if (this.nextNode != null) {
        this.nextNode.prevNode = this.prevNode;
    }
};

LinkListNode.prototype.prepend = function(node) {
    node.prevNode = this.prevNode;
    node.nextNode = this;
    if (this.prevNode != null) {
        this.prevNode.nextNode = node;
    }
    this.prevNode = node;
};

LinkListNode.prototype.append = function(node) {
    node.prevNode = this;
    node.nextNode = this.nextNode;
    if (this.nextNode != null) {
        this.nextNode.prevNode = node;
    }
    this.nextNode = node;
};

var DateUtil = {

	DAYS : 86400000,

    __dp8601RegEx : new RegExp(
        "^(-?\\d+)-(\\d+)-(\\d+)(?:[ tT](\\d+):(\\d+):([0-9\\.]+))?"),

    __dpShortRegEx : new RegExp(
        "^(\\d+)[/](\\d+)[/](\\d+)(?:\\s+(\\d+):(\\d+)(?::(\\d+(?:\\.\\d+)?))?(?:\\s*([AaPp]))?)?"),

	__dpYFRegEx : new RegExp(
        "^(-?\\d+)-(\\d+)-(\\d+)(?:\\s+(\\d+):(\\d+)(?::(\\d+(?:\\.\\d+)?))?(?:\\s*([AaPp]))?)?"),

    parse8601 : function(dStr, withTime) { // assumes zulu
        DateUtil.__dp8601RegEx.lastIndex = 0;
        var m = DateUtil.__dp8601RegEx.exec(dStr);
        if (m == null) return null;
        for (var i = 0; i < m.length; i++) if (m[i] == null) m[i] = 0;
        while (m.length < 7) m.push(0);
        if (withTime) {
            return new Date(Date.UTC(m[1], m[2] - 1, m[3], m[4], m[5], m[6]));
        } else {
            return new Date(m[1], m[2] - 1, m[3]);
        }
    },
    
    parseYF : function(dStr, withTime) {
        DateUtil.__dpYFRegEx.lastIndex = 0;
        var m = DateUtil.__dpYFRegEx.exec(dStr);
        if (m == null) return null;
        for (var i = 0; i < m.length; i++) if (m[i] == null) m[i] = 0;
        while (m.length < 8) m.push(0);
        if (withTime) {
        	if (m[7] == 0) m[7] = "A";
	        var hh = parseInt(m[4]);
	        if (m[7].toUpperCase() == "P") {
	            if (hh < 12) hh += 12;
	            m[4] = hh;
	        }  else if (hh == 12) { // 12am
	        	m[4] = 0;
	        }
            return new Date(m[1], m[2] - 1, m[3], m[4], m[5], m[6]);
        } else {
            return new Date(m[1], m[2] - 1, m[3]);
        }
    },

    to8601 : function(d, withTime) {
        if (withTime) {
            return numberPad(d.getUTCFullYear(), 4) +
                "-" + numberPad(d.getUTCMonth() + 1, 2) +
                "-" + numberPad(d.getUTCDate(), 2) + "T" + numberPad(d.getUTCHours(), 2) +
                ":" + numberPad(d.getUTCMinutes(), 2) +
                ":" + numberPad(d.getUTCSeconds(), 2) + "Z";
        }
        return numberPad(d.getFullYear(), 4) +
                "-" + numberPad(d.getMonth() + 1, 2) +
                "-" + numberPad(d.getDate(), 2);
    },

    toLocale : function(d, showTime) {
        return showTime ? d.toLocaleString() : d.toLocaleDateString() ;
    },

    toLocaleShort : function(d, showTime) {
        // var s = (d.getMonth() + 1) + "/"+
        //        d.getDate() + "/" +
        //        d.getFullYear();
        var s = numberPad(d.getFullYear(), 4) +
                "-" + numberPad(d.getMonth() + 1, 2) +
                "-" + numberPad(d.getDate(), 2);
        if (showTime) {
            var mer = " AM";
            var h = d.getHours();
            if (h >= 12) {
                if (h > 12) h -= 12;
                mer = " PM";
            } else if (h == 0) {
            	h = 12;
            }
            s += " " + h + ":" + numberPad(d.getMinutes(), 2) + mer;
        }
        return s;
    },
    
     toUTCShort : function(d, showTime) {
        var s = numberPad(d.getUTCFullYear(), 4) +
                "-" + numberPad(d.getUTCMonth() + 1, 2) +
                "-" + numberPad(d.getUTCDate(), 2);
        if (showTime) {
            var mer = " AM";
            var h = d.getUTCHours();
            if (h >= 12) {
                if (h > 12) h -= 12;
                mer = " PM";
            } else if (h == 0) {
            	h = 12;
            }
            s += " " + h + ":" + numberPad(d.getMinutes(), 2) + mer;
        }
        return s;
    },

    dayDiff : function(d1, d2) {
        return Math.floor((d1.getTime() - d2.getTime()) / DateUtil.DAYS);
    },
    
    addTime : function(dt, millis) {
    	return new Date(dt.getTime() + millis);
    },

    // TODO - handle hours
    toLocaleWords : function(d, showTime, dateThresh) {
        var dayd = DateUtil.dayDiff(d, new Date()) + 1;
        var str;
        if (!dateThresh) dateThresh = 100;
        if (dayd == 0) {
            str = "Today";
        } else if (dayd == 1) {
            str = "Tomorrow";
        } else if (dayd == -1) {
            str = "Yesterday";
        } else if (dayd < 0 && dayd > -dateThresh) {
            str =  (-dayd) + " days ago";
        } else {
            str = DateUtil.toLocaleShort(d, showTime);
        }
        return str;
    },
    
    getShortDay : function (dt) {
    	return ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][dt.getDay()];
    },

    parseLocaleShort : function(dStr) {
        DateUtil.__dpShortRegEx.lastIndex = 0;
        var m = DateUtil.__dpShortRegEx.exec(dStr);
        if (m == null) {
        	return DateUtil.parseYF(dStr, dStr.length > 11);
        }
        for (var i = 0; i < m.length; i++) if (m[i] == null) m[i] = 0;
        while (m.length < 8) m.push(0);
        if (m[7] == 0) m[7] = "A";
        var hh = parseInt(m[4]);
        if (m[7].toUpperCase() == "P") {
            if (hh < 12) hh += 12;
            m[4] = hh;
        }  else if (hh == 12) { // 12am
        	m[4] = 0;
        }
        var y = parseInt(m[3]);
        if (y < 70) {
            m[3] = 2000 + y;
        }
        return new Date(m[3], m[1] - 1, m[2], m[4], m[5], m[6]);
    },
    
    parseUTCShort : function (dStr) {
    	return this.asUTC(this.parseLocaleShort(dStr));
    },
    
    asUTC : function(dt) {
    	if (dt) {
    		return new Date(Date.UTC(dt.getFullYear(), dt.getMonth(), dt.getDate()));
    	}
    	return null;
    },
    
    asLocal : function(dt) {
    	if (dt) {
    		return new Date(dt.getUTCFullYear(), dt.getUTCMonth(), dt.getUTCDate());
    	}
    	return null;
    },

    __dpTimeRegEx : new RegExp("^0*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)?"),

    __dpLocTimeRegEx : new RegExp(
        "^0*(\\d+):0*(\\d+)(?::0*(\\d+(?:\\.\\d+)?))?(?:\\s*([AaPp]))?"),

    parseLocaleTime : function(tStr) {
        DateUtil.__dpLocTimeRegEx.lastIndex = 0;
        var ma = DateUtil.__dpLocTimeRegEx.exec(tStr);
        if (ma == null) return null;
        var hh = parseInt(ma[1]);
        if (ma[4] == null || ma[4] == "") {
            ma[4] = hh <= 6 ? "P" : "A";
        }
        if (ma[4].toUpperCase() == "P") {
            if (hh < 12) hh += 12;
        } else if (hh == 12) { // 12am
        	hh = 0;
        }
        var min = parseInt(ma[2]);
        var sec = (ma[3] == null || ma[3] == "") ? null : parseInt(ma[3]);
        return numberPad(hh, 2) + ":" + numberPad(min, 2) + (sec == null ? ":00" : (":" + numberPad(sec, 2)));
    },

    toLocaleTime : function(tStr) {
        DateUtil.__dpTimeRegEx.lastIndex = 0;
        var ma = DateUtil.__dpTimeRegEx.exec(tStr);
        if (ma == null) return null;

        var mer = " AM";
        var hh = parseInt(ma[1]);
        if (hh >= 12) {
            if (hh > 12) hh -= 12;
            mer = " PM";
        }
        var sec = (ma[3] == null || ma[3] == "") ? 0 : parseInt(ma[3]);
        return hh + ":" + ma[2] + (sec == 0 ? "" : (":" + ma[3])) +  mer;
    }
};



var Uri = new Object();

Uri.name = function(uri) {
    var pos = uri.lastIndexOf("/");
    if (pos >= 0) {
        return uri.substring(pos+1);
    } else {
        return uri;
    }
};

Uri.parent = function(uri) {
    while (uri.charAt(uri.length - 1) == "/") {
        uri = uri.substring(0, uri.length - 1);
    }
    var pos = uri.lastIndexOf("/");
    if (pos > 0) {
        return uri.substring(0, pos);
    } else {
        return "";
    }
};

Uri.absolute = function(base, uri) {
    if (uri.charAt(0) == "/" || base == "") {
        return uri;
    } else if (uri.indexOf("://") > 0) {
        return uri;
    } else {
        if (base.charAt(base.length - 1) == "/") {
            return base + uri;
        }
        return base + '/' + uri;
    }
};

Uri.root = function(uri) {
    var protIdx = uri.indexOf("://");
    if (protIdx > 0) protIdx += 3;
    else protIdx = uri.charAt(0) == "/" ? 1 : 0;
    var pos = uri.indexOf("/", protIdx);
    if (pos > 0) {
        return uri.substring(0, pos);
    }
    return uri;
};

Uri.localize = function(base, uri) {
	if (!base) return uri;
	if (base.charAt(base.length - 1) != "/") base += "/";
    if (uri.indexOf(base) == 0) {
        uri = uri.substring(base.length);
        while (uri.charAt(0) == "/") uri = uri.substring(1);
    }
    return uri;
};

Uri.reduce = function(uri) {
    var pos;

    if (uri.substring(0, 2) == ".." || (pos = uri.indexOf("../")) < 0) {
		return uri;
	}
    do {
        uri = Uri.parent(uri.substring(0, pos)) + uri.substring(pos + 2);
    } while ((pos = uri.indexOf("../")) >= 0);
    return uri;
};

/**
 * Certain extensions like "xml" may have an overriding extension like myfile.doc.xml, 
 * unless strict is true, this will return "doc";
 */
Uri.ext = function(name, strict) {
    var ep = name.lastIndexOf(".");
    if (ep < 1) return "";
    var ext = name.substring(ep + 1, name.length);
    if (!strict && ext == "xml") {
    	ep = name.lastIndexOf(".", ep - 1);
    	// support ".wxyz.xml"
    	if (ep > 1 && ep > (name.length - 10)) {
    		ext = name.substring(ep + 1, name.length - 4);
    	}	
    }
    return ext;
};

Uri.escape = function(str) {
    return encodeURIComponent(str);
};

// retains ? in 2nd index if exists
Uri.splitQuery = function(uri) {
	var ep = uri.indexOf("?");
    if (ep < 1) return [uri, ""];
    return [uri.substring(0, ep), uri.substring(ep)];
};

if (typeof Error == "undefined") {
	Error.prototype.constructor = Error;
	
	function Error(s) {
	    this.detail = s;
	}
	
	Error.prototype.toString = function() {
	    return this.detail;
	};
}

function numberPad(num, digits) {
    var numStr = "" + num;
    while (numStr.length < digits) numStr = "0" + numStr;
    return numStr;
}

function numberFormat(num, decimals, dec_point, thousands_sep, drop_dec) {

    if (dec_point == null) dec_point = ".";
    if (thousands_sep == null) thousands_sep = ",";
    if (drop_dec == null) drop_dec = false;
	
	var pre = "";
	if (num < 0) {
		num = -num;
		pre = "-";
	}
	var buf = "";
	var shft = numberPad(Math.round(num * Math.pow(10, decimals)), decimals + 1);
	
	var p1Len = shft.length - decimals;
	var p1Str = shft.substring(0, p1Len);
	var p2Str = shft.substring(p1Len);
	
	for (var ii = p1Len - 3; ii >= -2; ii -= 3) {
		buf = (ii > 0 ? thousands_sep : "") + p1Str.substring(ii, ii + 3) + buf;
	}
	if (decimals == 0 || (drop_dec && (parseInt(p2Str) == 0))) {
	    return pre + buf;
	}
	return pre + buf + dec_point + p2Str;
}


// http://www.JSON.org/json2.js 2008-03-24 Public Domain.
var JSON = function () {

    function f(n) {
        return n < 10 ? '0' + n : n;
    }

    Date.prototype.toJSON = function () {
        return this.getUTCFullYear()   + '-' +
             f(this.getUTCMonth() + 1) + '-' +
             f(this.getUTCDate())      + 'T' +
             f(this.getUTCHours())     + ':' +
             f(this.getUTCMinutes())   + ':' +
             f(this.getUTCSeconds())   + 'Z';
    };

    var escapeable = /["\\\x00-\x1f\x7f-\x9f]/g,
        gap,
        indent,
        meta = {
            '\b': '\\b',
            '\t': '\\t',
            '\n': '\\n',
            '\f': '\\f',
            '\r': '\\r',
            '"' : '\\"',
            '\\': '\\\\'
        },
        rep;

    function quote(string) {
        return escapeable.test(string) ?
            '"' + string.replace(escapeable, function (a) {
                var c = meta[a];
                if (typeof c === 'string') {
                    return c;
                }
                c = a.charCodeAt();
                return '\\u00' + Math.floor(c / 16).toString(16) + (c % 16).toString(16);
            }) + '"' :
            '"' + string + '"';
    }

    function str(key, holder) {
        var i, k, v, length, mind = gap, partial, value = holder[key];
        if (value && typeof value === 'object' && typeof value.toJSON === 'function') {
            value = value.toJSON(key);
        }
        if (typeof rep === 'function') {
            value = rep.call(holder, key, value);
        }
        switch (typeof value) {
        case 'string':
            return quote(value);

        case 'number':
            return isFinite(value) ? String(value) : 'null';

        case 'boolean':
        case 'null':
            return String(value);

        case 'object':
            if (!value) {
                return 'null';
            }
            gap += indent;
            partial = [];
            if (typeof value.length === 'number' && !(value.propertyIsEnumerable('length'))) {
                length = value.length;
                for (i = 0; i < length; i += 1) {
                    partial[i] = str(i, value) || 'null';
                }
                v = partial.length === 0 ? '[]' :
                    gap ? '[\n' + gap + partial.join(',\n' + gap) + '\n' + mind + ']' :
                          '[' + partial.join(',') + ']';
                gap = mind;
                return v;
            }
            if (typeof rep === 'object') {
                length = rep.length;
                for (i = 0; i < length; i += 1) {
                    k = rep[i];
                    if (typeof k === 'string') {
                        v = str(k, value, rep);
                        if (v) {
                            partial.push(quote(k) + (gap ? ': ' : ':') + v);
                        }
                    }
                }
            } else {
                for (k in value) {
                    v = str(k, value, rep);
                    if (v) {
                        partial.push(quote(k) + (gap ? ': ' : ':') + v);
                    }
                }
            }
            v = partial.length === 0 ? '{}' :
                gap ? '{\n' + gap + partial.join(',\n' + gap) + '\n' + mind + '}' :
                      '{' + partial.join(',') + '}';
            gap = mind;
            return v;
        }
    }
    return {
        stringify: function (value, replacer, space) {
            var i;
            gap = '';
            indent = '';
            if (space) {
                if (typeof space === 'number') {
                    for (i = 0; i < space; i += 1) {
                        indent += ' ';
                    }
                } else if (typeof space === 'string') {
                    indent = space;
                }
            }
            if (!replacer) {
                rep = function (key, value) {
                    if (!Object.hasOwnProperty.call(this, key)) {
                        return undefined;
                    }
                    return value;
                };
            } else if (typeof replacer === 'function' ||
                    (typeof replacer === 'object' &&
                     typeof replacer.length === 'number')) {
                rep = replacer;
            } else {
                throw new Error('JSON.stringify');
            }
            return str('', {'': value});
        },

        parse: function (text, reviver) {
            var j;

            function walk(holder, key) {
                var k, v, value = holder[key];
                if (value && typeof value === 'object') {
                    for (k in value) {
                        if (Object.hasOwnProperty.call(value, k)) {
                            v = walk(value, k);
                            if (v !== undefined) {
                                value[k] = v;
                            } else {
                                delete value[k];
                            }
                        }
                    }
                }
                return reviver.call(holder, key, value);
            }
            
            if (/^[\],:{}\s]*$/.test(text.replace(/\\["\\\/bfnrtu]/g, '@').
					replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g, ']').
					replace(/(?:^|:|,)(?:\s*\[)+/g, ''))) {
                j = eval('(' + text + ')');
                return typeof reviver === 'function' ? walk({'': j}, '') : j;
            }
            throw new SyntaxError('JSON.parse');
        },

        quote: quote
    };
}();
