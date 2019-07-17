/**
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.persistence.jdbc.test.spec;

import java.util.function.Supplier;

import com.ibm.watsonhealth.fhir.model.resource.Resource;
import com.ibm.watsonhealth.fhir.persistence.FHIRPersistence;
import com.ibm.watsonhealth.fhir.persistence.context.FHIRPersistenceContext;
import com.ibm.watsonhealth.fhir.persistence.util.SaltHash;

/**
 * context maintained as each operation is applied
 * @author rarnold
 *
 */
public class TestContext {
	// The persistence API
	private final FHIRPersistence persistence;

	// The current persistence context used for controlling persistence API operations
	private final Supplier<FHIRPersistenceContext> persistenceContextSupplier;

	// The fingerprint of the original resource
	private SaltHash originalFingerprint;
	
	// Current resource value
	private Resource resource;
	
	/**
	 * Initialize the context with the persistence API
	 */
	public TestContext(FHIRPersistence persistence, Supplier<FHIRPersistenceContext> persistenceContextSupplier) {
		this.persistence = persistence;
		this.persistenceContextSupplier = persistenceContextSupplier;
	}
	
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * Update the current resource value
	 * @param resource
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Getter for the persistence API
	 * @return
	 */
	public FHIRPersistence getPersistence() {
		return this.persistence;
	}

	/**
	 * Getter for a new {@link FHIRPersistenceContext}
	 * @return
	 */
	public FHIRPersistenceContext createPersistenceContext() {
		return this.persistenceContextSupplier.get();
	}
	
	public SaltHash getOriginalFingerprint() {
	    return this.originalFingerprint;
	}
	
	public void setOriginalFingerprint(SaltHash fingerprint) {
	    this.originalFingerprint = fingerprint;
	}
}