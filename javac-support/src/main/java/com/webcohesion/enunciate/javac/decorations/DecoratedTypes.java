/**
 * Copyright © 2006-2016 Web Cohesion (info@webcohesion.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webcohesion.enunciate.javac.decorations;

import com.webcohesion.enunciate.javac.decorations.element.DecoratedElement;
import com.webcohesion.enunciate.javac.decorations.element.DecoratedTypeElement;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedDeclaredType;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedExecutableType;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedPrimitiveType;
import com.webcohesion.enunciate.javac.decorations.type.DecoratedTypeMirror;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * @author Ryan Heaton
 */
public class DecoratedTypes implements Types {

  private final Types delegate;
  private final DecoratedProcessingEnvironment env;

  public DecoratedTypes(Types delegate, DecoratedProcessingEnvironment env) {
    while (delegate instanceof DecoratedTypes) {
      delegate = ((DecoratedTypes) delegate).delegate;
    }

    this.delegate = delegate;
    this.env = env;
  }

  public Element asElement(TypeMirror t) {
    while (t instanceof DecoratedTypeMirror) {
      t = ((DecoratedTypeMirror) t).getDelegate();
    }

    return ElementDecorator.decorate(delegate.asElement(t), this.env);
  }

  public TypeMirror capture(TypeMirror t) {
    while (t instanceof DecoratedTypeMirror) {
      t = ((DecoratedTypeMirror) t).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.capture(t), this.env);
  }

  public NullType getNullType() {
    return TypeMirrorDecorator.decorate(delegate.getNullType(), this.env);
  }

  public PrimitiveType getPrimitiveType(TypeKind kind) {
    return TypeMirrorDecorator.decorate(delegate.getPrimitiveType(kind), this.env);
  }

  public DeclaredType getDeclaredType(TypeElement type, TypeMirror... typeArgs) {
    while (type instanceof DecoratedTypeElement) {
      type = ((DecoratedTypeElement) type).getDelegate();
    }

    TypeMirror[] copy = new TypeMirror[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      TypeMirror t = typeArgs[i];
      while (t instanceof DecoratedTypeMirror) {
        t = ((DecoratedTypeMirror) t).getDelegate();
      }
      copy[i] = t;
    }

    return TypeMirrorDecorator.decorate(delegate.getDeclaredType(type, copy), this.env);
  }

  public DeclaredType getDeclaredType(DeclaredType containing, TypeElement type, TypeMirror... typeArgs) {
    while (containing instanceof DecoratedDeclaredType) {
      containing = ((DecoratedDeclaredType) containing).getDelegate();
    }

    while (type instanceof DecoratedTypeElement) {
      type = ((DecoratedTypeElement) type).getDelegate();
    }

    TypeMirror[] copy = new TypeMirror[typeArgs.length];
    for (int i = 0; i < typeArgs.length; i++) {
      TypeMirror t = typeArgs[i];
      while (t instanceof DecoratedTypeMirror) {
        t = ((DecoratedTypeMirror) t).getDelegate();
      }
      copy[i] = t;
    }

    return delegate.getDeclaredType(containing, type, copy);
  }

  public NoType getNoType(TypeKind kind) {
    return TypeMirrorDecorator.decorate(delegate.getNoType(kind), this.env);
  }

  public TypeMirror erasure(TypeMirror t) {
    while (t instanceof DecoratedTypeMirror) {
      t = ((DecoratedTypeMirror) t).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.erasure(t), this.env);
  }

  public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
    while (extendsBound instanceof DecoratedTypeMirror) {
      extendsBound = ((DecoratedTypeMirror) extendsBound).getDelegate();
    }

    while (superBound instanceof DecoratedTypeMirror) {
      superBound = ((DecoratedTypeMirror) superBound).getDelegate();
    }

    return delegate.getWildcardType(extendsBound, superBound);
  }

  public boolean isSameType(TypeMirror t1, TypeMirror t2) {
    while (t1 instanceof DecoratedTypeMirror) {
      t1 = ((DecoratedTypeMirror) t1).getDelegate();
    }

    while (t2 instanceof DecoratedTypeMirror) {
      t2 = ((DecoratedTypeMirror) t2).getDelegate();
    }

    return delegate.isSameType(t1, t2);
  }

  public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
    while (t1 instanceof DecoratedTypeMirror) {
      t1 = ((DecoratedTypeMirror) t1).getDelegate();
    }

    while (t2 instanceof DecoratedTypeMirror) {
      t2 = ((DecoratedTypeMirror) t2).getDelegate();
    }

    return delegate.isSubtype(t1, t2);
  }

  public TypeElement boxedClass(PrimitiveType p) {
    while (p instanceof DecoratedPrimitiveType) {
      p = ((DecoratedPrimitiveType) p).getDelegate();
    }

    return ElementDecorator.decorate(delegate.boxedClass(p), this.env);
  }

  public ArrayType getArrayType(TypeMirror componentType) {
    while (componentType instanceof DecoratedTypeMirror) {
      componentType = ((DecoratedTypeMirror) componentType).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.getArrayType(componentType), this.env);
  }

  public boolean contains(TypeMirror t1, TypeMirror t2) {
    while (t1 instanceof DecoratedTypeMirror) {
      t1 = ((DecoratedTypeMirror) t1).getDelegate();
    }

    while (t2 instanceof DecoratedTypeMirror) {
      t2 = ((DecoratedTypeMirror) t2).getDelegate();
    }

    return delegate.contains(t1, t2);
  }

  public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
    while (m1 instanceof DecoratedExecutableType) {
      m1 = ((DecoratedExecutableType) m1).getDelegate();
    }

    while (m2 instanceof DecoratedExecutableType) {
      m2 = ((DecoratedExecutableType) m2).getDelegate();
    }

    return delegate.isSubsignature(m1, m2);
  }

  public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
    while (t1 instanceof DecoratedTypeMirror) {
      t1 = ((DecoratedTypeMirror) t1).getDelegate();
    }

    while (t2 instanceof DecoratedTypeMirror) {
      t2 = ((DecoratedTypeMirror) t2).getDelegate();
    }

    return delegate.isAssignable(t1, t2);
  }

  public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
    while (t instanceof DecoratedTypeMirror) {
      t = ((DecoratedTypeMirror) t).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.directSupertypes(t), this.env);
  }

  public TypeMirror asMemberOf(DeclaredType containing, Element element) {
    while (containing instanceof DecoratedDeclaredType) {
      containing = ((DecoratedDeclaredType) containing).getDelegate();
    }

    while (element instanceof DecoratedElement) {
      element = ((DecoratedElement) element).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.asMemberOf(containing, element), this.env);
  }

  public PrimitiveType unboxedType(TypeMirror t) {
    while (t instanceof DecoratedTypeMirror) {
      t = ((DecoratedTypeMirror) t).getDelegate();
    }

    return TypeMirrorDecorator.decorate(delegate.unboxedType(t), this.env);
  }
}
