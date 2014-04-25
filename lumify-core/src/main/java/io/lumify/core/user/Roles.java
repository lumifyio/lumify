package io.lumify.core.user;

import org.json.JSONArray;

import java.util.*;

public enum Roles {
    READ(0x01),
    EDIT(0x02),
    PUBLISH(0x04),
    ADMIN(0x08);

    private final int value;

    Roles(int value) {
        this.value = value;
    }

    public static int toBits(Roles... indexHints) {
        return toBits(EnumSet.copyOf(Arrays.asList(indexHints)));
    }

    public static int toBits(Collection<Roles> roles) {
        byte b = 0;
        for (Roles hint : roles) {
            b |= hint.value;
        }
        return b;
    }

    public static Set<Roles> toSet(int roles) {
        Set<Roles> hints = new HashSet<Roles>();
        for (int i = 0; i < Roles.values().length; i++) {
            Roles role = Roles.values()[i];
            if ((roles & role.value) == role.value) {
                hints.add(role);
            }
        }
        return hints;
    }

    public static final Set<Roles> ALL = EnumSet.of(READ, EDIT, PUBLISH, ADMIN);

    public static final Set<Roles> NONE = new HashSet<Roles>();

    public static JSONArray toJson(Set<Roles> roles) {
        JSONArray json = new JSONArray();
        for (Roles role : roles) {
            json.put(role.name());
        }
        return json;
    }

    public static Set<Roles> stringToRoles(String rolesString) {
        if (rolesString == null) {
            return NONE;
        }
        String[] rolesStringParts = rolesString.split(",");
        Set<Roles> roles = new HashSet<Roles>();
        for (String rolesStringPart : rolesStringParts) {
            roles.add(stringToRole(rolesStringPart));
        }
        return roles;
    }

    public static Roles stringToRole(String rolesStringPart) {
        for (Roles role : Roles.values()) {
            if (role.name().equalsIgnoreCase(rolesStringPart)) {
                return role;
            }
        }
        return Roles.valueOf(rolesStringPart);
    }
}
