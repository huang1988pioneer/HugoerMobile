// Run with: node --test shared/tests/*.test.cjs
const test = require('node:test');
const assert = require('node:assert/strict');
const marked = require('../web/editor/vendor/marked.umd.js');
const { markdownToDelta, deltaToMarkdown } = require('../web/editor/markdown-delta.js')(marked, null);

const roundTrip = (markdown) => deltaToMarkdown(markdownToDelta(markdown));

function assertStable(markdown) {
  const once = roundTrip(markdown);
  assert.equal(roundTrip(once), once, 'serialisation should be idempotent');
  return once;
}

test('canonical markdown round-trips unchanged', () => {
  const markdown = [
    '# Title with *emphasis*',
    '',
    'A paragraph with **bold**, *italic*, ~~strike~~, `code` and [a link](https://example.com/a_b).',
    '',
    '## Lists',
    '',
    '- one',
    '  - nested',
    '    1. deep',
    '- [x] done',
    '- [ ] todo',
    '',
    '1. first',
    '2. second',
    '',
    '> quoted text',
    '',
    '```go',
    'x := 1',
    '',
    'fmt.Println(x)',
    '```',
    '',
    '---',
    '',
    '![cover image](/images/cover.png)',
    '',
  ].join('\n');
  assert.equal(roundTrip(markdown), markdown);
});

test('hugo shortcodes, tables and html are preserved verbatim', () => {
  const markdown = [
    '{{< figure src="a_b.png" title="x*y" >}}',
    '',
    '| a | b |',
    '|---|---|',
    '| 1 | 2 |',
    '',
    '<div class="note">',
    'raw *html*',
    '</div>',
    '',
    'Inline {{< ref "posts/a_b.md" >}} and <kbd>Ctrl</kbd> stay.',
    '',
    '{{% notice info %}}',
    'Body',
    '{{% /notice %}}',
    '',
  ].join('\n');
  const out = assertStable(markdown);
  assert.match(out, /\{\{< figure src="a_b\.png" title="x\*y" >\}\}/);
  assert.match(out, /\| a \| b \|\n\|---\|---\|\n\| 1 \| 2 \|/);
  assert.match(out, /<div class="note">\nraw \*html\*\n<\/div>/);
  assert.match(out, /Inline \{\{< ref "posts\/a_b\.md" >\}\} and <kbd>Ctrl<\/kbd> stay\./);
  assert.match(out, /\{\{% notice info %\}\}/);
  assert.match(out, /\{\{% \/notice %\}\}/);
});

test('literal markdown characters in text are escaped', () => {
  const delta = { ops: [
    { insert: '# not a heading *not em* snake_case _x_ <div> 1. &copy;\n' },
    { insert: '- not a list\n' },
  ] };
  const out = deltaToMarkdown(delta);
  assert.equal(out, '\\# not a heading \\*not em\\* snake_case \\_x\\_ \\<div> 1. &amp;copy;\n\n\\- not a list\n');
  assert.deepEqual(markdownToDelta(out).ops.map((op) => op.insert).join(''),
    '# not a heading *not em* snake_case _x_ <div> 1. &copy;\n- not a list\n');
});

test('marks keep surrounding whitespace outside delimiters', () => {
  const delta = { ops: [
    { insert: 'a ' },
    { insert: 'bold ', attributes: { bold: true } },
    { insert: 'both', attributes: { bold: true, italic: true } },
    { insert: ' end\n' },
  ] };
  const out = deltaToMarkdown(delta);
  assert.equal(out, 'a **bold *both*** end\n');
  assertStable(out);
});

test('complex structures fall back to raw blocks', () => {
  const markdown = '> - quoted list\n\n3. starts at three\n4. next\n\n- item\n\n  second paragraph\n';
  const ops = markdownToDelta(markdown).ops.filter((op) => typeof op.insert === 'object');
  assert.equal(ops.length, 3);
  ops.forEach((op) => assert.ok(op.insert.rawblock));
  assert.equal(roundTrip(markdown), markdown);
});

test('soft line breaks are joined, hard breaks kept', () => {
  assert.equal(roundTrip('中文第一行\n第二行\nand english\nwrap\n'), '中文第一行第二行 and english wrap\n');
  assert.equal(roundTrip('line one  \nline two\n'), 'line one  \nline two\n');
});

test('code fences grow when content contains backticks', () => {
  const delta = { ops: [
    { insert: '```' }, { insert: '\n', attributes: { 'code-block': 'md' } },
    { insert: 'a `tick`', attributes: { code: true } }, { insert: '\n' },
  ] };
  assert.equal(deltaToMarkdown(delta), '````md\n```\n````\n\n`` a `tick` ``\n');
});

test('bare urls and entities', () => {
  assert.equal(roundTrip('Visit https://example.com today &amp; <https://x.dev>\n'),
    'Visit https://example.com today & https://x.dev\n');
});

test('empty documents', () => {
  assert.equal(roundTrip(''), '');
  assert.deepEqual(markdownToDelta('').ops, [{ insert: '\n' }]);
});
