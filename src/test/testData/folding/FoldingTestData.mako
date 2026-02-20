<fold text='<%doc>...</%doc>'><%doc>
This documentation should be collapsed by default.
</%doc></fold>

<fold text='<%!...%>'><%!
    import os
%></fold>

<fold text='...'><%def name="greet">
Hello ${name}!
</%def></fold>

<fold text='...'><%block name="header">
<h1>Title</h1>
</%block></fold>

<fold text='...'>% for item in items:
    ${item}
% endfor</fold>

<fold text='...'>% if show:
    <p>Visible</p>
% endif</fold>

<fold text='...'>% while count > 0:
    <p>Counting</p>
% endwhile</fold>
