<%@page import="org.transitclock.domain.webstructs.WebAgency" %>
<%@page import="java.rmi.RemoteException" %>
<%@page import="org.transitclock.service.contract.ServerStatusInterface" %>
<%@page import="org.transitclock.monitoring.*" %>
<%@page import="java.util.List" %>
<%@ page import="org.transitclock.service.ServerStatusServiceImpl" %>

<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
    String agencyId = request.getParameter("a");
    if (agencyId == null || agencyId.isEmpty()) {
        response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
        return;
    }
%>
<html>
<head>
    <%@include file="/template/includes.jsp" %>

    <style>
        h3, .content {
            margin-left: 20%;
            margin-right: 20%;
        }
    </style>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title><fmt:message key="div.serwerstatus"/></title>
</head>
<body>
<%@include file="/template/header.jsp" %>
<div id="title"><fmt:message key="div.ssf"/> <%= WebAgency.getCachedWebAgency(agencyId).getAgencyName() %>
</div>

<%
    ServerStatusInterface serverStatusInterface =
            ServerStatusServiceImpl.instance();
    try {
        List<MonitorResult> monitorResults = serverStatusInterface.get().getMonitorResults();
        for (MonitorResult monitorResult : monitorResults) {
            if (monitorResult.getMessage() != null) {
%>
<h3><%= monitorResult.getType() %>
</h3>
<div class="content"><%= monitorResult.getMessage() %>
</div>
<%
        }
    }
} catch (RemoteException e) {
%><%= e.getMessage() %><%
    }
%>
</body>
</html>