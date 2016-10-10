package com.webcohesion.enunciate.modules.lombok;

import com.webcohesion.enunciate.javac.decorations.DecoratedProcessingEnvironment;
import com.webcohesion.enunciate.javac.decorations.ElementDecoration;
import com.webcohesion.enunciate.javac.decorations.element.DecoratedElement;
import com.webcohesion.enunciate.javac.decorations.element.DecoratedTypeElement;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleElementVisitor6;

/**
 * @author Ryan Heaton
 */
public class LombokDecoration extends SimpleElementVisitor6<Void, DecoratedProcessingEnvironment> implements ElementDecoration {

  @Override
  public void applyTo(DecoratedElement e, DecoratedProcessingEnvironment env) {
    e.accept(this, env);
  }

  @Override
  public Void visitType(TypeElement e, DecoratedProcessingEnvironment env) {
    DecoratedTypeElement typeElement = (DecoratedTypeElement) e;
    LombokMethodGenerator lombokMethodGenerator = new LombokMethodGenerator(typeElement, env);
    lombokMethodGenerator.generateLombokGettersAndSetters();
    return null;
  }

  @Override
  public Void visitUnknown(Element e, DecoratedProcessingEnvironment env) {
    //no-op
    return null;
  }





}
