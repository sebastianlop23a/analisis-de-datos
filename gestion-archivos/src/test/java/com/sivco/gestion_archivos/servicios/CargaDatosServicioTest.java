package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.DatoEnsayoTemporal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class CargaDatosServicioTest {

    @Autowired
    private CargaDatosServicio cargaDatosServicio;

    @Test
    void parsesCsvDataAndPreservesTimestamps() throws Exception {
        String csv = "Fecha,Hora,T1,T2\n"
                + "2026-10-18,10:00:00,20.5,21.5\n"
                + "2026-10-18,10:05:00,20.6,21.6\n";
        MockMultipartFile file = new MockMultipartFile(
                "archivo", "datos.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        List<DatoEnsayoTemporal> datos = cargaDatosServicio.cargarDatosCSV(file, 1L);

        assertEquals(4, datos.size());
        assertEquals(20.5, datos.get(0).getValor());
        assertEquals(21.5, datos.get(1).getValor());
        assertEquals(LocalDateTime.of(2026, 10, 18, 10, 0), datos.get(0).getTimestamp());
        assertEquals(0, datos.get(0).getNumeroSecuencia());
        assertFalse(datos.get(0).getAnormal());
    }
}
