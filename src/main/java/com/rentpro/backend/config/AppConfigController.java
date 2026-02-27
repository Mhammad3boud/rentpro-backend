package com.rentpro.backend.config;

import com.rentpro.backend.config.dto.AppVersionConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/app-config")
public class AppConfigController {

    @Value("${app.version.android.latest:1.0.0}")
    private String androidLatestVersion;
    @Value("${app.version.android.minSupported:1.0.0}")
    private String androidMinSupportedVersion;
    @Value("${app.version.android.updateUrl:https://play.google.com/store/apps/details?id=com.rentpro.app}")
    private String androidUpdateUrl;

    @Value("${app.version.ios.latest:1.0.0}")
    private String iosLatestVersion;
    @Value("${app.version.ios.minSupported:1.0.0}")
    private String iosMinSupportedVersion;
    @Value("${app.version.ios.updateUrl:https://apps.apple.com}")
    private String iosUpdateUrl;

    @Value("${app.version.web.latest:1.0.0}")
    private String webLatestVersion;
    @Value("${app.version.web.minSupported:1.0.0}")
    private String webMinSupportedVersion;
    @Value("${app.version.web.updateUrl:https://your-domain.example.com/download}")
    private String webUpdateUrl;

    @Value("${app.version.message:A new version is required to continue.}")
    private String versionMessage;

    @GetMapping("/version")
    public AppVersionConfigResponse getVersionConfig(@RequestParam(defaultValue = "web") String platform) {
        String normalized = platform.toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "android" -> new AppVersionConfigResponse(
                    "android",
                    androidLatestVersion,
                    androidMinSupportedVersion,
                    androidUpdateUrl,
                    versionMessage
            );
            case "ios" -> new AppVersionConfigResponse(
                    "ios",
                    iosLatestVersion,
                    iosMinSupportedVersion,
                    iosUpdateUrl,
                    versionMessage
            );
            default -> new AppVersionConfigResponse(
                    "web",
                    webLatestVersion,
                    webMinSupportedVersion,
                    webUpdateUrl,
                    versionMessage
            );
        };
    }
}
