package com.example.OldSchoolTeed.repository;

import com.example.OldSchoolTeed.entities.Pedido;
import com.example.OldSchoolTeed.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    //metodo para buscar todos los pedidos de una usuario
    List<Pedido> findAllByUsuario(Usuario usuario);
}
