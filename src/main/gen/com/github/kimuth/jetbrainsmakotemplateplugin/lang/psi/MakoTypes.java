// This is a generated file. Not intended for manual editing.
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoElementType;
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenType;
import com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.impl.*;

public interface MakoTypes {

  IElementType BLOCK_TAG = new MakoElementType("BLOCK_TAG");
  IElementType CODE_BLOCK = new MakoElementType("CODE_BLOCK");
  IElementType CONTROL_LINE = new MakoElementType("CONTROL_LINE");
  IElementType DEF_TAG = new MakoElementType("DEF_TAG");
  IElementType DOC_COMMENT = new MakoElementType("DOC_COMMENT");
  IElementType EXPRESSION = new MakoElementType("EXPRESSION");
  IElementType INCLUDE_TAG = new MakoElementType("INCLUDE_TAG");
  IElementType INHERIT_TAG = new MakoElementType("INHERIT_TAG");
  IElementType LINE_COMMENT_RULE = new MakoElementType("LINE_COMMENT_RULE");
  IElementType MODULE_BLOCK = new MakoElementType("MODULE_BLOCK");
  IElementType NAMESPACE_TAG = new MakoElementType("NAMESPACE_TAG");
  IElementType PAGE_TAG = new MakoElementType("PAGE_TAG");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == BLOCK_TAG) {
        return new MakoBlockTagImpl(node);
      }
      else if (type == CODE_BLOCK) {
        return new MakoCodeBlockImpl(node);
      }
      else if (type == CONTROL_LINE) {
        return new MakoControlLineImpl(node);
      }
      else if (type == DEF_TAG) {
        return new MakoDefTagImpl(node);
      }
      else if (type == DOC_COMMENT) {
        return new MakoDocCommentImpl(node);
      }
      else if (type == EXPRESSION) {
        return new MakoExpressionImpl(node);
      }
      else if (type == INCLUDE_TAG) {
        return new MakoIncludeTagImpl(node);
      }
      else if (type == INHERIT_TAG) {
        return new MakoInheritTagImpl(node);
      }
      else if (type == LINE_COMMENT_RULE) {
        return new MakoLineCommentRuleImpl(node);
      }
      else if (type == MODULE_BLOCK) {
        return new MakoModuleBlockImpl(node);
      }
      else if (type == NAMESPACE_TAG) {
        return new MakoNamespaceTagImpl(node);
      }
      else if (type == PAGE_TAG) {
        return new MakoPageTagImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
