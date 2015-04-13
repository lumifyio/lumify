package io.lumify.sql.model.workspace;

import com.google.common.collect.Lists;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.model.user.UserListenerUtil;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceEntity;
import io.lumify.core.model.workspace.WorkspaceUser;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.sql.model.user.SqlUser;
import io.lumify.sql.model.user.SqlUserRepository;
import io.lumify.web.clientapi.model.GraphPosition;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.util.IterableUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SqlWorkspaceRepositoryTest {
    private static final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private SqlWorkspaceRepository sqlWorkspaceRepository;
    private SqlUserRepository sqlUserRepository;

    private SqlUser testUser;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private UserListenerUtil userListenerUtil;
    private HibernateSessionManager sessionManager;

    @Before
    public void setUp() throws Exception {
        InMemoryGraph graph = InMemoryGraph.create();
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionManager = new HibernateSessionManager(configuration.buildSessionFactory(serviceRegistryBuilder));
        Map<?, ?> configMap = new HashMap<Object, Object>();
        Configuration lumifyConfiguration = new HashMapConfigurationLoader(configMap).createConfiguration();
        sqlUserRepository = new SqlUserRepository(lumifyConfiguration, sessionManager, authorizationRepository, userListenerUtil, graph);
        sqlWorkspaceRepository = new SqlWorkspaceRepository(sqlUserRepository, sessionManager, graph);
        testUser = (SqlUser) sqlUserRepository.addUser("123", "user 1", null, null, new String[0]);
    }

    @After
    public void teardown() {
        sessionManager.clearSession();
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
        sqlUser.setUserId("2");
        sqlWorkspaceRepository.delete(workspace, sqlUser);
    }

    @Test
    public void testDeleteWhenUsersHaveCurrentWorkspaceReference() {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlUserRepository.setCurrentWorkspace(testUser.getUserId(), workspace.getWorkspaceId());
        sqlWorkspaceRepository.delete(workspace, testUser);

        sessionManager.getSession().refresh(testUser);
        assertNull(testUser.getCurrentWorkspaceId());
    }

    @Test
    public void testAdd() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace", testUser);
        assertEquals(testUser.getUserId(), workspace.getWorkspaceCreator().getUserId());
        assertEquals("test workspace", workspace.getDisplayTitle());
        assertEquals(1, workspace.getSqlWorkspaceUserList().size());

        SqlWorkspaceUser sqlWorkspaceUser = workspace.getSqlWorkspaceUserList().iterator().next();
        assertEquals(WorkspaceAccess.WRITE.toString(), sqlWorkspaceUser.getWorkspaceAccess());
        assertEquals(workspace.getWorkspaceId(), sqlWorkspaceUser.getWorkspace().getWorkspaceId());
    }

    @Test
    public void testFindById() throws Exception {
        SqlWorkspace workspace = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace", testUser);
        SqlWorkspace workspace2 = (SqlWorkspace) sqlWorkspaceRepository.add("test workspace 2", testUser);

        SqlWorkspace testWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById(workspace.getWorkspaceId(), testUser);
        SqlWorkspace testWorkspace2 = (SqlWorkspace) sqlWorkspaceRepository.findById(workspace2.getWorkspaceId(), testUser);

        assertEquals(workspace.getWorkspaceCreator().getUserId(), testWorkspace.getWorkspaceCreator().getUserId());
        assertEquals(workspace2.getWorkspaceCreator().getUserId(), testWorkspace2.getWorkspaceCreator().getUserId());
    }

    @Test
    public void testFindAll() throws Exception {
        Iterable<Workspace> userIterable = sqlWorkspaceRepository.findAllForUser(testUser);
        assertTrue(IterableUtils.count(userIterable) == 0);

        sqlWorkspaceRepository.add("test workspace 1", testUser);
        sqlWorkspaceRepository.add("test workspace 2", testUser);
        sqlWorkspaceRepository.add("test workspace 3", testUser);
        userIterable = sqlWorkspaceRepository.findAllForUser(testUser);
        assertTrue(IterableUtils.count(userIterable) == 3);
    }

    @Test
    public void testSetTitle() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        assertEquals("test", sqlWorkspace.getDisplayTitle());
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "changed title", testUser);
        SqlWorkspace modifiedWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById(sqlWorkspace.getWorkspaceId(), testUser);
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
        sqlUser1.setUserId("2");
        sqlWorkspaceRepository.setTitle(sqlWorkspace, "test", sqlUser1);
    }


    @Test
    public void testFindUsersWithAccess() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        List<WorkspaceUser> workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace.getWorkspaceId(), testUser);
        assertFalse(workspaceUsers.isEmpty());
        assertEquals(testUser.getUserId(), workspaceUsers.get(0).getUserId());
        assertTrue(workspaceUsers.get(0).isCreator());
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testFindUsersWithAccessWithoutUserPermissions() {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setUserId("2");
        sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace.getWorkspaceId(), sqlUser);
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, testUser.getUserId(), WorkspaceAccess.WRITE, testUser);
        List<WorkspaceUser> workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace.getWorkspaceId(), testUser);
        assertTrue(workspaceUsers.size() == 1);
        assertEquals(workspaceUsers.get(0).getWorkspaceAccess(), WorkspaceAccess.WRITE);

        SqlUser testUser2 = (SqlUser) sqlUserRepository.addUser("456", "qwe", null, "", new String[0]);
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.READ, testUser2);
        workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace.getWorkspaceId(), testUser2);
        assertTrue(workspaceUsers.size() == 2);
        assertEquals(workspaceUsers.get(1).getWorkspaceAccess(), WorkspaceAccess.READ);

        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "1", WorkspaceAccess.NONE, testUser);
        workspaceUsers = sqlWorkspaceRepository.findUsersWithAccess(sqlWorkspace.getWorkspaceId(), testUser);
        assertEquals(workspaceUsers.get(0).getWorkspaceAccess(), WorkspaceAccess.NONE);
    }

    @Test(expected = LumifyAccessDeniedException.class)
    public void testUpdateUserOnWorkspaceWithoutPermissions() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);

        SqlUser sqlUser = new SqlUser();
        sqlUser.setUserId("2");
        sqlWorkspaceRepository.updateUserOnWorkspace(sqlWorkspace, "2", WorkspaceAccess.WRITE, sqlUser);
    }

    @Test
    public void testSoftDeleteEntityFromWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        String vertexId = "1234";

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, true, new GraphPosition(0, 0), testUser);
        List<SqlWorkspaceVertex> sqlWorkspaceVertexSet = sqlWorkspaceRepository.getSqlWorkspaceVertices(sqlWorkspace);

        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        SqlWorkspaceVertex sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertTrue(sqlWorkspaceVertex.isVisible());

        sqlWorkspaceRepository.softDeleteEntitiesFromWorkspace(sqlWorkspace, Lists.newArrayList("1234"), testUser);
        sqlWorkspaceVertexSet = sqlWorkspaceRepository.getSqlWorkspaceVertices(sqlWorkspace);

        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertFalse(sqlWorkspaceVertex.isVisible());
    }

    @Test
    public void testUpdateEntityOnWorkspace() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        String vertexId = "1234";

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, true, new GraphPosition(0, 0), testUser);

        List<SqlWorkspaceVertex> sqlWorkspaceVertexSet = sqlWorkspaceRepository.getSqlWorkspaceVertices(sqlWorkspace);
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        SqlWorkspaceVertex sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertEquals("1234", sqlWorkspaceVertex.getVertexId());
        assertEquals(0, sqlWorkspaceVertex.getGraphPositionX().intValue());
        assertEquals(0, sqlWorkspaceVertex.getGraphPositionY().intValue());
        assertTrue(sqlWorkspaceVertex.isVisible());

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, vertexId, false, new GraphPosition(1, 10), testUser);

        sqlWorkspaceVertexSet = sqlWorkspaceRepository.getSqlWorkspaceVertices(sqlWorkspace);
        assertTrue(sqlWorkspaceVertexSet.size() == 1);
        sqlWorkspaceVertex = sqlWorkspaceVertexSet.iterator().next();
        assertEquals("1234", sqlWorkspaceVertex.getVertexId());
        assertEquals(1, sqlWorkspaceVertex.getGraphPositionX().intValue());
        assertEquals(10, sqlWorkspaceVertex.getGraphPositionY().intValue());
        assertFalse(sqlWorkspaceVertex.isVisible());
    }

    @Test
    public void testFindEntities() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "123", true, new GraphPosition(0, 0), testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "345", true, new GraphPosition(1, 0), testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "678", true, new GraphPosition(2, 0), testUser);
        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "910", true, new GraphPosition(3, 0), testUser);

        List<WorkspaceEntity> workspaceEntities = sqlWorkspaceRepository.findEntities(sqlWorkspace, testUser);
        assertTrue(workspaceEntities.size() == 4);
    }

    @Test
    public void testCopy() throws Exception {
        SqlWorkspace sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.add("test", testUser);
        assertTrue(IterableUtils.count(sqlWorkspaceRepository.findAllForUser(testUser)) == 1);

        sqlWorkspaceRepository.updateEntityOnWorkspace(sqlWorkspace, "123", true, new GraphPosition(0, 0), testUser);
        sqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.findById(sqlWorkspace.getWorkspaceId(), testUser);

        SqlWorkspace copySqlWorkspace = (SqlWorkspace) sqlWorkspaceRepository.copy(sqlWorkspace, testUser);
        assertTrue(IterableUtils.count(sqlWorkspaceRepository.findAllForUser(testUser)) == 2);
        assertEquals("Copy of test", copySqlWorkspace.getDisplayTitle());
        assertTrue(copySqlWorkspace.getSqlWorkspaceVertices().size() == 1);
    }
}
