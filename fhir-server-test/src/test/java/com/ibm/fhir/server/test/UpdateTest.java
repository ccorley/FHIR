/*
 * (C) Copyright IBM Corp. 2017, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.server.test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.testng.annotations.Test;

import com.ibm.fhir.client.FHIRClient;
import com.ibm.fhir.client.FHIRRequestHeader;
import com.ibm.fhir.client.FHIRResponse;
import com.ibm.fhir.model.resource.OperationOutcome;
import com.ibm.fhir.model.resource.Patient;
import com.ibm.fhir.model.test.TestUtil;
import com.ibm.fhir.model.type.Date;

/**
 * Tests the update operation.
 */
public class UpdateTest extends FHIRServerTestBase {

    private Boolean updateCreateEnabled = null;

    private Patient savedUCPatient = null;

    /**
     * Retrieve the server's conformance statement to determine the status
     * of certain runtime options.
     * @throws Exception
     */
    @Test
    public void retrieveConfig() throws Exception {
        updateCreateEnabled = isUpdateCreateSupported();
        System.out.println("Update/Create enabled?: " + updateCreateEnabled.toString());
    }

    /**
     * Test the "update/create" behavior.
     */
    @Test(dependsOnMethods = {"retrieveConfig"})
    public void testUpdateCreate1() throws Exception {
        assertNotNull(updateCreateEnabled);

        // If the "Update/Create" feature is enabled, then test it.
        if (updateCreateEnabled.booleanValue()) {

            // Generate an ID for the new resource.
            String newId = UUID.randomUUID().toString();
            Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
            patient = patient.toBuilder().id(newId).build();

            // Create the new resource via the update operation.
            FHIRClient client = getFHIRClient();
            FHIRResponse response = client.update(patient);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());

            // Now read the resource to verify it's there.
            response = client.read("Patient", newId);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            Patient responsePatient = response.getResource(Patient.class);
            assertNotNull(responsePatient);
            savedUCPatient = responsePatient;
        }
    }
    /**
     * Test the normal update behavior.
     */
    @Test(dependsOnMethods = {"testUpdateCreate1"})
    public void testUpdateCreate2() throws Exception {
        assertNotNull(updateCreateEnabled);

        // If the "Update/Create" feature is enabled, then test the normal update behavior.
        if (updateCreateEnabled.booleanValue()) {

            Patient patient = savedUCPatient;
            patient = patient.toBuilder().birthDate(Date.of("1986-06-20")).build();

            // Update the resource that was previously created.
            FHIRClient client = getFHIRClient();
            FHIRResponse response = client.update(patient);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            String locationURI = response.getLocation();
            String[] locationTokens = parseLocationURI(locationURI);
            assertEquals(3, locationTokens.length);
            assertEquals("2", locationTokens[2]);

            // Now read the resource to verify it's there.
            response = client.vread(locationTokens[0], locationTokens[1], locationTokens[2]);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            Patient responsePatient = response.getResource(Patient.class);
            assertNotNull(responsePatient);
        }
    }

    /**
     * Test the base-level "update" behavior (negative test).
     */
    @Test(dependsOnMethods = {"retrieveConfig"})
    public void testUpdateOnly1() throws Exception {
        assertNotNull(updateCreateEnabled);

        // If the "Update/Create" feature is disabled, then make sure
        // we get an error when trying to do an update on a non-existent resource.
        if (!updateCreateEnabled.booleanValue()) {

            // Generate an ID for the new resource.
            String newId = UUID.randomUUID().toString();
            Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
            patient = patient.toBuilder().id(newId).build();

            // Call update for this new resource and make sure we get back an error.
            FHIRClient client = getFHIRClient();
            FHIRResponse response = client.update(patient);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
        }
    }

    /**
     * Test the base-level "update" behavior.
     */
    @Test(dependsOnMethods = {"retrieveConfig"})
    public void testUpdateOnly2() throws Exception {
        assertNotNull(updateCreateEnabled);

        // If the "Update/Create" feature is disabled, then test the base behavior
        // to make sure we can create and then update a resource.
        if (!updateCreateEnabled.booleanValue()) {

            // Generate an ID for the new resource.
            Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");

            // Call update for this new resource and make sure we get back an error.
            FHIRClient client = getFHIRClient();
            FHIRResponse response = client.create(patient);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
            String locationURI = response.getLocation();
            String[] locationTokens = parseLocationURI(locationURI);
            assertEquals(3, locationTokens.length);
            assertEquals("1", locationTokens[2]);

            // Read the new patient.
            response = client.read(locationTokens[0], locationTokens[1]);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            Patient createdPatient = response.getResource(Patient.class);
            assertNotNull(createdPatient);

            // Update the patient.
            createdPatient = createdPatient.toBuilder().birthDate(Date.of("1987-10-09")).build();

            response = client.update(createdPatient);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            locationURI = response.getLocation();
            locationTokens = parseLocationURI(locationURI);
            assertEquals(3, locationTokens.length);
            assertEquals("2", locationTokens[2]);
        }
    }


    /**
     * Test the base-level "update" behavior.
     */
    @Test(dependsOnMethods = {"retrieveConfig"})
    public void testUpdateCreate3() throws Exception {
        assertNotNull(updateCreateEnabled);

        if (updateCreateEnabled.booleanValue()) {

            Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");

            FHIRClient client = getFHIRClient();
            FHIRResponse response = client.create(patient);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
            String locationURI = response.getLocation();

            String[] locationTokens = response.parseLocation(response.getLocation());
            String deletedId = locationTokens[1];

            // Read the new patient.
            response = client.read(locationTokens[0], locationTokens[1]);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            Patient createdPatient = response.getResource(Patient.class);
            assertNotNull(createdPatient);


            response = client.delete("Patient", deletedId);
            assertNotNull(response);
            if (isDeleteSupported()) {
                assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
                assertNotNull(response.getETag());
                assertEquals("W/\"2\"", response.getETag());
            } else {
                assertResponse(response.getResponse(), Response.Status.METHOD_NOT_ALLOWED.getStatusCode());
            }


            // Read the new patient.
            response = client.read("Patient", deletedId);
            assertResponse(response.getResponse(), Response.Status.GONE.getStatusCode());


            // Update the patient.
            createdPatient = createdPatient.toBuilder().birthDate(Date.of("1987-10-09")).build();

            response = client.update(createdPatient);
            assertResponse(response.getResponse(), Response.Status.CREATED.getStatusCode());
            locationURI = response.getLocation();
            locationTokens = parseLocationURI(locationURI);
            assertEquals(3, locationTokens.length);
            assertEquals("3", locationTokens[2]);
        }
    }

    /**
     * Test the "update/create" return preference behavior.
     */
    @Test(dependsOnMethods = {"retrieveConfig"})
    public void testUpdateCreateWithReturnPref() throws Exception {
        assertNotNull(updateCreateEnabled);

        // If the "Update/Create" feature is enabled, then test it.
        if (updateCreateEnabled.booleanValue()) {

            Patient patient = TestUtil.readLocalResource("Patient_JohnDoe.json");
            FHIRClient client = getFHIRClient();
            FHIRRequestHeader returnPref;

            // Create the new resource with return pref of "minimal"
            String id1 = UUID.randomUUID().toString();
            patient = patient.toBuilder().id(id1).build();
            returnPref = new FHIRRequestHeader("Prefer", "return=minimal");
            FHIRResponse response1 = client.update(patient, returnPref);
            assertNotNull(response1);
            assertResponse(response1.getResponse(), Response.Status.CREATED.getStatusCode());
            assertTrue(response1.isEmpty());
            // Now read the resource to verify it's there.
            response1 = client.read("Patient", id1);
            assertNotNull(response1);
            assertResponse(response1.getResponse(), Response.Status.OK.getStatusCode());
            Patient responsePatient1 = response1.getResource(Patient.class);
            assertNotNull(responsePatient1);

            // Create the new resource with return pref of "representation"
            String id2 = UUID.randomUUID().toString();
            patient = patient.toBuilder().id(id2).build();
            returnPref = new FHIRRequestHeader("Prefer", "return=representation");
            FHIRResponse response2 = client.update(patient, returnPref);
            assertNotNull(response2);
            assertResponse(response2.getResponse(), Response.Status.CREATED.getStatusCode());
            assertFalse(response2.isEmpty());
            Patient responseResource = response2.getResource(Patient.class);
            // Now read the resource to verify it's there and that it matches the resource in the original response.
            response2 = client.read("Patient", id2);
            assertNotNull(response2);
            assertResponse(response2.getResponse(), Response.Status.OK.getStatusCode());
            Patient responsePatient2 = response2.getResource(Patient.class);
            assertNotNull(responsePatient2);
            assertEquals(responsePatient2, responseResource);

            // Create the new resource with return pref of "representation"
            String id3 = UUID.randomUUID().toString();
            patient = patient.toBuilder().id(id3).build();
            returnPref = new FHIRRequestHeader("Prefer", "return=OperationOutcome");
            FHIRResponse response3 = client.update(patient, returnPref);
            assertNotNull(response3);
            assertResponse(response3.getResponse(), Response.Status.CREATED.getStatusCode());
            assertFalse(response3.isEmpty());
            OperationOutcome oo = response3.getResource(OperationOutcome.class);
            assertTrue(oo.getIssue().toString().contains("dom-6: A resource should have narrative for robust management"));
            // Now read the resource to verify it's there.
            response3 = client.read("Patient", id3);
            assertNotNull(response3);
            assertResponse(response3.getResponse(), Response.Status.OK.getStatusCode());
            Patient responsePatient = response3.getResource(Patient.class);
            assertNotNull(responsePatient);
        }
    }

    /**
     * Test the 'normal' update return preference behavior.
     */
    @Test(dependsOnMethods = {"testUpdateCreate3"})
    public void testUpdateCreateWithReturnPref2() throws Exception {
        assertNotNull(updateCreateEnabled);
        FHIRClient client = getFHIRClient();
        FHIRRequestHeader returnPref;

        // If the "Update/Create" feature is enabled, then test the normal update behavior.
        if (updateCreateEnabled.booleanValue()) {

            Patient patient = savedUCPatient;
            patient = patient.toBuilder().birthDate(Date.of("1986-06-20")).build();

            // Update the resource that was previously created with return pref "minimal".
            returnPref = new FHIRRequestHeader("Prefer", "return=minimal");
            FHIRResponse response = client.update(patient, returnPref);
            assertNotNull(response);
            assertResponse(response.getResponse(), Response.Status.OK.getStatusCode());
            assertTrue(response.isEmpty());

            // Update the resource that was previously created with return pref "representation".
            returnPref = new FHIRRequestHeader("Prefer", "return=representation");
            FHIRResponse response2 = client.update(patient, returnPref);
            assertNotNull(response2);
            assertResponse(response2.getResponse(), Response.Status.OK.getStatusCode());
            assertFalse(response2.isEmpty());
            Patient responseResource = response2.getResource(Patient.class);
            assertNotNull(responseResource);

            // Update the resource that was previously created with return pref "OperationOutcome".
            returnPref = new FHIRRequestHeader("Prefer", "return=OperationOutcome");
            FHIRResponse response3 = client.update(patient, returnPref);
            assertNotNull(response3);
            assertResponse(response3.getResponse(), Response.Status.OK.getStatusCode());
            assertFalse(response3.isEmpty());
            OperationOutcome oo = response3.getResource(OperationOutcome.class);
            assertTrue(oo.getIssue().toString().contains("dom-6: A resource should have narrative for robust management"));
        }
    }
}
