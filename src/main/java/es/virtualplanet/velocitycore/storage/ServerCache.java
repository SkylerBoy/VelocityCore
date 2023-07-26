package es.virtualplanet.velocitycore.storage;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ServerCache {

    private final Map<String, Boolean> availableServers = new HashMap<>();
}
