package com.archie.sentinel.support;

import com.archie.sentinel.model.*;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestData {
    private TestData() {}

    public static Role role(String name) {
        Role r = BeanUtils.instantiateClass(Role.class);
        ReflectionTestUtils.setField(r, "name", name);
        return r;
    }

    public static Severity severity(String name) {
        Severity s = BeanUtils.instantiateClass(Severity.class);
        ReflectionTestUtils.setField(s, "name", name);
        return s;
    }

    public static IncidentStatus status(long id, String name) {
        IncidentStatus s = BeanUtils.instantiateClass(IncidentStatus.class);
        ReflectionTestUtils.setField(s, "id", (short) id);
        ReflectionTestUtils.setField(s, "name", name);
        return s;
    }

    public static User user(long id, String username, String roleName) {
        User u = new User(username, "hash", role(roleName));
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    public static Incident incident(long id, Severity severity, IncidentStatus status, User createdBy) {
        Incident i = new Incident("title", "desc", severity, status, createdBy);
        ReflectionTestUtils.setField(i, "id", id);
        return i;
    }

    public static Severity severityWithRank(String name, int rank) {
        Severity s = BeanUtils.instantiateClass(Severity.class);
        ReflectionTestUtils.setField(s, "name", name);
        ReflectionTestUtils.setField(s, "rankOrder", (short) rank);
        return s;
    }

}