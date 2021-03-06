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
package com.synopsys.integration.detect.lifecycle.run;

import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.DetectInfo;
import com.synopsys.integration.detect.DetectTool;
import com.synopsys.integration.detect.configuration.ConnectionManager;
import com.synopsys.integration.detect.configuration.DetectConfiguration;
import com.synopsys.integration.detect.configuration.DetectConfigurationFactory;
import com.synopsys.integration.detect.configuration.DetectProperty;
import com.synopsys.integration.detect.configuration.PropertyAuthority;
import com.synopsys.integration.detect.detector.DetectorEnvironment;
import com.synopsys.integration.detect.detector.DetectorFactory;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.exitcode.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.DetectContext;
import com.synopsys.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.synopsys.integration.detect.lifecycle.run.data.ProductRunData;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.synopsys.integration.detect.tool.ToolRunner;
import com.synopsys.integration.detect.tool.binaryscanner.BinaryScanToolResult;
import com.synopsys.integration.detect.tool.binaryscanner.BlackDuckBinaryScannerTool;
import com.synopsys.integration.detect.tool.detector.DetectorTool;
import com.synopsys.integration.detect.tool.detector.DetectorToolResult;
import com.synopsys.integration.detect.tool.polaris.PolarisTool;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerTool;
import com.synopsys.integration.detect.tool.signaturescanner.SignatureScannerToolResult;
import com.synopsys.integration.detect.util.executable.ExecutableRunner;
import com.synopsys.integration.detect.workflow.DetectToolFilter;
import com.synopsys.integration.detect.workflow.bdio.BdioManager;
import com.synopsys.integration.detect.workflow.bdio.BdioResult;
import com.synopsys.integration.detect.workflow.codelocation.BdioCodeLocationCreator;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.detect.workflow.hub.BlackduckPostActions;
import com.synopsys.integration.detect.workflow.hub.BlackduckReportOptions;
import com.synopsys.integration.detect.workflow.hub.CodeLocationWaitData;
import com.synopsys.integration.detect.workflow.hub.DetectBdioUploadService;
import com.synopsys.integration.detect.workflow.hub.DetectCodeLocationUnmapService;
import com.synopsys.integration.detect.workflow.hub.DetectProjectMappingService;
import com.synopsys.integration.detect.workflow.hub.DetectProjectService;
import com.synopsys.integration.detect.workflow.hub.DetectProjectServiceOptions;
import com.synopsys.integration.detect.workflow.hub.PolicyCheckOptions;
import com.synopsys.integration.detect.workflow.project.ProjectNameVersionDecider;
import com.synopsys.integration.detect.workflow.project.ProjectNameVersionOptions;
import com.synopsys.integration.detect.workflow.report.util.ReportConstants;
import com.synopsys.integration.detect.workflow.search.SearchOptions;
import com.synopsys.integration.bdio.SimpleBdioFactory;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.codelocation.CodeLocationCreationData;
import com.synopsys.integration.blackduck.codelocation.Result;
import com.synopsys.integration.blackduck.codelocation.bdioupload.UploadBatchOutput;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.util.IntegrationEscapeUtil;
import com.synopsys.integration.util.NameVersion;

public class RunManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetectContext detectContext;

    public RunManager(final DetectContext detectContext) {
        this.detectContext = detectContext;
    }

    public RunResult run(ProductRunData productRunData) throws DetectUserFriendlyException, InterruptedException, IntegrationException {
        //TODO: Better way for run manager to get dependencies so he can be tested. (And better ways of creating his objects)
        final DetectConfiguration detectConfiguration = detectContext.getBean(DetectConfiguration.class);
        final DetectConfigurationFactory detectConfigurationFactory = detectContext.getBean(DetectConfigurationFactory.class);
        final DirectoryManager directoryManager = detectContext.getBean(DirectoryManager.class);
        final EventSystem eventSystem = detectContext.getBean(EventSystem.class);
        final CodeLocationNameManager codeLocationNameManager = detectContext.getBean(CodeLocationNameManager.class);
        final BdioCodeLocationCreator bdioCodeLocationCreator = detectContext.getBean(BdioCodeLocationCreator.class);
        final ConnectionManager connectionManager = detectContext.getBean(ConnectionManager.class);
        final DetectInfo detectInfo = detectContext.getBean(DetectInfo.class);

        final RunResult runResult = new RunResult();
        final RunOptions runOptions = detectConfigurationFactory.createRunOptions();

        final DetectToolFilter detectToolFilter = runOptions.getDetectToolFilter();

        if (productRunData.shouldUseBlackDuckProduct()) {
            logger.info("Black Duck tools will run.");

            final BlackDuckRunData blackDuckRunData = productRunData.getBlackDuckRunData();

            if (blackDuckRunData.getPhoneHomeManager().isPresent()) {
                blackDuckRunData.getPhoneHomeManager().get().startPhoneHome();
            }

            DetectorEnvironment detectorEnvironment = new DetectorEnvironment(directoryManager.getSourceDirectory(), Collections.emptySet(), 0, null, false);
            DetectorFactory detectorFactory = detectContext.getBean(DetectorFactory.class);
            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.DOCKER)) {
                logger.info("Will include the docker tool.");
                ToolRunner toolRunner = new ToolRunner(eventSystem, detectorFactory.createDockerDetector(detectorEnvironment));
                toolRunner.run(runResult);
                logger.info("Docker actions finished.");
            } else {
                logger.info("Docker tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.BAZEL)) {
                logger.info("Will include the bazel tool.");
                ToolRunner toolRunner = new ToolRunner(eventSystem, detectorFactory.createBazelDetector(detectorEnvironment));
                toolRunner.run(runResult);
                logger.info("Bazel actions finished.");
            } else {
                logger.info("Bazel tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.DETECTOR)) {
                logger.info("Will include the detector tool.");
                final String projectBomTool = detectConfiguration.getProperty(DetectProperty.DETECT_PROJECT_DETECTOR, PropertyAuthority.None);
                final SearchOptions searchOptions = detectConfigurationFactory.createSearchOptions(directoryManager.getSourceDirectory());
                final DetectorTool detectorTool = new DetectorTool(detectContext);

                final DetectorToolResult detectorToolResult = detectorTool.performDetectors(searchOptions, projectBomTool);
                runResult.addToolNameVersionIfPresent(DetectTool.DETECTOR, detectorToolResult.bomToolProjectNameVersion);
                runResult.addDetectCodeLocations(detectorToolResult.bomToolCodeLocations);
                runResult.addApplicableDetectors(detectorToolResult.applicableDetectorTypes);

                if (detectorToolResult.failedDetectorTypes.size() > 0) {
                    eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_DETECTOR, "A detector failed."));
                }
                logger.info("Detector actions finished.");
            } else {
                logger.info("Detector tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            logger.info("Completed code location tools.");

            logger.info("Determining project info.");

            final ProjectNameVersionOptions projectNameVersionOptions = detectConfigurationFactory.createProjectNameVersionOptions(directoryManager.getSourceDirectory().getName());
            final ProjectNameVersionDecider projectNameVersionDecider = new ProjectNameVersionDecider(projectNameVersionOptions);
            final NameVersion projectNameVersion = projectNameVersionDecider.decideProjectNameVersion(runOptions.getPreferredTools(), runResult.getDetectToolProjectInfo());

            logger.info("Project name: " + projectNameVersion.getName());
            logger.info("Project version: " + projectNameVersion.getVersion());

            Optional<ProjectVersionWrapper> projectVersionWrapper = Optional.empty();

            if (blackDuckRunData.isOnline() && blackDuckRunData.getBlackDuckServicesFactory().isPresent()) {
                final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory().get();
                logger.info("Getting or creating project.");
                final DetectProjectServiceOptions options = detectConfigurationFactory.createDetectProjectServiceOptions();
                final DetectProjectMappingService detectProjectMappingService = new DetectProjectMappingService(blackDuckServicesFactory.createBlackDuckService());
                final DetectProjectService detectProjectService = new DetectProjectService(blackDuckServicesFactory, options, detectProjectMappingService);
                projectVersionWrapper = Optional.of(detectProjectService.createOrUpdateHubProject(projectNameVersion, options.getApplicationId()));

                if (projectVersionWrapper.isPresent() && runOptions.shouldUnmapCodeLocations()) {
                    logger.info("Unmapping code locations.");
                    final DetectCodeLocationUnmapService detectCodeLocationUnmapService = new DetectCodeLocationUnmapService(blackDuckServicesFactory.createBlackDuckService(), blackDuckServicesFactory.createCodeLocationService());
                    detectCodeLocationUnmapService.unmapCodeLocations(projectVersionWrapper.get().getProjectVersionView());
                } else {
                    logger.debug("Will not unmap code locations: Project view was not present, or should not unmap code locations.");
                }
            } else {
                logger.debug("Detect is not online, and will not create the project.");
            }

            logger.info("Completed project and version actions.");

            logger.info("Processing Detect Code Locations.");
            final CodeLocationWaitData codeLocationWaitData = new CodeLocationWaitData();
            final BdioManager bdioManager = new BdioManager(detectInfo, new SimpleBdioFactory(), new IntegrationEscapeUtil(), codeLocationNameManager, detectConfiguration, bdioCodeLocationCreator, directoryManager, eventSystem);
            final BdioResult bdioResult = bdioManager.createBdioFiles(runOptions.getAggregateName(), projectNameVersion, runResult.getDetectCodeLocations());

            if (bdioResult.getUploadTargets().size() > 0) {
                logger.info("Created " + bdioResult.getUploadTargets().size() + " BDIO files.");
                bdioResult.getUploadTargets().forEach(it -> eventSystem.publishEvent(Event.OutputFileOfInterest, it.getUploadFile()));
                if (blackDuckRunData.isOnline() && blackDuckRunData.getBlackDuckServicesFactory().isPresent()) {
                    logger.info("Uploading BDIO files.");
                    final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory().get();
                    final DetectBdioUploadService detectBdioUploadService = new DetectBdioUploadService(detectConfiguration, blackDuckServicesFactory.createBdioUploadService(), eventSystem);
                    final CodeLocationCreationData<UploadBatchOutput> uploadBatchOutputCodeLocationCreationData = detectBdioUploadService.uploadBdioFiles(bdioResult.getUploadTargets());
                    codeLocationWaitData.setFromBdioCodeLocationCreationData(uploadBatchOutputCodeLocationCreationData);
                }
            } else {
                logger.debug("Did not create any BDIO files.");
            }

            logger.info("Completed Detect Code Location processing.");

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.SIGNATURE_SCAN)) {
                logger.info("Will include the signature scanner tool.");
                final BlackDuckSignatureScannerOptions blackDuckSignatureScannerOptions = detectConfigurationFactory.createBlackDuckSignatureScannerOptions();
                final BlackDuckSignatureScannerTool blackDuckSignatureScannerTool = new BlackDuckSignatureScannerTool(blackDuckSignatureScannerOptions, detectContext);
                final SignatureScannerToolResult signatureScannerToolResult = blackDuckSignatureScannerTool.runScanTool(blackDuckRunData, projectNameVersion, runResult.getDockerTar());
                if (signatureScannerToolResult.getResult() == Result.SUCCESS && signatureScannerToolResult.getCreationData().isPresent()) {
                    codeLocationWaitData.setFromSignatureScannerCodeLocationCreationData(signatureScannerToolResult.getCreationData().get());
                }
                logger.info("Signature scanner actions finished.");
            } else {
                logger.info("Signature scan tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.BINARY_SCAN)) {
                logger.info("Will include the binary scanner tool.");
                if (blackDuckRunData.isOnline() && blackDuckRunData.getBlackDuckServicesFactory().isPresent()) {
                    final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory().get();
                    final BlackDuckBinaryScannerTool blackDuckBinaryScanner = new BlackDuckBinaryScannerTool(eventSystem, codeLocationNameManager, detectConfiguration, blackDuckServicesFactory);
                    BinaryScanToolResult result = blackDuckBinaryScanner.performBinaryScanActions(projectNameVersion);
                    if (result.isSuccessful()) {
                        codeLocationWaitData.setFromBinaryScan(result.getNotificationTaskRange(), result.getCodeLocationNames());
                    }
                }
                logger.info("Binary scanner actions finished.");
            } else {
                logger.info("Binary scan tool will not be run.");
            }

            logger.info(ReportConstants.RUN_SEPARATOR);
            if (projectVersionWrapper.isPresent() && blackDuckRunData.isOnline() && blackDuckRunData.getBlackDuckServicesFactory().isPresent()) {
                final BlackDuckServicesFactory blackDuckServicesFactory = blackDuckRunData.getBlackDuckServicesFactory().get();

                logger.info("Will perform Black Duck post actions.");
                final BlackduckReportOptions blackduckReportOptions = detectConfigurationFactory.createReportOptions();
                final PolicyCheckOptions policyCheckOptions = detectConfigurationFactory.createPolicyCheckOptions();
                final long timeoutInSeconds = detectConfigurationFactory.getTimeoutInSeconds();

                final BlackduckPostActions blackduckPostActions = new BlackduckPostActions(blackDuckServicesFactory, eventSystem);
                blackduckPostActions.perform(blackduckReportOptions, policyCheckOptions, codeLocationWaitData, projectVersionWrapper.get(), timeoutInSeconds);

                final boolean hasAtLeastOneBdio = !bdioResult.getUploadTargets().isEmpty();
                final boolean shouldHaveScanned = detectToolFilter.shouldInclude(DetectTool.SIGNATURE_SCAN);

                if (hasAtLeastOneBdio || shouldHaveScanned) {
                    final Optional<String> componentsLink = projectVersionWrapper.get().getProjectVersionView().getFirstLink(ProjectVersionView.COMPONENTS_LINK);
                    if (componentsLink.isPresent()) {
                        logger.info(String.format("To see your results, follow the URL: %s", componentsLink.get()));
                    }
                }

                logger.info("Black Duck actions have finished.");
            } else {
                logger.debug("Will not perform post actions: Detect is not online.");
            }
        } else {
            logger.info("Black Duck tools will NOT be run.");
        }

        if (productRunData.shouldUsePolarisProduct()) {
            logger.info(ReportConstants.RUN_SEPARATOR);
            if (detectToolFilter.shouldInclude(DetectTool.POLARIS)) {
                logger.info("Will include the Polaris tool.");
                PolarisServerConfig polarisServerConfig = productRunData.getPolarisRunData().getPolarisServerConfig();
                final PolarisTool polarisTool = new PolarisTool(eventSystem, directoryManager, new ExecutableRunner(), connectionManager, detectConfiguration, polarisServerConfig);
                polarisTool.runPolaris(new Slf4jIntLogger(logger), directoryManager.getSourceDirectory());
                logger.info("Polaris actions finished.");
            } else {
                logger.info("Polaris CLI tool will not be run.");
            }
        } else {
            logger.info("Polaris tools will NOT be run.");
        }

        logger.info("All tools have finished.");
        logger.info(ReportConstants.RUN_SEPARATOR);

        return runResult;
    }

}
