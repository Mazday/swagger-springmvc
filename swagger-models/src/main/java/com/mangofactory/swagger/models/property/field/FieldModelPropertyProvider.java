package com.mangofactory.swagger.models.property.field;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.mangofactory.swagger.models.BeanPropertyNamingStrategy;
import com.mangofactory.swagger.models.alternates.AlternateTypeProvider;
import com.mangofactory.swagger.models.property.BeanPropertyDefinitions;
import com.mangofactory.swagger.models.property.ModelProperty;
import com.mangofactory.swagger.models.property.provider.AbstractModelPropertiesProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import static com.mangofactory.swagger.models.property.BeanPropertyDefinitions.*;

@Component
public class FieldModelPropertyProvider extends AbstractModelPropertiesProvider {

  private final FieldProvider fieldProvider;
  private final AlternateTypeProvider alternateTypeProvider;
  private final BeanPropertyNamingStrategy namingStrategy;

  @Autowired
  public FieldModelPropertyProvider(FieldProvider fieldProvider, AlternateTypeProvider alternateTypeProvider,
      BeanPropertyNamingStrategy namingStrategy) {
    this.fieldProvider = fieldProvider;
    this.alternateTypeProvider = alternateTypeProvider;
    this.namingStrategy = namingStrategy;
  }

  @Override
  public Iterable<? extends ModelProperty> propertiesForSerialization(ResolvedType resolvedType, JsonView view) {
    List<ModelProperty> serializationCandidates = newArrayList();
    SerializationConfig serializationConfig = getSerializationConfigWithViews(view);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = Maps.uniqueIndex(beanDescription.findProperties(),
            BeanPropertyDefinitions.beanPropertyByInternalName());

    for (ResolvedField childField : fieldProvider.in(resolvedType)) {
      if (propertyLookup.containsKey(childField.getName())) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(childField.getName());
        Class<?>[] foundViews = propertyDefinition.findViews();
        if (null != view && null != foundViews && !isAssignableFromViews(foundViews, view.value())) {
          continue;
        }
        Optional<BeanPropertyDefinition> jacksonProperty
                = jacksonPropertyWithSameInternalName(beanDescription, propertyDefinition);
        AnnotatedMember member = propertyDefinition.getPrimaryMember();
        if (memberIsAField(member)) {
          String fieldName = name(jacksonProperty.get(), true, namingStrategy);
          serializationCandidates.add(new FieldModelProperty(fieldName, childField, alternateTypeProvider));
        }
      }
    }
    return serializationCandidates;
  }

  @Override
  public Iterable<? extends ModelProperty> propertiesForDeserialization(ResolvedType resolvedType, JsonView view) {
    List<ModelProperty> serializationCandidates = newArrayList();
    DeserializationConfig serializationConfig = getDeserializationConfigWithViews(view);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = uniqueIndex(beanDescription.findProperties(),
            BeanPropertyDefinitions.beanPropertyByInternalName());
    for (ResolvedField childField : fieldProvider.in(resolvedType)) {
      if (propertyLookup.containsKey(childField.getName())) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(childField.getName());
        Class<?>[] foundViews = propertyDefinition.findViews();
        if (null != view && null != foundViews && !isAssignableFromViews(foundViews, view.value())) {
          continue;
        }
        Optional<BeanPropertyDefinition> jacksonProperty
                = jacksonPropertyWithSameInternalName(beanDescription, propertyDefinition);
        AnnotatedMember member = propertyDefinition.getPrimaryMember();
        if (memberIsAField(member)) {
          String fieldName = name(jacksonProperty.get(), true, namingStrategy);
          serializationCandidates.add(new FieldModelProperty(fieldName, childField, alternateTypeProvider));
        }
      }
    }
    return serializationCandidates;
  }

  protected boolean memberIsAField(AnnotatedMember member) {
    return member != null
            && member.getMember() != null
            && Field.class.isAssignableFrom(member.getMember().getClass());
  }
}
