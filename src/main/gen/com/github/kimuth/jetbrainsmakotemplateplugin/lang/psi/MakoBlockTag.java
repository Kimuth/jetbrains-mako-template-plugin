// This is a generated file. Not intended for manual editing.
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;

public interface MakoBlockTag extends PsiNamedElement {

  @NotNull
  List<MakoBlockTag> getBlockTagList();

  @NotNull
  List<MakoCodeBlock> getCodeBlockList();

  @NotNull
  List<MakoControlLine> getControlLineList();

  @NotNull
  List<MakoDefTag> getDefTagList();

  @NotNull
  List<MakoDocComment> getDocCommentList();

  @NotNull
  List<MakoExpression> getExpressionList();

  @NotNull
  List<MakoIncludeTag> getIncludeTagList();

  @NotNull
  List<MakoInheritTag> getInheritTagList();

  @NotNull
  List<MakoLineCommentRule> getLineCommentRuleList();

  @NotNull
  List<MakoModuleBlock> getModuleBlockList();

  @NotNull
  List<MakoNamespaceTag> getNamespaceTagList();

  @NotNull
  List<MakoPageTag> getPageTagList();

  //WARNING: getName(...) is skipped
  //matching getName(MakoBlockTag, ...)
  //methods are not found in null

  //WARNING: setName(...) is skipped
  //matching setName(MakoBlockTag, ...)
  //methods are not found in null

  //WARNING: getNameIdentifier(...) is skipped
  //matching getNameIdentifier(MakoBlockTag, ...)
  //methods are not found in null

}
