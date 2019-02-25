/**
 * detectable
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
package com.synopsys.integration.detectable.detectable.inspector.nuget.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.synopsys.integration.detectable.detectable.executable.Executable;
import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectable.inspector.nuget.NugetInspector;
import com.synopsys.integration.detectable.detectable.inspector.nuget.NugetInspectorOptions;

public class DotNetCoreNugetInspector implements NugetInspector {

    private String dotnetExe;
    private String inspectorDll;
    private ExecutableRunner executableRunner;

    public DotNetCoreNugetInspector(String dotnetExe, String inspectorDll, ExecutableRunner executableRunner) {
        this.dotnetExe = dotnetExe;
        this.inspectorDll = inspectorDll;
        this.executableRunner = executableRunner;
    }

    @Override
    public ExecutableOutput execute(File workingDirectory, NugetInspectorOptions nugetInspectorOptions) throws ExecutableRunnerException, IOException {
        List<String> dotnetArguments = new ArrayList<String>();
        dotnetArguments.add(inspectorDll);
        dotnetArguments.addAll(NugetInspectorArguments.fromInspectorOptions(nugetInspectorOptions));

        final Executable hubNugetInspectorExecutable = new Executable(workingDirectory, new HashMap<>(), dotnetExe, dotnetArguments);
        final ExecutableOutput executableOutput = executableRunner.execute(hubNugetInspectorExecutable);
        return executableOutput;
    }
}