package com.example.scenar.ai;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpAiGateway implements AiGateway {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_RESPONSE_CHARACTERS = 256_000;

    private static final ExecutorService NETWORK_EXECUTOR =
            Executors.newFixedThreadPool(2, runnable -> {
                Thread thread = new Thread(runnable, "scenar-ai-network");
                thread.setDaemon(true);
                return thread;
            });

    private final String endpointUrl;
    private final Gson gson;
    private final Executor executor;

    public HttpAiGateway(String endpointUrl) {
        this(endpointUrl, NETWORK_EXECUTOR);
    }

    HttpAiGateway(String endpointUrl, Executor executor) {
        this.endpointUrl = validateEndpoint(endpointUrl);
        this.executor = executor;
        this.gson = new Gson();
    }

    @Override
    public void generate(
            String prompt,
            GatewayCallback callback
    ) {
        if (callback == null) {
            return;
        }

        if (prompt == null || prompt.trim().isEmpty()) {
            callback.onFailure(
                    "The incident description is empty."
            );
            return;
        }

        executor.execute(
                () -> sendRequest(prompt, callback)
        );
    }

    private void sendRequest(
            String prompt,
            GatewayCallback callback
    ) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(endpointUrl);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(
                    CONNECT_TIMEOUT_MS
            );
            connection.setReadTimeout(
                    READ_TIMEOUT_MS
            );
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
            );
            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            PromptRequest request =
                    new PromptRequest(prompt);

            String requestJson =
                    gson.toJson(request);

            byte[] requestBytes =
                    requestJson.getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(requestBytes.length);

            try (OutputStream outputStream =
                         connection.getOutputStream()) {
                outputStream.write(requestBytes);
                outputStream.flush();
            }

            int responseCode =
                    connection.getResponseCode();

            if (responseCode < 200
                    || responseCode >= 300) {

                callback.onFailure(
                        createHttpErrorMessage(
                                responseCode
                        )
                );
                return;
            }

            String responseBody = readResponse(
                    connection.getInputStream()
            );

            if (responseBody.trim().isEmpty()) {
                callback.onFailure(
                        "The AI service returned an empty response."
                );
                return;
            }

            /*
             * The backend must return the IncidentReport
             * JSON directly, without Markdown or another
             * response wrapper.
             */
            callback.onSuccess(responseBody);

        } catch (Exception exception) {
            callback.onFailure(
                    "The AI service is unavailable. "
                            + "The offline fallback will be used."
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponse(
            InputStream inputStream
    ) throws Exception {

        StringBuilder response =
                new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     inputStream,
                                     StandardCharsets.UTF_8
                             )
                     )) {

            String line;

            while ((line = reader.readLine()) != null) {
                if (response.length() + line.length()
                        > MAX_RESPONSE_CHARACTERS) {
                    throw new IllegalStateException(
                            "AI response exceeded the allowed size."
                    );
                }
                response.append(line);
            }
        }

        return response.toString();
    }

    private static String validateEndpoint(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "An AI endpoint is required."
            );
        }

        try {
            URI uri = URI.create(endpointUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            boolean secure = "https".equalsIgnoreCase(scheme);
            boolean localDebug = "http".equalsIgnoreCase(scheme)
                    && ("10.0.2.2".equals(host)
                    || "127.0.0.1".equals(host)
                    || "localhost".equalsIgnoreCase(host));

            if ((!secure && !localDebug) || host == null) {
                throw new IllegalArgumentException(
                        "Use HTTPS, except for a local debug endpoint."
                );
            }

            return uri.toString();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "A valid AI endpoint is required.",
                    exception
            );
        }
    }

    private String createHttpErrorMessage(
            int responseCode
    ) {
        if (responseCode == 401
                || responseCode == 403) {
            return "The AI service could not authenticate.";
        }

        if (responseCode == 429) {
            return "The AI service is busy. "
                    + "Please try again.";
        }

        if (responseCode >= 500) {
            return "The AI service is temporarily unavailable.";
        }

        return "The AI request failed with status "
                + responseCode
                + ".";
    }

    private static final class PromptRequest {

        private final String prompt;

        private PromptRequest(String prompt) {
            this.prompt = prompt;
        }
    }
}
