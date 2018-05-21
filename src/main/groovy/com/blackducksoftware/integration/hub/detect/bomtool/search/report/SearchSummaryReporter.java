/**
 * hub-detect
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.detect.bomtool.search.report;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.blackducksoftware.integration.hub.detect.diagnostic.DiagnosticsManager;
import com.blackducksoftware.integration.hub.detect.extraction.StrategyEvaluation;

@Component
public class SearchSummaryReporter {
    private final Logger logger = LoggerFactory.getLogger(SearchSummaryReporter.class);

    @Autowired
    public DiagnosticsManager diagnosticsManager;

    public void print(final List<StrategyEvaluation> results) {
        final Map<File, List<StrategyEvaluation>> byDirectory = new HashMap<>();
        for (final StrategyEvaluation result : results) {
            if (result.context == null || result.environment.getDirectory() == null) {
                logger.info("WUT");
            }
            final File directory = result.environment.getDirectory();
            if (!byDirectory.containsKey(directory)) {
                byDirectory.put(directory, new ArrayList<>());
            }
            byDirectory.get(directory).add(result);
        }

        printDirectoriesInfo(byDirectory);
        printDirectoriesDebug(byDirectory);

    }

    private void printDirectoriesInfo(final Map<File, List<StrategyEvaluation>> byDirectory) {

        logger.info("");
        logger.info("");
        logger.info(ReportConstants.HEADING);
        logger.info("Search results");
        logger.info(ReportConstants.HEADING);
        for (final File file : byDirectory.keySet()) {
            final List<StrategyEvaluation> results = byDirectory.get(file);

            final List<String> applied = new ArrayList<>();

            for (final StrategyEvaluation result : results) {
                final String strategyName = result.strategy.getBomToolType() + " - " + result.strategy.getName();
                if (result.isApplicable()) {
                    applied.add(strategyName);
                }
            }
            if (applied.size() > 0) {

                logger.info(file.getAbsolutePath());
                logger.info("\tAPPLIES: " + applied.stream().sorted().collect(Collectors.joining(", ")));

            }
        }
        logger.info(ReportConstants.HEADING);
        logger.info("");
        logger.info("");
    }

    private void printDirectoriesDebug(final Map<File, List<StrategyEvaluation>> byDirectory) {
        for (final File file : byDirectory.keySet()) {
            final List<StrategyEvaluation> results = byDirectory.get(file);

            final List<String> toPrint = new ArrayList<>();

            for (final StrategyEvaluation result : results) {
                final String strategyName = result.strategy.getDescriptiveName();
                if (result.isApplicable()) {
                    toPrint.add("      APPLIED: " + strategyName);
                } else {
                    if (result.applicable != null) {
                        toPrint.add("DID NOT APPLY: " + strategyName + " - " + result.applicable.toDescription());
                    } else if (result.searchable != null) {
                        toPrint.add("DID NOT APPLY: " + strategyName + " - "  + result.searchable.toDescription());
                    } else {
                        toPrint.add("DID NOT APPLY: " + strategyName + " - Unknown");
                    }
                }
            }
            if (toPrint.size() > 0) {

                debug(ReportConstants.HEADING);
                debug("Detailed search results for directory");
                debug(file.getAbsolutePath());
                debug(ReportConstants.HEADING);
                toPrint.stream().sorted().forEach(it -> debug(it));
                debug(ReportConstants.HEADING);
            }
        }
    }

    private void debug(final String line) {
        logger.debug(line);
        diagnosticsManager.printToSearchReport(line);
    }

}