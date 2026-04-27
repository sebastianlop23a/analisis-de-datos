package com.sivco.gestion_archivos.servicios.calibration;

import com.sivco.gestion_archivos.modelos.calibration.CalibrationPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests focused on CSV parsing behavior of {@link CalibrationManagementService}.
 */
class CalibrationManagementServiceTest {

    private CalibrationManagementService service = new CalibrationManagementService();

    @Test
    @DisplayName("Parser handles semicolon-delimited file and comma decimals")
    void testParseCsvWithSemicolonAndCommaDecimal() throws Exception {
        String csv = "patron_reading;instrument_reading\n" +
                     "1,5;2,5\n" +
                     "3,0;4,0\n";
        InputStream in = new ByteArrayInputStream(csv.getBytes());

        Method parser = CalibrationManagementService.class
                .getDeclaredMethod("parseCalibrationCSV", InputStream.class);
        parser.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<CalibrationPoint> points = (List<CalibrationPoint>) parser.invoke(service, in);

        assertEquals(2, points.size(), "Should parse two calibration points");
        assertEquals(1.5, points.get(0).getPatronReading(), 1e-6);
        assertEquals(2.5, points.get(0).getInstrumentReading(), 1e-6);
        assertEquals(3.0, points.get(1).getPatronReading(), 1e-6);
        assertEquals(4.0, points.get(1).getInstrumentReading(), 1e-6);
    }

    @Test
    @DisplayName("Parser throws when less than two valid numeric rows")
    void testParserInsufficientRows() throws Exception {
        String csv = "patron,instrument\n" +
                     "header-only\n" +
                     "5;notANumber\n";
        InputStream in = new ByteArrayInputStream(csv.getBytes());

        Method parser = CalibrationManagementService.class
                .getDeclaredMethod("parseCalibrationCSV", InputStream.class);
        parser.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<CalibrationPoint> points = (List<CalibrationPoint>) parser.invoke(service, in);

        // even though header exists, the parser should return an empty list
        assertEquals(0, points.size());

        // the uploadAndProcessCalibration call should then throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                service.uploadAndProcessCalibration(1L,
                        new org.springframework.mock.web.MockMultipartFile("file", "f.csv", "text/plain", csv.getBytes()),
                        RegressionModelType.LINEAR, null, null));
    }
}