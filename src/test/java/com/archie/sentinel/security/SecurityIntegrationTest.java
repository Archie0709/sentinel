package com.archie.sentinel.security;

import com.archie.sentinel.TestcontainersConfiguration;
import com.archie.sentinel.model.Incident;
import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityIntegrationTest {

    private static final String PASSWORD = "correct-horse-battery";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired SeverityRepository severityRepository;
    @Autowired IncidentStatusRepository statusRepository;
    @Autowired IncidentRepository incidentRepository;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User alice;
    private User bob;
    private Incident aliceIncident;   // incident assigned to alice

    @BeforeEach
    void setUp() {
        var adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        var analystRole = roleRepository.findByName("ANALYST").orElseThrow();

        userRepository.save(new User("admin1", passwordEncoder.encode(PASSWORD), adminRole));
        alice = userRepository.save(new User("alice1", passwordEncoder.encode(PASSWORD), analystRole));
        bob = userRepository.save(new User("bob1", passwordEncoder.encode(PASSWORD), analystRole));

        var incident = new Incident("Phishing email", "desc",
                severityRepository.findByName("LOW").orElseThrow(),
                statusRepository.findByName("PENDING").orElseThrow(),
                alice);
        incident.setAssignedTo(alice);
        aliceIncident = incidentRepository.saveAndFlush(incident);
    }

    private static RequestPostProcessor asAdmin() { return user("admin1").roles("ADMIN"); }
    private static RequestPostProcessor asAlice() { return user("alice1").roles("ANALYST"); }
    private static RequestPostProcessor asBob()   { return user("bob1").roles("ANALYST"); }

    // Tests for authentication

    @Test
    void unauthenticated_read_returns401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_write_returns401() throws Exception {
        mockMvc.perform(post("/api/incidents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","description":"d","severity":"LOW"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticated_write_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/incidents").with(asAlice())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","description":"d","severity":"LOW"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_withValidCredentials_returns204_andSessionIsUsable() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "alice1").param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice1"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void login_failures_look_identical_forWrongPasswordAndUnknownUser() throws Exception {
        var wrongPassword = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "alice1").param("password", "wrong-password-123"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        var unknownUser = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("username", "nobody").param("password", "wrong-password-123"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(unknownUser.getResponse().getContentAsString())
                .isEqualTo(wrongPassword.getResponse().getContentAsString());
    }

    @Test
    void login_issuesNewSessionId_toPreventSessionFixation() throws Exception {
        MockHttpSession before = new MockHttpSession();
        String oldId = before.getId();

        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf()).session(before)
                        .param("username", "alice1").param("password", PASSWORD))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(oldId);
    }

    // Tests for role-based access

    @Test
    void analyst_cannotListOrCreateUsers() throws Exception {
        mockMvc.perform(get("/api/users").with(asAlice()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users").with(asAlice()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"evil","password":"correct-horse-battery","role":"ADMIN"}"""))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByUsername("evil")).isFalse();
    }

    @Test
    void analyst_cannotPromoteThemselvesToAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/" + alice.getId() + "/role").with(asAlice()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isForbidden());

        var reloaded = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(reloaded.getRole().getName()).isEqualTo("ANALYST");
    }

    @Test
    void analyst_cannotReadAuditHistory_butAdminCan() throws Exception {
        mockMvc.perform(get("/api/incidents/" + aliceIncident.getId() + "/audit").with(asAlice()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidents/" + aliceIncident.getId() + "/audit").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void userList_neverExposesPasswordHashes() throws Exception {
        mockMvc.perform(get("/api/users").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    // Tests for object-level access (IDOR) and property-level access

    @Test
    void analyst_cannotModifyIncidentAssignedToSomeoneElse() throws Exception {
        mockMvc.perform(patch("/api/incidents/" + aliceIncident.getId() + "/severity")
                        .with(asBob()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"severity":"CRITICAL"}"""))
                .andExpect(status().isForbidden());

        var reloaded = incidentRepository.findById(aliceIncident.getId()).orElseThrow();
        assertThat(reloaded.getSeverity().getName()).isEqualTo("LOW");
        assertThat(auditLogRepository.findByIncidentIdOrderByIdDesc(aliceIncident.getId())).isEmpty();
    }

    @Test
    void analyst_cannotStealIncidentAssignedToSomeoneElse() throws Exception {
        mockMvc.perform(patch("/api/incidents/" + aliceIncident.getId() + "/assignee")
                        .with(asBob()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assignee":"bob1"}"""))
                .andExpect(status().isForbidden());

        var reloaded = incidentRepository.findById(aliceIncident.getId()).orElseThrow();
        assertThat(reloaded.getAssignedTo().getUsername()).isEqualTo("alice1");
    }

    @Test
    void assignedAnalyst_canModifyOwnIncident_andChangeIsAudited() throws Exception {
        mockMvc.perform(patch("/api/incidents/" + aliceIncident.getId() + "/severity")
                        .with(asAlice()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"severity":"HIGH"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("HIGH"));

        var history = auditLogRepository.findByIncidentIdOrderByIdDesc(aliceIncident.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getOldValue()).isEqualTo("LOW");
        assertThat(history.get(0).getNewValue()).isEqualTo("HIGH");
        assertThat(history.get(0).getChangedBy().getUsername()).isEqualTo("alice1");
    }

    @Test
    void createdBy_cannotBeForgedThroughRequestBody() throws Exception {
        mockMvc.perform(post("/api/incidents").with(asBob()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","description":"d","severity":"LOW","createdBy":"admin1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("bob1"));
    }

    @Test
    void invalidStatusTransition_isRejectedByRealTransitionTable() throws Exception {
        mockMvc.perform(patch("/api/incidents/" + aliceIncident.getId() + "/status")
                        .with(asAlice()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}"""))
                .andExpect(status().isConflict());

        var reloaded = incidentRepository.findById(aliceIncident.getId()).orElseThrow();
        assertThat(reloaded.getStatus().getName()).isEqualTo("PENDING");
    }
}
