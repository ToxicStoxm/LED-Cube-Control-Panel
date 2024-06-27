package com.x_tornado10.lccp.util.logging.network;

import com.x_tornado10.lccp.LCCP;

import java.text.SimpleDateFormat;
import java.util.*;

public class NetworkLogger {

    private final HashMap<UUID, String> networkEvents;
    private final TreeMap<Integer, UUID> order;

    public NetworkLogger() {
        networkEvents = new HashMap<>();
        order = new TreeMap<>();
    }

    public UUID addEvent(UUID id, String description) {
        if (networkEvents.putIfAbsent(id, description) == null) order.put(order.size(), id);
        return id;
    }

    public void printEvents() {
        alignNetworkEvents();
        boolean empty = networkEvents.isEmpty() && order.isEmpty();
        LCCP.logger.debug("-------------------- Network Events ---------------------------------------------------------------------------------------------------------");
        LCCP.logger.debug(empty ? "Couldn't find any network events!" : "Network event count: " + networkEvents.size());
        for (Map.Entry<Integer, UUID> entry : order.entrySet()) {
            LCCP.logger.debug(networkEvents.get(entry.getValue()) + " " + entry.getValue());
        }
        LCCP.logger.debug("----------------------------------------------------------------------------------------------------------------------------------------------");
    }

    public UUID getRandomUUID(String description) {
        return addEvent(UUID.randomUUID(), attachTime(description));
    }
    private String attachTime(String message) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        return "[" + df.format(new Date()) + "] " + message + "[ID]";
    }

    public void alignNetworkEvents() {
        // Extract the strings from the networkEvents map
        List<String> strings = new ArrayList<>(networkEvents.values());
        List<String> alignedStrings = alignStrings(strings);

        // Update the networkEvents map with the aligned strings
        int index = 0;
        for (UUID key : networkEvents.keySet()) {
            networkEvents.put(key, alignedStrings.get(index++));
        }
    }

    private List<String> alignStrings(List<String> strings) {
        // Split each string by the square brackets and determine the maximum length of each column
        List<String[]> splitStrings = new ArrayList<>();
        int[] maxLengths = new int[0];

        for (String s : strings) {
            // Adjusted regex to split directly after the closing bracket and before the opening bracket
            String[] parts = s.split("(?<=])(?=\\[)");
            splitStrings.add(parts);
            if (parts.length > maxLengths.length) {
                maxLengths = new int[parts.length];
            }
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() > maxLengths[i]) {
                    maxLengths[i] = parts[i].length();
                }
            }
        }

        // Construct the aligned strings
        List<String> alignedStrings = new ArrayList<>();
        for (String[] parts : splitStrings) {
            StringBuilder alignedString = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                alignedString.append(String.format("%-" + maxLengths[i] + "s", parts[i]));
                if (i < parts.length - 1) {
                    alignedString.append(" ");
                }
            }
            alignedStrings.add(alignedString.toString());
        }

        return alignedStrings;
    }

}
