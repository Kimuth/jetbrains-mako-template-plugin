// This is a generated file. Not intended for manual editing.
package com.schtilig.mako.lang.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.schtilig.mako.lang.psi.MakoTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class MakoParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return makoFile(b, l + 1);
  }

  /* ********************************************************** */
  // TAG_OPEN_BLOCK tag_attribute* TAG_CLOSE item_* END_TAG
  public static boolean block_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, BLOCK_TAG, "<block tag>");
    r = consumeToken(b, TAG_OPEN_BLOCK);
    p = r; // pin = 1
    r = r && report_error_(b, block_tag_1(b, l + 1));
    r = p && report_error_(b, consumeToken(b, TAG_CLOSE)) && r;
    r = p && report_error_(b, block_tag_3(b, l + 1)) && r;
    r = p && consumeToken(b, END_TAG) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean block_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_tag_1", c)) break;
    }
    return true;
  }

  // item_*
  private static boolean block_tag_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "block_tag_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "block_tag_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CODE_OPEN CODE_CONTENT* CODE_CLOSE
  public static boolean code_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "code_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CODE_BLOCK, "<code block>");
    r = consumeToken(b, CODE_OPEN);
    p = r; // pin = 1
    r = r && report_error_(b, code_block_1(b, l + 1));
    r = p && consumeToken(b, CODE_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // CODE_CONTENT*
  private static boolean code_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "code_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, CODE_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "code_block_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // CONTROL_LINE
  public static boolean control_line_stmt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "control_line_stmt")) return false;
    if (!nextTokenIs(b, CONTROL_LINE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CONTROL_LINE);
    exit_section_(b, m, CONTROL_LINE_STMT, r);
    return r;
  }

  /* ********************************************************** */
  // TAG_OPEN_DEF tag_attribute* TAG_CLOSE item_* END_TAG
  public static boolean def_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DEF_TAG, "<def tag>");
    r = consumeToken(b, TAG_OPEN_DEF);
    p = r; // pin = 1
    r = r && report_error_(b, def_tag_1(b, l + 1));
    r = p && report_error_(b, consumeToken(b, TAG_CLOSE)) && r;
    r = p && report_error_(b, def_tag_3(b, l + 1)) && r;
    r = p && consumeToken(b, END_TAG) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean def_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "def_tag_1", c)) break;
    }
    return true;
  }

  // item_*
  private static boolean def_tag_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "def_tag_3")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "def_tag_3", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // DOC_OPEN DOC_CONTENT* DOC_CLOSE
  public static boolean doc_comment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_comment")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DOC_COMMENT, "<doc comment>");
    r = consumeToken(b, DOC_OPEN);
    p = r; // pin = 1
    r = r && report_error_(b, doc_comment_1(b, l + 1));
    r = p && consumeToken(b, DOC_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // DOC_CONTENT*
  private static boolean doc_comment_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "doc_comment_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, DOC_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "doc_comment_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // EXPR_START EXPR_CONTENT* (FILTER_SEP EXPR_CONTENT*)* EXPR_END
  public static boolean expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION, "<expression>");
    r = consumeToken(b, EXPR_START);
    p = r; // pin = 1
    r = r && report_error_(b, expression_1(b, l + 1));
    r = p && report_error_(b, expression_2(b, l + 1)) && r;
    r = p && consumeToken(b, EXPR_END) && r;
    exit_section_(b, l, m, r, p, MakoParser::expression_recover);
    return r || p;
  }

  // EXPR_CONTENT*
  private static boolean expression_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, EXPR_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "expression_1", c)) break;
    }
    return true;
  }

  // (FILTER_SEP EXPR_CONTENT*)*
  private static boolean expression_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expression_2_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expression_2", c)) break;
    }
    return true;
  }

  // FILTER_SEP EXPR_CONTENT*
  private static boolean expression_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, FILTER_SEP);
    r = r && expression_2_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // EXPR_CONTENT*
  private static boolean expression_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_2_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, EXPR_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "expression_2_0_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // !(EXPR_START | EXPR_END | TEMPLATE_TEXT
  //                                | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT
  //                                | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                                | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                                | LINE_COMMENT)
  static boolean expression_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !expression_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // EXPR_START | EXPR_END | TEMPLATE_TEXT
  //                                | TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT
  //                                | TAG_OPEN_INCLUDE | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                                | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                                | LINE_COMMENT
  private static boolean expression_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_recover_0")) return false;
    boolean r;
    r = consumeToken(b, EXPR_START);
    if (!r) r = consumeToken(b, EXPR_END);
    if (!r) r = consumeToken(b, TEMPLATE_TEXT);
    if (!r) r = consumeToken(b, TAG_OPEN_DEF);
    if (!r) r = consumeToken(b, TAG_OPEN_BLOCK);
    if (!r) r = consumeToken(b, TAG_OPEN_INHERIT);
    if (!r) r = consumeToken(b, TAG_OPEN_INCLUDE);
    if (!r) r = consumeToken(b, TAG_OPEN_NAMESPACE);
    if (!r) r = consumeToken(b, TAG_OPEN_PAGE);
    if (!r) r = consumeToken(b, END_TAG);
    if (!r) r = consumeToken(b, CONTROL_LINE);
    if (!r) r = consumeToken(b, CODE_OPEN);
    if (!r) r = consumeToken(b, MODULE_OPEN);
    if (!r) r = consumeToken(b, DOC_OPEN);
    if (!r) r = consumeToken(b, LINE_COMMENT);
    return r;
  }

  /* ********************************************************** */
  // TAG_OPEN_INCLUDE tag_attribute* TAG_CLOSE
  public static boolean include_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, INCLUDE_TAG, "<include tag>");
    r = consumeToken(b, TAG_OPEN_INCLUDE);
    p = r; // pin = 1
    r = r && report_error_(b, include_tag_1(b, l + 1));
    r = p && consumeToken(b, TAG_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean include_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "include_tag_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_INHERIT tag_attribute* TAG_CLOSE
  public static boolean inherit_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inherit_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, INHERIT_TAG, "<inherit tag>");
    r = consumeToken(b, TAG_OPEN_INHERIT);
    p = r; // pin = 1
    r = r && report_error_(b, inherit_tag_1(b, l + 1));
    r = p && consumeToken(b, TAG_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean inherit_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "inherit_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "inherit_tag_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // template_text_content
  //                 | def_tag
  //                 | block_tag
  //                 | inherit_tag
  //                 | include_tag
  //                 | namespace_tag
  //                 | page_tag
  //                 | expression
  //                 | control_line_stmt
  //                 | code_block
  //                 | module_block
  //                 | doc_comment
  //                 | line_comment_rule
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = template_text_content(b, l + 1);
    if (!r) r = def_tag(b, l + 1);
    if (!r) r = block_tag(b, l + 1);
    if (!r) r = inherit_tag(b, l + 1);
    if (!r) r = include_tag(b, l + 1);
    if (!r) r = namespace_tag(b, l + 1);
    if (!r) r = page_tag(b, l + 1);
    if (!r) r = expression(b, l + 1);
    if (!r) r = control_line_stmt(b, l + 1);
    if (!r) r = code_block(b, l + 1);
    if (!r) r = module_block(b, l + 1);
    if (!r) r = doc_comment(b, l + 1);
    if (!r) r = line_comment_rule(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // LINE_COMMENT
  public static boolean line_comment_rule(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "line_comment_rule")) return false;
    if (!nextTokenIs(b, LINE_COMMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LINE_COMMENT);
    exit_section_(b, m, LINE_COMMENT_RULE, r);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean makoFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "makoFile")) return false;
    while (true) {
      int c = current_position_(b);
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "makoFile", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // MODULE_OPEN MODULE_CONTENT* CODE_CLOSE
  public static boolean module_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_block")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, MODULE_BLOCK, "<module block>");
    r = consumeToken(b, MODULE_OPEN);
    p = r; // pin = 1
    r = r && report_error_(b, module_block_1(b, l + 1));
    r = p && consumeToken(b, CODE_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // MODULE_CONTENT*
  private static boolean module_block_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "module_block_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!consumeToken(b, MODULE_CONTENT)) break;
      if (!empty_element_parsed_guard_(b, "module_block_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_NAMESPACE tag_attribute* TAG_CLOSE
  public static boolean namespace_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, NAMESPACE_TAG, "<namespace tag>");
    r = consumeToken(b, TAG_OPEN_NAMESPACE);
    p = r; // pin = 1
    r = r && report_error_(b, namespace_tag_1(b, l + 1));
    r = p && consumeToken(b, TAG_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean namespace_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "namespace_tag_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_OPEN_PAGE tag_attribute* TAG_CLOSE
  public static boolean page_tag(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "page_tag")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PAGE_TAG, "<page tag>");
    r = consumeToken(b, TAG_OPEN_PAGE);
    p = r; // pin = 1
    r = r && report_error_(b, page_tag_1(b, l + 1));
    r = p && consumeToken(b, TAG_CLOSE) && r;
    exit_section_(b, l, m, r, p, MakoParser::tag_recover);
    return r || p;
  }

  // tag_attribute*
  private static boolean page_tag_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "page_tag_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!tag_attribute(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "page_tag_1", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // TAG_ATTR_NAME TAG_ATTR_EQ TAG_ATTR_VALUE
  //                         | TAG_ATTR_NAME
  static boolean tag_attribute(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_attribute")) return false;
    if (!nextTokenIs(b, TAG_ATTR_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parseTokens(b, 0, TAG_ATTR_NAME, TAG_ATTR_EQ, TAG_ATTR_VALUE);
    if (!r) r = consumeToken(b, TAG_ATTR_NAME);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE
  //                         | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                         | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                         | EXPR_START | LINE_COMMENT | TEMPLATE_TEXT)
  static boolean tag_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !tag_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // TAG_OPEN_DEF | TAG_OPEN_BLOCK | TAG_OPEN_INHERIT | TAG_OPEN_INCLUDE
  //                         | TAG_OPEN_NAMESPACE | TAG_OPEN_PAGE
  //                         | END_TAG | CONTROL_LINE | CODE_OPEN | MODULE_OPEN | DOC_OPEN
  //                         | EXPR_START | LINE_COMMENT | TEMPLATE_TEXT
  private static boolean tag_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "tag_recover_0")) return false;
    boolean r;
    r = consumeToken(b, TAG_OPEN_DEF);
    if (!r) r = consumeToken(b, TAG_OPEN_BLOCK);
    if (!r) r = consumeToken(b, TAG_OPEN_INHERIT);
    if (!r) r = consumeToken(b, TAG_OPEN_INCLUDE);
    if (!r) r = consumeToken(b, TAG_OPEN_NAMESPACE);
    if (!r) r = consumeToken(b, TAG_OPEN_PAGE);
    if (!r) r = consumeToken(b, END_TAG);
    if (!r) r = consumeToken(b, CONTROL_LINE);
    if (!r) r = consumeToken(b, CODE_OPEN);
    if (!r) r = consumeToken(b, MODULE_OPEN);
    if (!r) r = consumeToken(b, DOC_OPEN);
    if (!r) r = consumeToken(b, EXPR_START);
    if (!r) r = consumeToken(b, LINE_COMMENT);
    if (!r) r = consumeToken(b, TEMPLATE_TEXT);
    return r;
  }

  /* ********************************************************** */
  // TEMPLATE_TEXT
  public static boolean template_text_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "template_text_content")) return false;
    if (!nextTokenIs(b, TEMPLATE_TEXT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, TEMPLATE_TEXT);
    exit_section_(b, m, TEMPLATE_TEXT_CONTENT, r);
    return r;
  }

}
