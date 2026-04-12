package net.jrodolfo.llm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSupportTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorRootRedirectsToHealth() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/actuator/health"));
    }

    @Test
    void actuatorInfoIncludesApplicationAndRuntimeDetails() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("llm-pet-project-backend"))
                .andExpect(jsonPath("$.runtime.provider").exists())
                .andExpect(jsonPath("$.runtime.sessionsDirectory").exists())
                .andExpect(jsonPath("$.runtime.mcp.enabled").exists())
                .andExpect(jsonPath("$.runtime.mcp.command").exists())
                .andExpect(jsonPath("$.runtime.mcp.workingDirectory").exists())
                .andExpect(jsonPath("$.docs.swaggerUi").value("/swagger-ui/index.html"));
    }
}
