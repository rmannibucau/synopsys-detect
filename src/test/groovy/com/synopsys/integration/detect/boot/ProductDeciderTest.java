package com.synopsys.integration.detect.boot;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.synopsys.integration.detect.configuration.DetectConfiguration;
import com.synopsys.integration.detect.configuration.DetectProperty;
import com.synopsys.integration.detect.configuration.PropertyAuthority;
import com.synopsys.integration.detect.exception.DetectUserFriendlyException;
import com.synopsys.integration.detect.lifecycle.boot.decision.ProductDecider;
import com.synopsys.integration.detect.lifecycle.boot.decision.ProductDecision;

public class ProductDeciderTest {

    @Test()
    public void shouldRunPolaris() throws DetectUserFriendlyException {
        File userHome = Mockito.mock(File.class);
        DetectConfiguration detectConfiguration = polarisConfiguration("POLARIS_ACCESS_TOKEN", "access token text", "POLARIS_URL", "http://polaris.com");

        ProductDecider productDecider = new ProductDecider();
        ProductDecision productDecision = productDecider.decide(detectConfiguration, userHome);

        Assert.assertTrue(productDecision.getPolarisDecision().shouldRun());
    }

    @Test()
    public void shouldRunBlackDuckOffline() throws DetectUserFriendlyException {
        File userHome = Mockito.mock(File.class);
        DetectConfiguration detectConfiguration = Mockito.mock(DetectConfiguration.class);
        Mockito.when(detectConfiguration.getBooleanProperty(DetectProperty.BLACKDUCK_OFFLINE_MODE, PropertyAuthority.None)).thenReturn(true);

        ProductDecider productDecider = new ProductDecider();
        ProductDecision productDecision = productDecider.decide(detectConfiguration, userHome);

        Assert.assertTrue(productDecision.getBlackDuckDecision().shouldRun());
        Assert.assertTrue(productDecision.getBlackDuckDecision().isOffline());
    }

    @Test()
    public void shouldRunBlackDuckOnline() throws DetectUserFriendlyException {
        File userHome = Mockito.mock(File.class);
        DetectConfiguration detectConfiguration = Mockito.mock(DetectConfiguration.class);
        Mockito.when(detectConfiguration.getProperty(DetectProperty.BLACKDUCK_URL, PropertyAuthority.None)).thenReturn("some-url");

        ProductDecider productDecider = new ProductDecider();
        ProductDecision productDecision = productDecider.decide(detectConfiguration, userHome);

        Assert.assertTrue(productDecision.getBlackDuckDecision().shouldRun());
        Assert.assertFalse(productDecision.getBlackDuckDecision().isOffline());
    }

    @Test()
    public void decidesNone() throws DetectUserFriendlyException {
        File userHome = Mockito.mock(File.class);
        DetectConfiguration detectConfiguration = Mockito.mock(DetectConfiguration.class);

        ProductDecider productDecider = new ProductDecider();
        ProductDecision productDecision = productDecider.decide(detectConfiguration, userHome);

        Assert.assertFalse(productDecision.willRunAny());
    }

    private DetectConfiguration polarisConfiguration(String... polarisKeys) {
        Map<String, String> keyMap = new HashMap<>();
        for (int i = 0; i < polarisKeys.length; i += 2){
            keyMap.put(polarisKeys[i], polarisKeys[i + 1]);
        }
        DetectConfiguration detectConfiguration = Mockito.mock(DetectConfiguration.class);
        Mockito.when(detectConfiguration.getProperties(Mockito.any())).thenReturn(keyMap);

        return detectConfiguration;
    }
}
