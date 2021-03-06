package com.aerospike.investigation;

import com.aerospike.client.*;
import com.aerospike.client.Record;
import com.aerospike.client.policy.*;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    private static int NUMBER_OF_THREADS = 5;
    private static int NUMBER_OF_OPERATIONS_PER_THREAD = 1000;

    private static Random random = new Random(LocalDateTime.now().getNano() * LocalDateTime.now().getSecond());

    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        ExecutorService es = Executors.newCachedThreadPool();
        int n = NUMBER_OF_THREADS;
        while (n-- > 0) {
            es.execute(Main::runWorker);
        }
        es.shutdown();
        try {
            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("ops/sec: " + (NUMBER_OF_THREADS * NUMBER_OF_OPERATIONS_PER_THREAD) / ((endTime - startTime) / 1000));
    }

    private static boolean runWorker() {
        // Set client default policies
        ClientPolicy clientPolicy = new ClientPolicy();
        clientPolicy.readPolicyDefault.replica = Replica.MASTER;
        clientPolicy.readPolicyDefault.socketTimeout = 100;
        clientPolicy.readPolicyDefault.totalTimeout = 100;
        clientPolicy.writePolicyDefault.commitLevel = CommitLevel.COMMIT_ALL;
        clientPolicy.writePolicyDefault.socketTimeout = 500;
        clientPolicy.writePolicyDefault.totalTimeout = 500;

        // Connect to the cluster.
        AerospikeClient client = new AerospikeClient(clientPolicy, new Host("127.0.0.1", 3000));

        try {

            Key key = new Key("test", "demo", "key1");
            client.delete(null, key);

            // Create Empty
            Bin init_bin = new Bin("bin1", "");
            client.put(null, key, init_bin);

            int n = NUMBER_OF_OPERATIONS_PER_THREAD;
            while (n-- > 0) {

                // Fetch
                Record record_to_update = client.get(new Policy(), key, "bin1");

                String expected_value = randomBytes();

                // Update a key-value in
                Bin bin_to_update = new Bin("bin1", base64_encode(expected_value));
                client.put(null, key, bin_to_update);

                // Verify value is set to something
                Record record_to_check = client.get(null, key, "bin1");
                String actual_value_base64 = (String) record_to_check.getValue("bin1");

                // Is there a value?
                if (actual_value_base64 == null) {
                    System.out.println("\nNull value fetched, expected a value of length: " + expected_value.length() + " GENERATION INFO original:=" + record_to_update.generation + " updated:=" + record_to_check.generation);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.close();

        return true;
    }

    public static String randomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = random.nextInt(1024);

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public static String randomBytes() {
        int leftLimit = 0;
        int rightLimit = 255;
        int targetStringLength = 100 + random.nextInt(1024 - 100);

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

    public static String base64_encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    public static String base64_decode(String s) {
        return new String(Base64.getDecoder().decode(s));
    }
}