package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceListTest extends RouteTestBase {
    private WorkspaceList workspaceList;

    @Mock
    private WorkspaceRepository mockWorkspaceRepository;

    @Override
    @Before
    public void setUp() throws Exception {
//        super.setUp();
        workspaceList = new WorkspaceList(mockWorkspaceRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        when(mockUser.getRowKey()).thenReturn("lumify");
//
//        WorkspaceRowKey activeRK = new WorkspaceRowKey("lumify", "active");
//        Workspace active = new Workspace(activeRK);
//        active.getMetadata().setCreator("lumify");
//
//        WorkspaceRowKey workspaceRK1 = new WorkspaceRowKey("lumify", "test1");
//        Workspace workspace1 = new Workspace(workspaceRK1);
//        workspace1.getMetadata().setCreator("lumify");
//
//        List<Workspace> workspaces = Lists.newArrayList(active, workspace1);
//
//        when(mockWorkspaceRepository.findAll(mockUser.getModelUserContext())).thenReturn(workspaces);
//        when(mockHttpSession.getAttribute("activeWorkspace")).thenReturn(activeRK.toString());
//
//        workspaceList.handle(mockRequest, mockResponse, mockHandlerChain);
//
//        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
//        assertEquals(2, response.getJSONArray("workspaces").length());
//        assertTrue(response.getJSONArray("workspaces").getJSONObject(0).getBoolean("active"));
    }
}
