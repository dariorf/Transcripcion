import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;

public class Principal {

    public static final String TOKEN = "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";

    public static void main(String[] args) {
        File audio = new File("/home/dariorf/Descargas/quothello-therequot-158832.mp3");
        System.out.println(transcribir(audio));
    }

    public static String transcribir(File audio) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(
                            new InetSocketAddress("192.168.0.11", 3128)
                    ))
                    .build();

            HttpEntity httpEntity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", audio, ContentType.create("audio/mpeg"), audio.getName())
                    .addTextBody("model", "whisper-1")
                    .build();

            Pipe pipe = Pipe.open();

            // Pipeline streams must be used in a multi-threaded environment. Using one
            // thread for simultaneous reads and writes can lead to deadlocks.
            new Thread(() -> {
                try (OutputStream outputStream = Channels.newOutputStream(pipe.sink())) {
                    // Write the encoded data to the pipeline.
                    httpEntity.writeTo(outputStream);
                } catch (IOException e) {
                }
            }).start();

            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/audio/transcriptions"))
                    .header("Authorization", "Bearer " + TOKEN)
                    .header("Content-Type", httpEntity.getContentType().getValue())
                    .POST(HttpRequest.BodyPublishers.ofInputStream(() -> Channels.newInputStream(pipe.source())))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, BodyHandlers.ofString(StandardCharsets.UTF_8));

            return response.body();
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }
    }
}
