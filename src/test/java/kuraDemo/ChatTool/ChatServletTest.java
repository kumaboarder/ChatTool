package kuraDemo.ChatTool;

import kuraDemo.entity.MessageEntity;
import net.arnx.jsonic.JSON;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServletTest {

    @InjectMocks
    private ChatServlet chatServlet;

    @Mock
    private Session mockSession1;

    @Mock
    private Session mockSession2;

    @Mock
    private RemoteEndpoint.Async mockAsyncRemote1;
    
    @Mock
    private RemoteEndpoint.Async mockAsyncRemote2;

    private static Field sessionsField;
    private static Set<Session> originalSessionsSet; // Renamed for clarity
    private static Field loggerField;
    private static Logger originalLogger;


    @BeforeEach
    void setUp() throws Exception {
        // Access and store original static 'ses' field
        sessionsField = ChatServlet.class.getDeclaredField("ses");
        sessionsField.setAccessible(true);
        originalSessionsSet = (Set<Session>) sessionsField.get(null);
        
        // Replace 'ses' with a new empty set for each test for isolation
        sessionsField.set(null, new CopyOnWriteArraySet<Session>());

        // Access and store original static 'log' field
        loggerField = ChatServlet.class.getDeclaredField("log");
        loggerField.setAccessible(true);
        originalLogger = (Logger) loggerField.get(null);

        // Setup mock behavior for sessions (used by multiple tests)
        lenient().when(mockSession1.getAsyncRemote()).thenReturn(mockAsyncRemote1);
        lenient().when(mockSession2.getAsyncRemote()).thenReturn(mockAsyncRemote2);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // Restore original static 'ses' field
        if (sessionsField != null && originalSessionsSet != null) {
            sessionsField.set(null, originalSessionsSet);
        }
        // Restore original static 'log' field
        if (loggerField != null && originalLogger != null) {
            loggerField.set(null, originalLogger);
        }
    }


    @Test
    void onOpen_addsSession() throws Exception {
        chatServlet.onOpen(mockSession1);
        Set<Session> currentSessions = (Set<Session>) sessionsField.get(null);
        assertTrue(currentSessions.contains(mockSession1), "Session should be added on open.");
        assertEquals(1, currentSessions.size());
    }

    @Test
    void onClose_removesSession() throws Exception {
        Set<Session> currentSessions = (Set<Session>) sessionsField.get(null);
        currentSessions.add(mockSession1); // Pre-add session
        
        chatServlet.onClose(mockSession1);
        assertFalse(currentSessions.contains(mockSession1), "Session should be removed on close.");
        assertTrue(currentSessions.isEmpty());
    }
    
    @Test
    void onMessage_validJson_sendsMessage() {
        MessageEntity testEntity = new MessageEntity();
        testEntity.setUser("testUser");
        testEntity.setMessage("Hello");
        String jsonMessage = JSON.encode(testEntity); // Use real JSONIC to encode for the test input

        // Mock static JSON.decode
        try (MockedStatic<JSON> mockedJson = Mockito.mockStatic(JSON.class)) {
            mockedJson.when(() -> JSON.decode(jsonMessage, MessageEntity.class)).thenReturn(testEntity);
            // Mock static JSON.encode used by sendMessage
            List<MessageEntity> expectedList = new ArrayList<>();
            expectedList.add(testEntity);
            mockedJson.when(() -> JSON.encode(expectedList)).thenReturn(jsonMessage);


            // Add a session to send message to
            chatServlet.onOpen(mockSession1);
            
            chatServlet.onMessage(jsonMessage);

            // Verify sendMessage logic (indirectly, that sendText was called)
            verify(mockAsyncRemote1).sendText(jsonMessage);
        }
    }
    
    @Test
    void onMessage_invalidJson_logsError() throws Exception {
        String invalidJsonMessage = "{\"user\":\"testUser\", \"message\":"; // Malformed
        Logger mockLogger = mock(Logger.class);
        loggerField.set(null, mockLogger); // Set the static log field to our mock

        try (MockedStatic<JSON> mockedJson = Mockito.mockStatic(JSON.class)) {
            // Make JSON.decode throw an exception for invalid JSON
            RuntimeException jsonException = new RuntimeException("Simulated JSON decode error");
            mockedJson.when(() -> JSON.decode(invalidJsonMessage, MessageEntity.class)).thenThrow(jsonException);

            chatServlet.onMessage(invalidJsonMessage);

            // Verify error was logged using the mocked static logger
            verify(mockLogger).error(jsonException.getMessage(), jsonException);
            // Verify no message was sent out
            verify(mockAsyncRemote1, never()).sendText(anyString());
        } finally {
             loggerField.set(null, originalLogger); // Restore original logger
        }
    }

    @Test
    void sendMessage_noClients() {
         // 'ses' is already empty at the start of this test due to setup
        MessageEntity msg = new MessageEntity();
        msg.setMessage("Test");
        List<MessageEntity> messageList = new ArrayList<>();
        messageList.add(msg);

        String jsonEncodedList = JSON.encode(messageList);
        try (MockedStatic<JSON> mockedJson = Mockito.mockStatic(JSON.class)) {
            mockedJson.when(() -> JSON.encode(messageList)).thenReturn(jsonEncodedList);
            
            ChatServlet.sendMessage(messageList); // Static method call
        }
        // No exceptions expected, and no sendText calls
        verify(mockAsyncRemote1, never()).sendText(anyString());
        verify(mockAsyncRemote2, never()).sendText(anyString());
    }

    @Test
    void sendMessage_oneClient() throws Exception {
        ((Set<Session>) sessionsField.get(null)).add(mockSession1);

        MessageEntity msg = new MessageEntity();
        msg.setMessage("Test");
        List<MessageEntity> messageList = new ArrayList<>();
        messageList.add(msg);
        String jsonEncodedList = JSON.encode(messageList);

        try (MockedStatic<JSON> mockedJson = Mockito.mockStatic(JSON.class)) {
            mockedJson.when(() -> JSON.encode(messageList)).thenReturn(jsonEncodedList);
            
            ChatServlet.sendMessage(messageList);
        }

        verify(mockAsyncRemote1).sendText(jsonEncodedList);
        verify(mockAsyncRemote2, never()).sendText(anyString());
    }

    @Test
    void sendMessage_multipleClients() throws Exception {
        Set<Session> currentSessions = (Set<Session>) sessionsField.get(null);
        currentSessions.add(mockSession1);
        currentSessions.add(mockSession2);

        MessageEntity msg = new MessageEntity();
        msg.setMessage("Broadcast");
        List<MessageEntity> messageList = new ArrayList<>();
        messageList.add(msg);
        String jsonEncodedList = JSON.encode(messageList);
        
        try (MockedStatic<JSON> mockedJson = Mockito.mockStatic(JSON.class)) {
            mockedJson.when(() -> JSON.encode(messageList)).thenReturn(jsonEncodedList);

            ChatServlet.sendMessage(messageList);
        }

        verify(mockAsyncRemote1).sendText(jsonEncodedList);
        verify(mockAsyncRemote2).sendText(jsonEncodedList);
    }
}
