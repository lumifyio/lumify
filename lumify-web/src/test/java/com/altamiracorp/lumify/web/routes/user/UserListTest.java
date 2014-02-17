package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UserListTest extends RouteTestBase {
    private UserList userList;

    @Mock
    private UserRepository mockUserRepository;

    @Override
    @Before
    public void setUp() throws Exception {
//        super.setUp();
        userList = new UserList(mockUserRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        UserRow userRow1 = new UserRow(new UserRowKey("rowKey1"));
//        userRow1.getMetadata().setUserName("test");
//        userRow1.getMetadata().setUserType(UserType.USER.toString());
//        UserRow user2 = new UserRow(new UserRowKey(""));
//        List<UserRow> users = Lists.newArrayList(userRow1, user2);
//
//        when(mockUserRepository.findAll(mockUser.getModelUserContext())).thenReturn(users);
//        userList.handle(mockRequest, mockResponse, mockHandlerChain);
//
//        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
//        assertNotNull(response.getJSONArray("users"));
//        assertEquals(2, response.getJSONArray("users").length());
//        assertEquals("rowKey1", response.getJSONArray("users").getJSONObject(0).getString("rowKey"));
//        assertEquals("offline", response.getJSONArray("users").getJSONObject(0).getString("status"));
//        assertEquals("test", response.getJSONArray("users").getJSONObject(0).getString("userName"));
//        assertEquals("user", response.getJSONArray("users").getJSONObject(0).getString("userType"));
    }
}
