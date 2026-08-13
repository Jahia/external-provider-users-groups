<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="javascript" resources="jquery.min.js,jquery-ui.min.js,admin-bootstrap.js,bootstrap-filestyle.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>
<template:addResources type="javascript" resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js"/>

<script type="text/javascript" charset="utf-8">
    $(document).ready(function() {
        var providersTable = $('#providersTable');

        providersTable.dataTable({
            "sDom": "<'row-fluid'<'span6'l><'span6 text-right'f>r>t<'row-fluid'<'span6'i><'span6 text-right'p>>",
            "iDisplayLength": 10,
            "sPaginationType": "bootstrap",
            "aaSorting": [] //this option disable sort by default, the user steal can use column names to sort the table
        });
    });
</script>


<h2><fmt:message key="serverSettings.manageUserGroupProviders"/></h2>
<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-error' : 'alert-success'}"><button type="button" class="close" data-dismiss="alert">&times;</button>${fn:escapeXml(msg.text)}</div>
</c:forEach>
<table id="providersTable" class="table table-bordered table-striped table-hover">
    <thead>
    <tr>
        <%--<th class="{sorter: false}">&nbsp;</th>--%>
        <%--<th>#</th>--%>
        <th>
            <fmt:message key="label.key"/>
        </th>
        <th>
            <fmt:message key="label.userGroupProvider.class"/>
        </th>
        <th>
            <fmt:message key="label.userGroupProvider.supportsGroups"/>
        </th>
        <th>
            <fmt:message key="label.userGroupProvider.location"/>
        </th>
            <th width="100px">
                <fmt:message key="label.status"/>
            </th>
        <th class="{sorter: false}">
            <fmt:message key="label.actions"/>
        </th>
    </tr>
    </thead>

    <tbody>

    <c:forEach items="${userGroupProviders}" var="userGroupProvider" varStatus="loopStatus">
        <tr>
            <td>
                ${userGroupProvider.key}
            </td>
            <td>
                ${userGroupProvider.providerClass}
            </td>
            <td>
                ${userGroupProvider.groupSupported}

            </td>
            <td>
                <c:choose>
                    <c:when test="${empty userGroupProvider.siteKey}">
                        <fmt:message key="label.userGroupProvider.location.global"/>
                    </c:when>
                    <c:otherwise>
                        <fmt:message key="label.userGroupProvider.location.local">
                            <fmt:param value="${userGroupProvider.siteKey}"/>
                        </fmt:message>
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <c:choose>
                    <c:when test="${not userGroupProvider.targetAvailable}">
                <span class="badge badge-important">
                    <fmt:message key="label.userGroupProvider.targetUnavailable"/>
                </span>
                    </c:when>
                    <c:when test="${userGroupProvider.running}">
                <span class="badge badge-success">
                    <fmt:message key="label.userGroupProvider.running"/>
                </span>
                    </c:when>
                    <c:otherwise>
                <span class="badge badge-warning">
                    <fmt:message key="label.userGroupProvider.stopped"/>
                </span>
                    </c:otherwise>
                </c:choose>
            </td>
            <td>
                <form style="margin: 0;" action="${flowExecutionUrl}" method="post">
                    <input type="hidden" name="providerKey" value="${userGroupProvider.key}"/>
                    <c:if test="${userGroupProvider.editSupported or userGroupProvider.deleteSupported}">
                        <input type="hidden" name="providerClass" value="${userGroupProvider.providerClass}"/>
                    </c:if>

                    <c:choose>
                        <c:when test="${userGroupProvider.running}">
                            <button class="btn" type="submit" name="_eventId_suspendProvider">
                                <i class="icon icon-pause"></i>&nbsp;<fmt:message key="label.userGroupProvider.suspend"/>
                            </button>
                        </c:when>
                        <c:otherwise>
                            <button class="btn" type="submit" name="_eventId_resumeProvider">
                                <i class="icon icon-play"></i>&nbsp;<fmt:message key="label.userGroupProvider.resume"/>
                            </button>
                        </c:otherwise>
                    </c:choose>

                    <c:if test="${userGroupProvider.editSupported}">
                        <button class="btn" type="submit" name="_eventId_editProvider">
                            <i class="icon icon-pencil"></i>&nbsp;<fmt:message key="label.edit"/>
                        </button>
                    </c:if>

                    <c:if test="${userGroupProvider.deleteSupported}">
                        <button class="btn btn-danger" type="submit" name="_eventId_deleteProvider">
                            <i class="icon icon-white icon-trash"></i>&nbsp;<fmt:message key="label.delete"/>
                        </button>
                    </c:if>
                </form>
            </td>
        </tr>
    </c:forEach>
    </tbody>
</table>

<c:if test="${not empty createConfigurations}">
    <c:forEach items="${createConfigurations}" var="createConfiguration" varStatus="loopStatus">
        <form style="margin: 0;" action="${flowExecutionUrl}" method="post">
            <input type="hidden" name="providerClass" value="${createConfiguration.key}"/>
            <input type="hidden" name="providerName" value="${createConfiguration.value.name}"/>

            <button class="btn" type="submit" name="_eventId_createProvider">
                <i class="icon icon-plus"></i>&nbsp;<fmt:message key="label.userGroupProvider.create">
                        <fmt:param value="${createConfiguration.value.name}"/>
                    </fmt:message>
            </button>
        </form>
    </c:forEach>
</c:if>