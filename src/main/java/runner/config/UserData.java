package runner.config;

import java.io.Serializable;

public class UserData implements Serializable {
    private String connectionString;

    public UserData(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getConnectionString() {
        return connectionString;
    }
}
