package io.lumify.web.clientapi.model;

import com.google.common.base.Joiner;
import org.json.JSONArray;

import java.util.*;

public enum Privilege {
    READ(0x01),
    COMMENT(0x16),
    EDIT(0x02),
    PUBLISH(0x04),
    ADMIN(0x08);

    private final int value;

    Privilege(int value) {
        this.value = value;
    }

    public static int toBits(Privilege... indexHints) {
        return toBits(EnumSet.copyOf(Arrays.asList(indexHints)));
    }

    public static int toBits(Collection<Privilege> privileges) {
        byte b = 0;
        for (Privilege hint : privileges) {
            b |= hint.value;
        }
        return b;
    }

    public static Set<Privilege> toSet(int privileges) {
        Set<Privilege> hints = new HashSet<Privilege>();
        for (int i = 0; i < Privilege.values().length; i++) {
            Privilege privilege = Privilege.values()[i];
            if ((privileges & privilege.value) == privilege.value) {
                hints.add(privilege);
            }
        }
        return hints;
    }

    public static final Set<Privilege> ALL = EnumSet.of(READ, COMMENT, EDIT, PUBLISH, ADMIN);

    public static final Set<Privilege> NONE = new HashSet<Privilege>();

    public static JSONArray toJson(Set<Privilege> privileges) {
        JSONArray json = new JSONArray();
        for (Privilege privilege : privileges) {
            json.put(privilege.name());
        }
        return json;
    }

    public static Set<Privilege> stringToPrivileges(String privilegesString) {
        if (privilegesString == null || privilegesString.equalsIgnoreCase("NONE")) {
            return NONE;
        }

        if (privilegesString.equalsIgnoreCase("ALL")) {
            return ALL;
        }

        String[] privilegesStringParts = privilegesString.split(",");
        Set<Privilege> privileges = new HashSet<Privilege>();
        for (String privilegesStringPart : privilegesStringParts) {
            if (privilegesStringPart.trim().length() == 0) {
                continue;
            }
            privileges.add(stringToPrivilege(privilegesStringPart));
        }
        return privileges;
    }

    public static Privilege stringToPrivilege(String privilegesStringPart) {
        privilegesStringPart = privilegesStringPart.trim();
        for (Privilege privilege : Privilege.values()) {
            if (privilege.name().equalsIgnoreCase(privilegesStringPart)) {
                return privilege;
            }
        }
        return Privilege.valueOf(privilegesStringPart);
    }

    public static String toString(Collection<Privilege> privileges) {
        return Joiner.on(",").join(privileges);
    }

    public static boolean hasAll(Set<Privilege> userPrivileges, Set<Privilege> requiredPrivileges) {
        for (Privilege privilege : requiredPrivileges) {
            if (!userPrivileges.contains(privilege)) {
                return false;
            }
        }
        return true;
    }
}
