package com.example.OldSchoolTeed.service;

import com.example.OldSchoolTeed.dto.ProveedorRequest;
import com.example.OldSchoolTeed.dto.ProveedorResponse;

import java.util.List;

public interface ProveedorService {


     //[Admin] Crea un nuevo proveedor.
    ProveedorResponse createProveedor(ProveedorRequest request);

     //[Admin] Obtiene un proveedor por su ID.
    ProveedorResponse getProveedorById(Integer id);


     // [Admin] Obtiene una lista de todos los proveedores.
    List<ProveedorResponse> getAllProveedores();


   // [Admin] Actualiza un proveedor existente.
    ProveedorResponse updateProveedor(Integer id, ProveedorRequest request);


     //[Admin] Elimina un proveedor.
    void deleteProveedor(Integer id);
}