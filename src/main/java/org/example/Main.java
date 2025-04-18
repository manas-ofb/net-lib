package org.example;


public class Main {
    public static void main(String[] args) {

        CachedHttpClient client = new CachedHttpClient();
        String testUrl = "https://jsonplaceholder.typicode.com/posts/1";

        for (int i = 0; i < 10; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    String response = client.get(testUrl).get();
                    System.out.println("Thread " + id + ": " +
                        response.substring(0, Math.min(60, response.length())) + "...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}