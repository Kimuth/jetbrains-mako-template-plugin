<%inherit file="base.html"/>
<%namespace name="util" file="util.mako"/>
<%def name="greet">
Hello ${name | h}!
</%def>
<%block name="header">
<h1>${title}</h1>
</%block>
<%include file="footer.mako"/>
<% x = 1 %>
<%! import os %>
## This is a comment
<%doc>Documentation</%doc>
% for item in items:
${item}
% endfor
