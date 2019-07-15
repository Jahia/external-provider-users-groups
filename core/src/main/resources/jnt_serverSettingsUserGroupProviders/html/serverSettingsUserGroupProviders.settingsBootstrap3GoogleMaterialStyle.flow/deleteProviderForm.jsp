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

<template:addResources type="javascript"
                       resources="jquery.min.js,jquery-ui.min.js,bootstrap-filestyle.min.js,jquery.metadata.js,jquery.tablesorter.js,jquery.tablecloth.js,jquery.blockUI.js,workInProgress.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css,tablecloth.css"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting"
                                                                       value="${functions:escapeJavaScript(i18nWaiting)}"/>



<div class="page-header">
    <h2><fmt:message key="serverSettings.manageUserGroupProviders"/></h2>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger">${error}</div>
</c:if>

<div class="panel panel-default">
    <div class="panel-heading">
        <h4>
            <fmt:message key="label.userGroupProvider.delete">
                <fmt:param value="${providerKey}"/>
            </fmt:message>
        </h4>
    </div>
    <div class="panel-body">
        <p class="text-danger"><fmt:message key="label.userGroupProvider.delete.confirm"/></p>

        <form style="margin: 0;" action="${flowExecutionUrl}" method="post" onsubmit="workInProgress('${i18nWaiting}')">
            <input type="hidden" name="providerKey" value="${providerKey}"/>
            <input type="hidden" name="providerClass" value="${providerClass}"/>

            <div class="form-group form-group-sm">
                <button class="btn btn-danger btn-raised pull-right" type="submit" name="_eventId_delete">
                    <fmt:message key="label.delete"/>
                </button>
                <button class="btn btn-default pull-right" type="submit" name="_eventId_cancel">
                    <fmt:message key="label.cancel"/>
                </button>
            </div>
        </form>
    </div>
</div>
