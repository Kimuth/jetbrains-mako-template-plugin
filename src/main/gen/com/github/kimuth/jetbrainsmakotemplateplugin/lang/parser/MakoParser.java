// This is a generated file. Not intended for manual editing.
package com.github.kimuth.jetbrainsmakotemplateplugin.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.github.kimuth.jetbrainsmakotemplateplugin.lang.psi.MakoTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MakoParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return makoFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // TAG_OPEN_BLOCK tag_attribute* TAG_CLOSE item_* END_TAG
  public static boolean block_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, BLOCK_TAG, "<block tag>");
    result_ = consumeToken(builder_, TAG_OPEN_BLOCK);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, block_tag_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, TAG_CLOSE)) && result_;
    result_ = pinned_ && report_error_(builder_, block_tag_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, END_TAG) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean block_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "block_tag_1", pos_)) break;
    }
    return true;
  }

  // item_*
  private static boolean block_tag_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "block_tag_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!item_(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "block_tag_3", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CODE_OPEN CODE_CONTENT* CODE_CLOSE
  public static boolean code_block(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_block")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CODE_BLOCK, "<code block>");
    result_ = consumeToken(builder_, CODE_OPEN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, code_block_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, CODE_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // CODE_CONTENT*
  private static boolean code_block_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_block_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, CODE_CONTENT)) break;
      if (!empty_element_parsed_guard_(builder_, "code_block_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CONTROL_LINE
  public static boolean control_line(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "control_line")) return false;
    if (!nextTokenIs(builder_, CONTROL_LINE)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CONTROL_LINE);
    exit_section_(builder_, marker_, CONTROL_LINE, result_);
    return result_;
  }

  /* ********************************************************** */
  // TAG_OPEN_DEF tag_attribute* TAG_CLOSE item_* END_TAG
  public static boolean def_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "def_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DEF_TAG, "<def tag>");
    result_ = consumeToken(builder_, TAG_OPEN_DEF);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, def_tag_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, consumeToken(builder_, TAG_CLOSE)) && result_;
    result_ = pinned_ && report_error_(builder_, def_tag_3(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, END_TAG) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean def_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "def_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "def_tag_1", pos_)) break;
    }
    return true;
  }

  // item_*
  private static boolean def_tag_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "def_tag_3")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!item_(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "def_tag_3", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // DOC_OPEN DOC_CONTENT* DOC_CLOSE
  public static boolean doc_comment(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doc_comment")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, DOC_COMMENT, "<doc comment>");
    result_ = consumeToken(builder_, DOC_OPEN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, doc_comment_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, DOC_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // DOC_CONTENT*
  private static boolean doc_comment_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "doc_comment_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, DOC_CONTENT)) break;
      if (!empty_element_parsed_guard_(builder_, "doc_comment_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END
  public static boolean expression(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, EXPRESSION, "<expression>");
    result_ = consumeToken(builder_, EXPR_START);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, expression_1(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, expression_2(builder_, level_ + 1)) && result_;
    result_ = pinned_ && consumeToken(builder_, EXPR_END) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::expression_recover);
    return result_ || pinned_;
  }

  // EXPR_CONTENT*
  private static boolean expression_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, EXPR_CONTENT)) break;
      if (!empty_element_parsed_guard_(builder_, "expression_1", pos_)) break;
    }
    return true;
  }

  // (FILTER_SEP EXPR_CONTENT*)*
  private static boolean expression_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_2")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!expression_2_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "expression_2", pos_)) break;
    }
    return true;
  }

  // FILTER_SEP EXPR_CONTENT*
  private static boolean expression_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, FILTER_SEP);
    result_ = result_ && expression_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // EXPR_CONTENT*
  private static boolean expression_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_2_0_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, EXPR_CONTENT)) break;
      if (!empty_element_parsed_guard_(builder_, "expression_2_0_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // !(EXPR_END | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT
  //                                | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                                | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                                | LINE_COMMENT)
  static boolean expression_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !expression_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // EXPR_END | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT
  //                                | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                                | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                                | LINE_COMMENT
  private static boolean expression_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "expression_recover_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, EXPR_END);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_DEF);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_BLOCK);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_INHERIT);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_INCLUDE);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_NAMESPACE);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_PAGE);
    if (!result_) result_ = consumeToken(builder_, END_TAG);
    if (!result_) result_ = consumeToken(builder_, CONTROL_LINE);
    if (!result_) result_ = consumeToken(builder_, CODE_OPEN);
    if (!result_) result_ = consumeToken(builder_, MODULE_OPEN);
    if (!result_) result_ = consumeToken(builder_, DOC_OPEN);
    if (!result_) result_ = consumeToken(builder_, LINE_COMMENT);
    return result_;
  }

  /* ********************************************************** */
  // TAG_OPEN_INCLUDE tag_attribute* TAG_CLOSE
  public static boolean include_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "include_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INCLUDE_TAG, "<include tag>");
    result_ = consumeToken(builder_, TAG_OPEN_INCLUDE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, include_tag_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, TAG_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean include_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "include_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "include_tag_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_INHERIT tag_attribute* TAG_CLOSE
  public static boolean inherit_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, INHERIT_TAG, "<inherit tag>");
    result_ = consumeToken(builder_, TAG_OPEN_INHERIT);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, inherit_tag_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, TAG_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean inherit_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "inherit_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "inherit_tag_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // def_tag
  //                 | block_tag
  //                 | inherit_tag
  //                 | include_tag
  //                 | namespace_tag
  //                 | page_tag
  //                 | expression
  //                 | control_line
  //                 | code_block
  //                 | module_block
  //                 | doc_comment
  //                 | line_comment_rule
  //                 | TEMPLATE_TEXT
  static boolean item_(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "item_")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = def_tag(builder_, level_ + 1);
    if (!result_) result_ = block_tag(builder_, level_ + 1);
    if (!result_) result_ = inherit_tag(builder_, level_ + 1);
    if (!result_) result_ = include_tag(builder_, level_ + 1);
    if (!result_) result_ = namespace_tag(builder_, level_ + 1);
    if (!result_) result_ = page_tag(builder_, level_ + 1);
    if (!result_) result_ = expression(builder_, level_ + 1);
    if (!result_) result_ = control_line(builder_, level_ + 1);
    if (!result_) result_ = code_block(builder_, level_ + 1);
    if (!result_) result_ = module_block(builder_, level_ + 1);
    if (!result_) result_ = doc_comment(builder_, level_ + 1);
    if (!result_) result_ = line_comment_rule(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, TEMPLATE_TEXT);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // LINE_COMMENT
  public static boolean line_comment_rule(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "line_comment_rule")) return false;
    if (!nextTokenIs(builder_, LINE_COMMENT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, LINE_COMMENT);
    exit_section_(builder_, marker_, LINE_COMMENT_RULE, result_);
    return result_;
  }

  /* ********************************************************** */
  // item_*
  static boolean makoFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "makoFile")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!item_(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "makoFile", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MODULE_OPEN MODULE_CONTENT* CODE_CLOSE
  public static boolean module_block(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "module_block")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, MODULE_BLOCK, "<module block>");
    result_ = consumeToken(builder_, MODULE_OPEN);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, module_block_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, CODE_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // MODULE_CONTENT*
  private static boolean module_block_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "module_block_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!consumeToken(builder_, MODULE_CONTENT)) break;
      if (!empty_element_parsed_guard_(builder_, "module_block_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_NAMESPACE tag_attribute* TAG_CLOSE
  public static boolean namespace_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namespace_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, NAMESPACE_TAG, "<namespace tag>");
    result_ = consumeToken(builder_, TAG_OPEN_NAMESPACE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, namespace_tag_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, TAG_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean namespace_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "namespace_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "namespace_tag_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_PAGE tag_attribute* TAG_CLOSE
  public static boolean page_tag(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "page_tag")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, PAGE_TAG, "<page tag>");
    result_ = consumeToken(builder_, TAG_OPEN_PAGE);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, page_tag_1(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, TAG_CLOSE) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, MakoParser::tag_recover);
    return result_ || pinned_;
  }

  // tag_attribute*
  private static boolean page_tag_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "page_tag_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!tag_attribute(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "page_tag_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE
  //                         | TAG_ATTR_NAME
  static boolean tag_attribute(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tag_attribute")) return false;
    if (!nextTokenIs(builder_, TAG_ATTR_NAME)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = parseTokens(builder_, 0, TAG_ATTR_NAME, TAG_ATTR_EQ, TAG_ATTR_VALUE);
    if (!result_) result_ = consumeToken(builder_, TAG_ATTR_NAME);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // !(TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE
  //                         | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                         | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                         | EXPR_START | LINE_COMMENT)
  static boolean tag_recover(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tag_recover")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !tag_recover_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE
  //                         | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                         | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                         | EXPR_START | LINE_COMMENT
  private static boolean tag_recover_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "tag_recover_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, TAG_OPEN_DEF);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_BLOCK);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_INHERIT);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_INCLUDE);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_NAMESPACE);
    if (!result_) result_ = consumeToken(builder_, TAG_OPEN_PAGE);
    if (!result_) result_ = consumeToken(builder_, END_TAG);
    if (!result_) result_ = consumeToken(builder_, CONTROL_LINE);
    if (!result_) result_ = consumeToken(builder_, CODE_OPEN);
    if (!result_) result_ = consumeToken(builder_, MODULE_OPEN);
    if (!result_) result_ = consumeToken(builder_, DOC_OPEN);
    if (!result_) result_ = consumeToken(builder_, EXPR_START);
    if (!result_) result_ = consumeToken(builder_, LINE_COMMENT);
    return result_;
  }

}
