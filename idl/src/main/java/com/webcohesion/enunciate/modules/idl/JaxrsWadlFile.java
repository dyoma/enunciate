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
package com.webcohesion.enunciate.modules.idl;

import com.webcohesion.enunciate.facets.FacetFilter;
import com.webcohesion.enunciate.modules.jaxb.model.SchemaInfo;
import com.webcohesion.enunciate.modules.jaxrs.EnunciateJaxrsContext;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * @author Ryan Heaton
 */
public class JaxrsWadlFile extends BaseXMLInterfaceDescriptionFile {

  private final EnunciateJaxrsContext jaxrsContext;
  private final List<SchemaInfo> schemas;
  private final String stylesheetUri;
  private final String baseUri;

  public JaxrsWadlFile(EnunciateJaxrsContext jaxrsContext, List<SchemaInfo> schemas, String stylesheetUri, String baseUri, Map<String, String> namespacePrefixes, FacetFilter facetFilter) {
    super("application.wadl", namespacePrefixes, facetFilter);
    this.jaxrsContext = jaxrsContext;
    this.schemas = schemas;
    this.stylesheetUri = stylesheetUri;
    this.baseUri = baseUri;
  }

  @Override
  protected Map<String, Object> createModel() {
    Map<String, Object> model = super.createModel();
    model.put("wadlStylesheetUri", this.stylesheetUri);
    model.put("pathResourceGroups", this.jaxrsContext.getResourceGroupsByPath());
    model.put("schemas", this.schemas);
    String baseUri = this.baseUri;
    if (baseUri == null) {
      baseUri = "./"; //if the base uri isn't configured, we'll try a relative base uri.
    }
    model.put("baseUri", baseUri);
    String assumedBaseUri = this.baseUri;
    model.put("assumedBaseUri", baseUri);
    return model;
  }

  @Override
  protected URL getTemplateURL() {
    return IDLModule.class.getResource("wadl.fmt");
  }
}
