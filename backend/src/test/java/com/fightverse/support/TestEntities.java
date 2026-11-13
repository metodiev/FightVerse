package com.fightverse.support;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * Entities receive their identifier from JPA. Unit tests assign one explicitly so ownership checks
 * (which dereference the id) can be exercised without a persistence context.
 */
public final class TestEntities {

  private TestEntities() {}

  public static <T> T assignId(T entity, UUID id) {
    try {
      Field field = entity.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(entity, id);
      return entity;
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("cannot assign id to " + entity.getClass().getName(), ex);
    }
  }
}
