/*
 * (C) Copyright IBM Corp. 2016, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.ibm.fhir.audit.logging.api.AuditLogEventType;
import com.ibm.fhir.audit.logging.api.AuditLogService;
import com.ibm.fhir.audit.logging.api.AuditLogServiceFactory;
import com.ibm.fhir.audit.logging.beans.ApiParameters;
import com.ibm.fhir.audit.logging.beans.AuditLogEntry;
import com.ibm.fhir.audit.logging.beans.Batch;
import com.ibm.fhir.audit.logging.beans.ConfigData;
import com.ibm.fhir.audit.logging.beans.Context;
import com.ibm.fhir.audit.logging.beans.Data;
import com.ibm.fhir.config.FHIRConfigHelper;
import com.ibm.fhir.config.FHIRConfiguration;
import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.core.FHIRUtilities;
import com.ibm.fhir.model.resource.Basic;
import com.ibm.fhir.model.resource.Bundle;
import com.ibm.fhir.model.resource.Bundle.Entry;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.CodeableConcept;
import com.ibm.fhir.model.type.Coding;
import com.ibm.fhir.model.type.Id;
import com.ibm.fhir.model.type.Meta;
import com.ibm.fhir.model.type.code.HTTPVerb;
import com.ibm.fhir.model.util.FHIRUtil;

/**
 * This class provides convenience methods for FHIR Rest services that need to write FHIR audit log entries.
 */
public class RestAuditLogger {

    private static final String CLASSNAME = RestAuditLogger.class.getName();
    private static final Logger log = java.util.logging.Logger.getLogger(CLASSNAME);

    private static final String HEADER_IBM_APP_USER = "IBM-App-User";
    private static final String HEADER_CLIENT_CERT_CN = "IBM-App-cli-CN";
    private static final String HEADER_CLIENT_CERT_ISSUER_OU = "IBM-App-iss-OU";
    private static final String HEADER_CORRELATION_ID = "IBM-DP-correlationid";

    private static final String COMPONENT_ID = "fhir-server";


    /**
     * Builds an audit log entry for a 'create' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object being created.
     * @param startTime - The start time of the create request execution.
     * @param endTime - The end time of the create request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logCreate(HttpServletRequest request, Resource resource, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logCreate";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_CREATE);
        populateAuditLogEntry(entry, request, resource, startTime, endTime, responseStatus);

        entry.getContext().setAction("C");
        entry.setDescription("FHIR Create request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for an 'update' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param oldResource - The previous version of the Resource, before it was updated.
     * @param updatedResource - The updated version of the Resource.
     * @param startTime - The start time of the update request execution.
     * @param endTime - The end time of the update request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logUpdate(HttpServletRequest request, Resource oldResource, Resource updatedResource, Date startTime, Date endTime,
                                 Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logUpdate";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_UPDATE);
        populateAuditLogEntry(entry, request, updatedResource, startTime, endTime, responseStatus);

        entry.getContext().setAction("U");
        entry.setDescription("FHIR Update request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for an 'patch' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param oldResource - The previous version of the Resource, before it was patched.
     * @param updatedResource - The patched version of the Resource.
     * @param startTime - The start time of the patch request execution.
     * @param endTime - The end time of the patch request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logPatch(HttpServletRequest request, Resource oldResource, Resource updatedResource, Date startTime, Date endTime,
                                 Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logPatch";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_PATCH);
        populateAuditLogEntry(entry, request, updatedResource, startTime, endTime, responseStatus);

        entry.getContext().setAction("P");
        entry.setDescription("FHIR Patch request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'read' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object being read.
     * @param startTime - The start time of the read request execution.
     * @param endTime - The end time of the read request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logRead(HttpServletRequest request, Resource resource, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logRead";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_READ);
        populateAuditLogEntry(entry, request, resource, startTime, endTime, responseStatus);

        entry.getContext().setAction("R");
        entry.setDescription("FHIR Read request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'delete' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object being deleted.
     * @param startTime - The start time of the read request execution.
     * @param endTime - The end time of the read request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logDelete(HttpServletRequest request, Resource resource, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logDelete";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_DELETE);
        populateAuditLogEntry(entry, request, resource, startTime, endTime, responseStatus);

        entry.getContext().setAction("D");
        entry.setDescription("FHIR Delete request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'version-read' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object being read.
     * @param startTime - The start time of the read request execution.
     * @param endTime - The end time of the read request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logVersionRead(HttpServletRequest request, Resource resource, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logVersionRead";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_VREAD);
        populateAuditLogEntry(entry, request, resource, startTime, endTime, responseStatus);

        entry.getContext().setAction("R");
        entry.setDescription("FHIR VersionRead request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'history' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param bundle - The Bundle that is returned to the REST service caller.
     * @param startTime - The start time of the bundle request execution.
     * @param endTime - The end time of the bundle request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logHistory(HttpServletRequest request, Bundle bundle, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logHistory";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_HISTORY);
        long totalHistory = 0;

        populateAuditLogEntry(entry, request, null, startTime, endTime, responseStatus);
        if (bundle != null) {
            if (bundle.getTotal() != null) {
                totalHistory = bundle.getTotal().getValue().longValue();
            }
            entry.getContext().setBatch(Batch.builder().resourcesRead(totalHistory).build());
        }
        entry.getContext().setAction("R");
        entry.setDescription("FHIR History request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'validate' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object being validated.
     * @param startTime - The start time of the validate request execution.
     * @param endTime - The end time of the validate request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logValidate(HttpServletRequest request, Resource resource, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logValidate";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_VALIDATE);
        populateAuditLogEntry(entry, request, resource, startTime, endTime, responseStatus);

        entry.getContext().setAction("R");
        entry.setDescription("FHIR Validate request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'bundle' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param bundle - The Bundle that is returned to the REST service caller.
     * @param startTime - The start time of the bundle request execution.
     * @param endTime - The end time of the bundle request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logBundle(HttpServletRequest request, Bundle bundle, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logBundle";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_BUNDLE);
        long readCount = 0;
        long createCount = 0;
        long updateCount = 0;
        HTTPVerb requestMethod;

        populateAuditLogEntry(entry, request, null, startTime, endTime, responseStatus);
        if (bundle != null) {
            for (Entry bundleEntry : bundle.getEntry()) {
                if (bundleEntry.getRequest() != null && bundleEntry.getRequest().getMethod() != null) {
                    requestMethod = bundleEntry.getRequest().getMethod();
                    switch (HTTPVerb.ValueSet.from(requestMethod.getValue()))  {
                    case GET:
                        readCount++;
                        break;
                    case POST:
                        createCount++;
                        break;
                    case PUT:
                        updateCount++;
                        break;
                    default:
                        break;

                    }
                }
            }
        }
        entry.getContext().setBatch(Batch.builder()
                .resourcesCreated(createCount)
                .resourcesRead(readCount)
                .resourcesUpdated(updateCount).build());
        entry.setDescription("FHIR Bundle request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'search' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param queryParms - The query parameters passed to the search REST service.
     * @param bundle - The Bundle that is returned to the REST service caller.
     * @param startTime - The start time of the bundle request execution.
     * @param endTime - The end time of the bundle request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logSearch(HttpServletRequest request, MultivaluedMap<String, String> queryParms, Bundle bundle, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logSearch";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_SEARCH);
        populateAuditLogEntry(entry, request, null, startTime, endTime, responseStatus);
        long totalSearch = 0;

        if (queryParms != null && !queryParms.isEmpty()) {
            entry.getContext().setQueryParameters(queryParms.toString());
        }
        if (bundle != null) {
            if (bundle.getTotal() != null) {
                totalSearch = bundle.getTotal().getValue().longValue();
            }
            entry.getContext().setBatch(Batch.builder().resourcesRead(totalSearch).build());
        }
        entry.getContext().setAction("R");
        entry.setDescription("FHIR Search request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Builds an audit log entry for a 'metadata' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param startTime - The start time of the metadata request execution.
     * @param endTime - The end time of the metadata request execution.
     * @param responseStatus - The response status.
     * @throws Exception
     */
    public static void logMetadata(HttpServletRequest request, Date startTime, Date endTime, Response.Status responseStatus) throws Exception {
        final String METHODNAME = "logMetadata";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_METADATA);
        populateAuditLogEntry(entry, request, null, startTime, endTime, responseStatus);

        entry.getContext().setAction("R");
        entry.setDescription("FHIR Metadata request");

        auditLogSvc.logEntry(entry);
        log.exiting(CLASSNAME, METHODNAME);
    }

    /**
     * Logs an Audit Log Entry for FHIR server configuration data.
     * @param configData - The configuration data to be saved in the audit log.
     * @throws Exception
     */
    public static void logConfig(String configData) throws Exception {
        final String METHODNAME = "logConfig";
        log.entering(CLASSNAME, METHODNAME);

        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_CONFIGDATA);
        entry.setConfigData(ConfigData.builder().serverStartupParameters(configData).build());
        entry.setDescription("FHIR ConfigData request");

        auditLogSvc.logEntry(entry);

        log.exiting(METHODNAME, METHODNAME);

    }

    /**
     * Builds an audit log entry for an 'operation' REST service invocation.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param operationName - The name of the operation being executed.
     * @param resourceTypeName - The name of the resource type that is the target of the operation.
     * @param logicalId - The logical id of the target resource.
     * @param versionId - The version id of the target resource.
     * @param startTime - The start time of the metadata request execution.
     * @param endTime - The end time of the metadata request execution.
     * @param responseStatus - The response status.
     */
    public static void logOperation(HttpServletRequest request, String operationName, String resourceTypeName, String logicalId,
                                    String versionId, Date startTime, Date endTime, Response.Status responseStatus) {
        final String METHODNAME = "logOperation";
        log.entering(CLASSNAME, METHODNAME);

        Basic.Builder tempResourceBuilder = null;
        Meta meta;
        AuditLogService auditLogSvc = AuditLogServiceFactory.getService();
        AuditLogEntry entry = initLogEntry(AuditLogEventType.FHIR_OPERATION);

        try {
            if (resourceTypeName != null) {
                tempResourceBuilder = Basic.builder().code(CodeableConcept.builder()
                        .coding(Coding.builder().code(Code.of("forLogging")).build()).build());
                if (logicalId != null) {
                    tempResourceBuilder.id(logicalId);
                }
                if (versionId != null) {
                    meta = Meta.builder().versionId(Id.of(versionId)).build();
                    tempResourceBuilder.meta(meta);
                }
            }
            populateAuditLogEntry(entry, request, tempResourceBuilder != null? tempResourceBuilder.build() : null, startTime, endTime, responseStatus);
            entry.getContext().setAction("O");
            entry.setDescription("FHIR Operation request");
            entry.getContext().setOperationName(operationName);

            auditLogSvc.logEntry(entry);

        }
        catch(Throwable e) {
            log.log(Level.SEVERE, "Failure recording operation audit log entry ", e);
        }

        log.exiting(CLASSNAME, METHODNAME);
    }


    /**
     * Populates the passed audit log entry, with attributes common to all REST services.
     * @param entry - The AuditLogEntry to be populated.
     * @param request - The HttpServletRequest representation of the REST request.
     * @param resource - The Resource object.
     * @param startTime - The start time of the request execution.
     * @param endTime - The end time of the request execution.
     * @param responseStatus - The response status.
     * @return AuditLogEntry - an initialized audit log entry.
     */
    private static AuditLogEntry populateAuditLogEntry(AuditLogEntry entry, HttpServletRequest request, Resource resource,
                                                 Date startTime, Date endTime, Response.Status responseStatus) {
        final String METHODNAME = "populateAuditLogEntry";
        log.entering(CLASSNAME, METHODNAME);

        StringBuffer requestUrl;
        String patientIdExtUrl;
        List<String> userList = new ArrayList<>();

        // Build a list of possible user names. Pick the first non-null user name to include in the audit log entry.
        userList.add(request.getHeader(HEADER_IBM_APP_USER));
        userList.add(request.getHeader(HEADER_CLIENT_CERT_CN));

        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            userList.add(userPrincipal.getName());
        }
        for (String userName : userList) {
            if (userName != null && !userName.isEmpty()) {
                entry.setUserName(userName);
                break;
            }
        }

        entry.setLocation(new StringBuilder()
                            .append(request.getRemoteAddr())
                            .append("/")
                            .append(request.getRemoteHost()).toString());
        entry.setContext(new Context());
        requestUrl = request.getRequestURL();
        if (request.getQueryString() != null) {
            requestUrl.append("?");
            requestUrl.append(request.getQueryString());
        }
        entry.getContext().setApiParameters(
                 ApiParameters.builder()
                .request(requestUrl.toString())
                .status(responseStatus.getStatusCode()).build());
        entry.getContext().setStartTime(FHIRUtilities.formatTimestamp(startTime));
        entry.getContext().setEndTime(FHIRUtilities.formatTimestamp(endTime));
        if (resource!= null) {
            entry.getContext().setData(Data.builder().resourceType(resource.getClass().getSimpleName()).build());
            if (resource.getId() != null) {
                entry.getContext().getData().setId(resource.getId());
            }
            if (resource.getMeta() != null && resource.getMeta().getVersionId() != null) {
                entry.getContext().getData().setVersionId(resource.getMeta().getVersionId().getValue());
            }
        }

        entry.setClientCertCn(request.getHeader(HEADER_CLIENT_CERT_CN));
        entry.setClientCertIssuerOu(request.getHeader(HEADER_CLIENT_CERT_ISSUER_OU));
        entry.setCorrelationId(request.getHeader(HEADER_CORRELATION_ID));

        patientIdExtUrl = FHIRConfigHelper.getStringProperty(FHIRConfiguration.PROPERTY_AUDIT_PATIENT_ID_EXTURL, null);
        entry.setPatientId(FHIRUtil.getExtensionStringValue(resource, patientIdExtUrl));
        entry.getContext().setRequestUniqueId(FHIRRequestContext.get().getRequestUniqueId());

        log.exiting(CLASSNAME, METHODNAME);
        return entry;
    }

    /**
     * Builds and returns an AuditLogEntry with the minimum required fields populated.
     * @param eventType - A valid type of audit log event
     * @return AuditLogEntry with the minimum required fields populated.
     */
    private static AuditLogEntry initLogEntry(AuditLogEventType eventType) {
        final String METHODNAME = "initLogEntry";
        log.entering(CLASSNAME, METHODNAME);

        String timestamp;
        String componentIp = null;
        AuditLogEntry logEntry;
        String tenantId;

        tenantId = FHIRRequestContext.get().getTenantId();
        timestamp = FHIRUtilities.formatTimestamp(new Date(System.currentTimeMillis()));
        try {
            componentIp = InetAddress.getLocalHost().getHostAddress();
        }
        catch(UnknownHostException e) {
            log.severe("Failure acquiring host name or IP: " + e.getMessage());
        }

        logEntry = new AuditLogEntry(COMPONENT_ID, eventType.value(), timestamp, componentIp, tenantId);
        log.exiting(CLASSNAME, METHODNAME);
        return logEntry;
    }
}