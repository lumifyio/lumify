package com.altamiracorp.lumify.web.routes;

import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.web.AuthenticationProvider;
import com.altamiracorp.lumify.web.WebApp;
import com.altamiracorp.miniweb.HandlerChain;
import org.mockito.Mockito;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.when;

public abstract class RouteTestBase {
    public HttpServletRequest mockRequest;
    public HttpServletResponse mockResponse;
    public HandlerChain mockHandlerChain;
    public WebApp mockApp;
    public StringWriter responseStringWriter;
    public ServletOutputStream mockResponseOutputStream;

    public TermMentionRepository mockTermMentionRepository;
    public User mockUser;
    public HttpSession mockHttpSession;

    public void setUp() throws Exception {
        responseStringWriter = new StringWriter();
        mockResponseOutputStream = Mockito.mock(ServletOutputStream.class);

        mockApp = Mockito.mock(WebApp.class);
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        mockHandlerChain = Mockito.mock(HandlerChain.class);

        mockTermMentionRepository = Mockito.mock(TermMentionRepository.class);

        mockUser = Mockito.mock(User.class);
        mockHttpSession = Mockito.mock(HttpSession.class);

        //request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
        when(mockRequest.getScheme()).thenReturn("http");
        when(mockRequest.getServerName()).thenReturn("testServerName");
        when(mockRequest.getServerPort()).thenReturn(80);

        when(mockResponse.getWriter()).thenReturn(new PrintWriter(responseStringWriter));
        when(mockResponse.getOutputStream()).thenReturn(mockResponseOutputStream);

        when(mockRequest.getSession()).thenReturn(mockHttpSession);
        when(AuthenticationProvider.getUser(mockHttpSession)).thenReturn(mockUser);
    }
}
