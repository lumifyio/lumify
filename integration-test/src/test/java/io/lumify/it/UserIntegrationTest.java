package io.lumify.it;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiUser;
import io.lumify.web.clientapi.model.ClientApiUsers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UserIntegrationTest extends TestBase {
    @Test
    public void testUsers() throws IOException, ApiException {
        createUsers();
        verifyUsers();
    }

    public void createUsers() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        lumifyApi.logout();
    }

    private void verifyUsers() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiUsers users = lumifyApi.getUserApi().getAll();
        assertEquals(2, users.getUsers().size());
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (ClientApiUser user : users.getUsers()) {
            if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_1)) {
                foundUser1 = true;
            } else if (user.getUserName().equalsIgnoreCase(USERNAME_TEST_USER_2)) {
                foundUser2 = true;
            } else {
                throw new RuntimeException("Invalid user: " + user);
            }
        }
        assertTrue("Could not find " + USERNAME_TEST_USER_1, foundUser1);
        assertTrue("Could not find " + USERNAME_TEST_USER_2, foundUser2);

        lumifyApi.logout();
    }
}
