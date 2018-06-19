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
package com.blackducksoftware.integration.hub.detect.manager.result.codelocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.blackducksoftware.integration.hub.detect.model.BdioCodeLocation;
import com.blackducksoftware.integration.hub.detect.model.BomToolType;
import com.blackducksoftware.integration.hub.detect.model.DetectCodeLocation;

public class DetectCodeLocationResult {
    List<BdioCodeLocation> bdioCodeLocations;
    Map<DetectCodeLocation, String> codeLocationNames;
    Set<BomToolType> failedBomTools;

    public DetectCodeLocationResult(final List<BdioCodeLocation> bdioCodeLocations, final Set<BomToolType> failedBomTools, final Map<DetectCodeLocation, String> codeLocationNames) {
        this.bdioCodeLocations = bdioCodeLocations;
        this.failedBomTools = failedBomTools;
        this.codeLocationNames = codeLocationNames;
    }

    public Map<DetectCodeLocation, String> getCodeLocationNames() {
        return codeLocationNames;
    }

    public List<BdioCodeLocation> getBdioCodeLocations() {
        return bdioCodeLocations;
    }

    public Set<BomToolType> getFailedBomToolTypes() {
        return failedBomTools;
    }
}