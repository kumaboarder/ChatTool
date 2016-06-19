package kuraDemo.ChatTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import kuraDemo.entity.MessageEntity;
import net.arnx.jsonic.JSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint(value = "/chat")
public class ChatServlet{

	private static Set<Session> ses = new CopyOnWriteArraySet<>();
	static Logger log = LoggerFactory.getLogger(ChatServlet.class);

	String user = "default User";

	@OnOpen
	public void onOpen(Session session) {
		log.debug("onOpen : " + session);
		ses.add(session);
	}

	@OnMessage
	public void onMessage(String message) {
		log.debug("onMessage : " + message);
		try{
			MessageEntity msgEntity = JSON.decode(message, MessageEntity.class);
			this.setUser(msgEntity.getUser());

			sendMessage(createMessage(message));

		}catch(Exception e){
			log.error(e.getMessage(), e);
		}
	}

	@OnClose
	public void onClose(Session session) {
		log.debug("onClose : " + session);
		ses.remove(session);
	}

	public static void sendMessage(List<MessageEntity> messageList) {
		log.debug("sendMessage : " + messageList);
		String retMessage = JSON.encode(messageList);

		for (Session ses : ses) {
			ses.getAsyncRemote().sendText(retMessage);
		}
	}

	private List<MessageEntity> createMessage(String msg){
		List<MessageEntity> ret = new ArrayList<MessageEntity>();
		MessageEntity msgEntity = JSON.decode(msg, MessageEntity.class);
		ret.add(msgEntity);
		return ret;
	}

	private void setUser(String userName){
		this.user = userName;
	}
}