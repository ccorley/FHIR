/*
 * (C) Copyright IBM Corp. 2016, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.resources;

import static com.ibm.fhir.server.util.IssueTypeToHttpStatusMapper.issueListToStatus;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonPatch;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.fhir.config.FHIRRequestContext;
import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.core.HTTPReturnPreference;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.patch.FHIRJsonPatch;
import com.ibm.fhir.model.patch.FHIRPatch;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.model.util.FHIRUtil;
import com.ibm.fhir.path.patch.FHIRPathPatch;
import com.ibm.fhir.persistence.exception.FHIRPersistenceResourceNotFoundException;
import com.ibm.fhir.rest.FHIRRestOperationResponse;
import com.ibm.fhir.server.annotation.PATCH;
import com.ibm.fhir.server.util.FHIRRestHelper;
import com.ibm.fhir.server.util.RestAuditLogger;

@Path("/")
@Consumes({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON,
        FHIRMediaType.APPLICATION_FHIR_XML, MediaType.APPLICATION_XML })
@Produces({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON,
        FHIRMediaType.APPLICATION_FHIR_XML, MediaType.APPLICATION_XML })
@RolesAllowed("FHIRUsers")
@RequestScoped
public class Patch extends FHIRResource {
    private static final Logger log = java.util.logging.Logger.getLogger(Patch.class.getName());

    // The JWT of the current caller. Since this is a request scoped resource, the
    // JWT will be injected for each JAX-RS request. The injection is performed by
    // the mpJwt feature.
    @Inject
    private JsonWebToken jwt;

    public Patch() throws Exception {
        super();
    }

    @PATCH
    @Consumes({ FHIRMediaType.APPLICATION_JSON_PATCH })
    @Produces({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON })
    @Path("{type}/{id}")
    public Response patch(@PathParam("type") String type, @PathParam("id") String id, JsonArray array,
            @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch) {
        log.entering(this.getClass().getName(), "patch(String,String,JsonArray)");
        Date startTime = new Date();
        Response.Status status = null;
        FHIRRestOperationResponse ior = null;

        try {
            checkInitComplete();

            FHIRPatch patch = createPatch(array);

            FHIRRestHelper helper = new FHIRRestHelper(getPersistenceImpl());
            ior = helper.doPatch(type, id, patch, ifMatch, null, null);

            status = ior.getStatus();
            ResponseBuilder response = Response.status(status)
                    .location(toUri(getAbsoluteUri(getRequestBaseUri(type), ior.getLocationURI().toString())));

            Resource resource = ior.getResource();
            if (resource != null && HTTPReturnPreference.REPRESENTATION == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(resource);
            } else if (ior.getOperationOutcome() != null && HTTPReturnPreference.OPERATION_OUTCOME == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(ior.getOperationOutcome());
            }
            response = addHeaders(response, resource);
            return response.build();
        } catch (FHIRPersistenceResourceNotFoundException e) {
            status = Status.METHOD_NOT_ALLOWED;
            return exceptionResponse(e, status);
        } catch (FHIROperationException e) {
            status = issueListToStatus(e.getIssues());
            return exceptionResponse(e, status);
        } catch (Exception e) {
            status = Status.INTERNAL_SERVER_ERROR;
            return exceptionResponse(e, status);
        } finally {
            try {
                RestAuditLogger.logPatch(httpServletRequest,
                        ior != null ? ior.getPrevResource() : null,
                        ior != null ? ior.getResource() : null,
                        startTime, new Date(), status);
            } catch (Exception e) {
                log.log(Level.SEVERE, AUDIT_LOGGING_ERR_MSG, e);
            }

            log.exiting(this.getClass().getName(), "patch(String,String,JsonArray)");
        }
    }

    @PATCH
    @Path("{type}/{id}")
    public Response patch(@PathParam("type") String type, @PathParam("id") String id, Parameters parameters,
            @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch) {
        log.entering(this.getClass().getName(), "patch(String,String,Parameters)");
        Date startTime = new Date();
        Response.Status status = null;
        FHIRRestOperationResponse ior = null;

        try {
            checkInitComplete();

            FHIRPatch patch;
            try {
                patch = FHIRPathPatch.from(parameters);
            } catch(IllegalArgumentException e) {
                throw buildRestException(e.getMessage(), IssueType.INVALID);
            }

            FHIRRestHelper helper = new FHIRRestHelper(getPersistenceImpl());
            ior = helper.doPatch(type, id, patch, ifMatch, null, null);

            ResponseBuilder response =
                    Response.ok().location(toUri(getAbsoluteUri(getRequestBaseUri(type), ior.getLocationURI().toString())));
            status = ior.getStatus();
            response.status(status);

            Resource resource = ior.getResource();
            if (resource != null && HTTPReturnPreference.REPRESENTATION == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(resource);
            } else if (ior.getOperationOutcome() != null &&
                       HTTPReturnPreference.OPERATION_OUTCOME == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(ior.getOperationOutcome());
            }
            response = addHeaders(response, resource);
            return response.build();
        } catch (FHIRPersistenceResourceNotFoundException e) {
            status = Status.METHOD_NOT_ALLOWED;
            return exceptionResponse(e, status);
        } catch (FHIROperationException e) {
            status = issueListToStatus(e.getIssues());
            return exceptionResponse(e, status);
        } catch (Exception e) {
            status = Status.INTERNAL_SERVER_ERROR;
            return exceptionResponse(e, status);
        } finally {
            try {
                RestAuditLogger.logPatch(httpServletRequest,
                        ior != null ? ior.getPrevResource() : null,
                        ior != null ? ior.getResource() : null,
                        startTime, new Date(), status);
            } catch (Exception e) {
                log.log(Level.SEVERE, AUDIT_LOGGING_ERR_MSG, e);
            }

            log.exiting(this.getClass().getName(), "patch(String,String,Parameters)");
        }
    }

    @PATCH
    @Consumes({ FHIRMediaType.APPLICATION_JSON_PATCH })
    @Produces({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON })
    @Path("{type}")
    public Response conditionalPatch(@PathParam("type") String type, JsonArray array, @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch) {
        log.entering(this.getClass().getName(), "conditionalPatch(String,String,JsonArray)");
        Date startTime = new Date();
        Response.Status status = null;
        FHIRRestOperationResponse ior = null;

        try {
            checkInitComplete();

            FHIRPatch patch = createPatch(array);

            String searchQueryString = httpServletRequest.getQueryString();
            if (searchQueryString == null || searchQueryString.isEmpty()) {
                String msg =
                        "Cannot PATCH to resource type endpoint unless a search query string is provided for a conditional patch.";
                throw buildRestException(msg, IssueType.INVALID);
            }

            FHIRRestHelper helper = new FHIRRestHelper(getPersistenceImpl());
            ior = helper.doPatch(type, null, patch, ifMatch, searchQueryString, null);

            ResponseBuilder response =
                    Response.ok().location(toUri(getAbsoluteUri(getRequestBaseUri(type), ior.getLocationURI().toString())));
            status = ior.getStatus();
            response.status(status);

            Resource resource = ior.getResource();
            if (resource != null && HTTPReturnPreference.REPRESENTATION == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(resource);
            } else if (ior.getOperationOutcome() != null &&
                    HTTPReturnPreference.OPERATION_OUTCOME == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(ior.getOperationOutcome());
            }

            response = addHeaders(response, ior.getResource());

            return response.build();
        } catch (FHIRPersistenceResourceNotFoundException e) {
            status = Status.METHOD_NOT_ALLOWED;
            return exceptionResponse(e, status);
        } catch (FHIROperationException e) {
            status = issueListToStatus(e.getIssues());
            return exceptionResponse(e, status);
        } catch (Exception e) {
            status = Status.INTERNAL_SERVER_ERROR;
            return exceptionResponse(e, status);
        } finally {
            try {
                RestAuditLogger.logPatch(httpServletRequest,
                        ior != null ? ior.getPrevResource() : null,
                        ior != null ? ior.getResource() : null,
                        startTime, new Date(), status);
            } catch (Exception e) {
                log.log(Level.SEVERE, AUDIT_LOGGING_ERR_MSG, e);
            }

            log.exiting(this.getClass().getName(), "conditionalPatch(String,String,JsonArray)");
        }
    }

    @PATCH
    @Path("{type}")
    public Response conditionalPatch(@PathParam("type") String type, Parameters parameters, @HeaderParam(HttpHeaders.IF_MATCH) String ifMatch) {
        log.entering(this.getClass().getName(), "conditionalPatch(String,String,Parameters)");
        Date startTime = new Date();
        Response.Status status = null;
        FHIRRestOperationResponse ior = null;

        try {
            checkInitComplete();

            FHIRPatch patch;
            try {
                patch = FHIRPathPatch.from(parameters);
            } catch(IllegalArgumentException e) {
                throw buildRestException(e.getMessage(), IssueType.INVALID);
            }

            String searchQueryString = httpServletRequest.getQueryString();
            if (searchQueryString == null || searchQueryString.isEmpty()) {
                String msg =
                        "Cannot PATCH to resource type endpoint unless a search query string is provided for a conditional patch.";
                throw buildRestException(msg, IssueType.INVALID);
            }

            FHIRRestHelper helper = new FHIRRestHelper(getPersistenceImpl());
            ior = helper.doPatch(type, null, patch, ifMatch, searchQueryString, null);

            status = ior.getStatus();
            ResponseBuilder response = Response.status(status)
                    .location(toUri(getAbsoluteUri(getRequestBaseUri(type), ior.getLocationURI().toString())));

            Resource resource = ior.getResource();
            if (resource != null && HTTPReturnPreference.REPRESENTATION == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(resource);
            } else if (ior.getOperationOutcome() != null && HTTPReturnPreference.OPERATION_OUTCOME == FHIRRequestContext.get().getReturnPreference()) {
                response.entity(ior.getOperationOutcome());
            }

            response = addHeaders(response, ior.getResource());

            return response.build();
        } catch (FHIRPersistenceResourceNotFoundException e) {
            status = Status.METHOD_NOT_ALLOWED;
            return exceptionResponse(e, status);
        } catch (FHIROperationException e) {
            status = issueListToStatus(e.getIssues());
            return exceptionResponse(e, status);
        } catch (Exception e) {
            status = Status.INTERNAL_SERVER_ERROR;
            return exceptionResponse(e, status);
        } finally {
            try {
                RestAuditLogger.logPatch(httpServletRequest,
                        ior != null ? ior.getPrevResource() : null,
                        ior != null ? ior.getResource() : null,
                        startTime, new Date(), status);
            } catch (Exception e) {
                log.log(Level.SEVERE, AUDIT_LOGGING_ERR_MSG, e);
            }

            log.exiting(this.getClass().getName(), "conditionalPatch(String,String,Parameters)");
        }
    }

    private FHIRPatch createPatch(JsonArray array) throws FHIROperationException {
        try {
            FHIRPatch patch = FHIRPatch.patch(array);
            JsonPatch jsonPatch = patch.as(FHIRJsonPatch.class).getJsonPatch();
            for (JsonValue value : jsonPatch.toJsonArray()) {
                // validate path
                String path = value.asJsonObject().getString("path");
                if ("/id".equals(path) ||
                        "/meta/versionId".equals(path) ||
                        "/meta/lastUpdated".equals(path)) {
                    throw new IllegalArgumentException("Path: '" + path
                            + "' is not allowed in a patch operation.");
                }
            }
            return patch;
        } catch (Exception e) {
            String msg = "Invalid patch: " + e.getMessage();
            throw new FHIROperationException(msg, e)
                    .withIssue(FHIRUtil.buildOperationOutcomeIssue(msg, IssueType.INVALID));
        }
    }
}
