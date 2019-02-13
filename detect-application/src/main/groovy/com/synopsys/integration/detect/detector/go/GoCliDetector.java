/**
 * detect-application
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
package com.synopsys.integration.detect.detector.go;

import java.io.File;
import java.util.List;

import com.synopsys.integration.detect.detector.Detector;
import com.synopsys.integration.detect.detector.DetectorEnvironment;
import com.synopsys.integration.detect.detector.DetectorException;
import com.synopsys.integration.detect.detector.DetectorType;
import com.synopsys.integration.detect.detector.ExtractionId;
import com.synopsys.integration.detect.util.executable.CacheableExecutableFinder;
import com.synopsys.integration.detect.util.executable.CacheableExecutableFinder.CacheableExecutableType;
import com.synopsys.integration.detect.workflow.extraction.Extraction;
import com.synopsys.integration.detect.workflow.file.DetectFileFinder;
import com.synopsys.integration.detect.workflow.search.result.DetectorResult;
import com.synopsys.integration.detect.workflow.search.result.ExecutableNotFoundDetectorResult;
import com.synopsys.integration.detect.workflow.search.result.FileNotFoundDetectorResult;
import com.synopsys.integration.detect.workflow.search.result.InspectorNotFoundDetectorResult;
import com.synopsys.integration.detect.workflow.search.result.PassedDetectorResult;

public class GoCliDetector extends Detector {
    public static final String GOFILE_FILENAME_PATTERN = "*.go";

    private final DetectFileFinder fileFinder;
    private final GoInspectorManager goInspectorManager;
    private final CacheableExecutableFinder cacheableExecutableFinder;
    private final GoDepExtractor goDepExtractor;

    private File goExe;
    private String goDepInspector;

    public GoCliDetector(final DetectorEnvironment environment, final DetectFileFinder fileFinder, final CacheableExecutableFinder cacheableExecutableFinder, final GoInspectorManager goInspectorManager,
        final GoDepExtractor goDepExtractor) {
        super(environment, "Go Cli", DetectorType.GO_DEP);
        this.fileFinder = fileFinder;
        this.cacheableExecutableFinder = cacheableExecutableFinder;
        this.goInspectorManager = goInspectorManager;
        this.goDepExtractor = goDepExtractor;
    }

    @Override
    public DetectorResult applicable() {
        final List<File> found = fileFinder.findFiles(environment.getDirectory(), GOFILE_FILENAME_PATTERN);
        if (found == null || found.size() == 0) {
            return new FileNotFoundDetectorResult(GOFILE_FILENAME_PATTERN);
        }

        return new PassedDetectorResult();
    }

    @Override
    public DetectorResult extractable() throws DetectorException {
        goExe = cacheableExecutableFinder.getExecutable(CacheableExecutableType.GO);
        if (goExe == null) {
            return new ExecutableNotFoundDetectorResult("go");
        }

        goDepInspector = goInspectorManager.evaluate();
        if (goDepInspector == null) {
            return new InspectorNotFoundDetectorResult("go");
        }

        return new PassedDetectorResult();
    }

    @Override
    public Extraction extract(final ExtractionId extractionId) {
        return goDepExtractor.extract(environment.getDirectory(), goExe, goDepInspector);
    }
}