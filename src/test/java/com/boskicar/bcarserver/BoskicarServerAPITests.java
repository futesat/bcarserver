package com.boskicar.bcarserver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.*;

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

    // Status Tests
    @Test
    public void testStatusBasic() throws Exception {
        mockMvc.perform(get("/status/false")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastFBOrderSpeed").exists())
                .andExpect(jsonPath("$.lastFBOrderType").exists());
    }

    @Test
    public void testStatusComplete() throws Exception {
        mockMvc.perform(get("/status/true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastFBOrderSpeed").exists())
                .andExpect(jsonPath("$.lastFBOrderType").exists())
                .andExpect(jsonPath("$.lights").exists())
                .andExpect(jsonPath("$.fans").exists());
    }

    // Movement Control Tests
    @Test
    public void testForward() throws Exception {
        mockMvc.perform(post("/forward/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testBackward() throws Exception {
        mockMvc.perform(post("/backward/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testStop() throws Exception {
        mockMvc.perform(post("/stop")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testJoystickControl() throws Exception {
        mockMvc.perform(post("/joystick/90/75")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testThrottleControl() throws Exception {
        mockMvc.perform(post("/throttle/60/30")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSteeringWheel() throws Exception {
        mockMvc.perform(post("/steeringwheel/90")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // System Control Tests
    @Test
    public void testLightsOn() throws Exception {
        mockMvc.perform(post("/lights/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testLightsOff() throws Exception {
        mockMvc.perform(post("/lights/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testFansOn() throws Exception {
        mockMvc.perform(post("/fans/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testFansOff() throws Exception {
        mockMvc.perform(post("/fans/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testMobileControlOn() throws Exception {
        mockMvc.perform(post("/mobilecontrol/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testMobileControlOff() throws Exception {
        mockMvc.perform(post("/mobilecontrol/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testEngineControlOn() throws Exception {
        mockMvc.perform(post("/enginecontrol/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testEngineControlOff() throws Exception {
        mockMvc.perform(post("/enginecontrol/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSteeringWheelControlOn() throws Exception {
        mockMvc.perform(post("/steeringwheelcontrol/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSteeringWheelControlOff() throws Exception {
        mockMvc.perform(post("/steeringwheelcontrol/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testThrottleControlOn() throws Exception {
        mockMvc.perform(post("/throttlecontrol/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testThrottleControlOff() throws Exception {
        mockMvc.perform(post("/throttlecontrol/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Edge Cases Tests
    @Test
    public void testForwardMaxSpeed() throws Exception {
        mockMvc.perform(post("/forward/100")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testForwardMinSpeed() throws Exception {
        mockMvc.perform(post("/forward/0")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testJoystickFullCircle() throws Exception {
        // Test all quadrants
        mockMvc.perform(post("/joystick/0/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/joystick/90/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/joystick/180/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/joystick/270/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSteeringWheelBoundaries() throws Exception {
        // Test min angle
        mockMvc.perform(post("/steeringwheel/0")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Test max angle
        mockMvc.perform(post("/steeringwheel/180")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Test center
        mockMvc.perform(post("/steeringwheel/90")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // Integration Tests
    @Test
    public void testMovementSequence() throws Exception {
        // Forward
        mockMvc.perform(post("/forward/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Stop
        mockMvc.perform(post("/stop")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Backward
        mockMvc.perform(post("/backward/50")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Stop
        mockMvc.perform(post("/stop")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSystemControlSequence() throws Exception {
        // Turn everything on
        mockMvc.perform(post("/lights/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/fans/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/mobilecontrol/ON")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check status
        mockMvc.perform(get("/status/true")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Turn everything off
        mockMvc.perform(post("/lights/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/fans/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(post("/mobilecontrol/OFF")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
