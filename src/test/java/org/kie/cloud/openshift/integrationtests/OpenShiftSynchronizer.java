package org.kie.cloud.openshift.integrationtests;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftSynchronizer {

    public static void waitUntilAllRoutesAreAvailable(OpenShiftClient openShiftClient, String projectName) {
        waitForCondition(() -> {
            for(Route route : openShiftClient.routes().inNamespace(projectName).list().getItems()) {
                try {
                    URL url = new URL("http://" + route.getSpec().getHost());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);

                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    connection.disconnect();

                    if(responseCode == 503) {
                        return false;
                    } else {
                        return true;
                    }
                } catch (IOException e) {
                    return false;
                }
            }
            return true;
        });
    }

    private static void waitForCondition(BooleanSupplier condition) {
        Duration waitingDuration = Duration.ofSeconds(30);
        Instant timeout = Instant.now().plus(waitingDuration);

        while (Instant.now().isBefore(timeout)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new RuntimeException("Timeout while waiting for condition.");
    }
}
