package com.blackducksoftware.integration.hub.detect.bomtool.pip;

import com.blackducksoftware.integration.hub.detect.model.BomToolType;
import com.blackducksoftware.integration.hub.detect.util.DetectFileManager;
import com.blackducksoftware.integration.hub.detect.util.executable.Executable;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunner;
import com.blackducksoftware.integration.hub.detect.util.executable.ExecutableRunnerException;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PipInspectorManager {
    final Logger logger = LoggerFactory.getLogger(PipInspectorManager.class);

    public static final String INSPECTOR_NAME = "pip-inspector.py";

    @Autowired
    private ExecutableRunner executableRunner;

    @Autowired
    private DetectFileManager detectFileManager;

    public File extractInspectorScript() throws IOException {
        InputStream insptectorFileStream = getClass().getResourceAsStream(String.format("/%s" ,INSPECTOR_NAME));
        String inpsectorScriptContents = IOUtils.toString(insptectorFileStream, StandardCharsets.UTF_8);
        File inspectorScript = detectFileManager.createFile(BomToolType.PIP, INSPECTOR_NAME);
        logger.info("PIP INSPECTOR MANAGER");
        logger.info("PIP CREATED INSPECTORY SCRIPT " + inspectorScript);
        logger.info("PIP CREATED INSPECTORY SCRIPT " + inspectorScript.getAbsolutePath());

        File wroteTo = detectFileManager.writeToFile(inspectorScript, inpsectorScriptContents);

        logger.info("PIP WROTE TO INSPECTORY SCRIPT " + wroteTo);
        logger.info("PIP WROTE TO INSPECTORY SCRIPT " + wroteTo.getAbsolutePath());
        return wroteTo;
    }

    public  String runInspector(File sourceDirectory, String pythonPath, File inspectorScript, String projectName, String requirementsFilePath) throws ExecutableRunnerException {
        logger.info("PIP INSPECTOR MANAGER");
        logger.info("PIP INSPECTORY SCRIPT " + inspectorScript);
        logger.info("PIP INSPECTORY SCRIPT " + inspectorScript.getAbsolutePath());
        List<String> inspectorArguments = new ArrayList<>();
        inspectorArguments.add(inspectorScript.getAbsolutePath());

        if (StringUtils.isNotBlank(requirementsFilePath)) {
            File requirementsFile = new File(requirementsFilePath);
            inspectorArguments.add(String.format("--requirements=%s",requirementsFile.getAbsolutePath()));
        }

        if (StringUtils.isNotBlank(projectName)) {
            inspectorArguments.add(String.format("--projectname=%s", projectName));
        }

        Executable pipInspector = new Executable(sourceDirectory, pythonPath, inspectorArguments);
        return executableRunner.execute(pipInspector).getStandardOutput();
    }

}