import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        String config = "src/main/resources/app.config.my";
        Service myService = new Service(config);
        myService.start();
    }
}
