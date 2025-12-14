package com.boskicar.bcarserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BcarserverApplication.class)
@AutoConfigureMockMvc
public class BoskicarServerAPITests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testStatus() throws Exception {
        mockMvc.perform(get("/status/false")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testStatusComplete() throws Exception {
        // This is expected to fail if GPIOs are not initialized and code isn't
        // null-safe
        mockMvc.perform(get("/status/true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testLights() throws Exception {
        mockMvc.perform(post("/lights/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
