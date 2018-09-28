<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="/template/includes.jsp" %>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><fmt:message key="div.specifyparametrs6" /></title>

  <!-- Load in Select2 files so can create fancy route selector -->
  <link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/css/select2.min.css" rel="stylesheet" />
  <script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0/js/select2.min.js"></script>

  <link href="params/reportParams.css" rel="stylesheet"/>
</head>
<body>
<%@include file="/template/header.jsp" %>
<div id="title">
   <fmt:message key="div.spsabr" />
</div>

<div id="mainDiv">
<form action="schAdhByTimeChart.jsp" method="POST">
   <%-- For passing agency param to the report --%>
   <input type="hidden" name="a" value="<%= request.getParameter("a")%>">

   <jsp:include page="params/routeSingle.jsp" />

   <jsp:include page="params/fromDateNumDaysTime.jsp" />

   <div class="param">
    <label for="allowableEarly"><fmt:message key="div.aear" /></label>
    <input id="allowableEarly" name="allowableEarly"
    	title="How early a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a negative number to indicate
    	early."
    	size="1"
    	value="1.0" /> <span class="note"><fmt:message key="div.minutes" /></span>
  </div>
<br/>
   <div class="param">
    <label for="allowableLate"><fmt:message key="div.alat" /></label>
    <input id="allowableLate" name="allowableLate"
    	title="How late a vehicle can arrive compared to the prediction
    	and still be acceptable. Must be a positive number to indicate
    	late."
    	size="1"
    	value="4.0"/> <span class="note"><fmt:message key="div.minutes" /></span>
  </div>

    <jsp:include page="params/submitReport.jsp" />
  </form>
</div>

</body>
</html>
