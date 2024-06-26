<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <%@include file="/template/includes.jsp" %>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title><fmt:message key="div.SpecifyParameters"/></title>

    <!-- Load in Select2 files so can create fancy route selector -->
    <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet"/>
    <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js" type="text/javascript"></script>

    <link href="params/reportParams.css" rel="stylesheet"/>
</head>
<body>
<%@include file="/template/header.jsp" %>
<div id="title">
    <fmt:message key="div.SpecifyParameters"/>
</div>

<div id="mainDiv">
    <form action="predAccuracyRangeChart.jsp" method="POST">
        <%-- For passing agency param to the report --%>
        <input type="hidden" name="a" value="<%= request.getParameter("a")%>">

        <jsp:include page="params/routeAllOrSingle.jsp"/>

        <jsp:include page="params/fromDateNumDaysTime.jsp"/>

        <jsp:include page="params/predictionSource.jsp"/>

        <div class="param">
            <label for="predictionType"><fmt:message key="div.ptype"/>:</label>
            <select id="predictionType" name="predictionType"
                    title="Specifies whether or not to show prediction accuracy for
     	predictions that were affected by a layover. Select 'All' to show
     	data for predictions, 'Affected by layover' to only see data where
     	predictions affected by when a driver is scheduled to leave a layover, 
     	or 'Not affected by layover' if you only want data for predictions 
     	that were not affected by layovers.">
                <option value=""><fmt:message key="div.pall"/></option>
                <option value="AffectedByWaitStop"><fmt:message key="div.paff"/></option>
                <option value="NotAffectedByWaitStop"><fmt:message key="div.pnaff"/></option>
            </select>
        </div>

        <div class="param">
            <label for="allowableEarly"><fmt:message key="div.aear"/>:</label>
            <input id="allowableEarly" name="allowableEarly"
                   title="How early a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a positive number to indicate
    	early."
                   value="5.0"
                   step="0.1"
                   type="number"/> <span class="note"><fmt:message key="div.minutes"/></span>
        </div>

        <div class="param">
            <label for="allowableLate"><fmt:message key="div.alat"/>:</label>
            <input id="allowableLate" name="allowableLate"
                   title="How late a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a positive number to indicate
    	late."
                   value="5.0"
                   step="0.1"
                   type="number"/> <span class="note"><fmt:message key="div.minutes"/></span>
        </div>

        <jsp:include page="params/submitReport.jsp"/>
    </form>
</div>

</body>
</html>