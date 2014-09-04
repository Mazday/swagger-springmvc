package com.mangofactory.swagger.core;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.annotation.JsonView;
import com.mangofactory.swagger.models.ResolvedTypes;
import com.mangofactory.swagger.readers.operation.HandlerMethodResolver;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Type;

import static com.mangofactory.swagger.models.ResolvedTypes.*;
import static com.mangofactory.swagger.readers.operation.HandlerMethodResolver.*;

public final class ModelUtils {

  private ModelUtils() {
    throw new UnsupportedOperationException();
  }

  public static ResolvedType handlerReturnType(TypeResolver resolver, HandlerMethod handlerMethod) {
    Class hostClass = use(handlerMethod.getBeanType())
            .or(handlerMethod.getMethod().getDeclaringClass());
    return new HandlerMethodResolver(resolver).methodReturnType(handlerMethod.getMethod(), hostClass);
  }

  public static String getModelName(TypeResolver resolver, Type clazz) {
    return ResolvedTypes.typeName(asResolved(resolver, clazz));
  }

  public static String getResponseClassName(ResolvedType returnType) {
    return ResolvedTypes.responseTypeName(returnType);
  }

  public static String getResponseClassName(ResolvedType returnType, JsonView views) {
     StringBuilder sb = new StringBuilder(ResolvedTypes.responseTypeName(returnType));
     if (null != views) {
        String delim = "";
        for (Class<?> cl: views.value()) {
           sb.append(delim).append(cl.getSimpleName());
           delim = "And";
        }
     }
     return sb.toString();
  }
}
