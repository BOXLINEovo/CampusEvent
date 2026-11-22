package com.campusevent.application.security;

import java.util.UUID;

/**
 * Fixed demo identities. No registration or complex permission system in the initial version.
 */
public final class DemoAccounts {

    public static final UUID STUDENT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID STUDENT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ADMIN = UUID.fromString("99999999-9999-9999-9999-999999999999");

    private DemoAccounts() {
    }
}
