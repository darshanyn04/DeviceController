package com.fireflinkCloud.deviceManager.controller;

import com.fireflinkCloud.deviceManager.service.StartAndroid;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/android")
public class AndroidDeviceController {

    @Autowired
    private StartAndroid startAndroid;


    @PostMapping("/start-device")
    public Map<String, String> startAndroidDevice(@RequestBody Map<String, String> requestParams) {

        return startAndroid.startAndroidDevice(requestParams);
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
