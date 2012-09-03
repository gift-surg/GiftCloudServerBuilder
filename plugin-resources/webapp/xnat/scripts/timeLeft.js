/*
 * The SESSION_EXPIRATION_TIME cookie returned from the server is double quoted for some reason
 * so unquote it before parsing it out.
 */
var patt = new RegExp("\"", "g");
function parseExpirationTimeTuple (tuple) {
  var values = tuple.replace(patt,"").split(",");
  var ret = {};
  if (values.length == 2) {
    ret.flag = values[0];
    ret.maxIdleTime = parseInt(values[1]);
  }
  return ret;
}
  
  /**
   * These cookies are available across tabs and windows:
   * - expirationTime : Stores a tuple that is (0|1, maximum idle time)
   *                    Each time the server responds the flag is toggled from 0 to 1 or vice versa.
   *                    This way we know that the cookie has changed. 
   *                    The server cannot just send the new session expiration time because the system clock
   *                    on the client and the server may be out of sync. The client must keep track of the
   *                    amount of time left in the session solely using its own clock.
   * - dialogDisplay : Boolean flag that indicates if the warning dialog is currently being displayed
   *                   Used to synchronize the hiding of the warning dialog.
   * - sessionTimeout : A boolean flag that indicates whether this session has timed out. Used to synchronize
   *                    redirecting to the login page. 
   * - serverResponseTime : store the time it takes for a response to reach the client from the server
   *  
   * The dialogDisplay and sessionTimeout need some explanation. Given the scenario where there are two session tabs A and B
   * (1) Currently a warning dialog pops up only once per session per tab. If a user extends a session in tab A
   * , tab B has no way of knowing this without the dialogDisplay cookie. 
   * (2) If A times out the page which then redirects to the login page, when that login page is loaded the server 
   * updates the SESSION_EXPIRATION_TIME cookie to a new value even though the session has expired. This happens 
   * because the redirect to the login page counts as a request. At this point the SESSION_EXPIRATION_TIME is wrong 
   * and if a user were to refresh tab B they would be redirected to the login page. 'sessionTimout' is used to 
   * broadcast to all tabs and windows that the session has indeed expired and they should take some action.
   */  
var synchronizingCookies = {
  get : function (cookieName) {
    if (YAHOO.util.Cookie.exists(cookieName)) {
      return YAHOO.util.Cookie.get(cookieName);
    }
    else {
      return null;
    }
  },
  expirationTime : {
    name : "SESSION_EXPIRATION_TIME",
    // no set function, there is no reason to ever change its value.
    get : function () {
      if (YAHOO.util.Cookie.exists(synchronizingCookies.expirationTime.name)) {
	return parseExpirationTimeTuple(YAHOO.util.Cookie.get(synchronizingCookies.expirationTime.name));
      }
      else {
	return null;
      }
    }
  },
  dialogDisplay : {
    name : "SESSION_EXPIRATION_TIME_DIALOG_DISPLAYING",
    set : function (status) {
      YAHOO.util.Cookie.set(synchronizingCookies.dialogDisplay.name, status, {path : '/'});
    },
    get : function () {
      return synchronizingCookies.get(synchronizingCookies.dialogDisplay.name);
    }
  },
  sessionTimeout : {
    name : "SESSION_EXPIRATION_TIMEOUT",
    set : function (status) {
      YAHOO.util.Cookie.set(synchronizingCookies.sessionTimeout.name, status, {path : '/'});
    },
    get : function () {
      return synchronizingCookies.get(synchronizingCookies.sessionTimeout.name);
    } 
  },
  serverResponseTime : {
    name : "SERVER_RESPONSE_TIME",
    set : function (responseTime) {
      YAHOO.util.Cookie.set(synchronizingCookies.serverResponseTime.name, responseTime, {path : "/"});
    },
    get : function () {
      if (YAHOO.util.Cookie.exists(synchronizingCookies.serverResponseTime.name)) {
	return parseInt(YAHOO.util.Cookie.get(synchronizingCookies.serverResponseTime.name));
      }
      else {
	return null;
      }      
    }
  }
};

/**
 * Local variables, think of them as thread-local variables. 
 * warningDisplayedOnce - indicates if the warning dialog has been displayed already for this
 *                        session
 * timerInterval - the interval in milliseconds at which to update the state of the session counter, 
 *                 the warning dialog and the various synchronizing cookies. This value is immutable.
 * popupTime - The time in seconds in before the end of the session at which to popup a warning dialog.
 *             Should be less than that length of the session.
 * expirationTime - A local copy of the time this session will expire. It is periodically synchronized with 
 *                  synchronizingCookies.expirationTime if it has changed. If it has the session has been extended
 * waitOneMoreCycle - This is an ugly, ugly hack. 
 *               IE 8 and under seem to try and refresh the page before the session runs out. So when we get to the 
 *               point where we think we are about to redirect to the login page and the user is running IE we wait
 *               one extra time interval just to make sure.
 */
var locals = {
  warningDisplayedOnce : false,
  timerInterval : 1000, // milliseconds
  popupTime : 59, // seconds
  expirationTime : { // see the comments on synchronizingCookies.expirationTime for an explanation on what this object represents
    flag : "-1",
    timeLeft : -1 // milliseconds
  }, 
  waitOneMoreCycle : false
};

/**
 * Set the synchronizating cookies to their base state.
 * The starting state is:
 * 1. A fresh expiration time is set
 * 2. No pop-ups are being displayed on any tabs or windows
 * 3. A fresh session is started on all tabs and windows
 * 4. No tabs or windows have timed out
 */
function refreshSynchronizingCookies () {
  synchronizingCookies.dialogDisplay.set("false");
  synchronizingCookies.sessionTimeout.set("false");
};

function disableButtons (dialog) {
  var buttons = dialog.getButtons();
  for (var i = 0; i < buttons.length; i++) {
    buttons[i].set('disabled',true);
  }
};

function enableButtons (dialog) {
  var buttons = dialog.getButtons();
  for (var i = 0; i < buttons.length; i++) {
    buttons[i].set('disabled',false);
  }
};

/**
 * If a user double-clicks a button in YUI's SimpleDialog 
 * the callback associated with that button is run twice.
 * So we have to disable the buttons after each click and 
 * enable them again when the dialog is shown.
 */
function hideWarningDialog (dialog) {
  disableButtons(dialog);
  synchronizingCookies.dialogDisplay.set("false");
  dialog.hide();
};
function showWarningDialog(dialog) {
  enableButtons(dialog);
  synchronizingCookies.dialogDisplay.set("true");
  dialog.show();
};

/**
 * If the user wants to extend the session, hide the dialog and "touch" the server
 */
var handleOk = function () {
  hideWarningDialog(warningDialog);
  touchCallback.startTime = new Date().getTime();
  YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/data/version?XNAT_CSRF=' + window.csrfToken,touchCallback,null);
};

/**
 * If the user does not want to extend the session broadcast their choice 
 * to all tabs and ensure that they all close their dialogs.
 */
var handleCancel = function () {
  hideWarningDialog(warningDialog);
  locals.warningDisplayedOnce = true;
};

/**
 * After touching the server, reset the synchronizing cookies and local variables
 */
var touchCallback = {
  cache : false, // needed because otherwise IE will cache the responses
  success : function (res) {
    var sessionExpired = res.responseText.indexOf("<HTML>") != -1;
    if (sessionExpired) {
      redirectToLogin();
    }
    else {
      refreshSynchronizingCookies();
      locals.warningDisplayedOnce = false;
    }
  },
  failure : function () {}
};

/**
 * Used to zero pad the time displays
 */
var zeroPad = function (x) {
  if (x < 10) {
    return "0"+x;
  }
  else {
    return "" + x;
  }
};

/**
 * The warning dialog
 */
var warningDialog = new YAHOO.widget.SimpleDialog("session_timeout_dialog", {
						    width:"300px",
						    close:true,
						    fixedcenter:true,
						    constraintoviewport:true,
						    modal:true,
						    icon:YAHOO.widget.SimpleDialog.ICON_WARN,
						    visible:true,
						    draggable:true,
						    hideAfterSubmit : true,
						    buttons : [
						      { text : 'Renew', handler : handleOk, isDefault:true },
						      { text : 'Close' , handler : handleCancel }
						    ]
						  });

function initWarningDialog(dialog) {
  dialog.manager = this;
  dialog.render(document.body);
  dialog.setHeader("Session Timeout Warning");
  dialog.setBody("");
  dialog.bringToTop();
  dialog.hide();   
};

/**
 * Return the timestamp as hours, minutes and seconds. Used to update the session counter
 * and the warning dialog.
 * 
 * Also hold onto the number of milliseconds left and the expiration time.
 * This value is used to determine when to redirect the page to the login page.
 */
function parseTimestamp (time) {
  var millisecondsLeft = time - (new Date().getTime());
  var secondsLeft = Math.floor(millisecondsLeft / 1000);
  var secondsPart = secondsLeft;
  var minutesPart = Math.floor(secondsPart / 60);
  secondsPart = secondsPart % 60;
  var hoursPart = Math.floor(minutesPart / 60);
  minutesPart = minutesPart % 60;
  return {
    time : time,
    millisecondsLeft : millisecondsLeft,
    secondsLeft : secondsLeft,
    secondsPart : secondsPart,
    minutesPart : minutesPart,
    hoursPart : hoursPart
  };
};

/**
 * See the comments for "locals" to see why this function necessary
 */
function checkIfFinalCycle () {
  if (locals.waitOneMoreCycle) {
    redirectToLogin();
  }
};
  
/**
 * Check if the global cookie's flag is different from what is stored locally.
 * If it is the user has extended the session from this or some other tab and 
 * we reset the expiration time of the current tab.
 * 
 * If it hasn't we recalculate the amount of time left based on the old time.
 * 
 */
function syncSessionExpirationCookieWithLocal () {
  var cookieExpirationTime = synchronizingCookies.expirationTime.get();
  if (locals.expirationTime.flag !== cookieExpirationTime.flag) {
    locals.warningDisplayedOnce = false;
    locals.expirationTime.flag = cookieExpirationTime.flag;
    locals.expirationTime.timeLeft = parseTimestamp((new Date().getTime()) + cookieExpirationTime.maxIdleTime - synchronizingCookies.serverResponseTime.get());
  }
  else {
    var oldTime = locals.expirationTime.timeLeft.time;
    locals.expirationTime.timeLeft = parseTimestamp(oldTime);
  }
}

/**
 * Determine whether to show the popup and update it or hide it.
 * There are 4 possibilities:
 * 1. The dialog needs to be displayed and this tab is currently displaying so 
 * update the message with the session timer countdown.
 * 2. The dialog needs to be displayed and this tab is not currently displaying it so toggle 
 * the local display variable, show the dialog and recurse so that case (1) is executed.
 * 3. The dialog should not be displayed and it is currently being shown so hide it.
 * 4. The dialog should not be displayed and is not displayed. Hide the dialog anyway 
 * in case another tab as been opened while the popup was open in this one.
 */
function updateMessageOrHide (dialog) {
  if (synchronizingCookies.dialogDisplay.get() === "true" && locals.warningDisplayedOnce) {
    var timeLeft = locals.expirationTime.timeLeft;
    dialog.setBody("Your XNAT session will expire in " + timeLeft.hoursPart + "h " 
	           + zeroPad(timeLeft.minutesPart) + "mins " +
		   + zeroPad(timeLeft.secondsPart) + 'secs .</br> Click "Renew" to reset session timer.');  
  }
  else if (synchronizingCookies.dialogDisplay.get() === "true" && !locals.warningDisplayedOnce) {
    locals.warningDisplayedOnce = true;
    showWarningDialog(dialog);
    updateMessageOrHide(dialog);
  }
  else if (synchronizingCookies.dialogDisplay.get() === "false" && locals.warningDisplayedOnce) {
    hideWarningDialog(dialog);
  }
  else if (synchronizingCookies.dialogDisplay.get() === "false" && !locals.warningDisplayedOnce) {
    hideWarningDialog(dialog);
  }
};

/**
 * If the session has expired just refreshing the page should redirect to the login page.
 */
function redirectToLogin () {
    YAHOO.util.Cookie.set('WARNING_BAR','OPEN',{path:'/'});
    synchronizingCookies.sessionTimeout.set("true");
	var currTime = (new Date()).getTime();
	YAHOO.util.Cookie.set('SESSION_TIMEOUT_TIME',currTime,{path:'/'});
    window.location.reload(true);
}

/**
 * Control the display of the amount of time left in the session and determine when
 * the session has timed out.
 * 
 * If it is time to warn the user and we haven't already broadcast tell the 
 * other tabs and windows that they need to open the popup dialog
 * 
 * If this session will expire before we come back around to this function stop
 * displaying a timer.
 * 
 * If this or some other tab has timed out first make sure to tell the other 
 * tabs and then redirect to the login page
 * 
 */
function sessionCountdown() {
  var timeLeft = locals.expirationTime.timeLeft;
  document.getElementById('timeLeft').innerHTML=timeLeft.hoursPart + ":" 
    + zeroPad(timeLeft.minutesPart) + ":"
    + zeroPad(timeLeft.secondsPart);
  if ((timeLeft.secondsLeft < locals.popupTime) && (!locals.warningDisplayedOnce)) {
    synchronizingCookies.dialogDisplay.set("true");
  }
  
  if (timeLeft.millisecondsLeft <= locals.timerInterval) {
    synchronizingCookies.dialogDisplay.set("false");
    document.getElementById('timeLeft').innerHTML="Time Left: Session Expired.";
  }
  if (synchronizingCookies.sessionTimeout.get() === "true" ||
      timeLeft.millisecondsLeft <= 0 ||
      timeLeft.millisecondsLeft == undefined) {
    redirectToLogin();
  }
}
  
/**
 * Initialize the synchronizing cookies and warning dialog and kick off the 
 * counter.
 */
refreshSynchronizingCookies();
initWarningDialog(warningDialog);
var initialServerTouch = {
  cache : false,
  startTime : 0,
  success : function(res) {
    if (synchronizingCookies.serverResponseTime.get() === null) {
      synchronizingCookies.serverResponseTime.set((parseInt(new Date().getTime()) - initialServerTouch.startTime) / 2);
    }
    touchCallback.success(res);
    setInterval("syncSessionExpirationCookieWithLocal();updateMessageOrHide(warningDialog);sessionCountdown();", locals.timerInterval);
  }
};
initialServerTouch.startTime = new Date().getTime();  
YAHOO.util.Connect.asyncRequest('GET',serverRoot +'/data/version?XNAT_CSRF=' + window.csrfToken,initialServerTouch,null);  
