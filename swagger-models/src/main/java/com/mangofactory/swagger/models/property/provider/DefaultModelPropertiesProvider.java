package com.mangofactory.swagger.models.property.provider;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.mangofactory.swagger.models.property.ModelProperty;
import com.mangofactory.swagger.models.property.bean.BeanModelPropertyProvider;
import com.mangofactory.swagger.models.property.constructor.ConstructorModelPropertyProvider;
import com.mangofactory.swagger.models.property.field.FieldModelPropertyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component(value = "default")
public class DefaultModelPropertiesProvider implements ModelPropertiesProvider {

  private final FieldModelPropertyProvider fieldModelPropertyProvider;
  private final BeanModelPropertyProvider beanModelPropertyProvider;
  private final ConstructorModelPropertyProvider constructorModelPropertyProvider;

  @Autowired
  public DefaultModelPropertiesProvider(BeanModelPropertyProvider beanModelPropertyProvider,
                                        FieldModelPropertyProvider fieldModelPropertyProvider,
                                        ConstructorModelPropertyProvider constructorModelPropertyProvider) {
    this.beanModelPropertyProvider = beanModelPropertyProvider;
    this.fieldModelPropertyProvider = fieldModelPropertyProvider;
    this.constructorModelPropertyProvider = constructorModelPropertyProvider;
  }

  @Override
  public Iterable<? extends ModelProperty> propertiesForSerialization(ResolvedType type, JsonView views) {
    return Iterables.concat(fieldModelPropertyProvider.propertiesForSerialization(type, views),
            beanModelPropertyProvider.propertiesForSerialization(type, views),
            constructorModelPropertyProvider.propertiesForSerialization(type, views));
  }

  @Override
  public Iterable<? extends ModelProperty> propertiesForDeserialization(ResolvedType type, JsonView views) {
    return Iterables.concat(fieldModelPropertyProvider.propertiesForDeserialization(type, views),
            beanModelPropertyProvider.propertiesForDeserialization(type, views),
            constructorModelPropertyProvider.propertiesForDeserialization(type, views));
  }

  @Override
  public void setObjectMapper(ObjectMapper objectMapper) {
    fieldModelPropertyProvider.setObjectMapper(objectMapper);
    beanModelPropertyProvider.setObjectMapper(objectMapper);
    constructorModelPropertyProvider.setObjectMapper(objectMapper);
  }
}

