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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.ibm.fhir.core.FHIRMediaType;
import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.rest.FHIRRestOperationResponse;
import com.ibm.fhir.server.util.FHIRRestHelper;
import com.ibm.fhir.server.util.RestAuditLogger;

@Path("/")
@Consumes({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON,
        FHIRMediaType.APPLICATION_FHIR_XML, MediaType.APPLICATION_XML })
@Produces({ FHIRMediaType.APPLICATION_FHIR_JSON, MediaType.APPLICATION_JSON,
        FHIRMediaType.APPLICATION_FHIR_XML, MediaType.APPLICATION_XML })
@RolesAllowed("FHIRUsers")
@RequestScoped
public class VRead extends FHIRResource {
    private static final Logger log = java.util.logging.Logger.getLogger(VRead.class.getName());

    // The JWT of the current caller. Since this is a request scoped resource, the
    // JWT will be injected for each JAX-RS request. The injection is performed by
    // the mpJwt feature.
    @Inject
    private JsonWebToken jwt;

    public VRead() throws Exception {
        super();
    }

    @GET
    @Path("{type}/{id}/_history/{vid}")
    public Response vread(@PathParam("type") String type, @PathParam("id") String id, @PathParam("vid") String vid) {
        log.entering(this.getClass().getName(), "vread(String,String,String)");
        Date startTime = new Date();
        Response.Status status = null;
        FHIRRestOperationResponse ior = null;

        try {
            checkInitComplete();

            FHIRRestHelper helper = new FHIRRestHelper(getPersistenceImpl());
            Resource resource = helper.doVRead(type, id, vid, null);
            status = Status.OK;
            ResponseBuilder response = Response.ok().entity(resource);
            response = addHeaders(response, resource);
            return response.build();
        } catch (FHIROperationException e) {
            status = issueListToStatus(e.getIssues());
            return exceptionResponse(e, status);
        } catch (Exception e) {
            status = Status.INTERNAL_SERVER_ERROR;
            return exceptionResponse(e, status);
        } finally {
            try {
                RestAuditLogger.logVersionRead(httpServletRequest,
                        ior != null ? ior.getResource() : null,
                        startTime, new Date(), status);
            } catch (Exception e) {
                log.log(Level.SEVERE, AUDIT_LOGGING_ERR_MSG, e);
            }

            log.exiting(this.getClass().getName(), "vread(String,String,String)");
        }
    }
}
