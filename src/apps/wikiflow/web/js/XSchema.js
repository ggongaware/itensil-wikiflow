/**
 * (c) 2005 Itensil, Inc.
 *  ggongaware (at) itensil.com
 *
 **/

var xsdTypeNameSpaces = {
        "http://www.w3.org/2001/XMLSchema" : "_default",
        "http://itensil.com/ns/xforms" : "itensilXF",
        _default : "http://www.w3.org/2001/XMLSchema" };

var XSD_MAPPED_TYPE = {
    STRING : 0,
    BOOLEAN : 1,
    FLOAT : 2,
    INTEGER : 3,
    DATE : 4,
    DATETIME : 5,
    TIME : 6,
    REGEX : 7,
    FUNC : 8,
    OTHER : 10
    };

var xsdTypes = {
            _default : {
                anyURI : { mapped : XSD_MAPPED_TYPE.STRING }, // URI (Uniform Resource Identifier)
                base64Binary : { mapped : XSD_MAPPED_TYPE.STRING }, // Binary content coded as "base64"
                "boolean" : { mapped : XSD_MAPPED_TYPE.BOOLEAN }, // Boolean (true or false)
                "byte" : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Signed value of 8 bits
                date : { mapped : XSD_MAPPED_TYPE.DATE }, // Gregorian calendar date
                dateTime : { mapped : XSD_MAPPED_TYPE.DATETIME }, // Instant of time (Gregorian calendar)
                decimal : { mapped : XSD_MAPPED_TYPE.FLOAT }, // Decimal numbers
                "double" : { mapped : XSD_MAPPED_TYPE.FLOAT }, // IEEE 64/bit floating/point
                duration : { mapped : XSD_MAPPED_TYPE.STRING }, // Time durations
                ENTITIES : { mapped : XSD_MAPPED_TYPE.STRING }, // Whitespace/separated list of unparsed entity references
                ENTITY : { mapped : XSD_MAPPED_TYPE.STRING }, // Reference to an unparsed entity
                "float" : { mapped : XSD_MAPPED_TYPE.FLOAT }, // IEEE 32/bit floating/point
                gDay : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Recurring period of time: monthly day
                gMonth : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Recurring period of time: yearly month
                gMonthDay : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Recurring period of time: yearly day
                gYear : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Period of one year
                gYearMonth : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Period of one month
                hexBinary : { mapped : XSD_MAPPED_TYPE.STRING }, // Binary contents coded in hexadecimal
                ID : { mapped : XSD_MAPPED_TYPE.STRING }, // Definition of unique identifiers
                IDREF : { mapped : XSD_MAPPED_TYPE.STRING }, // Definition of references to unique identifiers
                IDREFS : { mapped : XSD_MAPPED_TYPE.STRING }, // Definition of lists of references to unique identifiers
                "int" : { mapped : XSD_MAPPED_TYPE.INTEGER }, // 32/bit signed integers
                integer : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Signed integers of arbitrary length
                language : { mapped : XSD_MAPPED_TYPE.STRING }, // RFC 1766 language codes
                "long" : { mapped : XSD_MAPPED_TYPE.INTEGER }, // 64/bit signed integers
                Name : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^[a-z_:]+[a-z0-9_:\\.]*$", "i")}, // XML 1.O name
                NCName : { mapped : XSD_MAPPED_TYPE.STRING }, // Unqualified names
                negativeInteger : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Strictly negative integers of arbitrary length
                NMTOKEN : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^[a-z0-9_:\\.]+$", "i")}, // XML 1.0 name token (NMTOKEN)
                NMTOKENS : { mapped : XSD_MAPPED_TYPE.STRING }, // List of XML 1.0 name tokens (NMTOKEN)
                nonNegativeInteger : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Integers of arbitrary length positive or equal to zero
                nonPositiveInteger : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Integers of arbitrary length negative or equal to zero
                normalizedString : { mapped : XSD_MAPPED_TYPE.STRING }, // Whitespace/replaced strings
                NOTATION : { mapped : XSD_MAPPED_TYPE.STRING }, // Emulation of the XML 1.0 feature
                positiveInteger : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Strictly positive integers of arbitrary length
                QName : { mapped : XSD_MAPPED_TYPE.STRING }, // Namespaces in XML/qualified names
                "short" : { mapped : XSD_MAPPED_TYPE.INTEGER }, // 32/bit signed integers
                string : { mapped : XSD_MAPPED_TYPE.STRING }, // Any string
                time : { mapped : XSD_MAPPED_TYPE.TIME }, // Point in time recurring each day
                token : { mapped : XSD_MAPPED_TYPE.STRING }, // Whitespace/replaced and collapsed strings
                unsignedByte : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Unsigned value of 8 bits
                unsignedInt : { mapped : XSD_MAPPED_TYPE.INTEGER }, // Unsigned integer of 32 bits
                unsignedLong : { mapped : XSD_MAPPED_TYPE.INTEGER } // Unsigned integer of 64 bits
                },
            itensilXF : {
                currencyUSD : { mapped : XSD_MAPPED_TYPE.FLOAT },
                currencyGBP : { mapped : XSD_MAPPED_TYPE.FLOAT },
                currencyEUR : { mapped : XSD_MAPPED_TYPE.FLOAT },
                estimate : { mapped : XSD_MAPPED_TYPE.FLOAT },
                percent : { mapped : XSD_MAPPED_TYPE.FLOAT },
                uniqueId : { mapped : XSD_MAPPED_TYPE.STRING},
                uniquePath : { mapped : XSD_MAPPED_TYPE.FUNC, func : function(val) { return Uri.name(val) != "";} },
                email : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^\\w\\S*@\\w\\S+\\.\\w\\w+$")},
                fileName : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^[^;:\\*\\?<>\\|\\\\/\"]+$")},
                varName : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^[a-z_]+[a-z0-9_\\.]*$", "i")},
				http : { mapped : XSD_MAPPED_TYPE.REGEX, regex : new RegExp("^https?://[0-9a-z_!~*'().&=+$%-@:^/{}\\\\\\[\\];#]+$", "i")}
            }
    };

XsdTypeManager.prototype.constructor = XsdTypeManager;

function XsdTypeManager() {
    this.__nsRes = new NamespaceResolver();
    this.__typeCache = new Object();
}

XsdTypeManager.prototype.rebuild = function() {
	this.__typeCache = new Object();
};

XsdTypeManager.prototype.getTypeInfo = function(type, contextNode) {
    var inf = this.__typeCache[type];
    if (inf == null) {
        var qn = type.split(":");
        var prefix = "";
        var localName = qn[0];
        if (qn.length >= 2) {
            prefix = localName;
            localName = qn[1];
        }
        var ns = this.__nsRes.getNamespace(prefix, contextNode);
        var xtns = xsdTypeNameSpaces[ns];
        if (xtns != null) {
            inf = xsdTypes[xtns][localName];
        }
        if (inf == null) {
            inf = { mapped : XSD_MAPPED_TYPE.STRING };
        }
        inf.namespace = ns;
        inf.type = localName;
        this.__typeCache[type] = inf;
    }
    return inf;
};

XsdTypeManager.prototype.isValid = function(value, type, contextNode) {
    if (value == null) return true;
    if (type == null) return true;
    var inf = this.getTypeInfo(type, contextNode);
    if (inf == null) return true;
    return this.isValidInf(value, inf);
};

XsdTypeManager.prototype.isValidInf = function(value, inf) {
    switch (inf.mapped) {
        case XSD_MAPPED_TYPE.INTEGER: return !isNaN(parseInt(value));
        case XSD_MAPPED_TYPE.FLOAT: return !isNaN(parseFloat(value));
        case XSD_MAPPED_TYPE.DATE:
            return DateUtil.parse8601(value, false) != null;
        case XSD_MAPPED_TYPE.DATETIME:
            return DateUtil.parse8601(value, true) != null;
        case XSD_MAPPED_TYPE.TIME:
            return DateUtil.parseLocaleTime(value) != null;
       	case XSD_MAPPED_TYPE.FUNC:
       		return inf.func(value);
       	case XSD_MAPPED_TYPE.REGEX:
       		return inf.regex.test(value);
        case XSD_MAPPED_TYPE.STRING:
        default:
            return true;
    }
};




/**
 * XmlId
 */
function XmlId(prefix) {
    this.variables = new Object();
    this.prefix = prefix || "";
}

XmlId.prototype.addVar = function(varName) {
	varName = Uri.localize(this.prefix, varName);
    this.variables[varName] = true;
};

XmlId.prototype.removeVar = function(varName) {
	varName = Uri.localize(this.prefix, varName);
    delete this.variables[varName];
};

XmlId.prototype.hasVar = function(varName) {
	varName = Uri.localize(this.prefix, varName);
	return this.variables[varName];
};

XmlId.prototype.uniqueVar = function(guess) {
	guess = Uri.localize(this.prefix, guess);
	
	// trim whitespace
	var wsEnd = guess.length - 1;
	var wsStart = 0;
	while (wsEnd > 0 && guess.charAt(wsEnd) == " ") wsEnd--;
    while (wsStart < wsEnd && guess.charAt(wsStart) == " ") wsStart++;
    guess = guess.substring(wsStart, wsEnd + 1);
    
    // chop numbers off end
    var chPnt = guess.length - 1;
    while (chPnt >= 0 && XmlId.NUMBERS.indexOf(guess.charAt(chPnt)) >= 0) {
        chPnt--;
    }
    var uKey = guess.substring(0, chPnt+1);
    var uk = parseInt(guess.substring(chPnt+1, guess.length));
    if (isNaN(uk)) {
        uk = 2;
        if (uKey.charAt(uKey.length - 1) != " ") uKey += " ";
    }
    while (guess in this.variables) {
        guess = uKey + (uk++);
    }
    return Uri.absolute(this.prefix, guess);
};


XmlId.makeVar = function(label, maxLen) {
    var ucLabel = label.toLowerCase();
    if (!maxLen) maxLen = 8;

    // limit to maxLen char name prefix
    var len = label.length > maxLen ? maxLen : label.length;
    var buf = "";
    for (var i=0; i < len; i++) {
        var ch = ucLabel.charAt(i);
        if (XmlId.LETTERS.indexOf(ch) >= 0) {
            buf += label.charAt(i);
        } else if (XmlId.NUMBERS.indexOf(ch) >= 0)  {
            if ( i == 0 ) {
                 buf += "_";
            } else {
                buf += ch;
            }
        } else {
            buf += "_";
        }
    }
    return buf;
};

XmlId.NUMBERS = "0123456789";
XmlId.LETTERS = "abcdefghijklmnopqrstuvwxyz";
