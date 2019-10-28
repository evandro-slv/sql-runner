package runner.config;

import java.io.*;

public class UserDataRepository {
    private static String FILENAME = "UserData";

    public void save(UserData userData) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILENAME))) {
            oos.writeObject(userData);
        }
    }

    public UserData read() throws ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILENAME))) {
            return (UserData) ois.readObject();
        } catch(IOException e) {
            return null;
        }
    }
}
