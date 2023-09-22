package com.example.hsa_22_sharding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Hsa22ShardingApplication implements CommandLineRunner {



    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Hsa22ShardingApplication.class, args);
    }

    @Override
    public void run(String... args) {
        dbTest();
    }

    void dbTest() {
        var arr = new Customer[10_000];
        boolean c = true;
        for (int i = 0; i < 10000; i++) {
            arr[i] = new Customer(i, c ? 1 : 2, "FirstName", "LastName");
            c = !c;
        }

        System.out.println("Hii ");

        var writeStart = Instant.now().toEpochMilli();
        for(var a : arr)
            insert(a);
        System.out.println("Write time: " + (Instant.now().toEpochMilli() - writeStart));

        var readStart = Instant.now().toEpochMilli();
        get();
        System.out.println("Read time: " + (Instant.now().toEpochMilli() - readStart));
    }

    void insert(Customer a) {
        jdbcTemplate.update("INSERT INTO customer(id, category, first_name, last_name) VALUES (?, ?, ?, ?)",
                a.id(), a.category(), a.firstName(), a.lastName());
    }

    List<Map<String, Object>> get() {
        return jdbcTemplate.queryForList("SELECT  * FROM customer");
    }
}
