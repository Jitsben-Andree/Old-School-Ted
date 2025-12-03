package com.example.OldSchoolTeed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ProductoResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String talla;
    private BigDecimal precio;
    private Boolean activo;
    private String categoriaNombre;
    private Integer stock;

    private String imageUrl;


    private List<ImagenDto> galeriaImagenes;

    private String colorDorsal;
    private List<LeyendaDto> leyendas;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImagenDto {
        private Integer id;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeyendaDto {
        private Integer id;
        private String nombre;
        private String numero;
    }

    private BigDecimal precioOriginal;
    private BigDecimal descuentoAplicado;
    private String nombrePromocion;
    private List<PromocionSimpleDto> promocionesAsociadas;
}