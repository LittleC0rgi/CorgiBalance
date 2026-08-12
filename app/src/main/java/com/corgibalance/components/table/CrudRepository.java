package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;

import java.util.List;

public interface CrudRepository<T extends BaseModel> {

    List<T> findAll();

    T create(T entity);

    void update(T entity);

    void delete(T entity);
}
