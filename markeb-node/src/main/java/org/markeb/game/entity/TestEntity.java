package org.markeb.game.entity;

import lombok.Data;
import org.markeb.persistent.annotation.PersistentEntity;
import org.markeb.persistent.entity.Identifiable;

/**
 * Test Entity for storage module testing
 */
@Data
@PersistentEntity(collection = "test_entity", cacheTtl = 300)
public class TestEntity implements Identifiable<Integer> {

    /**
     * Entity ID
     */
    private Integer id;

    /**
     * Name field
     */
    private String name;

    /**
     * Creation timestamp
     */
    private long createTime;

    /**
     * Update timestamp
     */
    private long updateTime;

    public TestEntity() {
    }

    public TestEntity(Integer id, String name) {
        this.id = id;
        this.name = name;
        this.createTime = System.currentTimeMillis();
        this.updateTime = this.createTime;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}

