package com.blackducksoftware.integration.hub.packman

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.bom.BomImportRequestService
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder
import com.blackducksoftware.integration.hub.buildtool.BuildToolConstants
import com.blackducksoftware.integration.hub.global.HubServerConfig
import com.blackducksoftware.integration.hub.rest.RestConnection
import com.blackducksoftware.integration.hub.service.HubServicesFactory
import com.blackducksoftware.integration.log.Slf4jIntLogger

@Component
class BdioUploader {
    private final Logger logger = LoggerFactory.getLogger(BdioUploader.class)

    @Autowired
    PackmanProperties packmanProperties

    void uploadBdioFiles(List<File> createdBdioFiles) {
        if (!createdBdioFiles) {
            return
        }

        try {
            Slf4jIntLogger slf4jIntLogger = new Slf4jIntLogger(logger)
            HubServerConfig hubServerConfig = createBuilder(slf4jIntLogger).build()
            RestConnection restConnection = hubServerConfig.createCredentialsRestConnection(slf4jIntLogger)

            HubServicesFactory hubServicesFactory = new HubServicesFactory(restConnection);

            BomImportRequestService bomImportRequestService = hubServicesFactory.createBomImportRequestService()

            createdBdioFiles.each { file ->
                logger.info("uploading ${file.name} to ${packmanProperties.getHubUrl()}")
                bomImportRequestService.importBomFile(file, BuildToolConstants.BDIO_FILE_MEDIA_TYPE)
                if (Boolean.valueOf(packmanProperties.getCleanupBdioFiles())) {
                    file.delete()
                }
            }
        } catch (Exception e) {
            logger.error("Your Hub configuration is not valid: ${e.message}")
        }
    }

    private HubServerConfigBuilder createBuilder(Slf4jIntLogger slf4jIntLogger) {
        HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder()
        hubServerConfigBuilder.setHubUrl(packmanProperties.getHubUrl())
        hubServerConfigBuilder.setTimeout(packmanProperties.getHubTimeout())
        hubServerConfigBuilder.setUsername(packmanProperties.getHubUsername())
        hubServerConfigBuilder.setPassword(packmanProperties.getHubPassword())

        hubServerConfigBuilder.setProxyHost(packmanProperties.getHubProxyHost())
        hubServerConfigBuilder.setProxyPort(packmanProperties.getHubProxyPort())
        hubServerConfigBuilder.setProxyUsername(packmanProperties.getHubProxyUsername())
        hubServerConfigBuilder.setProxyPassword(packmanProperties.getHubProxyPassword())

        hubServerConfigBuilder.setAutoImportHttpsCertificates(packmanProperties.getHubAutoImportCertificate())
        hubServerConfigBuilder.setLogger(slf4jIntLogger)

        hubServerConfigBuilder
    }
}
