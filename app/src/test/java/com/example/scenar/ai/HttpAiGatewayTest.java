package com.example.scenar.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class HttpAiGatewayTest {
    @Test
    public void acceptsHttpsAndLocalDebugEndpoints() {
        assertNotNull(new HttpAiGateway("https://example.com/api/incident"));
        assertNotNull(new HttpAiGateway("http://10.0.2.2:3000/api/incident"));
        assertNotNull(new HttpAiGateway("http://127.0.0.1:3000/api/incident"));
    }

    @Test
    public void rejectsInsecureRemoteEndpoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new HttpAiGateway("http://example.com/api/incident"));
    }

    @Test
    public void rejectsBlankPromptBeforeStartingNetworkWork() {
        AtomicReference<String> error = new AtomicReference<>();
        HttpAiGateway gateway = new HttpAiGateway(
                "https://example.com/api/incident",
                Runnable::run
        );

        gateway.generate("  ", new AiGateway.GatewayCallback() {
            @Override
            public void onSuccess(String jsonResponse) {
                fail("Expected a failure callback");
            }

            @Override
            public void onFailure(String message) {
                error.set(message);
            }
        });

        assertEquals("The incident description is empty.", error.get());
    }
}
