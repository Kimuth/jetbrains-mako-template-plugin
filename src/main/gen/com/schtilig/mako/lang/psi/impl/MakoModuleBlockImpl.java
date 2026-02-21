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

public class MakoModuleBlockImpl extends MakoModuleBlockMixin implements MakoModuleBlock {

  public MakoModuleBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull MakoVisitor visitor) {
    visitor.visitModuleBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof MakoVisitor) accept((MakoVisitor)visitor);
    else super.accept(visitor);
  }

}
