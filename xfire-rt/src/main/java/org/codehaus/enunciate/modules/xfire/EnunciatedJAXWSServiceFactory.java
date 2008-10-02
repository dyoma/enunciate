/*
 * Copyright 2006-2008 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.enunciate.modules.xfire;

import org.codehaus.xfire.XFireFactory;
import org.codehaus.xfire.annotations.AnnotationException;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.annotations.WebServiceAnnotation;
import org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations;
import org.codehaus.xfire.exchange.MessageSerializer;
import org.codehaus.xfire.fault.FaultSender;
import org.codehaus.xfire.handler.OutMessageSender;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.PostInvocationHandler;
import org.codehaus.xfire.service.binding.ServiceInvocationHandler;
import org.codehaus.xfire.soap.AbstractSoapBinding;
import org.codehaus.xfire.soap.SoapConstants;
import org.springframework.beans.factory.annotation.Autowired;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * The enunciate implementation of the JAXWS service factory.
 *
 * @author Ryan Heaton
 */
public class EnunciatedJAXWSServiceFactory extends AnnotationServiceFactory {

  private final Properties paramNames = new Properties();
  private ValidationEventHandler validationEventHandler = new DefaultValidationEventHandler();

  /**
   * An annotation service factory that is initialized with the {@link org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations} and
   * the default transport manager.
   */
  public EnunciatedJAXWSServiceFactory() {
    super(new Jsr181WebAnnotations(),
          XFireFactory.newInstance().getXFire().getTransportManager());

    InputStream in = getClass().getResourceAsStream("/enunciate-soap-parameter-names.properties");
    if (in != null) {
      try {
        paramNames.load(in);
      }
      catch (IOException e) {
        //fall through....
      }
    }
  }


  /**
   * Ensures that any service created has MTOM enabled.
   * 
   * @param clazz The class.
   * @param name The name.
   * @param namespace The namespace.
   * @param properties The properties.
   * @return The service.
   */
  @Override
  public Service create(final Class clazz, String name, String namespace, Map properties) {
    Service service = super.create(clazz, name, namespace, properties);
    service.setProperty(SoapConstants.MTOM_ENABLED, Boolean.TRUE.toString());
    return service;
  }

  /**
   * The handlers for a service include the defaults, with the exception of the {@link org.codehaus.enunciate.modules.xfire.EnunciatedJAXWSWebFaultHandler}.
   *
   * @param service The service for which to register the handlers.
   */
  protected void registerHandlers(Service service) {
    service.addInHandler(new ServiceInvocationHandler());
    service.addInHandler(new PostInvocationHandler());
    service.addOutHandler(new OutMessageSender());
    service.addFaultHandler(new FaultSender());
    service.addFaultHandler(new EnunciatedJAXWSWebFaultHandler());
  }

  /**
   * The service name according to the JAXWS specification is looked up with the metadata on the endpoint
   * interface and defaults to the simple name of the endpoint interface + "Service" if not specified.
   *
   * @param clazz      The class for which to lookup the service name.
   * @param annotation The relevant annotation to use.
   * @param current    A suggested name (ignored).
   * @return The service name.
   */
  @Override
  protected String createServiceName(Class clazz, WebServiceAnnotation annotation, String current) {
    WebAnnotations webAnnotations = getAnnotations();
    Class endpointInterface = clazz;
    String eiValue = annotation.getEndpointInterface();
    if (eiValue != null && eiValue.length() > 0) {
      //the metadata is supplied on another class...
      try {
        endpointInterface = loadClass(annotation.getEndpointInterface());
        if (!webAnnotations.hasWebServiceAnnotation(endpointInterface)) {
          throw new AnnotationException("Endpoint interface " + endpointInterface.getName() + " does not have a WebService annotation");
        }

        WebServiceAnnotation eiAnnotation = webAnnotations.getWebServiceAnnotation(endpointInterface);
        String serviceName = eiAnnotation.getServiceName();
        if ((serviceName != null) && (serviceName.length() > 0)) {
          return serviceName;
        }
      }
      catch (ClassNotFoundException e) {
        throw new AnnotationException("Couldn't find endpoint interface " + annotation.getEndpointInterface(), e);
      }
    }
    else {
      String serviceName = annotation.getServiceName();
      if ((serviceName != null) && (serviceName.length() > 0)) {
        return serviceName;
      }
    }

    return endpointInterface.getSimpleName() + "Service";
  }


  @Override
  public WebAnnotations getAnnotations() {
    return super.getAnnotations();
  }

  /**
   * The faults don't need to be initialized.  The {@link org.codehaus.enunciate.modules.xfire.EnunciatedJAXWSWebFaultHandler}
   * will handle it.
   *
   * @param service The service.
   * @param op      The operation.
   */
  @Override
  protected void initializeFaults(final Service service, final OperationInfo op) {
    //no-op....
  }

  /**
   * The serializer for a SOAP message.  For Enunciate it is a {@link org.codehaus.enunciate.modules.xfire.EnunciatedJAXWSMessageBinding}.
   *
   * @param binding The binding.
   * @return The default serializer for the binding.
   */
  @Override
  protected MessageSerializer getSerializer(AbstractSoapBinding binding) {
    EnunciatedJAXWSMessageBinding messageBinding = new EnunciatedJAXWSMessageBinding();
    messageBinding.setValidationEventHandler(getValidationEventHandler());
    return messageBinding;
  }

  /**
   * The input message name depends on the metadata of the operation.  If the operation is rpc/lit, the
   * input message name is the operation name.  If the operation is doc/lit bare, the input message name
   * is the element name of the input parameter.  Otherwise (doc/lit wrapped), the input message name
   * is wrapped with the wrapper element, as described in the JAXWS specification.
   *
   * @param op The operation for which to determine the input message name.
   * @return The input message name.
   */
  @Override
  protected QName createInputMessageName(OperationInfo op) {
    Method method = op.getMethod();
    Class ei = method.getDeclaringClass();
    SOAPBinding.Style style = SOAPBinding.Style.DOCUMENT;
    SOAPBinding.ParameterStyle paramStyle = SOAPBinding.ParameterStyle.WRAPPED;

    if (method.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding annotation = method.getAnnotation(SOAPBinding.class);
      style = annotation.style();
      paramStyle = annotation.parameterStyle();
    }
    else if (ei.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding annotation = ((SOAPBinding) ei.getAnnotation(SOAPBinding.class));
      style = annotation.style();
      paramStyle = annotation.parameterStyle();
    }

    if (style == SOAPBinding.Style.RPC) {
      //if it's an rpc-style method call, the message name is the operation name.
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      String operationName = method.getName();
      if (method.isAnnotationPresent(WebMethod.class)) {
        WebMethod annotation = method.getAnnotation(WebMethod.class);
        if (annotation.operationName().length() > 0) {
          operationName = annotation.operationName();
        }
      }

      return new QName(namespace, operationName);
    }
    else if (paramStyle == SOAPBinding.ParameterStyle.BARE) {
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      //find the first "in" param.  Presumably, there are no more, since we're BARE.
      WebParam annotation = null;
      int paramIndex;
      Annotation[][] parameterAnnotations = method.getParameterAnnotations();
      PARAM_ANNOTATIONS : for (paramIndex = 0; paramIndex < parameterAnnotations.length; paramIndex++) {
        Annotation[] annotations = parameterAnnotations[paramIndex];
        for (Annotation candidate : annotations) {
          if (candidate instanceof WebParam && !((WebParam) candidate).header()) {
            WebParam.Mode mode = ((WebParam) candidate).mode();
            switch(mode) {
              case IN:
              case INOUT:
                annotation = (WebParam) candidate;
                break PARAM_ANNOTATIONS;
            }
          }
        }
      }

      String name;
      if (annotation == null || !"".equals(annotation.name())) {
        name = loadParamName(ei, method, annotation == null ? 0 : paramIndex);
      }
      else {
        name = annotation.name();
      }

      return new QName(namespace, name);
    }
    else {
      //default doc/lit behavior.
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      String name = method.getName();
      if (method.isAnnotationPresent(RequestWrapper.class)) {
        RequestWrapper wrapper = method.getAnnotation(RequestWrapper.class);

        if (!"".equals(wrapper.targetNamespace())) {
          namespace = wrapper.targetNamespace();
        }

        if (!"".equals(wrapper.localName())) {
          name = wrapper.localName();
        }
      }

      return new QName(namespace, name);
    }
  }

  /**
   * Loads the parameter name for the specified parameter.
   *
   * @param ei The endpoint interface.
   * @param method The method.
   * @param paramIndex The parameter index.
   * @return The parameter name.
   */
  protected String loadParamName(Class ei, Method method, int paramIndex) {
    String operationName = method.getName();
    if (method.isAnnotationPresent(WebMethod.class)) {
      WebMethod wm = method.getAnnotation(WebMethod.class);
      if (wm.operationName().length() > 0) {
        operationName = wm.operationName();
      }
    }

    String paramName = null;
    String paramNames = (String) this.paramNames.get(ei.getName() + "#" + operationName);
    if (paramNames != null) {
      String[] paramsNames = paramNames.split(",");
      if (paramsNames.length > paramIndex) {
        paramName = paramsNames[paramIndex];
      }
    }

    if (paramName == null) {
      throw new IllegalStateException("Unknown parameter name for parameter '" + paramIndex + "' of web method " + ei.getName() + "#" + operationName);
    }
    
    return paramName;
  }

  @Override
  protected QName createOutputMessageName(OperationInfo op) {
    Method method = op.getMethod();
    Class ei = method.getDeclaringClass();
    SOAPBinding.Style style = SOAPBinding.Style.DOCUMENT;
    SOAPBinding.ParameterStyle paramStyle = SOAPBinding.ParameterStyle.WRAPPED;

    if (method.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding annotation = method.getAnnotation(SOAPBinding.class);
      style = annotation.style();
      paramStyle = annotation.parameterStyle();
    }
    else if (ei.isAnnotationPresent(SOAPBinding.class)) {
      SOAPBinding annotation = ((SOAPBinding) ei.getAnnotation(SOAPBinding.class));
      style = annotation.style();
      paramStyle = annotation.parameterStyle();
    }

    if (style == SOAPBinding.Style.RPC) {
      //if it's an rpc-style method call, the message name is the operation name.
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      String operationName = method.getName();
      if (method.isAnnotationPresent(WebMethod.class)) {
        WebMethod annotation = method.getAnnotation(WebMethod.class);
        if (annotation.operationName().length() > 0) {
          operationName = annotation.operationName();
        }
      }

      return new QName(namespace, operationName);
    }
    else if (paramStyle == SOAPBinding.ParameterStyle.BARE) {
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      String name;
      if (method.getReturnType() == Void.TYPE) {
        //find the first "out" param.  Presumably, there are no more, since we're BARE.
        WebParam annotation = null;
        int paramIndex;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        PARAM_ANNOTATIONS : for (paramIndex = 0; paramIndex < parameterAnnotations.length; paramIndex++) {
          Annotation[] annotations = parameterAnnotations[paramIndex];
          for (Annotation candidate : annotations) {
            if (candidate instanceof WebParam && !((WebParam) candidate).header()) {
              WebParam.Mode mode = ((WebParam) candidate).mode();
              switch(mode) {
                case OUT:
                case INOUT:
                  annotation = (WebParam) candidate;
                  break PARAM_ANNOTATIONS;
              }
            }
          }
        }

        if (annotation == null || !"".equals(annotation.name())) {
          name = loadParamName(ei, method, annotation == null ? 0 : paramIndex);
        }
        else {
          name = annotation.name();
        }
      }
      else {
        WebResult annotation = method.getAnnotation(WebResult.class);
        name = annotation == null || "".equals(annotation.name()) ? method.getName() + "Response" : annotation.name();
      }
      
      return new QName(namespace, name);
    }
    else {
      //default doc/lit behavior.
      String namespace = ((WebService) ei.getAnnotation(WebService.class)).targetNamespace();
      if ("".equals(namespace)) {
        namespace = calculateNamespaceURI(ei);
      }

      String name = method.getName();
      if (method.isAnnotationPresent(ResponseWrapper.class)) {
        ResponseWrapper wrapper = method.getAnnotation(ResponseWrapper.class);

        if (!"".equals(wrapper.targetNamespace())) {
          namespace = wrapper.targetNamespace();
        }

        if (!"".equals(wrapper.localName())) {
          name = wrapper.localName();
        }
      }

      return new QName(namespace, name);
    }
  }

  /**
   * Overridden to fix a bug in XFire.
   *
   * @param method The method.
   * @param paramIndex The parameter index.
   * @return Whether the parameter index is an out param.
   */
  @Override
  protected boolean isOutParam(Method method, int paramIndex) {
    //xfire 1.2.1 chokes on -1...
    return paramIndex == -1 || super.isOutParam(method, paramIndex);
  }

  /**
   * Overridden to fix a bug in XFire.
   *
   * @param method The method.
   * @param paramIndex The parameter index.
   * @return Whether the parameter index is an in param.
   */
  @Override
  protected boolean isInParam(Method method, int paramIndex) {
    //xfire 1.2.1 chokes on -1...
    return paramIndex != -1 && super.isInParam(method, paramIndex);
  }

  /**
   * XFire defaults the name of the return value to "out".  The JAXWS spec says "return"....
   *
   * @param service The service.
   * @param op The operation.
   * @param method The method.
   * @param paramNumber The parameter number.
   * @param doc Whether its doc-style binding.
   * @return The out parameter name.
   */
  @Override
  protected QName getOutParameterName(final Service service, final OperationInfo op, final Method method, final int paramNumber, final boolean doc) {
    QName parameterName = super.getOutParameterName(service, op, method, paramNumber, doc);

    if (paramNumber == -1) {
      WebResult webResult = method.getAnnotation(WebResult.class);
      if ((webResult == null) || ("".equals(webResult.name()))) {
        parameterName = new QName(parameterName.getNamespaceURI(), "return");
      }
    }

    return parameterName;
  }

  /**
   * Calculates a namespace URI for a given package.  Default implementation uses the algorithm defined in
   * section 3.2 of the jax-ws spec.
   *
   * @param jaxwsClass The class for which to calculate the namespace based on the JAXWS spec.
   * @return The calculated namespace uri.
   */
  protected String calculateNamespaceURI(Class jaxwsClass) {
    Package pckg = jaxwsClass.getPackage();
    String[] tokens = pckg.getName().split("\\.");
    String uri = "http://";
    for (int i = tokens.length - 1; i >= 0; i--) {
      uri += tokens[i];
      if (i != 0) {
        uri += ".";
      }
    }
    uri += "/";
    return uri;
  }

  /**
   * The validation event handler.
   *
   * @return The validation event handler.
   */
  public ValidationEventHandler getValidationEventHandler() {
    return validationEventHandler;
  }

  /**
   * The validation event handler.
   *
   * @param validationEventHandler The validation event handler.
   */
  @Autowired( required = false )
  public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
    this.validationEventHandler = validationEventHandler;
  }
}
