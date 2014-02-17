package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRowKey;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceByRowKeyTest extends RouteTestBase {
    private WorkspaceByRowKey workspaceByRowKey;
    private WorkspaceRowKey workspaceRowKey;

    @Mock
    private WorkspaceRepository mockWorkspaceRespository;

    @Before
    @Override
    public void setUp() throws Exception {
//        super.setUp();

//        workspaceByRowKey = new WorkspaceByRowKey(mockWorkspaceRespository);
//        workspaceRowKey = new WorkspaceRowKey("lumify", "Default - lumify");
//        when(mockRequest.getAttribute("workspaceRowKey")).thenReturn(workspaceRowKey.toString());
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        Workspace workspace = new Workspace(workspaceRowKey);
//        workspace.getMetadata().setCreator("lumify");
//        when(mockWorkspaceRespository.findByRowKey(workspaceRowKey.toString(), mockUser.getModelUserContext())).thenReturn(workspace);
//        when(mockRequest.getSession()).thenReturn(mockHttpSession);
//
//        UserRowKey userRowKey = new UserRowKey("lumify");
//        when(mockUser.getRowKey()).thenReturn(userRowKey.toString());
//
//        workspaceByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);
//        JSONObject responseJson = new JSONObject(responseStringWriter.getBuffer().toString());
//        assertEquals(workspaceRowKey.toString(), responseJson.getString("id"));
//        assertFalse(responseJson.getBoolean("isSharedToUser"));
//        assertEquals("lumify", responseJson.getString("createdBy"));
//        assertTrue(responseJson.getBoolean("isEditable"));
    }

    @Test
    public void testHandleWithNullWorkspace() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        when(mockWorkspaceRespository.findByRowKey(workspaceRowKey.toString(), mockUser.getModelUserContext())).thenReturn(null);
//        workspaceByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);
//        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testHandleWithNullResult() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        Workspace workspace = new Workspace(workspaceRowKey);
//        workspace.getMetadata().setCreator("");
//        when(mockWorkspaceRespository.findByRowKey(workspaceRowKey.toString(), mockUser.getModelUserContext())).thenReturn(workspace);
//        when(workspace.toJson(mockUser)).thenReturn(null);
//
//        workspaceByRowKey.handle(mockRequest, mockResponse, mockHandlerChain);
//        verify(mockResponse).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
