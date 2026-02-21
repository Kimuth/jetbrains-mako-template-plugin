// This is a generated file. Not intended for manual editing.
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes.*;
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.*;

public class MakoDefTagImpl extends MakoDefTagMixin implements MakoDefTag {

  public MakoDefTagImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MakoVisitor visitor) {
    visitor.visitDefTag(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MakoVisitor) accept((MakoVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<MakoBlockTag> getBlockTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoBlockTag.class);
  }

  @Override
  @NotNull
  public List<MakoCodeBlock> getCodeBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoCodeBlock.class);
  }

  @Override
  @NotNull
  public List<MakoControlLineStmt> getControlLineStmtList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoControlLineStmt.class);
  }

  @Override
  @NotNull
  public List<MakoDefTag> getDefTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoDefTag.class);
  }

  @Override
  @NotNull
  public List<MakoDocComment> getDocCommentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoDocComment.class);
  }

  @Override
  @NotNull
  public List<MakoExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoExpression.class);
  }

  @Override
  @NotNull
  public List<MakoIncludeTag> getIncludeTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoIncludeTag.class);
  }

  @Override
  @NotNull
  public List<MakoInheritTag> getInheritTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoInheritTag.class);
  }

  @Override
  @NotNull
  public List<MakoLineCommentRule> getLineCommentRuleList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoLineCommentRule.class);
  }

  @Override
  @NotNull
  public List<MakoModuleBlock> getModuleBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoModuleBlock.class);
  }

  @Override
  @NotNull
  public List<MakoNamespaceTag> getNamespaceTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoNamespaceTag.class);
  }

  @Override
  @NotNull
  public List<MakoPageTag> getPageTagList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoPageTag.class);
  }

  @Override
  @NotNull
  public List<MakoTemplateTextContent> getTemplateTextContentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, MakoTemplateTextContent.class);
  }

}
