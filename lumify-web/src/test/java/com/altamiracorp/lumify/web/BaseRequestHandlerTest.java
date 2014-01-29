package com.altamiracorp.lumify.web;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaseRequestHandlerTest {

    private static final String REPLY = "Reply";
    private static final JSONArray JSON_ARRAY = new JSONArray();
    private static final JSONObject JSON_OBJECT = new JSONObject();

    private static final String TEST_PARAM = "foo";
    private static final String TEST_PARAM_VALUE = "1";

    @Mock(answer=Answers.CALLS_REAL_METHODS)
    private BaseRequestHandler mock;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private PrintWriter writer;

    @Test(expected = NullPointerException.class)
    public void testRequiredParameterInvalidRequest() {
        mock.getRequiredParameter(null, TEST_PARAM);
    }

    @Test(expected = RuntimeException.class)
    public void testRequiredParameterInvalidParameter() {
        mock.getRequiredParameter(request, null);
    }

    @Test(expected = RuntimeException.class)
    public void testRequiredParameterEmptyParameter() {
        mock.getRequiredParameter(request, "");
    }

    @Test
    public void testRequiredParameter() {
        when(request.getParameter(TEST_PARAM)).thenReturn(TEST_PARAM_VALUE);
        assertEquals(TEST_PARAM_VALUE, mock.getRequiredParameter(request, TEST_PARAM));
        verify(request, times(1)).getParameter(eq(TEST_PARAM));
    }


    @Test
    public void testRequiredParameterAsLong() {
        when(request.getParameter(TEST_PARAM)).thenReturn(TEST_PARAM_VALUE);
        assertEquals(Long.parseLong(TEST_PARAM_VALUE), mock.getRequiredParameterAsLong(request, TEST_PARAM));
        verify(request, times(1)).getParameter(eq(TEST_PARAM));
    }

    @Test
    public void testRequiredParameterAsDouble() {
        when(request.getParameter(TEST_PARAM)).thenReturn(TEST_PARAM_VALUE);
        assertEquals(Double.parseDouble(TEST_PARAM_VALUE), mock.getRequiredParameterAsDouble(request, TEST_PARAM), 0.001);
        verify(request, times(1)).getParameter(eq(TEST_PARAM));
    }

    @Test(expected = NullPointerException.class)
    public void testOptionalParameterInvalidRequest() {
        mock.getOptionalParameter(null, TEST_PARAM);
    }

    @Test
    public void testOptionalParameterFound() {
        when(request.getParameter(TEST_PARAM)).thenReturn(TEST_PARAM_VALUE);
        assertEquals(TEST_PARAM_VALUE, mock.getOptionalParameter(request, TEST_PARAM));
        verify(request, times(1)).getParameter(eq(TEST_PARAM));
    }

    @Test
    public void testOptionalParameterNotFound() {
        when(request.getParameter(TEST_PARAM)).thenReturn(null);
        assertEquals(null, mock.getOptionalParameter(request, TEST_PARAM));
        verify(request, times(1)).getParameter(eq(TEST_PARAM));
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithJsonObjectInvalidResponse() {
        mock.respondWithJson(null, JSON_OBJECT);
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithJsonObjectInvalidJsonObject() {
        mock.respondWithJson(response, (JSONObject)null);
    }

    @Test
    public void testRespondWithJsonObject() throws IOException {
        when(response.getWriter()).thenReturn(writer);
        mock.respondWithJson(response, JSON_OBJECT);
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithJsonArrayInvalidResponse() {
        mock.respondWithJson(null, JSON_ARRAY);
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithJsonArrayInvalidJsonArray() {
        mock.respondWithJson(response, (JSONArray)null);
    }

    @Test
    public void testRespondWithJsonArray() throws IOException {
        when(response.getWriter()).thenReturn(writer);
        mock.respondWithJson(response, JSON_ARRAY);
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithPlaintextInvalidResponse() {
        mock.respondWithPlaintext(null, REPLY);
    }

    @Test(expected = NullPointerException.class)
    public void testRespondWithPlaintextInvalidPlaintext() {
        mock.respondWithPlaintext(response, (String)null);
    }

    @Test
    public void testRespondWithPlaintext() throws IOException {
        when(response.getWriter()).thenReturn(writer);
        mock.respondWithPlaintext(response, REPLY);
    }

    @Test(expected = RuntimeException.class)
    public void testRespondWithJsonWriterException() throws IOException {
        when(response.getWriter()).thenThrow(new IOException());
        mock.respondWithJson(response, JSON_OBJECT);
    }
}
