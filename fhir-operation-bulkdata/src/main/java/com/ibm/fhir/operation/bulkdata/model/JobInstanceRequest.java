/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.operation.bulkdata.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.operation.bulkdata.model.type.Input;
import com.ibm.fhir.operation.bulkdata.model.type.JobParameter;
import com.ibm.fhir.operation.bulkdata.model.type.StorageDetail;

/**
 * BulkExport Job Instance Request
 *
 * <pre>
 * {
    "applicationName": "fhir-bulkimportexport-webapp",
    "moduleName": "fhir-bulkimportexport.war",
    "jobXMLName": "FhirBulkExportChunkJob",
    "jobParameters": {
        "fhir.resourcetype": "Patient",
        "cos.bucket.name": "fhir-r4-connectathon",
        "cos.location": "us",
        "cos.endpointurl": "https://fake.cloud",
        "cos.credential.ibm": "Y",
        "cos.api.key": "key",
        "cos.srvinst.id": "crn:v1:bluemix:public:cloud-object-storage:global:a/fake::",
        "fhir.search.fromdate": "2019-08-01"
    }
    }
 * </pre>
 */
public class JobInstanceRequest {
    private String applicationName;
    private String moduleName;
    private String jobXMLName;
    private JobParameter jobParameters;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getJobXMLName() {
        return jobXMLName;
    }

    public void setJobXMLName(String jobXMLName) {
        this.jobXMLName = jobXMLName;
    }

    public JobParameter getJobParameters() {
        return jobParameters;
    }

    public void setJobParameters(JobParameter jobParameters) {
        this.jobParameters = jobParameters;
    }

    /**
     * Builder is a convenience pattern to assemble to Java Object that reflects the BatchManagement pattern.
     */
    public static class Builder implements JobParameter.Builder {
        private JobInstanceRequest request = new JobInstanceRequest();
        private JobParameter jobParameter = new JobParameter();

        private Builder() {
            // Intentionally hiding from external callers.
        }

        public Builder applicationName(String applicationName) {
            request.setApplicationName(applicationName);
            return this;
        }

        public Builder moduleName(String moduleName) {
            request.setModuleName(moduleName);
            return this;
        }

        public Builder jobXMLName(String jobXMLName) {
            request.setJobXMLName(jobXMLName);
            return this;
        }

        public Builder fhirResourceType(String fhirResourceType) {
            jobParameter.setFhirResourceType(fhirResourceType);
            return this;
        }

        public Builder fhirSearchFromDate(String fhirSearchFromDate) {
            jobParameter.setFhirSearchFromDate(fhirSearchFromDate);
            return this;
        }

        public Builder cosBucketName(String cosBucketName) {
            jobParameter.setCosBucketName(cosBucketName);
            return this;
        }

        public Builder cosLocation(String cosLocation) {
            jobParameter.setCosLocation(cosLocation);
            return this;
        }

        public Builder cosEndpointUrl(String cosEndpointUrl) {
            jobParameter.setCosEndpointUrl(cosEndpointUrl);
            return this;
        }

        public Builder cosCredentialIbm(String cosCredentialIbm) {
            jobParameter.setCosCredentialIbm(cosCredentialIbm);
            return this;
        }

        public Builder cosApiKey(String cosApiKey) {
            jobParameter.setCosApiKey(cosApiKey);
            return this;
        }

        public Builder cosSrvInstId(String cosSrvInstId) {
            jobParameter.setCosSrvInstId(cosSrvInstId);
            return this;
        }

        public Builder fhirTenant(String fhirTenant) {
            jobParameter.setFhirTenant(fhirTenant);
            return this;
        }

        public Builder fhirDataStoreId(String fhirDataStoreId) {
            jobParameter.setFhirDataStoreId(fhirDataStoreId);
            return this;
        }

        public Builder fhirPatientGroupId(String fhirPatientGroupId) {
            jobParameter.setFhirPatientGroupId(fhirPatientGroupId);
            return this;
        }

        public Builder cosBucketPathPrefix(String cosBucketPathPrefix) {
            jobParameter.setCosBucketPathPrefix(cosBucketPathPrefix);
            return this;
        }

        public Builder fhirTypeFilters(String fhirTypeFilters) {
            jobParameter.setFhirTypeFilters(fhirTypeFilters);
            return this;
        }

        public Builder fhirDataSourcesInfo(List<Input> inputs) {
            jobParameter.setInputs(inputs);
            return this;
        }

        public Builder fhirStorageType(StorageDetail storageDetails) {
            jobParameter.setStorageDetails(storageDetails);
            return this;
        }

        public JobInstanceRequest build() {
            request.setJobParameters(jobParameter);
            return request;
        }

        public Builder cosBucketNameOperationOutcome(String cosBucketNameOperationOutcome) {
            jobParameter.setCosOperationBucketNameOo(cosBucketNameOperationOutcome);
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parser
     */
    public static class Parser {
        private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(null);

        private Parser() {
            // No Imp
        }

        public static JobInstanceRequest parse(InputStream in) throws FHIROperationException {
            try (JsonReader jsonReader = JSON_READER_FACTORY.createReader(in, StandardCharsets.UTF_8)) {
                JsonObject jsonObject = jsonReader.readObject();
                JobInstanceRequest.Builder builder = JobInstanceRequest.builder();

                if (jsonObject.containsKey("applicationName")) {
                    String applicationName = jsonObject.getString("applicationName");
                    builder.applicationName(applicationName);
                }

                if (jsonObject.containsKey("moduleName")) {
                    String moduleName = jsonObject.getString("moduleName");
                    builder.moduleName(moduleName);
                }

                if (jsonObject.containsKey("jobXMLName")) {
                    String jobXMLName = jsonObject.getString("jobXMLName");
                    builder.jobXMLName(jobXMLName);
                }

                if (jsonObject.containsKey("jobParameters")) {
                    JsonObject obj = jsonObject.getJsonObject("jobParameters");
                    JobParameter.Parser.parse(builder, obj);
                }

                return builder.build();
            } catch (Exception e) {
                throw new FHIROperationException(
                        "Problem parsing the Bulk Export Job's instance request from the server", e);
            }
        }
    }

    /**
     * Generates JSON from this object.
     */
    public static class Writer {
        // This is an internal model and does not need to honor _pretty printing as it is only communicating with the java batch framework. 
        private static final Map<java.lang.String, Object> properties =
                Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
        private static final JsonGeneratorFactory PRETTY_PRINTING_GENERATOR_FACTORY =
                Json.createGeneratorFactory(properties);

        private Writer() {
            // No Op
        }

        /**
         * @param obj
         * @param withSensitive
         *                      - indicates it should or should not be included.
         * @return
         * @throws IOException
         */
        public static String generate(JobInstanceRequest obj, boolean withSensitive) throws IOException {
            String o = "{}";
            try (StringWriter writer = new StringWriter();) {
                try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writer);) {
                    generator.writeStartObject();

                    if (obj.getApplicationName() != null) {
                        generator.write("applicationName", obj.getApplicationName());
                    }

                    if (obj.getModuleName() != null) {
                        generator.write("moduleName", obj.getModuleName());
                    }

                    if (obj.getJobXMLName() != null) {
                        generator.write("jobXMLName", obj.getJobXMLName());
                    }

                    generator.writeStartObject("jobParameters");

                    JobParameter parameter = obj.getJobParameters();
                    JobParameter.Writer.generate(generator, parameter, withSensitive);

                    generator.writeEnd();
                }
                o = writer.toString();
            }
            return o;
        }
    }
}