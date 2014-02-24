package com.altamiracorp.lumify.web.routes.workspace;

import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceDeleteTest extends RouteTestBase {
    private WorkspaceDelete workspaceDelete;

    @Mock
    private WorkspaceRepository mockWorkspaceRepository;

    @Before
    @Override
    public void setUp() throws Exception {
//        super.setUp();
        workspaceDelete = new WorkspaceDelete(mockWorkspaceRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        when(mockRequest.getAttribute("workspaceRowKey")).thenReturn("workspaceRowKey");
//
//        workspaceDelete.handle(mockRequest, mockResponse, mockHandlerChain);
//        verify(mockWorkspaceRepository, times(1)).delete(any(WorkspaceRowKey.class), any(ModelUserContext.class));
//        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
//        assertTrue(response.getBoolean("success"));
    }
}
