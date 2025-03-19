package com.fireflinkCloud.deviceManager.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/android")
public class AndroidDeviceController {

    private final DockerClient dockerClient;

    public AndroidDeviceController() {
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

    @PostMapping("/start-device")
    public Map<String, String> startAndroidDevice(@RequestBody Map<String, String> requestParams) {
        Map<String, String> response = new HashMap<>();

        try {
        String imageName = requestParams.getOrDefault("imageName", "darshan419/pixel-6:latest");
        String containerName = requestParams.getOrDefault("containerName", "android-device");
        int mjpegPort=Integer.parseInt(requestParams.get("MJPEG_PORT"));
        // int number = Integer.parseInt(mjpegPort);

        // Pull the image if not available
        dockerClient.pullImageCmd(imageName).start().awaitCompletion();

        // Convert request parameters to environment variables
        List<String> envVars = requestParams.entrySet().stream()
                .map(entry -> entry.getKey().toUpperCase() + "=" + entry.getValue())
                .collect(Collectors.toList());


        Ports portBindings = new Ports();
        // portBindings.bind(ExposedPort.tcp(4723), Ports.Binding.bindPort(4723));
        // portBindings.bind(ExposedPort.tcp(5555), Ports.Binding.bindPort(5555));
        portBindings.bind(ExposedPort.tcp(5900), Ports.Binding.bindPort(mjpegPort));

        // Create the container
        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(containerName)
                .withHostConfig(new HostConfig()
                        // .withPortBindings(portBindings)
                        .withPrivileged(true)  // Equivalent to --privileged
                        .withDevices(new Device("rwm", "/dev/kvm", "/dev/kvm"))  // --device /dev/kvm
                        .withGroupAdd(Arrays.asList("kvm"))  // Equivalent to --group-add kvm
                        .withNetworkMode("host")
                )
                .withEnv(envVars)  // Set environment variables
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



    @GetMapping("/containers")
    public Map<String, Object> listRunningContainers() {
        Map<String, Object> response = new HashMap<>();

        try {
            response.put("containers", dockerClient.listContainersCmd().withShowAll(true).exec());
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return response;
    }
    @PostMapping("/stop-device")
    public Map<String, String> stopAndroidDevice(@RequestBody Map<String, String> requestParams) {
        Map<String, String> response = new HashMap<>();
        String containerId = requestParams.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            response.put("error", "Container ID is required");
            return response;
        }

        try {
            dockerClient.stopContainerCmd(containerId).exec();
            response.put("status", "Container stopped successfully");
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return response;
    }
    @DeleteMapping("/remove-device")
    public Map<String, String> removeAndroidDevice(@RequestBody Map<String, String> requestParams) {
        Map<String, String> response = new HashMap<>();
        String containerId = requestParams.get("containerId");

        if (containerId == null || containerId.isEmpty()) {
            response.put("error", "Container ID is required");
            return response;
        }

        try {
            dockerClient.removeContainerCmd(containerId).exec();
            response.put("status", "Container removed successfully");
        } catch (Exception e) {
            response.put("error", e.getMessage());
        }

        return response;
    }

}
