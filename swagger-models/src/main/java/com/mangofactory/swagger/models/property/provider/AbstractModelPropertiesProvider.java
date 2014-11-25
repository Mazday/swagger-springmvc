package com.mangofactory.swagger.models.property.provider;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.Arrays;

public abstract class AbstractModelPropertiesProvider implements ModelPropertiesProvider {

  private ObjectMapper objectMapper;

  protected SerializationConfig getSerializationConfigWithViews(JsonView views) {
    SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
    if (null != views && views.value().length > 0) {
      for (Class<?> cl: views.value()) {
        serializationConfig = serializationConfig.withView(cl);
      }
    }
    return serializationConfig;
  }

  protected DeserializationConfig getDeserializationConfigWithViews(JsonView views) {
    DeserializationConfig serializationConfig = objectMapper.getDeserializationConfig();
    if (null != views && views.value().length > 0) {
      for (Class<?> cl: views.value()) {
        serializationConfig = serializationConfig.withView(cl);
      }
    }
    return serializationConfig;
  }

  public boolean isAssignableFromViews(Class<?>[] foundView, Class<?>[] view) {
    return Iterables.all(Arrays.asList(foundView), assignableFormViews(view));
  }

  private Predicate<Class<?>> assignableFormViews(final Class<?>[] views) {
    return new Predicate<Class<?>>() {
      @Override
      public boolean apply(Class<?> clazz) {
        for (Class<?> view: views) {
          if (clazz.isAssignableFrom(view)) {
            return true;
          }
        }
        return false;
      }
    };
  }
  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

}
