## CRCT-01: Expression in attribute value — expect NO red squiggle on ${cls}
<div class="${cls}">Hello world</div>

## CRCT-02: Control lines — expect NO red squiggles on %for/%endfor
%for item in items:
<li>${item}</li>
%endfor

## HINJ-04: HTML error squiggles — <span> without close SHOULD get squiggle; <p> should NOT
<span>unclosed span
<p>unclosed paragraph

## HINJ-01: HTML syntax coloring — HTML tags should be colored differently from Mako constructs
<div class="container">
  <h1>Title</h1>
  <p>Content</p>
</div>

## HINJ-05: CSS in <style> — type "color:" and check for CSS completions
<style>
body {
  color: red;
}
</style>

## HINJ-06: JS in <script> — type "document." and check for member completions
<script>
document.getElementById("app");
</script>
