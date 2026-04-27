package com.sivco.gestion_archivos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = "com.sivco.gestion_archivos.modelos")
public class GestionArchivosApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionArchivosApplication.class, args);
    }

}
