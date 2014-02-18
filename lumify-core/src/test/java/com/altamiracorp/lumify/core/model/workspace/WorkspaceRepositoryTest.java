package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.MockSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ModelUtil;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class WorkspaceRepositoryTest {
    private MockSession session;
    private WorkspaceRepository workspaceRepository;

    @Mock
    private User user;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        session = new MockSession();
        ModelUtil.initializeTables(session, user);
        workspaceRepository = new WorkspaceRepository(session);
    }

    @Test
    public void testFindByRowKey() {
        String rowKeyString = "testUser:testWorkspace";
        Row<RowKey> row = new Row<RowKey>(Workspace.TABLE_NAME, new RowKey(rowKeyString));

        ColumnFamily workspaceContentColumnFamily = new ColumnFamily(WorkspaceContent.NAME);
        workspaceContentColumnFamily
                .set(WorkspaceContent.DATA, "testData")
                .set("extra", "textExtra");
        row.addColumnFamily(workspaceContentColumnFamily);

        ColumnFamily extraColumnFamily = new ColumnFamily("testExtraColumnFamily");
        extraColumnFamily
                .set("testExtraColumn", "testExtraValue");
        row.addColumnFamily(extraColumnFamily);

        session.tables.get(Workspace.TABLE_NAME).add(row);

        Workspace workspace = workspaceRepository.findByRowKey(rowKeyString, user.getModelUserContext());
        assertEquals(rowKeyString, workspace.getRowKey().toString());
        assertEquals(2, workspace.getColumnFamilies().size());

        WorkspaceContent workspaceContent = workspace.getContent();
        assertEquals(WorkspaceContent.NAME, workspaceContent.getColumnFamilyName());
        assertEquals("testData", workspaceContent.getData());
        assertEquals("textExtra", workspaceContent.get("extra").toString());

        ColumnFamily foundExtraColumnFamily = workspace.get("testExtraColumnFamily");
        assertNotNull("foundExtraColumnFamily", foundExtraColumnFamily);
        assertEquals("testExtraValue", foundExtraColumnFamily.get("testExtraColumn").toString());
    }

    @Test
    public void testSave() {
        Workspace workspace = new Workspace("testUser", "testWorkspace");

        workspace.getContent()
                .setData("testData")
                .set("testExtra", "testExtraValue");

        workspace.addColumnFamily(
                new ColumnFamily("testExtraColumnFamily")
                        .set("testExtraColumn", "testExtraValue"));

        workspaceRepository.save(workspace);

        assertEquals(1, session.tables.get(Workspace.TABLE_NAME).size());
        Row row = session.tables.get(Workspace.TABLE_NAME).get(0);
        assertEquals(RowKeyHelper.buildMinor("testUser", "testWorkspace"), row.getRowKey().toString());

        assertEquals(2, row.getColumnFamilies().size());

        ColumnFamily workspaceContentColumnFamily = row.get(WorkspaceContent.NAME);
        assertEquals(WorkspaceContent.NAME, workspaceContentColumnFamily.getColumnFamilyName());
        assertEquals("testData", workspaceContentColumnFamily.get(WorkspaceContent.DATA).toString());
        assertEquals("testExtraValue", workspaceContentColumnFamily.get("testExtra").toString());

        ColumnFamily extraColumnFamily = row.get("testExtraColumnFamily");
        assertNotNull("extraColumnFamily", extraColumnFamily);
        assertEquals(1, extraColumnFamily.getColumns().size());
        assertEquals("testExtraValue", extraColumnFamily.get("testExtraColumn").toString());
    }
}