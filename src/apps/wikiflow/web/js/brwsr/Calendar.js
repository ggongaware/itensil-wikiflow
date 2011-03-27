/**
 * (c) 2005 Itensil, Inc.
 * ggongaware (at) itensil.com
 * Lib: Calendar
 */

var calPopObjs = new Object();
var calPopDiag = null;
var calLastBtn = null;

// toggles
function calPopShow(evt, button, timeObj, showTime) {
    var calId = "calPop00";

    //create levarage dialog functionality for calendar pop 
    if (calPopDiag == null || !calPopDiag.isShowing()) {
		calPopDiag = calPopCreateDialog(calId, false, false, showTime, button);
    }
    else if (calLastBtn === button) {
        calPopHide();
        return;
    }

    calPopRevive(button);
    calLastBtn = button;
    calPopObjs[calId].activate(timeObj, showTime);

 }
 
function calPopIsVisible() {
	return calPopDiag != null && calPopDiag.isShowing();
}

function calPopCreateDialog(calId, use_week_select, pickweek, showTime, button) {
    var cal = new CalPop(calId, use_week_select, pickweek);
    cal.showTime = showTime;
    calPopObjs[calId] = cal;

	var diag = new Dialog("" , true);
	diag.canResize = false;
	diag.autoRemove=true;
	diag.render(document.body);
	 
	var hElem = makeElement(diag.contentElement, "div", "calPop");
    hElem._cal = cal;
    hElem.id = "cal.pop";

    var html = '<div id="' + calId + '"></div>' +
        '<table style="margin-left:12px;' + (showTime ? "" : "display:none") +  '" id="' + calId + 'Time">' +
        '<tr>' +
        '<td><input type="text" id="' + calId + 'HH" style="width:24px" maxlength="2" class="bbWidget" value="" onkeyup="calPopKey(\'' + calId + '\')"></td>' +
        '<td>:</td>' +
        '<td><input type="text" id="' + calId + 'MM" style="width:24px" maxlength="2" class="bbWidget" value="" onkeyup="calPopKey(\'' + calId + '\')"></td>' +
        '<td><select id="' + calId + 'Mer" class="bbWidget" style="width:50px" onchange="calTimeChange(\'' + calId + '\')"><option value="AM">AM<option value="PM">PM</select></td>' +
        '</tr>' +
        '</table>';
    hElem.innerHTML = html;

	return diag;
};


function calPopCreate(calId, use_week_select, pickweek, showTime) {
    var cal = new CalPop(calId, use_week_select, pickweek);
    cal.showTime = showTime;
    calPopObjs[calId] = cal;
    Ephemeral.register(cal);
    var hElem = makeElement(document.body, "div", "calPop");
    hElem._cal = cal;
    hElem.id = "cal.pop";
    hElem.onmouseout = calPopDecay;
    setEventHandler(hElem, "onmousedown", calMouseDown);
    hElem.onmouseover = calPopRevive;
    var html = '<div id="' + calId + '"></div>' +
        '<table style="margin-left:12px;' + (showTime ? "" : "display:none") +  '" id="' + calId + 'Time">' +
        '<tr>' +
        '<td><input type="text" id="' + calId + 'HH" style="width:24px" maxlength="2" class="bbWidget" value="" onkeyup="calPopKey(\'' + calId + '\')"></td>' +
        '<td>:</td>' +
        '<td><input type="text" id="' + calId + 'MM" style="width:24px" maxlength="2" class="bbWidget" value="" onkeyup="calPopKey(\'' + calId + '\')"></td>' +
        '<td><select id="' + calId + 'Mer" class="bbWidget" style="width:50px" onchange="calTimeChange(\'' + calId + '\')"><option value="AM">AM<option value="PM">PM</select></td>' +
        '</tr>' +
        '</table>';
    hElem.innerHTML = html;
    return hElem;
}


var calPopTimer = null;

function calPopHide() {
    clearTimeout(calPopTimer);
    if(calPopDiag && calPopDiag.isShowing()) calPopDiag.close();
    return;
}

function calPopRevive(button) {
	if (calPopDiag) {
    	var r = getViewBounds(button);
		var y = (r.y + r.h - 200);
    	var x = (r.x + r.w) - 151;
    	if (x < 0) x = 0;
		calPopDiag.show(x, y, null, 170, 180);	
	}
}

function CalPop(calId, use_week_select, pickweek) {
    this._listeners = [];
    this.calId = calId;
    this.use_week_select = use_week_select;
    this.pickweek = pickweek;
    this.setTime(new Date());
}

CalPop.prototype.setTime = function(time) {
    this.currday  = time.getDate();
    this.currmonth  = time.getMonth() + 1;
    this.curryear = time.getFullYear();
    this.hours = time.getHours();
    this.meridiem = "AM";
    if (this.hours >= 12) {
        if (this.hours > 12) this.hours -= 12;
        this.meridiem = "PM";
    } else if (this.hours == 0) {
    	this.hours = 12;
    }
    this.minutes = numberPad(time.getMinutes(), 2);
};

CalPop.prototype.toString = function() {
    return numberPad(this.curryear, 4) +
                "-" + numberPad(this.currmonth, 2) +
                "-" + numberPad(this.currday, 2) + (this.showTime ? (" " +
        this.hours + ":" + this.minutes + " " + this.meridiem) : "");
};


// listener = function(CalPopObj) {}
CalPop.prototype.addListener = function(listener) {
    this._listeners.push(listener);
}


var monthnames =
  ["","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
var monthlengths = [0,31,28,31,30,31,30,31,31,30,31,30,31];


function calPopGetMonthName(month) {
    return monthnames[month];
}

var calKeyTimer = null;

function calPopKey(calId) {
    clearTimeout(calKeyTimer);
    calKeyTimer = setTimeout("calTimeChange('" + calId + "')", 500);
}

function calTimeChange(calId) {
    calPopObjs[calId].updateTime();
}

CalPop.prototype.drawMonth = function() {

  var day = this.currday;
  var month = this.currmonth;
  var year = this.curryear;
  var pick_week = this.pick_week;

  var dow = calPopFirstDOW(month,year);
  var monlen = calPopMonthlength(month,year);
  var weekday;
  var theday  = 1;
  var html = "";
  var id = this.calId;

  var elem = document.getElementById(this.calId);

  html += '<table border="0" cellpadding="2" cellspacing="2" width="145">' + "\n";

  html += '<tr>';
  html += '<td class="calArrow" align="center" onclick="calPopChangeMon(-1,\'' + id + '\')" title="Previous Month">&lt;</td>';
  html += '<td class="calMonth" colspan="5" align="center" nowrap>' +  this.getMonthYearSelect(month,year,5,5,'calMonth') +'</td>';
  html += '<td class="calArrow" align="center" onclick="calPopChangeMon(1,\'' + id + '\')" title="Next Month">&gt;</td>';
  if (this.use_week_select) {
    html += '<td class="calName">&nbsp;</td>';
  }
  html += "</tr>\n";

  html += '<tr>';
  html += '<td class="calName">S</td>';
  html += '<td class="calName">M</td>';
  html += '<td class="calName">T</td>';
  html += '<td class="calName">W</td>';
  html += '<td class="calName">T</td>';
  html += '<td class="calName">F</td>';
  html += '<td class="calName">S</td>';
  if (this.use_week_select) {
    html += '<td class="calName">&nbsp;</td>';
  }
  html += "</tr>\n";

  if (pick_week) {
    if(0 < day && day <= (7-dow)) {
            html += '<tr class="calSelweek">';
    } else {
            html += '<tr class="cal">';
    }
  } else {
    html += '<tr class="cal">';
  }

  for (weekday=1; weekday <= dow; weekday++) {
    html += '<td>&nbsp;</td>';
  }

  while (theday <= monlen) {
    if (weekday > 7) {
      weekday =1;
      if (this.use_week_select) {
        html += '<td class="calArrow" align="center" onclick="calPopPickDay(' + (theday-1) + ',\'' + id + '\',true)" title="Select Week">«&nbsp;</td>';
      }

      if (pick_week) {
        if (theday <= day && day < (theday+7)) {
          html += '</tr><tr class="calSelweek">';
        } else {
          html += '</tr><tr class="cal">' + "\n";
        }
      } else {
        html += '</tr><tr class="cal">' + "\n";
      }
    }
    html += '<td';
    if (theday == day && !pick_week) {
      html += ' class="calSelday"';
    }
    html += ' onclick="calPopPickDay(' + theday + ',\'' + id + '\',false)" title="Select Day">';

    html +=  theday + '</td>';

    theday++;
    weekday++;

  }

  while (weekday < 8) {
    html += '<td>&nbsp;</td>';
    weekday++;
  }

  if (this.use_week_select) {
    html += '<td class="calArrow" align="center" onclick="calPopPickDay(' + (theday-1) + ',\'' + id + '\',true)" title="Select Week">«&nbsp;</td>';
  }

  html += "</tr>\n";
  html += "</table>\n";

  return html;
};

CalPop.prototype.getMonthYearSelect = function(month,year,yearsBefore,yearsAfter,className) {
	var html = "";
	
	
	html += '<select class="'+className+'" onChange="calPopSetMon(this.options[this.selectedIndex].value, \'' + this.calId + '\')">';
	var i;
	for (i = 0; i <= 12; i++) {
		html += '<option value="' + i +'"' + (i == month ? ' SELECTED' : '') + '>' +
			monthnames[i] + '</option>';
	}
	html += '</select>';
	html += '<select class="'+className+'" onChange="calPopSetYear(this.options[this.selectedIndex].value, \'' + this.calId + '\')">';
	var i;
	for (i = (year - yearsBefore); i < (year + yearsBefore); i++) {
		html += '<option value="' + i +'"' + (i == year ? ' SELECTED' : '') + '>' + i +'</option>';
	}
	html += '</select>'
	
	return html;
};

CalPop.prototype.getDiffMonth = function(thisMonth, diff) {

	var i = (thisMonth+diff+12000)%12;
	if (i == 0) i = 12;
	return i;
};

CalPop.prototype.getDiffYear = function(thisYear, thisMonth, diff) {
	if (diff <=0 )
		while( diff < 0) {
			thisMonth -= 1;
			diff += 1;
			if(thisMonth == 0) {
				thisMonth = 12;
				thisYear -= 1;
			}
		}

	if (diff >=0 )
		while( diff > 0) {
			thisMonth += 1;
			diff -= 1;
			if(thisMonth == 13) {
				thisMonth = 1;
				thisYear += 1;
			}
		}
	return thisYear;
};

CalPop.prototype.pickDay = function(day, pick_week) {

    this.currday = day;
    this.pick_week = pick_week;
    for (var i=0; i < this._listeners.length; ++i)
        this._listeners[i](this);
    this.update();
};



CalPop.prototype.moveMonth = function(step) {

    this.currmonth += step;
    if (this.currmonth <= 0) {
        this.currmonth = 12 + this.currmonth;
        this.curryear--;
    }
    if (this.currmonth > 12) {
        this.currmonth = this.currmonth - 12;
        this.curryear++;
    }
    var ml = calPopMonthlength(this.currmonth, this.curryear);
    if (this.currday > ml){
        this.currday = ml
    }
    this.update();
};

CalPop.prototype.setMonthYear = function(mon, year) {
    if (mon) this.currmonth = mon;
    if (year) this.curryear = year;
    var ml = calPopMonthlength(this.currmonth, this.curryear);
    if (this.currday > ml){
        this.currday = ml
    }
    this.update();
};

CalPop.prototype.update = function() {
    this.timeObj.setTime(this.toString());
    document.getElementById(this.calId).innerHTML = this.drawMonth();
};

CalPop.prototype.hide = function() {
    calPopHide();
};

CalPop.prototype.updateTime = function() {
    var hhElem = document.getElementById(this.calId + "HH");
    this.hours = hhElem.value;
    if (isNaN(parseInt(this.hours)))  {
        this.hours = "5";
    }
    var mmElem = document.getElementById(this.calId + "MM");
    this.minutes = mmElem.value;
    if (isNaN(parseInt(this.minutes)))  {
        this.minutes = "00";
    }
    var merElem = document.getElementById(this.calId + "Mer");
    this.meridiem = getSelectVal(merElem);
    this.timeObj.setTime(this.toString());
};

CalPop.prototype.activate = function(timeObj, showTime) {
    this.timeObj = timeObj;
    this.showTime = showTime;
    var d = DateUtil.parseLocaleShort(this.timeObj.getTime());
    if (d == "" || d == null) d = new Date();
    this.setTime(d);
    var timeElem = document.getElementById(this.calId + "Time");
    if (showTime) {
        timeElem.style.display = "";
        var hhElem = document.getElementById(this.calId + "HH");
        hhElem.value = this.hours;
        var mmElem = document.getElementById(this.calId + "MM");
        mmElem.value = this.minutes;
        var merElem = document.getElementById(this.calId + "Mer");
        setSelectVal(merElem, this.meridiem);
    }
    document.getElementById(this.calId).innerHTML = this.drawMonth();
};

function calPopPickDay(day, id, pick_week) {
	var calObj = calPopObjs[id];
    calObj.pickDay(day, pick_week);
    if (!calObj.showTime) {
    	window.setTimeout(function() { calObj.hide(); }, 300);
    }
}

function calPopMonthlength(month,year)  {
    if (month == 2 && (year%4) == 0) {
        return 29;
    }
    return monthlengths[month];
}

function calPopFirstDOW(month,year) {
    return (new Date(year, month-1, 1)).getDay();
}

function calPopChangeMon(step,id) {
    calPopObjs[id].moveMonth(parseInt(step));
}

function calPopSetMon(mon,id) {
    calPopObjs[id].setMonthYear(parseInt(mon));
}

function calPopSetYear(year,id) {
    calPopObjs[id].setMonthYear(null, parseInt(year));
}