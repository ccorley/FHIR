/**
 * (C) Copyright IBM Corp. 2016,2017,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.client.impl;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;

/**
 * This class is responsible for adding the Authorization header to outbound REST API requests.
 */
public class FHIRBasicAuthenticator implements ClientRequestFilter {
    private String username;
    private String password;
    
    // Prevent use of the default ctor.
    protected FHIRBasicAuthenticator() {
    }
    
    
    public FHIRBasicAuthenticator(String user, String password) {
        setUsername(user);
        setPassword(password);
    }

    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.client.ClientRequestFilter#filter(javax.ws.rs.client.ClientRequestContext)
     * 
     * This method is called by the JAX-RS client runtime and will add an Authorization header to the
     * outbound REST API request to supply the necessary basic auth security token.
     */
    @Override
    public void filter(ClientRequestContext ctxt) throws IOException {
        if (getUsername() != null && !getUsername().isEmpty()) {
            MultivaluedMap<String, Object> headers = ctxt.getHeaders();
            String basicAuthToken = getUsername() + ":" + getPassword();
            String basicAuthString = "Basic " + DatatypeConverter.printBase64Binary(basicAuthToken.getBytes());
            headers.add("Authorization", basicAuthString);  
        }
    }
}
