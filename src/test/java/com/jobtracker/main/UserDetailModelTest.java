package com.jobtracker.main;

import com.jobtracker.main.users.UserDetailModel;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The principal is serialized into spring_session, so it must survive a round trip and keep a stable UID. */
public class UserDetailModelTest {

    @Test
    public void serialVersionUidIsFixed() {
        // Changing it logs everyone out on the next deploy. Only do that on purpose.
        assertEquals(1L, ObjectStreamClass.lookup(UserDetailModel.class).getSerialVersionUID());
    }

    @Test
    public void survivesSerializationWithErasedCredentials() throws Exception {
        UUID id = UUID.randomUUID();
        UserDetailModel model = new UserDetailModel(id, "user", "{bcrypt}hash");
        model.eraseCredentials();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(model);
        }
        UserDetailModel copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (UserDetailModel) in.readObject();
        }

        assertEquals(id, copy.getId());
        assertEquals("user", copy.getUsername());
        assertNull(copy.getPassword());
        assertTrue(copy.getAuthorities().isEmpty());
    }
}
