package com.ecopoint.controller;

import com.ecopoint.exceptions.InvalidDataException;
import com.ecopoint.model.Crew;
import com.ecopoint.model.GenerationSource;
import com.ecopoint.model.Institution;
import com.ecopoint.model.SourceType;
import com.ecopoint.model.User;
import com.ecopoint.model.UserRole;
import com.ecopoint.model.WasteCategory;
import com.ecopoint.model.WasteGroup;
import com.ecopoint.service.CrewService;
import com.ecopoint.service.GenerationSourceService;
import com.ecopoint.service.InstitutionService;
import com.ecopoint.service.UserService;
import com.ecopoint.service.WasteCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Routes and JSON of the administration resources (spec 003, section 5.1).
 * Permissions are checked in the services and in {@code PermissionMatrixIT}.
 *
 * @author craulgomez
 */
@WebMvcTest(controllers = {InstitutionController.class, GenerationSourceController.class, UserController.class,
        CrewController.class, WasteCategoryController.class})
@AutoConfigureMockMvc(addFilters = false)
@Import(com.ecopoint.config.ClockConfig.class)
class AdministrationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CurrentUser currentUser;
    @MockitoBean
    private InstitutionService institutions;
    @MockitoBean
    private GenerationSourceService sources;
    @MockitoBean
    private UserService users;
    @MockitoBean
    private CrewService crews;
    @MockitoBean
    private WasteCategoryService categories;

    private final User teacher = new User(10L, "Daniela", "Arias", "daniela@unicesar.edu.co", UserRole.DOCENTE, 1L);

    @BeforeEach
    void setUp() {
        when(currentUser.of(any())).thenReturn(teacher);
    }

    @Test
    void r320_institutionIsCreatedWithItsMapCenter() throws Exception {
        when(institutions.create(eq(teacher), any())).thenAnswer(call -> {
            Institution institution = call.getArgument(1);
            institution.setId(3L);
            return institution;
        });

        mvc.perform(post("/api/v1/institutions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Colegio Nuevo","address":"Calle 1",
                         "mapCenter":{"latitude":10.4700000,"longitude":-73.2500000},"mapZoom":18}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.mapCenter.latitude").value(10.47))
                .andExpect(jsonPath("$.mapZoom").value(18))
                .andExpect(jsonPath("$.settings.qualityTolerancePercent").exists());
    }

    @Test
    void ca205_invalidSourceCodeIsReportedOnTheCodeField() throws Exception {
        when(sources.create(eq(teacher), eq(1L), any(), any(), any()))
                .thenThrow(new InvalidDataException("codigo", "El código debe tener de 2 a 6 caracteres."));

        mvc.perform(post("/api/v1/institutions/1/sources").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"aulas12\",\"name\":\"Aulas\",\"type\":\"AULAS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("code"));
    }

    @Test
    void r101_unknownSourceTypeIsInvalidData() throws Exception {
        mvc.perform(post("/api/v1/institutions/1/sources").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CAF\",\"name\":\"Cafetería\",\"type\":\"CAFETERIA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_INVALID_DATA"));
    }

    @Test
    void r321_sourcesAreListedWithTheirState() throws Exception {
        GenerationSource store = new GenerationSource(5L, 1L, "TI", "Tienda escolar", SourceType.TIENDA_ESCOLAR);
        when(sources.list(1L, true)).thenReturn(List.of(store));

        mvc.perform(get("/api/v1/institutions/1/sources").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TI"))
                .andExpect(jsonPath("$[0].type").value("TIENDA_ESCOLAR"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].hasGeometry").value(false));
    }

    @Test
    void rn005_sourceWithoutBagsIsDeleted() throws Exception {
        mvc.perform(delete("/api/v1/institutions/1/sources/5")).andExpect(status().isNoContent());

        verify(sources).delete(teacher, 1L, 5L);
    }

    @Test
    void r103_usersComeInPagesAndNeverWithTheirPassword() throws Exception {
        List<User> many = IntStream.range(0, 3)
                .mapToObj(i -> new User((long) i, "Nombre", "Apellido", "u" + i + "@unicesar.edu.co",
                        UserRole.ESTUDIANTE_UNIVERSITARIO, 1L))
                .toList();
        when(users.list(teacher, UserRole.ESTUDIANTE_UNIVERSITARIO, null)).thenReturn(many);

        mvc.perform(get("/api/v1/users").param("role", "ESTUDIANTE_UNIVERSITARIO").param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].email").value("u2@unicesar.edu.co"))
                .andExpect(jsonPath("$.items[0].password").doesNotExist())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    void r322_userIsCreatedWithItsInitialPassword() throws Exception {
        when(users.create(eq(teacher), any(), eq("Segura2026"))).thenAnswer(call -> {
            User user = call.getArgument(1);
            user.setId(20L);
            user.setInstitutionId(1L);
            return user;
        });

        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Mario","lastName":"Gómez","email":"mario@unicesar.edu.co",
                         "role":"ESTUDIANTE_UNIVERSITARIO","password":"Segura2026"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.firstName").value("Mario"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void r322_emptyPasswordOnEditKeepsTheCurrentOne() throws Exception {
        when(users.update(eq(teacher), eq(11L), any(), eq(null))).thenReturn(
                new User(11L, "Laura", "Pérez", "laura@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L));

        mvc.perform(put("/api/v1/users/11").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Laura","lastName":"Pérez","email":"laura@unicesar.edu.co",
                         "role":"ESTUDIANTE_UNIVERSITARIO","password":""}"""))
                .andExpect(status().isOk());
        verify(users).update(eq(teacher), eq(11L), any(), eq(null));
    }

    @Test
    void r323_crewShowsItsResponsibleAndMembers() throws Exception {
        User student = new User(11L, "Laura", "Pérez", "laura@unicesar.edu.co", UserRole.ESTUDIANTE_UNIVERSITARIO, 1L);
        Crew crew = new Crew(4L, "Cuadrilla A", teacher);
        crew.addMember(student);
        when(crews.list(teacher)).thenReturn(List.of(crew));

        mvc.perform(get("/api/v1/crews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cuadrilla A"))
                .andExpect(jsonPath("$[0].responsibleId").value(10))
                .andExpect(jsonPath("$[0].responsibleName").value("Daniela Arias"))
                .andExpect(jsonPath("$[0].memberIds[0]").value(11));
    }

    @Test
    void r324_subcategoriesShowTheirGroupAndWhetherTheyAreHazardous() throws Exception {
        when(categories.list(WasteGroup.SIN_COLOR)).thenReturn(
                List.of(new WasteCategory(3L, "Pilas", WasteGroup.SIN_COLOR, "Pilas y baterías")));

        mvc.perform(get("/api/v1/waste-categories").param("group", "SIN_COLOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pilas"))
                .andExpect(jsonPath("$[0].group").value("SIN_COLOR"))
                .andExpect(jsonPath("$[0].hazardous").value(true));
    }
}
