// This is a generated file. Not intended for manual editing.
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiLanguageInjectionHost;

public class MakoVisitor extends PsiElementVisitor {

  public void visitBlockTag(@NotNull MakoBlockTag o) {
    visitPsiNamedElement(o);
  }

  public void visitCodeBlock(@NotNull MakoCodeBlock o) {
    visitPsiLanguageInjectionHost(o);
  }

  public void visitControlLineStmt(@NotNull MakoControlLineStmt o) {
    visitPsiElement(o);
  }

  public void visitDefTag(@NotNull MakoDefTag o) {
    visitPsiNamedElement(o);
  }

  public void visitDocComment(@NotNull MakoDocComment o) {
    visitPsiElement(o);
  }

  public void visitExpression(@NotNull MakoExpression o) {
    visitPsiLanguageInjectionHost(o);
  }

  public void visitIncludeTag(@NotNull MakoIncludeTag o) {
    visitPsiElement(o);
  }

  public void visitInheritTag(@NotNull MakoInheritTag o) {
    visitPsiElement(o);
  }

  public void visitLineCommentRule(@NotNull MakoLineCommentRule o) {
    visitPsiElement(o);
  }

  public void visitModuleBlock(@NotNull MakoModuleBlock o) {
    visitPsiLanguageInjectionHost(o);
  }

  public void visitNamespaceTag(@NotNull MakoNamespaceTag o) {
    visitPsiElement(o);
  }

  public void visitPageTag(@NotNull MakoPageTag o) {
    visitPsiElement(o);
  }

  public void visitTemplateTextContent(@NotNull MakoTemplateTextContent o) {
    visitPsiElement(o);
  }

  public void visitPsiLanguageInjectionHost(@NotNull PsiLanguageInjectionHost o) {
    visitElement(o);
  }

  public void visitPsiNamedElement(@NotNull PsiNamedElement o) {
    visitElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
