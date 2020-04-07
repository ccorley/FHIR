#!/usr/bin/env bash
###############################################################################
# (C) Copyright IBM Corp. 2016, 2020
#
# SPDX-License-Identifier: Apache-2.0
###############################################################################

echo "Performing integration test post-processing..."

# The full path to the directory of this script, no matter where its called from
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
WORKSPACE="$( dirname "${DIR}" )"

export SIT=${WORKSPACE}/SIT
if [ ! -d ${SIT} ]; then
    echo "ERROR: ${SIT} not found!"
    exit 2
fi

# Stop the fhir server.
echo "Stopping the fhir server..."
${SIT}/wlp/bin/server stop fhir-server


# Gather up all the log files and test results
it_results=${SIT}/integration-test-results
rm -fr ${it_results} 2>/dev/null
mkdir -p ${it_results}/server-logs
mkdir -p ${it_results}/fhir-server-test

echo "Gathering post-test server logs..."
cp -pr ${SIT}/wlp/usr/servers/fhir-server/logs ${it_results}/server-logs

echo "Gathering integration test output"
cp -pr ${WORKSPACE}/fhir-server-test/target/surefire-reports/* ${it_results}/fhir-server-test

echo "Integration test post-processing completed!"

exit 0
