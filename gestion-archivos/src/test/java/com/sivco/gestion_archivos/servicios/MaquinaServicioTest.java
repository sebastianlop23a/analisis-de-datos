package com.sivco.gestion_archivos.servicios;

import com.sivco.gestion_archivos.modelos.Ensayo;
import com.sivco.gestion_archivos.modelos.Maquina;
import com.sivco.gestion_archivos.repositorios.EnsayoRepositorio;
import com.sivco.gestion_archivos.repositorios.MaquinaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaquinaServicioTest {

    @Mock
    private MaquinaRepositorio maquinaRepositorio;

    @Mock
    private EnsayoRepositorio ensayoRepositorio;

    @InjectMocks
    private MaquinaServicio maquinaServicio;

    @Test
    void shouldDeactivateMachineWhenItHasAssociatedTests() {
        Maquina maquina = new Maquina();
        maquina.setId(7L);
        maquina.setActiva(true);
        when(maquinaRepositorio.findById(7L)).thenReturn(Optional.of(maquina));
        when(ensayoRepositorio.findByMaquinaId(7L)).thenReturn(List.of(new Ensayo()));

        String mensaje = maquinaServicio.eliminarMaquina(7L);

        assertFalse(maquina.getActiva());
        assertEquals("La máquina se desactivó porque tiene ensayos asociados; el historial se conservó.", mensaje);
        verify(maquinaRepositorio).save(maquina);
        verify(maquinaRepositorio, never()).delete(maquina);
    }

    @Test
    void shouldDeleteMachineWhenItHasNoAssociatedTests() {
        Maquina maquina = new Maquina();
        maquina.setId(8L);
        when(maquinaRepositorio.findById(8L)).thenReturn(Optional.of(maquina));
        when(ensayoRepositorio.findByMaquinaId(8L)).thenReturn(List.of());

        String mensaje = maquinaServicio.eliminarMaquina(8L);

        assertEquals("Máquina eliminada.", mensaje);
        verify(maquinaRepositorio).delete(maquina);
        verify(maquinaRepositorio, never()).save(maquina);
    }
}