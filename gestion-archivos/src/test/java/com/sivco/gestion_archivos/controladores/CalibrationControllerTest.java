package com.sivco.gestion_archivos.controladores;

import com.sivco.gestion_archivos.servicios.calibration.CalibrationManagementService;
import com.sivco.gestion_archivos.modelos.calibration.RegressionModelType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic controller tests for the calibration REST endpoints.
 */
@WebMvcTest(CalibrationController.class)
class CalibrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CalibrationManagementService calibrationManagementService;

    @Test
    @DisplayName("Uploading a file with insufficient points returns 400 and message")
    void uploadInsufficientPoints() throws Exception {
        // simulate service throwing the validation exception
        when(calibrationManagementService.uploadAndProcessCalibration(anyLong(), any(), any(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Se requieren al menos 2 puntos de calibración para calcular un modelo de regresión"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "cal.csv", MediaType.TEXT_PLAIN_VALUE,
                "10,20\n".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/calibrations/upload/1")
                        .file(file)
                        .param("modelType", RegressionModelType.LINEAR.name()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Se requieren al menos 2 puntos de calibración para calcular un modelo de regresión"));
    }
}