package com.example.scenar.ai;

public interface AiGateway {

    void generate(
            String prompt,
            GatewayCallback callback
    );

    interface GatewayCallback {
        void onSuccess(String jsonResponse);

        void onFailure(String errorMessage);
    }
}