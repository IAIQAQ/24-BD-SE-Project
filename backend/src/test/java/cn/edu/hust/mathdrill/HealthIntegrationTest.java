package cn.edu.hust.mathdrill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"server.ssl.enabled=false", "mathdrill.seed-enabled=true"})
@AutoConfigureMockMvc
class HealthIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void publicHealthContainsOnlyStatus() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}", true));
    }
}
