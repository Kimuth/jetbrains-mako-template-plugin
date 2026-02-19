package com.github.kimuth.jetbrainsmakotemplateplugin.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static com.github.kimuth.jetbrainsmakotemplateplugin.lang.MakoTokenTypes.*;
import com.intellij.psi.TokenType;

%%

%class _MakoLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%{
  public _MakoLexer() {
    this((java.io.Reader)null);
  }

  private int braceDepth = 0;

  private void resetBraceDepth() { braceDepth = 0; }
%}

%state EXPRESSION
%state TAG_ATTRS
%state CODE_BLOCK
%state MODULE_BLOCK
%state DOC_COMMENT

LineTerminator = \r|\n|\r\n
WhiteSpace     = [ \t]+

%%

<YYINITIAL> {
  // Expression blocks: ${...}
  "${"                             { yybegin(EXPRESSION); resetBraceDepth(); return EXPR_START; }

  // Single-line comment: ## comment text
  "##" [^\r\n]*                    { return LINE_COMMENT; }

  // Doc comment block: <%doc> ... </%doc>
  "<%doc>"                         { yybegin(DOC_COMMENT); return DOC_OPEN; }

  // Module-level code block: <%! ... %>
  "<%!"                            { yybegin(MODULE_BLOCK); return MODULE_OPEN; }

  // Named block tags: <%def, <%block, <%inherit, <%include, <%namespace, <%page
  // Push back the letter so TAG_ATTRS state can read the full tag name as TAG_ATTR_NAME
  "<%" [a-zA-Z]                    { yypushback(1); yybegin(TAG_ATTRS); return TAG_OPEN; }

  // Anonymous code block starting with whitespace: <% code %>
  "<%" {WhiteSpace}                { yypushback(yytext().length() - 2); yybegin(CODE_BLOCK); return CODE_OPEN; }

  // Anonymous code block at end of line: <%\n
  "<%" {LineTerminator}            { yypushback(yytext().length() - 2); yybegin(CODE_BLOCK); return CODE_OPEN; }

  // Closing tags: </%def>, </%block>, etc.
  "</" "%" [a-zA-Z]+ ">"          { return END_TAG; }

  // Escaped percent: %% at line start is literal template text (MUST come before control line rule)
  ^[ \t]* "%%"                     { return TEMPLATE_TEXT; }

  // Control line: % keyword ... at line start (entire line, excluding %%)
  ^[ \t]* "%" [^%\r\n] [^\r\n]*   { return CONTROL_LINE; }

  // Newlines are whitespace
  {LineTerminator}                 { return TokenType.WHITE_SPACE; }

  // Template text: consume runs of non-special characters
  [^$<%\r\n#]+                     { return TEMPLATE_TEXT; }

  // Individual special characters that weren't matched by earlier rules
  [$<%#]                           { return TEMPLATE_TEXT; }

  // Fallback for any remaining character
  [^]                              { return TEMPLATE_TEXT; }
}

<EXPRESSION> {
  // Open brace — increase nesting depth
  "{"                              { braceDepth++; return EXPR_CONTENT; }

  // Close brace — either decrease nesting or end expression
  "}"                              {
                                     if (braceDepth > 0) {
                                       braceDepth--;
                                       return EXPR_CONTENT;
                                     } else {
                                       resetBraceDepth();
                                       yybegin(YYINITIAL);
                                       return EXPR_END;
                                     }
                                   }

  // Filter separator: | not followed by | (Python || stays as EXPR_CONTENT)
  "|" / [^|]                       { return FILTER_SEP; }

  // Python boolean OR: || stays as expression content
  "||"                             { return EXPR_CONTENT; }

  // Single | at end of input (edge case)
  "|"                              { return FILTER_SEP; }

  // String literals — skip contents to avoid false brace/pipe matches
  \" ( [^\"\\] | \\. )* \"        { return EXPR_CONTENT; }
  ' ( [^'\\] | \\. )* '           { return EXPR_CONTENT; }

  // Runs of non-special expression characters
  [^\}\{|\"']+                     { return EXPR_CONTENT; }

  // Fallback for any remaining character in expression
  .                                { return EXPR_CONTENT; }
}

<TAG_ATTRS> {
  // Self-closing or regular close
  "%>"                             { yybegin(YYINITIAL); return TAG_CLOSE; }
  "/>"                             { yybegin(YYINITIAL); return TAG_CLOSE; }

  // Attribute/tag names (also catches the tag keyword itself after TAG_OPEN pushback)
  [a-zA-Z_][a-zA-Z0-9_]*          { return TAG_ATTR_NAME; }

  // Attribute assignment
  "="                              { return TAG_ATTR_EQ; }

  // Attribute values — double or single quoted
  \" [^\"]* \"                     { return TAG_ATTR_VALUE; }
  ' [^']* '                        { return TAG_ATTR_VALUE; }

  // Whitespace between attributes
  {WhiteSpace} | {LineTerminator}  { return TokenType.WHITE_SPACE; }

  // Unexpected character in tag
  .                                { return TokenType.BAD_CHARACTER; }
}

<CODE_BLOCK> {
  // End of anonymous code block
  "%>"                             { yybegin(YYINITIAL); return CODE_CLOSE; }

  // Code content — everything until %>
  [^%]+ | "%" / [^>]              { return CODE_CONTENT; }

  // Lone % at end of input
  "%"                              { return CODE_CONTENT; }
}

<MODULE_BLOCK> {
  // End of module-level code block (reuse CODE_CLOSE token)
  "%>"                             { yybegin(YYINITIAL); return CODE_CLOSE; }

  // Module content — everything until %>
  [^%]+ | "%" / [^>]              { return MODULE_CONTENT; }

  // Lone % at end of input
  "%"                              { return MODULE_CONTENT; }
}

<DOC_COMMENT> {
  // End of doc comment
  "</%doc>"                        { yybegin(YYINITIAL); return DOC_CLOSE; }

  // Doc comment content — everything until </%doc>
  [^<]+ | "<" / [^/]              { return DOC_CONTENT; }

  // Partial close </ that isn't <%
  "</" / [^%]                     { return DOC_CONTENT; }

  // Lone < at end
  "<"                              { return DOC_CONTENT; }

  // Fallback
  [^]                              { return DOC_CONTENT; }
}
