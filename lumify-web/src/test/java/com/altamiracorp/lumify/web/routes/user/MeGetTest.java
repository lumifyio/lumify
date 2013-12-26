package com.altamiracorp.lumify.web.routes.user;

import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.user.UserRowKey;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpSession;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class MeGetTest extends RouteTestBase {
    private MeGet meGet;

    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private User mockUser;
    @Mock
    private HttpSession mockHttpSession;

    @Before
    @Override
    public void setUp () throws Exception {
        super.setUp();
        meGet = new MeGet(mockUserRepository);
    }

    @Test
    public void testHandle () throws Exception {
        when(mockRequest.getSession()).thenReturn(mockHttpSession);
        when(AuthenticationProvider.getUser(mockHttpSession)).thenReturn(mockUser);
        when(mockUser.getUsername()).thenReturn("testUserName");

        UserRow user = new UserRow(new UserRowKey("testUserRowKey"));
        user.getMetadata().setUserName("testUserName");
        user.getMetadata().setStatus(UserStatus.OFFLINE);
        when(mockUserRepository.findOrAddUser("testUserName", mockUser)).thenReturn(user);

        meGet.handle(mockRequest, mockResponse, mockHandlerChain);

        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
        assertEquals(user.getRowKey().toString(), response.getString("rowKey"));
        assertEquals("testUserName", response.getString("userName"));
        assertEquals("offline", response.getString("status"));
    }
}
