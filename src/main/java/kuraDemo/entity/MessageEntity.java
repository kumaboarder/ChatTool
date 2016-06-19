package kuraDemo.entity;

public class MessageEntity {
    private String message;
    private String user;
    private String date;
    private String command = "";
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	@Override
	public String toString() {
		return "MessageEntity [message=" + message + ", user=" + user
				+ ", date=" + date + ", command=" + command + "]";
	}
}
