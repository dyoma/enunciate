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
package com.webcohesion.enunciate.modules.jackson1.api.impl;

import com.webcohesion.enunciate.api.datatype.*;
import com.webcohesion.enunciate.facets.FacetFilter;
import com.webcohesion.enunciate.modules.jackson1.model.Member;
import com.webcohesion.enunciate.modules.jackson1.model.ObjectTypeDefinition;
import com.webcohesion.enunciate.modules.jackson1.model.types.JsonClassType;
import com.webcohesion.enunciate.modules.jackson1.model.types.JsonType;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * @author Ryan Heaton
 */
public class ObjectDataTypeImpl extends DataTypeImpl {

  private final ObjectTypeDefinition typeDefinition;
  private final List<DataTypeReference.ContainerType> containers;

  public ObjectDataTypeImpl(ObjectTypeDefinition typeDefinition) {
    super(typeDefinition);
    this.typeDefinition = typeDefinition;
    containers = null;
  }

  public ObjectDataTypeImpl(ObjectTypeDefinition typeDefinition, List<DataTypeReference.ContainerType> containers) {
    super(typeDefinition);
    this.typeDefinition = typeDefinition;
    this.containers = containers;
  }

  @Override
  public BaseType getBaseType() {
    return BaseType.object;
  }

  @Override
  public List<? extends Value> getValues() {
    return null;
  }

  @Override
  public List<? extends Property> getProperties() {
    SortedSet<Member> members = this.typeDefinition.getMembers();
    ArrayList<Property> properties = new ArrayList<Property>(members.size());
    FacetFilter facetFilter = this.typeDefinition.getContext().getContext().getConfiguration().getFacetFilter();
    for (Member member : members) {
      if (!facetFilter.accept(member)) {
        continue;
      }

      if (member.getChoices().size() > 1) {
        JsonTypeInfo.As inclusion = member.getSubtypeIdInclusion();
        if (inclusion == JsonTypeInfo.As.WRAPPER_ARRAY || inclusion == JsonTypeInfo.As.WRAPPER_OBJECT) {
          for (Member choice : member.getChoices()) {
            properties.add(new PropertyImpl(choice));
          }
        }
        else {
          properties.add(new PropertyImpl(member));
        }
      }
      else {
        properties.add(new PropertyImpl(member));
      }
    }

    return properties;
  }


  public List<? extends Property> getRequiredProperties() {
    ArrayList<Property> requiredProperties = new ArrayList<Property>();
    for (Property property : getProperties()) {
      if (property.isRequired()) {
        requiredProperties.add(property);
      }
    }
    return requiredProperties;
  }

  @Override
  public List<DataTypeReference> getSupertypes() {
    ArrayList<DataTypeReference> supertypes = null;

    JsonType supertype = this.typeDefinition.getSupertype();
    while (supertype != null) {
      if (supertypes == null) {
        supertypes = new ArrayList<DataTypeReference>();
      }

      supertypes.add(new DataTypeReferenceImpl(supertype));
      supertype = supertype instanceof JsonClassType ?
        ((JsonClassType)supertype).getTypeDefinition() instanceof ObjectTypeDefinition ?
          ((ObjectTypeDefinition)((JsonClassType)supertype).getTypeDefinition()).getSupertype()
          : null
        : null;
    }

    return supertypes;
  }

  @Override
  public Example getExample() {
    return new ExampleImpl(this.typeDefinition, this.containers);
  }
}
