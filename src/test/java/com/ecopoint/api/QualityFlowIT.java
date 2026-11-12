package com.ecopoint.api;

import com.ecopoint.persistence.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quartering, density, double entry, alerts and photos through the API against
 * the demo data (spec 003, T19).
 *
 * @author craulgomez
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "ecopoint.upload-dir=target/test-uploads")
class QualityFlowIT extends PostgresIntegrationTest {

    private static final String PASSWORD = "EcoPoint2026";
    private static final long RUNNING_STUDY = 2L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private ApiClient teacher;
    private ApiClient student;
    private long paper;
    private long food;

    @BeforeEach
    void loadDemoDataAndLogIn() throws Exception {
        runScript("002_seed.sql");
        teacher = ApiClient.loggedIn(mvc, "docente.demo@unicesar.edu.co", PASSWORD);
        student = ApiClient.loggedIn(mvc, "estudiante.demo@unicesar.edu.co", PASSWORD);
        paper = jdbc.queryForObject("SELECT id FROM SUBCATEGORIA_RESIDUO WHERE grupo = 'BLANCO' ORDER BY id LIMIT 1",
                Long.class);
        food = jdbc.queryForObject("SELECT id FROM SUBCATEGORIA_RESIDUO WHERE grupo = 'VERDE' ORDER BY id LIMIT 1",
                Long.class);
    }

    @Test
    void rn052_conformingQuarteringShowsItsCompositionAndDensityIsMeasured() throws Exception {
        long shift = shiftWithSixBags("MANANA");

        student.json(post("/api/v1/shifts/" + shift + "/quartering"), quartering("30.00", "19.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualityStatus").value("CONFORME"))
                .andExpect(jsonPath("$.shiftNetWeight").value(72.00))
                .andExpect(jsonPath("$.differencePercent").value(1.00))
                .andExpect(jsonPath("$.groupComposition.BLANCO").exists())
                .andExpect(jsonPath("$.results", hasSize(2)));

        student.json(post("/api/v1/shifts/" + shift + "/density"), "{\"containerVolume\":0.200,\"netWeight\":30.00}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.density").value(150.00));

        teacher.perform(get("/api/v1/shifts/" + shift))
                .andExpect(jsonPath("$.hasQuartering").value(true))
                .andExpect(jsonPath("$.hasDensity").value(true));
    }

    @Test
    void rn070_nonConformingQuarteringRaisesAnAlertThatIsAttendedAndTheQuarteringAnnulled() throws Exception {
        long shift = shiftWithSixBags("TARDE");

        student.json(post("/api/v1/shifts/" + shift + "/quartering"), quartering("30.00", "17.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualityStatus").value("NO_CONFORME"));

        String alerts = teacher.perform(get("/api/v1/alerts").param("studyId", String.valueOf(RUNNING_STUDY))
                        .param("attended", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type").value("CUARTEO_NO_CONFORME"))
                .andReturn().getResponse().getContentAsString();
        long alert = json.readTree(alerts).path("items").get(0).path("id").asLong();

        student.json(post("/api/v1/alerts/" + alert + "/attend"), "{\"reason\":\"Se volvió a pesar la submuestra.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attended").value(true));

        teacher.json(post("/api/v1/shifts/" + shift + "/quartering/annul"),
                        "{\"reason\":\"La báscula estaba descalibrada.\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualityStatus").value("ANULADO"));
    }

    @Test
    void rn062_doubleEntryVerifiesAndCorrectsWithHistory() throws Exception {
        long shift = shiftWithSixBags("MANANA");
        List<Long> bags = bagIds(shift);

        student.perform(post("/api/v1/verifications/bag/" + bags.get(0)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ERR_BUSINESS_RULE"));

        teacher.perform(post("/api/v1/verifications/bag/" + bags.get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification.status").value("VERIFICADO"))
                .andExpect(jsonPath("$.shiftId").value(shift));

        teacher.json(post("/api/v1/verifications/bag/" + bags.get(1) + "/correct"),
                        "{\"values\":{\"grossWeight\":12.04},\"reason\":\"En el papel dice 12,04\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verification.status").value("CORREGIDO"));

        teacher.perform(get("/api/v1/records/bag/" + bags.get(1) + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].field").value("grossWeight"))
                .andExpect(jsonPath("$[0].oldValue").value("12.40"))
                .andExpect(jsonPath("$[0].newValue").value("12.04"));

        teacher.perform(get("/api/v1/bags/" + bags.get(1)))
                .andExpect(jsonPath("$.netWeight").value(11.64));

        teacher.perform(get("/api/v1/verifications").param("studyId", String.valueOf(RUNNING_STUDY))
                        .param("shiftId", String.valueOf(shift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.recordId == " + bags.get(0) + " && @.recordType == 'bag')]").isEmpty())
                .andExpect(jsonPath("$[?(@.recordId == " + bags.get(2) + " && @.recordType == 'bag')]").isNotEmpty());
    }

    @Test
    void ca205_correctionThatBreaksARuleIsRejectedWithItsField() throws Exception {
        long shift = shiftWithSixBags("MANANA");
        long bag = bagIds(shift).get(0);

        teacher.json(post("/api/v1/verifications/bag/" + bag + "/correct"),
                        "{\"values\":{\"tare\":13.00},\"reason\":\"Error\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("values.tare"));
    }

    @Test
    void rn090_photoOfABagIsUploadedListedAndDownloaded() throws Exception {
        long shift = shiftWithSixBags("MANANA");
        long bag = bagIds(shift).get(0);
        byte[] photo = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        String body = student.perform(multipart("/api/v1/evidences")
                        .file(new MockMultipartFile("file", "balanza.png", "image/png", photo))
                        .param("targetType", "BAG").param("targetId", String.valueOf(bag))
                        .param("description", "Foto de la balanza"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andReturn().getResponse().getContentAsString();
        String url = json.readTree(body).path("url").asText();

        student.perform(get("/api/v1/evidences").param("targetType", "BAG").param("targetId", String.valueOf(bag)))
                .andExpect(jsonPath("$", hasSize(1)));
        student.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().bytes(photo));

        student.perform(multipart("/api/v1/evidences")
                        .file(new MockMultipartFile("file", "informe.pdf", "application/pdf", photo))
                        .param("targetType", "BAG").param("targetId", String.valueOf(bag)))
                .andExpect(status().isBadRequest());
    }

    private long shiftWithSixBags(String type) throws Exception {
        String opened = teacher.json(post("/api/v1/studies/" + RUNNING_STUDY + "/shifts"),
                        "{\"dayNumber\":3,\"type\":\"" + type + "\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode shiftNode = json.readTree(opened);
        long shift = shiftNode.path("id").asLong();
        long source = shiftNode.path("sources").get(0).path("sourceId").asLong();
        student.json(put("/api/v1/shifts/" + shift + "/population"), "{\"students\":300,\"teachers\":12,\"staff\":6}")
                .andExpect(status().isOk());
        for (int i = 0; i < 6; i++) {
            student.json(post("/api/v1/shifts/" + shift + "/bags"),
                            "{\"sourceId\":" + source + ",\"grossWeight\":12.40,\"tare\":0.40,\"volume\":0.080}")
                    .andExpect(status().isCreated());
        }
        return shift;
    }

    private List<Long> bagIds(long shift) throws Exception {
        String body = teacher.perform(get("/api/v1/shifts/" + shift + "/bags")).andReturn().getResponse()
                .getContentAsString();
        return StreamSupport.stream(json.readTree(body).path("items").spliterator(), false)
                .map(node -> node.path("id").asLong())
                .toList();
    }

    private String quartering(String paperWeight, String foodWeight) {
        return "{\"sampleWeight\":50.00,\"results\":[{\"categoryId\":" + paper + ",\"weight\":" + paperWeight
                + "},{\"categoryId\":" + food + ",\"weight\":" + foodWeight + "}]}";
    }
}
