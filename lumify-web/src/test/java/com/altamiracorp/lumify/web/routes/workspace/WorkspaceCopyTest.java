package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserRow;
import com.altamiracorp.lumify.core.model.user.UserRowKey;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRowKey;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceCopyTest extends RouteTestBase {
    private WorkspaceCopy workspaceCopy;

    @Mock
    private WorkspaceRepository mockWorkspaceRepository;
    @Mock
    private UserRepository mockUserRepository;
    @Mock
    private UserRow mockUserRow;

    @Before
    @Override
    public void setUp() throws Exception {
//        super.setUp();

        workspaceCopy = new WorkspaceCopy(mockWorkspaceRepository, mockUserRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        String userId = "lumify";
//        String title = "Default - lumify";
//        WorkspaceRowKey workspaceRowKey = new WorkspaceRowKey(userId, title);
//        Workspace workspace = new Workspace(workspaceRowKey);
//        workspace.getMetadata().setTitle(title);
//        workspace.getMetadata().setCreator(userId);
//        UserRowKey userRowKey = new UserRowKey(userId);
//
//        when(mockRequest.getAttribute("workspaceRowKey")).thenReturn(workspaceRowKey.toString());
//        when(mockUser.getRowKey()).thenReturn(userId);
//        when(mockUserRow.getRowKey()).thenReturn(userRowKey);
//        when(mockUserRepository.findOrAddUser(mockUser.getUsername(), mockUser)).thenReturn(mockUserRow);
//        when(mockWorkspaceRepository.findByRowKey(workspaceRowKey.toString(), mockUser.getModelUserContext())).thenReturn(workspace);
//
//        workspaceCopy.handle(mockRequest, mockResponse, mockHandlerChain);
//
//        JSONObject results = new JSONObject(responseStringWriter.getBuffer().toString());
//        verify(mockWorkspaceRepository, times(1)).save(any(Workspace.class), any(ModelUserContext.class));
//        assertFalse(results.getBoolean("isSharedToUser"));
//        assertEquals("Copy of " + title, results.getString("title"));
//        assertEquals(userId, results.getString("createdBy"));
//        assertTrue(results.getBoolean("isEditable"));
    }
}
