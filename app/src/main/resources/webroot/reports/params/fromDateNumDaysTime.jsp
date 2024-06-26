<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="language" value="" scope="session"/>
<fmt:setLocale
        value="${not empty param.language ? param.language : not empty language ? language : pageContext.request.locale}"/>
<fmt:requestEncoding value="UTF-8"/>
<fmt:setBundle basename="org.transitclock.i18n.text"/>
<%-- For specifying a begin date, number of days, begin time, and end time --%>

<script src="../javascript/jquery-timepicker/jquery.timepicker.min.js"></script>
<link rel="stylesheet" type="text/css" href="../javascript/jquery-timepicker/jquery.timepicker.css"></link>

<script>
    $(function () {
        var calendarIconTooltip = "Popup calendar to select date";

        $("#beginDate").datepicker({
            dateFormat: "dd-mm-yy",
            showOtherMonths: true,
            selectOtherMonths: true,
            // Show button for calendar
            buttonImage: "images/calendar.png",
            buttonImageOnly: true,
            showOn: "both",
            // Don't allow going past current date
            maxDate: 0,
            // onClose is for restricting end date to be after start date,
            // though it is potentially confusing to user
            onClose: function (selectedDate) {
                // Strangely need to set the title attribute for the icon again
                // so that don't revert back to a "..." tooltip
                // FIXME $(".ui-datepicker-trigger").attr("title", calendarIconTooltip);
            }
        });

        // Use a better tooltip than the default "..." for the calendar icon
        $(".ui-datepicker-trigger").attr("title", calendarIconTooltip);

        $("#beginTime, #endTime").timepicker({timeFormat: "H:i"})
            .on('change', function (evt) {
                if (evt.originalEvent) { // manual change
                    // validate that this looks like HH:MM
                    if (!evt.target.value.match(/^(([0,1][0-9])|(2[0-3])):[0-5][0-9]$/))
                        evt.target.value = evt.target.oldval ? evt.target.oldval : "";
                }
                evt.target.oldval = evt.target.value;
            });

    });
</script>

<%
    String currentDateStr = org.transitclock.utils.Time.dateStr(new java.util.Date());
%>

<div class="param">
    <label for="beginDate"><fmt:message key="div.bd"/></label>
    <input type="text" id="beginDate" name="beginDate"
           title="The first day of the range you want to examine data for.
    	<br><br> Begin date must be before the end date."
           size="10"
           value="<%= currentDateStr%>"/>
</div>

<div class="param">
    <label for=numDays><fmt:message key="div.nod"/></label>
    <select id="numDays" name="numDays"
            title="The number of days you want to examine data for.">
        <option value="1">1</option>
        <option value="2">2</option>
        <option value="3">3</option>
        <option value="4">4</option>
        <option value="5">5</option>
        <option value="6">6</option>
        <option value="7">7</option>
        <option value="8">8</option>
        <option value="9">9</option>
        <option value="10">10</option>
        <option value="11">11</option>
        <option value="12">12</option>
        <option value="13">13</option>
        <option value="14">14</option>
        <option value="15">15</option>
        <option value="16">16</option>
        <option value="17">17</option>
        <option value="18">18</option>
        <option value="19">19</option>
        <option value="20">20</option>
        <option value="21">21</option>
        <option value="22">22</option>
        <option value="23">23</option>
        <option value="24">24</option>
        <option value="25">25</option>
        <option value="26">26</option>
        <option value="27">27</option>
        <option value="28">28</option>
        <option value="29">29</option>
        <option value="30">30</option>
        <option value="31">31</option>
    </select>
</div>

<div class="param">
    <label for="beginTime"><fmt:message key="div.bt"/></label>
    <input id="beginTime" name="beginTime"
           title="Optional begin time of day to limit query to. Useful if
    	    want to see result just for rush hour, for example. Leave blank 
    		if want data for entire day. 
    		<br/><br/>Format: hh:mm, as in '07:00' for 7AM."
           size="3"
           value=""/> <span class="note">(hh:mm)</span>
</div>

<div class="param">
    <label for="endTime"><fmt:message key="div.et"/></label>
    <input id="endTime" name="endTime"
           title="Optional end time of day to limit query to. Useful if
    	    want to see result just for rush hour, for example. Leave blank 
    		if want data for entire day. 
    		<br/><br/>Format: hh:mm, as in '09:00' for 9AM. 
    		Use '23:59' for midnight."
           size="3"
           value=""/> <span class="note">(hh:mm)</span>
</div>