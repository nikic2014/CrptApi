package org.example;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class Test {
    private static Gson gson = new Gson();
    OkHttpClient client = new OkHttpClient();
    private final Lock lock = new ReentrantLock();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private ConcurrentLinkedDeque<Long> queueInProgress = new ConcurrentLinkedDeque<>();
    private long lastTime = 0;

    private final TimeUnit timeUnit;
    private final int requestLimit;

    public void createDocument(String documentJson, String signature) throws IOException {
        waitForPermission();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(documentJson, JSON);
        Request request = new Request.Builder()
                .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();

    }

    private void waitForPermission() {
        lock.lock();

        try {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTime;

            while (requestCount.get() >= requestLimit && timeUnit.toMillis(1) > elapsedTime) {
                currentTime = System.currentTimeMillis();
                lastTime = !queueInProgress.isEmpty() ? queueInProgress.getFirst() : lastTime;
                elapsedTime = currentTime - lastTime;
            }

            while (!queueInProgress.isEmpty() && timeUnit.toMillis(1) < elapsedTime) {
                currentTime = System.currentTimeMillis();
                lastTime = !queueInProgress.isEmpty() ? queueInProgress.getFirst() : lastTime;
                elapsedTime = currentTime - lastTime;

                requestCount.decrementAndGet();
                System.out.println(queueInProgress.getFirst()/1000);
                queueInProgress.removeFirst();
            }

            if (requestCount.get() < requestLimit) {
                currentTime = System.currentTimeMillis();

                queueInProgress.add(currentTime);
                requestCount.incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    @AllArgsConstructor
    static
    class Description {
        private String participantInn;
    }

    @AllArgsConstructor
    static
    class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;
    }
    @AllArgsConstructor
    static
    class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private java.util.List<Product> products = new java.util.ArrayList<>();
        private String reg_date;
        private String reg_number;
    }

    public static void main(String[] args) throws IOException {
        Product product = new Product("string","2020-01-23","string","string","string","2020-01-23","string","string",
                "string");
        Description description = new Description("1234567890");
        List<Product> products = new ArrayList<>();
        products.add(product);
        Document document = new Document(description, "123", "status", "LP_INTRODUCE_GOODS", true, "string", "string", "string"
                , "2020-01-23", "string", products, "2020-01-23", "string");
        String documentJsongson = gson.toJson(document);

        Test test = new Test(TimeUnit.MINUTES, 10);
        for (int i = 0; i < 25; i++) {
            test.createDocument(documentJsongson, "signature");
        }
    }
}
