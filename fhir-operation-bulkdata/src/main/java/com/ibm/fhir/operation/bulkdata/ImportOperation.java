/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.operation.bulkdata;

import java.io.InputStream;
import java.util.List;

import com.ibm.fhir.exception.FHIROperationException;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.resource.OperationDefinition;
import com.ibm.fhir.model.resource.Parameters;
import com.ibm.fhir.model.resource.Resource;
import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.operation.AbstractOperation;
import com.ibm.fhir.operation.bulkdata.model.type.Input;
import com.ibm.fhir.operation.bulkdata.model.type.StorageDetail;
import com.ibm.fhir.operation.bulkdata.processor.BulkDataFactory;
import com.ibm.fhir.operation.bulkdata.util.BulkDataImportUtil;
import com.ibm.fhir.operation.context.FHIROperationContext;
import com.ibm.fhir.rest.FHIRResourceHelpers;

/**
 * BulkData Specification Proposal:
 * <a href= "https://github.com/smart-on-fhir/bulk-import/blob/master/import.md">$import</a>
 */
public class ImportOperation extends AbstractOperation {
    private static final String FILE = "import.json";

    public ImportOperation() {
        super();
    }

    @Override
    protected OperationDefinition buildOperationDefinition() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(FILE);) {
            return FHIRParser.parser(Format.JSON).parse(in);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected Parameters doInvoke(FHIROperationContext operationContext, Class<? extends Resource> resourceType,
            String logicalId, String versionId, Parameters parameters, FHIRResourceHelpers resourceHelper)
            throws FHIROperationException {
        // Checks the Import Type 
        checkImportType(operationContext.getType());

        // Parameter: inputFormat
        String inputFormat = BulkDataImportUtil.retrieveInputFormat(parameters);

        // Parameter: inputSource
        String inputSource = BulkDataImportUtil.retrieveInputSource(parameters);

        // Parameter: input
        List<Input> inputs = BulkDataImportUtil.retrieveInputs(parameters);

        // Parameter: storageDetail
        StorageDetail storageDetail = BulkDataImportUtil.retrieveStorageDetails(parameters);

        return BulkDataFactory.getTenantInstance().importBulkData(inputFormat, inputSource, inputs, storageDetail,
                operationContext);
    }

    private void checkImportType(FHIROperationContext.Type type) throws FHIROperationException {
        // Check Import Type is System.  We only support system right now. 

        if (!FHIROperationContext.Type.SYSTEM.equals(type)) {
            throw buildExceptionWithIssue("Invalid call $import operation call only system is allowed",
                    IssueType.INVALID);
        }
    }
}