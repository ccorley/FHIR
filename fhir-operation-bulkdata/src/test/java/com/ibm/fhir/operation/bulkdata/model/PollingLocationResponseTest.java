/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.operation.bulkdata.model;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.testng.annotations.Test;

import com.ibm.fhir.model.type.Instant;
import com.ibm.fhir.operation.bulkdata.model.PollingLocationResponse.Output;

/**
 * Simple Test for the Rough Response defined in the BulkData Export
 */
public class PollingLocationResponseTest {
    private static final Map<java.lang.String, Object> properties =
            Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
    private static final JsonGeneratorFactory PRETTY_PRINTING_GENERATOR_FACTORY =
            Json.createGeneratorFactory(properties);

    @Test
    public void testOuptutSerialization() throws IOException {
        // Tests the output serialization. 
        PollingLocationResponse.Output output = new PollingLocationResponse.Output("test1", "test2", "3", "test4");
        String actual = null;
        String expected =
                "\n{\n" + "    \"type\": \"test1\",\n" + "    \"url\": \"test2\",\n" + "    \"count\": 3,\n"
                        + "    \"inputUrl\": \"test4\"\n" + "}";
        try (StringWriter writerx = new StringWriter();) {
            try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writerx);) {
                PollingLocationResponse.Output.Writer.generate(generator, output);
            }
            actual = writerx.toString();
        }
        assertNotNull(actual);
        assertEquals(actual, expected);

        // Test Setting Output
        output.setCount("1");
        output.setInputUrl("2");
        output.setUrl("3");
        output.setType("OperationOutcome");

        expected =
                "\n{\n" + "    \"type\": \"OperationOutcome\",\n" + "    \"url\": \"3\",\n" + "    \"count\": 1,\n"
                        + "    \"inputUrl\": \"2\"\n" + "}";
        try (StringWriter writerx = new StringWriter();) {
            try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writerx);) {
                PollingLocationResponse.Output.Writer.generate(generator, output);
            }
            actual = writerx.toString();
        }
        assertNotNull(actual);
        assertEquals(actual, expected);

        // Test $export
        output.setInputUrl(null);
        expected =
                "\n" + "{\n" + "    \"type\": \"OperationOutcome\",\n" + "    \"url\": \"3\",\n"
                        + "    \"count\": 1\n" + "}";
        try (StringWriter writerx = new StringWriter();) {
            try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writerx);) {
                PollingLocationResponse.Output.Writer.generate(generator, output);
            }
            actual = writerx.toString();
        }
        assertNotNull(actual);
        assertEquals(actual, expected);

        // Test $export
        output   = new PollingLocationResponse.Output("test1", "test2", "3");
        expected =
                "\n" + "{\n" + "    \"type\": \"test1\",\n" + "    \"url\": \"test2\",\n" + "    \"count\": 3\n"
                        + "}";
        try (StringWriter writerx = new StringWriter();) {
            try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writerx);) {
                PollingLocationResponse.Output.Writer.generate(generator, output);
            }
            actual = writerx.toString();
        }
        assertNotNull(actual);
        assertEquals(actual, expected);

        assertNotNull(output.toString());

        // Empty
        output   = new PollingLocationResponse.Output(null, null, null);
        expected = "\n{\n}";
        try (StringWriter writerx = new StringWriter();) {
            try (JsonGenerator generator = PRETTY_PRINTING_GENERATOR_FACTORY.createGenerator(writerx);) {
                PollingLocationResponse.Output.Writer.generate(generator, output);
            }
            actual = writerx.toString();
        }
        assertNotNull(actual);
        assertEquals(actual, expected);
    }

    @Test
    public void testResponseMetadataJsonEmpty() throws IOException {
        PollingLocationResponse metadata = new PollingLocationResponse();
        assertEquals(PollingLocationResponse.Writer.generate(metadata), "\n" + "{\n" + "}");
    }

    @Test
    public void testResponseMetadataJsonFull() throws IOException {
        PollingLocationResponse metadata = new PollingLocationResponse();
        metadata.setRequest("request");
        metadata.setRequiresAccessToken(Boolean.FALSE);
        Instant now = Instant.now();
        metadata.setTransactionTime(now.toString());
        assertEquals(
                PollingLocationResponse.Writer.generate(metadata)
                        .replaceFirst(now.getValue().format(Instant.PARSER_FORMATTER), ""),
                "\n" + "{\n" + "    \"transactionTime\": \"{\\n    \\\"instant\\\": \\\"\\\"\\n}\",\n"
                        + "    \"request\": \"request\",\n" + "    \"requiresAccessToken\": false\n" + "}");
    }

    @Test
    public void testResponseMetadataJsonFullWithOutput() throws IOException {
        Output output = new Output("type1", "url1", "1000");
        assertEquals(output.getType(), "type1");
        output.setType("type2");
        assertEquals(output.getUrl(), "url1");
        output.setUrl("url2");
        assertEquals(output.getCount(), "1000");
        output.setCount("2000");

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("type", "url", "1000"));
        outputs.add(output);

        PollingLocationResponse metadata = new PollingLocationResponse();

        metadata.setRequest("request");
        metadata.setRequiresAccessToken(Boolean.FALSE);
        Instant now = Instant.now();
        metadata.setTransactionTime(now.toString());
        assertNotNull(metadata.getTransactionTime());

        metadata.setOutput(outputs);
        assertFalse(metadata.getOutput().isEmpty());
        assertEquals(
                PollingLocationResponse.Writer.generate(metadata)
                        .replaceFirst(now.getValue().format(Instant.PARSER_FORMATTER), ""),
                "\n" + "{\n" + "    \"transactionTime\": \"{\\n    \\\"instant\\\": \\\"\\\"\\n}\",\n"
                        + "    \"request\": \"request\",\n" + "    \"requiresAccessToken\": false,\n"
                        + "    \"output\": [\n" + "        {\n" + "            \"type\": \"type\",\n"
                        + "            \"url\": \"url\",\n" + "            \"count\": 1000\n" + "        },\n"
                        + "        {\n" + "            \"type\": \"type2\",\n" + "            \"url\": \"url2\",\n"
                        + "            \"count\": 2000\n" + "        }\n" + "    ]\n" + "}");
    }

    @Test
    public void testResponseMetadataJsonFullWithOutputAndError() throws IOException {
        Output output = new Output("type1", "url1", "1000");
        assertEquals(output.getType(), "type1");
        output.setType("type2");
        assertEquals(output.getUrl(), "url1");
        output.setUrl("url2");
        assertEquals(output.getCount(), "1000");
        output.setCount("2000");

        List<Output> outputs = new ArrayList<>();
        outputs.add(new Output("type", "url", "1000"));
        outputs.add(output);

        List<Output> errors = new ArrayList<>();
        errors.add(new Output("type3", "url3", "3000"));
        errors.add(new Output("type4", "url4", "4000"));

        PollingLocationResponse metadata = new PollingLocationResponse();

        metadata.setRequest("request");
        metadata.setRequiresAccessToken(Boolean.FALSE);
        Instant now = Instant.now();
        metadata.setTransactionTime(now.toString());
        assertNotNull(metadata.getTransactionTime());
        metadata.setError(errors);

        metadata.setOutput(outputs);
        assertFalse(metadata.getOutput().isEmpty());
        assertFalse(metadata.getError().isEmpty());
        assertEquals(
                PollingLocationResponse.Writer.generate(metadata)
                        .replaceFirst(now.getValue().format(Instant.PARSER_FORMATTER), ""),
                "\n" + "{\n" + "    \"transactionTime\": \"{\\n    \\\"instant\\\": \\\"\\\"\\n}\",\n"
                        + "    \"request\": \"request\",\n" + "    \"requiresAccessToken\": false,\n"
                        + "    \"output\": [\n" + "        {\n" + "            \"type\": \"type\",\n"
                        + "            \"url\": \"url\",\n" + "            \"count\": 1000\n" + "        },\n"
                        + "        {\n" + "            \"type\": \"type2\",\n" + "            \"url\": \"url2\",\n"
                        + "            \"count\": 2000\n" + "        }\n" + "    ],\n" + "    \"error\": [\n"
                        + "        {\n" + "            \"type\": \"type3\",\n" + "            \"url\": \"url3\",\n"
                        + "            \"count\": 3000\n" + "        },\n" + "        {\n"
                        + "            \"type\": \"type4\",\n" + "            \"url\": \"url4\",\n"
                        + "            \"count\": 4000\n" + "        }\n" + "    ]\n" + "}");
    }
}