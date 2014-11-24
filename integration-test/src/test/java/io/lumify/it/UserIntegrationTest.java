package io.lumify.it;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiUser;
import io.lumify.web.clientapi.model.ClientApiUsers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UserIntegrationTest extends TestBase {
    private String user1Id;
    private String user2Id;

    @Test
    public void testUsers() throws IOException, ApiException {
        createUsers();
        verifyGetAll();
        verifyGetByIds();
    }

    public void createUsers() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        user1Id = lumifyApi.getUserApi().getMe().getId();
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        user2Id = lumifyApi.getUserApi().getMe().getId();
        lumifyApi.logout();
    }

    private void verifyGetAll() throws ApiException {
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

    private void verifyGetByIds() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);

        List<String> ids = new ArrayList<String>();
        ids.add(user1Id);
        ids.add(user2Id);
        ClientApiUsers users = lumifyApi.getUserApi().getManyByIds(ids);
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
