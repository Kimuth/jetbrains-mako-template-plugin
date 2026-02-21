// This is a generated file. Not intended for manual editing.
package com.schtilig.mako.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.schtilig.mako.lang.psi.MakoTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.schtilig.mako.lang.psi.*;

public class MakoDocCommentImpl extends ASTWrapperPsiElement implements MakoDocComment {

  public MakoDocCommentImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MakoVisitor visitor) {
    visitor.visitDocComment(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MakoVisitor) accept((MakoVisitor)visitor);
    else super.accept(visitor);
  }

}
