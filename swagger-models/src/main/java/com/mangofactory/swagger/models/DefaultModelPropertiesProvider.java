package com.mangofactory.swagger.models;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.ResolvedField;
import com.fasterxml.classmate.members.ResolvedMethod;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.mangofactory.swagger.models.alternates.AlternateTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import static com.google.common.base.Predicates.*;
import static com.mangofactory.swagger.models.Accessors.*;
import static com.mangofactory.swagger.models.BeanModelProperty.*;

@Component
public class DefaultModelPropertiesProvider implements ModelPropertiesProvider {

  private static final Logger log = LoggerFactory.getLogger(DefaultModelPropertiesProvider.class);
  private ObjectMapper objectMapper;
  private final TypeResolver typeResolver;
  private final AlternateTypeProvider alternateTypeProvider;
  private final AccessorsProvider accessors;
  private final FieldsProvider fields;

  @Autowired
  public DefaultModelPropertiesProvider(TypeResolver typeResolver, AlternateTypeProvider alternateTypeProvider,
      AccessorsProvider accessors, FieldsProvider fields) {
    this.typeResolver = typeResolver;
    this.alternateTypeProvider = alternateTypeProvider;
    this.accessors = accessors;
    this.fields = fields;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<? extends ModelProperty> serializableProperties(ResolvedType resolvedType, JsonView views) {
    List<ModelProperty> serializationCandidates = newArrayList();
    SerializationConfig serializationConfig = this.getSerializationConfigWithViews(views);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = uniqueIndex(beanDescription.findProperties(),
            beanPropertyByInternalName());
    for (ResolvedMethod childProperty : accessors.in(resolvedType)) {
      if (propertyLookup.containsKey(propertyName(childProperty.getName()))) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(propertyName(childProperty.getName()));
        Class<?>[] foundViews = propertyDefinition.findViews();
        if (null != views && null != foundViews && !isAssignableFromViews(foundViews, views.value())) {
           continue;
        }
        Optional<BeanPropertyDefinition> jacksonProperty = jacksonPropertyWithSameInternalName(beanDescription,
                propertyDefinition);
        AnnotatedMember member = propertyDefinition.getPrimaryMember();
        if (accessorMemberIs(childProperty, methodName(member))) {
          serializationCandidates.add(beanModelProperty(childProperty, jacksonProperty));
        }
      }
    }
    return serializationCandidates;
  }

  private Optional<BeanPropertyDefinition> jacksonPropertyWithSameInternalName(BeanDescription beanDescription,
      BeanPropertyDefinition propertyDefinition) {
    return FluentIterable.from(beanDescription
            .findProperties()).firstMatch(withSameInternalName(propertyDefinition));
  }

  private String methodName(AnnotatedMember member) {
    if (member == null || member.getMember() == null) {
      return "";
    }
    return member.getMember().getName();
  }

  private List<? extends ModelProperty> serializableFields(ResolvedType resolvedType, JsonView views) {
    List<ModelProperty> serializationCandidates = newArrayList();
    SerializationConfig serializationConfig = this.getSerializationConfigWithViews(views);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = uniqueIndex(beanDescription.findProperties(),
            beanPropertyByInternalName());
    for (ResolvedField childField : fields.in(resolvedType)) {
      if (propertyLookup.containsKey(childField.getName())) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(childField.getName());
         Class<?>[] foundViews = propertyDefinition.findViews();
         if (null != views && null != foundViews && !isAssignableFromViews(foundViews, views.value())) {
            continue;
         }
        Optional<BeanPropertyDefinition> jacksonProperty
                = jacksonPropertyWithSameInternalName(beanDescription, propertyDefinition);
        AnnotatedMember member = propertyDefinition.getPrimaryMember();
        if (memberIsAField(member)) {
          serializationCandidates.add(new FieldModelProperty(jacksonProperty.get().getName(), childField,
                  alternateTypeProvider));
        }
      }
    }
    return serializationCandidates;
  }

  private boolean memberIsAField(AnnotatedMember member) {
    return member != null
            && member.getMember() != null
            && Field.class.isAssignableFrom(member.getMember().getClass());
  }


  public List<? extends ModelProperty> deserializableProperties(ResolvedType resolvedType, JsonView views) {
    List<ModelProperty> serializationCandidates = newArrayList();
    DeserializationConfig serializationConfig = this.getDeserializationConfigWithViews(views);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = uniqueIndex(beanDescription.findProperties(),
            beanPropertyByInternalName());
    for (ResolvedMethod childProperty : accessors.in(resolvedType)) {

      if (propertyLookup.containsKey(propertyName(childProperty.getName()))) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(propertyName(childProperty.getName()));
         Class<?>[] foundViews = propertyDefinition.findViews();
         if (null != views && null != foundViews && !isAssignableFromViews(foundViews, views.value())) {
            continue;
         }
        Optional<BeanPropertyDefinition> jacksonProperty
                = jacksonPropertyWithSameInternalName(beanDescription, propertyDefinition);
        try {
          AnnotatedMember member = propertyDefinition.getPrimaryMember();
          if (accessorMemberIs(childProperty, methodName(member))) {
            serializationCandidates.add(beanModelProperty(childProperty, jacksonProperty));
          }
        } catch (Exception e) {
          log.warn(e.getMessage());
        }
      }
    }
    return serializationCandidates;
  }

  private BeanModelProperty beanModelProperty(ResolvedMethod childProperty, Optional<BeanPropertyDefinition>
          jacksonProperty) {
    return new BeanModelProperty(jacksonProperty.get().getName(),
            childProperty, isGetter(childProperty.getRawMember()), typeResolver, alternateTypeProvider);
  }

  public List<? extends ModelProperty> deserializableFields(ResolvedType resolvedType, JsonView views) {
    List<ModelProperty> serializationCandidates = newArrayList();
    DeserializationConfig serializationConfig = this.getDeserializationConfigWithViews(views);
    BeanDescription beanDescription = serializationConfig.introspect(TypeFactory.defaultInstance()
            .constructType(resolvedType.getErasedType()));
    Map<String, BeanPropertyDefinition> propertyLookup = uniqueIndex(beanDescription.findProperties(),
            beanPropertyByInternalName());
    for (ResolvedField childField : fields.in(resolvedType)) {
      if (propertyLookup.containsKey(childField.getName())) {
        BeanPropertyDefinition propertyDefinition = propertyLookup.get(childField.getName());
         Class<?>[] foundViews = propertyDefinition.findViews();
         if (null != views && null != foundViews && !isAssignableFromViews(foundViews, views.value())) {
            continue;
         }
        Optional<BeanPropertyDefinition> jacksonProperty
                = jacksonPropertyWithSameInternalName(beanDescription, propertyDefinition);
        AnnotatedMember member = propertyDefinition.getPrimaryMember();
        if (memberIsAField(member)) {
          serializationCandidates.add(new FieldModelProperty(jacksonProperty.get().getName(), childField,
                  alternateTypeProvider));
        }
      }
    }
    return serializationCandidates;
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

  private Predicate<BeanPropertyDefinition> withSameInternalName(final BeanPropertyDefinition propertyDefinition) {
    return new Predicate<BeanPropertyDefinition>() {
      @Override
      public boolean apply(BeanPropertyDefinition input) {
        return input.getInternalName() == propertyDefinition.getInternalName();
      }
    };
  }

  private Function<BeanPropertyDefinition, String> beanPropertyByInternalName() {
    return new Function<BeanPropertyDefinition, String>() {
      @Override
      public String apply(BeanPropertyDefinition input) {
        return input.getInternalName();
      }
    };
  }

  private SerializationConfig getSerializationConfigWithViews(JsonView views) {
     SerializationConfig serializationConfig = objectMapper.getSerializationConfig();
     if (null != views && views.value().length > 0) {
        for (Class<?> cl: views.value()) {
           serializationConfig = serializationConfig.withView(cl);
        }
     }
     return serializationConfig;
  }

   private DeserializationConfig getDeserializationConfigWithViews(JsonView views) {
      DeserializationConfig serializationConfig = objectMapper.getDeserializationConfig();
      if (null != views && views.value().length > 0) {
         for (Class<?> cl: views.value()) {
            serializationConfig = serializationConfig.withView(cl);
         }
      }
      return serializationConfig;
   }

  @Override
  public Iterable<? extends ModelProperty> propertiesForSerialization(ResolvedType type, JsonView views) {
    ArrayList<ModelProperty> modelProperties = newArrayList(serializableFields(type, views));
    modelProperties.addAll(serializableProperties(type, views));
    return modelProperties;
  }

  @Override
  public Iterable<? extends ModelProperty> propertiesForDeserialization(ResolvedType type, JsonView views) {
    ArrayList<ModelProperty> modelProperties = newArrayList(deserializableFields(type, views));
    modelProperties.addAll(deserializableProperties(type, views));
    return modelProperties;
  }
}

