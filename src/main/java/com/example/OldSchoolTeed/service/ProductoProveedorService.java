package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProductoProveedorRequest;
import com.example.OldSchoolTeed.dto.ProductoProveedorResponse;
import com.example.OldSchoolTeed.dto.UpdatePrecioCostoRequest;

import java.util.List;

public interface ProductoProveedorService {

    ProductoProveedorResponse createAsignacion(ProductoProveedorRequest request);


    List<ProductoProveedorResponse> getAsignacionesPorProducto(Integer productoId);

    List<ProductoProveedorResponse> getAsignacionesPorProveedor(Integer proveedorId);


    ProductoProveedorResponse updatePrecioCosto(Integer asignacionId, UpdatePrecioCostoRequest request);

    void deleteAsignacion(Integer asignacionId);
}
