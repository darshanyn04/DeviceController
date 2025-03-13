package com.fireflinkCloud.deviceManager.service;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Device;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

@Service
public class StartAndroid {

    private final DockerClient dockerClient;

    public StartAndroid() {
        // Docker client configuration
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock") // Correct Unix socket path
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);


        // Initialize the Docker client
//        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    public Map<String, String> startAndroidDevice(Map<String, String> requestParams) {
        Map<String, String> response = new HashMap<>();

        try {
            String imageName = requestParams.getOrDefault("imageName", "darshan419/pixel-6:latest");
            String containerName = requestParams.getOrDefault("containerName", "android-device");

            // Pull the image if not available
            dockerClient.pullImageCmd(imageName).start().awaitCompletion();

            // Define ports
            ExposedPort[] exposedPorts = {
                    ExposedPort.tcp(5900),
                    ExposedPort.tcp(4723),
                    ExposedPort.tcp(5555),
                    ExposedPort.tcp(4444)
            };

            Ports portBindings = new Ports();
            portBindings.bind(ExposedPort.tcp(5900), Ports.Binding.bindPort(5900));
            portBindings.bind(ExposedPort.tcp(4723), Ports.Binding.bindPort(4723));
            portBindings.bind(ExposedPort.tcp(5555), Ports.Binding.bindPort(5555));
            portBindings.bind(ExposedPort.tcp(4444), Ports.Binding.bindPort(4444));

            // Create the container
            CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                    .withName(containerName)
                    .withHostConfig(new HostConfig()
                            .withPrivileged(true) // Equivalent to --privileged
                            .withDevices(new Device("rwm", "/dev/kvm", "/dev/kvm")) // Equivalent to --device /dev/kvm
                            .withPortBindings(portBindings) // Apply correct port mappings
                            .withNetworkMode("host") // Equivalent to --network host
                    )
                    .withExposedPorts(exposedPorts) // Ensure ports are exposed
                    .withEnv(
                            "APPIUM_PORT=4723",
                            "NODE_PORT=5555",
                            "SELENIUM_HUB_URL=" + requestParams.getOrDefault("seleniumHubUrl", "http://103.182.210.85:4444"),
                            "DEVICE_NAME=" + requestParams.getOrDefault("deviceName", "Pixel_6"),
                            "PLATFORM_VERSION=" + requestParams.getOrDefault("platformVersion", "11.0"),
                            "MJPEG_PORT=9100",
                            "WDA_PORT=8200",
                            "HUB_URL=" + requestParams.getOrDefault("hubUrl", "103.182.210.91")
                    )
                    .exec();

            // Start the container
            dockerClient.startContainerCmd(container.getId()).exec();

            response.put("status", "Container started successfully");
            response.put("containerId", container.getId());

        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return response;
    }

}
