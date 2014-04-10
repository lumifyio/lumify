package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceAccess;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceEntity;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceUser;
import com.altamiracorp.lumify.sql.model.user.SqlUser;
import com.altamiracorp.lumify.sql.model.user.SqlUserRepository;
import com.altamiracorp.securegraph.util.IterableUtils;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SqlWorkspaceRepositoryTest {
    private final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private SqlWorkspaceRepository sqlWorkspaceRepository;
    private static org.hibernate.cfg.Configuration configuration;
    private static SessionFactory sessionFactory;
    private SqlUserRepository sqlUserRepository;

    private SqlUser testUser;

    @Before
    public void setUp() throws Exception {
        configuration = new org.hibernate.cfg.Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionFactory = configuration.buildSessionFactory(serviceRegistryBuilder);
        sqlUserRepository = new SqlUserRepository();
        sqlUserRepository.setSessionFactory(sessionFactory);
        sqlWorkspaceRepository = new SqlWorkspaceRepository();
        sqlWorkspaceRepository.setSessionFactory(sessionFactory);

        testUser = (SqlUser) sqlUserRepository.addUser("123", "user 1", null, new String[0]);
    }

    @Test
    public void testDelete() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlWorkspaceRepository.delete(workspace, testUser);

        assertNull(sqlWorkspaceRepository.findById("1", testUser));
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testDeleteWithUserPermissions() {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.delete(workspace, sqlUser);
    }

    @Test
    public void testAdd() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace", testUser);
        assertEquals("1", workspace.getCreator().getUserId());
        assertEquals("test workspace", workspace.getDisplayTitle());
        assertEquals(1, workspace.getSqlWorkspaceUser().size());

        SqlWorkspaceUser sqlWorkspaceUser = workspace.getSqlWorkspaceUser().iterator().next();
        assertEquals(WorkspaceAccess.WRITE.toString(), sqlWorkspaceUser.getWorkspaceAccess());
        assertEquals("1", sqlWorkspaceUser.getWorkspace().getId());
    }

    @Test
    public void testFindAll() throws Exception {
        Iterable<Workspace> userIterable = sqlWorkspaceRepository.findAll(testUser);
        assertTrue(IterableUtils.count(userIterable) == 0);

        sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlWorkspaceRepository.add("test workspace 2", testUser);
        sqlWorkspaceRepository.add("test workspace 3", testUser);
        userIterable = sqlWorkspaceRepository.findAll(testUser);
        assertTrue(IterableUtils.count(userIterable) == 3);
    }

    @Test
    public void testSetTitle() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        assertEquals("test", sqlWorkspace.getDisplayTitle());
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "changed title", testUser);
        SqlWorkspace modifiedWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById("1", testUser);
        assertEquals("changed title", modifiedWorkspace.getDisplayTitle());
    }

    @Test(expected = LumifyException.class)
    public void testSetTitleWithInvalidWorkspace() {
        sqlWorkspaceRepository.setTitle(new SqlWorkspace(), "test", new SqlUser());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testSetTitleWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser1 = new SqlUser();
        sqlUser1.setId(2);
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "test", sqlUser1);
    }


    @Test
    public void testFindUsersWithAccess() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        List<WorkspaceUser> workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, testUser);
        assertFalse(workspaceUsers.isEmpty());
        assertEquals("1", workspaceUsers.get(0).getUserId());
        assertTrue(workspaceUsers.get(0).isCreator());
    }

    @Test(expected = LumifyException.class)
    public void testFindUsersWithAccessWithInvalidWorkspace() {
        sqlWorkspaceRepository.findUsersWithAccess(new SqlWorkspace(), new SqlUser());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testFindUsersWithAccessWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, sqlUser);
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.WRITE, testUser);
        List<WorkspaceUser> workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, testUser);
        assertTrue(workspaceUsers.size() == 1);
        assertEquals(workspaceUsers.get(0).getWorkspaceAccess(), WorkspaceAccess.WRITE);

        SqlUser testUser2 = (SqlUser) sqlUserRepository.addUser("456", "qwe", "", new String[0]);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.READ, testUser2);
        workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, testUser2);
        assertTrue(workspaceUsers.size() == 2);
        assertEquals(workspaceUsers.get(1).getWorkspaceAccess(), WorkspaceAccess.READ);

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.NONE, testUser);
        workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace, testUser);
        assertEquals(workspaceUsers.get(0).getWorkspaceAccess(), WorkspaceAccess.NONE);
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspaceWithoutPermissions() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setId(2);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, sqlUser);
    }

    @Test
    public void testSoftDeleteEntityFromWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        String vertexId = "1234";

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, true, 0, 0, testUser);
        sqlWorkspace = (SqlWorkspace)sqlWorkspaceRepository.findById("1", testUser);
        Set<SqlWorkspaceVertex> sqlWorkspaceVertexSet = sqlWorkspace.getSqlWorkspaceVertices();
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        SqlWorkspaceVertex sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertTrue(sqlWorkspaceVertex.isVisible());

        sqlWorkspaceRepository.softDeleteEntityFromWorkspace(sqlWorkspace, "1234", testUser);
        sqlWorkspace = (SqlWorkspace)sqlWorkspaceRepository.findById("1", testUser);
        sqlWorkspaceVertexSet = sqlWorkspace.getSqlWorkspaceVertices();
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertFalse(sqlWorkspaceVertex.isVisible());
    }

    @Test
    public void testUpdateEntityOnWorkspace () throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        String vertexId = "1234";

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, true, 0, 0, testUser);

        sqlWorkspace = (SqlWorkspace)sqlWorkspaceRepository.findById("1", testUser);
        Set<SqlWorkspaceVertex> sqlWorkspaceVertexSet = sqlWorkspace.getSqlWorkspaceVertices();
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        SqlWorkspaceVertex sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertEquals("1234", sqlWorkspaceVertex.getSqlVertex().getVertexId());
        assertEquals(0, sqlWorkspaceVertex.getGraphPositionX());
        assertEquals(0, sqlWorkspaceVertex.getGraphPositionY());
        assertTrue(sqlWorkspaceVertex.isVisible());

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, false, 1, 10, testUser);

        sqlWorkspace = (SqlWorkspace)sqlWorkspaceRepository.findById("1", testUser);
        sqlWorkspaceVertexSet = sqlWorkspace.getSqlWorkspaceVertices();
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertEquals("1234", sqlWorkspaceVertex.getSqlVertex().getVertexId());
        assertEquals(1, sqlWorkspaceVertex.getGraphPositionX());
        assertEquals(10, sqlWorkspaceVertex.getGraphPositionY());
        assertFalse(sqlWorkspaceVertex.isVisible());
    }

    @Test
    public void testFindEntities () throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "123", true, 0, 0, testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "345", true, 1, 0, testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "678", true, 2, 0, testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "910", true, 3, 0, testUser);

        sqlWorkspace = (SqlWorkspace)sqlWorkspaceRepository.findById("1", testUser);
        Set<SqlWorkspaceVertex> sqlWorkspaceVertexSet = sqlWorkspace.getSqlWorkspaceVertices();
        assertTrue(sqlWorkspaceVertexSet.size() == 4);

        List<WorkspaceEntity> workspaceEntities = sqlWorkspaceRepository.findEntities(sqlWorkspace, testUser);
        assertTrue(workspaceEntities.size() == 4);
    }
}
