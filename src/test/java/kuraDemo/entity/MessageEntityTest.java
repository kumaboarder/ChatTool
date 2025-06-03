package kuraDemo.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MessageEntityTest {

    @Test
    void testToString() {
        MessageEntity entity = new MessageEntity();
        entity.setUser("testUser");
        entity.setMessage("Hello World");
        entity.setDate("2023-10-27");
        entity.setCommand("post");

        String expected = "MessageEntity [" +
                          "message=Hello World" +
                          ", user=testUser" +
                          ", date=2023-10-27" +
                          ", command=post" +
                          "]";
        assertEquals(expected, entity.toString());
    }

    @Test
    void testGettersAndSetters() {
        MessageEntity entity = new MessageEntity();

        entity.setUser("anotherUser");
        assertEquals("anotherUser", entity.getUser());

        entity.setMessage("Test Message");
        assertEquals("Test Message", entity.getMessage());

        entity.setDate("2023-10-28");
        assertEquals("2023-10-28", entity.getDate());

        entity.setCommand("status");
        assertEquals("status", entity.getCommand());
    }
}
