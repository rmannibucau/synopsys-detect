/**
 * synopsys-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detect.tool.signaturescanner;

import java.io.File;
import java.util.List;

import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DetectFileFinder;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchBuilder;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.util.NameVersion;

public class OfflineBlackDuckSignatureScanner extends BlackDuckSignatureScanner {
    public OfflineBlackDuckSignatureScanner(final DirectoryManager directoryManager, final DetectFileFinder detectFileFinder,
            final CodeLocationNameManager codeLocationNameManager, final BlackDuckSignatureScannerOptions signatureScannerOptions, final EventSystem eventSystem, final ScanBatchRunner scanBatchRunner) {
        super(directoryManager, detectFileFinder, codeLocationNameManager, signatureScannerOptions, eventSystem, scanBatchRunner);
    }

    @Override
    protected ScanBatch createScanBatch(final NameVersion projectNameVersion, File installDirectory, final List<SignatureScanPath> signatureScanPaths, final File dockerTarFile) {
        final ScanBatchBuilder scanJobBuilder = createDefaultScanBatchBuilder(projectNameVersion, installDirectory, signatureScanPaths, dockerTarFile);
        scanJobBuilder.fromBlackDuckServerConfig(null);//temporarily need to do this. fix when hub common updates;
        return scanJobBuilder.build();
    }

}
