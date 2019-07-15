<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<template:addResources type="css" resources="datatables/css/bootstrap-theme.css,tablecloth.css"/>
<template:addResources type="css" resources="files.css"/>
<template:addResources type="css" resources="css/createEditForm.css"/>
<template:addResources type="javascript"
                       resources="jquery.min.js,jquery-ui.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.blockUI.js,workInProgress.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>
<template:addResources type="javascript"
                       resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js,settings/dataTables.initializer.js"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting"
                                                                       value="${functions:escapeJavaScript(i18nWaiting)}"/>

<script type="text/javascript" charset="utf-8">
    $(document).ready(function () {
        dataTablesSettings.init('providersTable', 10, [], null, null);

    });
</script>

<div class="page-header">
    <h2><fmt:message key="serverSettings.manageUserGroupProviders"/></h2>
</div>


<c:forEach var="msg" items="${flowRequestContext.messageContext.allMessages}">
    <div class="${msg.severity == 'ERROR' ? 'validationError' : ''} alert ${msg.severity == 'ERROR' ? 'alert-danger' : 'alert-success'}">
        <button type="button" class="close" data-dismiss="alert">&times;</button>
            ${fn:escapeXml(msg.text)}</div>
</c:forEach>
<div class="panel panel-default">
    <div class="panel-body">
        <table id="providersTable" class="table table-bordered table-striped">
            <thead>
            <tr>
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
                <span class="label label-danger">
                    <fmt:message key="label.userGroupProvider.targetUnavailable"/>
                </span>
                            </c:when>
                            <c:when test="${userGroupProvider.running}">
                <span class="label label-success">
                    <fmt:message key="label.userGroupProvider.running"/>
                </span>
                            </c:when>
                            <c:otherwise>
                <span class="label label-warning">
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
                            <c:if test="${userGroupProvider.editSupported}">
                                <input type="hidden" name="editJSP" value="${userGroupProvider.editJSP}"/>
                            </c:if>

                            <c:choose>
                                <c:when test="${userGroupProvider.running}">
                                    <button data-toggle="tooltip" data-placement="bottom" title=""
                                            data-original-title="<fmt:message key="label.userGroupProvider.suspend"/>"
                                            class="btn btn-warning btn-fab btn-fab-xs" type="submit"
                                            name="_eventId_suspendProvider">
                                        <i class="material-icons">pause</i>
                                    </button>
                                </c:when>
                                <c:otherwise>
                                    <button data-placement="bottom" title=""
                                            data-original-title="<fmt:message key="label.userGroupProvider.resume"/>"
                                            data-toggle="tooltip"
                                            class="btn btn-success btn-fab btn-fab-xs" type="submit"
                                            name="_eventId_resumeProvider">
                                        <i class="material-icons">play_arrow</i>
                                    </button>
                                </c:otherwise>
                            </c:choose>

                            <c:if test="${userGroupProvider.editSupported}">
                                <button data-placement="bottom" title=""
                                        data-original-title="<fmt:message key="label.edit"/>"
                                        class="btn btn-default btn-fab btn-fab-xs"
                                        type="submit" name="_eventId_editProvider" data-toggle="tooltip">
                                    <i class="material-icons">edit</i>
                                </button>
                            </c:if>

                            <c:if test="${userGroupProvider.deleteSupported}">
                                <button data-placement="bottom" title=""
                                        data-original-title="<fmt:message key="label.delete"/>"
                                        class="btn btn-danger btn-fab btn-fab-xs"
                                        type="submit"
                                        name="_eventId_deleteProvider" data-toggle="tooltip">
                                    <i class="material-icons">delete</i>
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
                    <input type="hidden" name="createJSP" value="${createConfiguration.value.createJSP}"/>

                    <button class="btn btn-raised btn-primary pull-right" type="submit" name="_eventId_createProvider">
                        <fmt:message key="label.userGroupProvider.create">
                            <fmt:param value="${createConfiguration.value.name}"/>
                        </fmt:message>
                    </button>
                </form>
            </c:forEach>
        </c:if>

    </div>
</div>
