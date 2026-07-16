/*
 * Create chat - done
 * Send message in chat - done
 * Get notification of message - done
 *
 *
 * Singleton
 * Observer for Notification
 * Content strategy
 * */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatApplication {
    public static void main() {
        ChatApp chatApp = ChatApp.getInstance();

        ChatUser u1 = chatApp.registerUser(1);
        ChatUser u2 = chatApp.registerUser(2);
        ChatUser u3 = chatApp.registerUser(3);
        ChatUser u4 = chatApp.registerUser(4);

        Chat c1 = chatApp.createChat(1, List.of(u1, u2, u3));
        Chat c2 = chatApp.createChat(2, List.of(u1, u2, u4));

        // send message
        chatApp.sendMessage(c1, u1, new TextContent("Hello everyone"));
        System.out.println();

        chatApp.sendMessage(c2, u2, new TextContent("Hi everyone"));
        System.out.println();

        // notifications received by each user
        HashMap<Integer, ChatUser> users = chatApp.users;
        for (ChatUser chatUser: users.values()) {
            System.out.println("Notifications of user: " + chatUser.userId);
            List<Notification> notifications = chatUser.notifications;
            for (Notification notification: notifications) {
                notification.msg.content.display();
            }
            System.out.println();
        }
    }
}

class ChatApp {
    public static ChatApp chatApp;
    HashMap<Integer, Chat> chats;
    HashMap<Integer, ChatUser> users;

    private ChatApp() {
        this.chats = new HashMap<>();
        this.users = new HashMap<>();
    }

    public static ChatApp getInstance() {
        if (chatApp == null) {
            chatApp = new ChatApp();
        }
        return  chatApp;
    }

    public ChatUser registerUser(int userId) {
        if (users.containsKey(userId)) return null;

        ChatUser u = new ChatUser(userId);
        users.put(u.userId, u);
        return u;
    }

    public Chat createChat(int chatId, List<ChatUser> users) {
        if (chats.containsKey(chatId)) return null;

        Chat c = new Chat(1, users);
        chats.put(chatId, c);
        return c;
    }

    public WhatsAppMessage sendMessage(Chat c, ChatUser u, ContentStrategy content) {
        Chat chat = this.chats.get(c.chatId);
        ChatUser user = this.users.get(u.userId);

        WhatsAppMessage msg = new WhatsAppMessage(content, user.userId, chat.chatId);
        msg.content.display();
        chat.sendMessage(msg);
        return msg;
    }

}

class Chat {
    int chatId;
    HashMap<Integer, ChatUser> participants;
    List<WhatsAppMessage> messages;

    public Chat(int chatId, List<ChatUser> users) {
        this.chatId = chatId;
        participants = new HashMap<>();
        for (ChatUser u : users) {
            participants.put(u.userId, u);
        }

        messages = new ArrayList<>();
    }

    public void sendMessage(WhatsAppMessage msg) {
        messages.add(msg);
        System.out.println("Sending notification to users");
        for (ChatUser user : participants.values()) {
            if (user.userId != msg.senderId) {
                user.sendNotification(this.chatId, msg);
            }
        }
    }
}

interface ContentStrategy {
    public void display();
}

class TextContent implements ContentStrategy {
    String text;
    public TextContent(String text) {
        this.text = text;
    }

    public void display() {
        System.out.println("This is a text message: " + text);
    }
}

class ImageContent implements ContentStrategy {
    String path;
    public ImageContent(String path) {
        this.path = path;
    }

    public void display() {
        System.out.println("This is an image message having path: " + path);
    }
}

class WhatsAppMessage {
    ContentStrategy content;
    int senderId;
    int chatId;

    public WhatsAppMessage(ContentStrategy content, int userId, int chatId) {
        this.content = content;
        this.senderId = userId;
        this.chatId = chatId;
    }
}

class ChatUser {
    int userId;
    List<Notification> notifications;

    public ChatUser(int userId) {
        this.userId = userId;
        notifications = new ArrayList<>();
    }

    public void sendNotification(int chatId, WhatsAppMessage msg) {
        notifications.add(new Notification(chatId, msg));
        System.out.println("Notification received by user " + userId);
    }
}

class Notification {
    int chatId;
    WhatsAppMessage msg;

    public Notification(int chatId, WhatsAppMessage msg) {
        this.chatId = chatId;
        this.msg = msg;
    }
}