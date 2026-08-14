package com.corgibalance.repositories;

import java.util.List;

public interface CrudRepository<T> {

    List<T> findAll();

    T create(T entity);

    void update(T entity);

    void delete(T entity);
}
