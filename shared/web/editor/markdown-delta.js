/*
 * Markdown <-> Quill Delta conversion for the Hugoer article editor.
 *
 * Markdown is parsed with marked's lexer and mapped directly onto Quill
 * Delta ops (no HTML round trip). Anything Quill cannot represent faithfully
 * (tables, raw HTML, Hugo shortcodes, reference definitions, complex lists or
 * quotes) is kept verbatim in `rawblock` / `rawinline` embeds so saving never
 * loses content.
 */
(function (root, factory) {
  if (typeof module === 'object' && module.exports) {
    module.exports = factory;
  } else {
    root.HugoerMarkdown = factory(root.marked, root);
  }
})(typeof self !== 'undefined' ? self : this, function (marked, env) {
  'use strict';

  var BLOCK_EMBEDS = { divider: true, rawblock: true };
  var MAX_INDENT = 8;
  var SHORTCODE = '\\{\\{<[\\s\\S]*?>\\}\\}|\\{\\{%[\\s\\S]*?%\\}\\}';
  var SHORTCODE_BLOCK = new RegExp('^ {0,3}(' + SHORTCODE + ')[ \\t]*(?:\\n+|$)');
  var SHORTCODE_INLINE = new RegExp('^(?:' + SHORTCODE + ')');
  var CJK = /[\u2e80-\u9fff\uac00-\ud7af\uf900-\ufaff\uff00-\uffef\u3000-\u303f]/;

  var shortcodeBlock = {
    name: 'shortcodeBlock',
    level: 'block',
    tokenizer: function (src) {
      var match = SHORTCODE_BLOCK.exec(src);
      if (match) return { type: 'shortcodeBlock', raw: match[0], text: match[1] };
    }
  };

  var shortcodeInline = {
    name: 'shortcodeInline',
    level: 'inline',
    start: function (src) {
      var index = src.search(/\{\{[<%]/);
      return index < 0 ? undefined : index;
    },
    tokenizer: function (src) {
      var match = SHORTCODE_INLINE.exec(src);
      if (match) return { type: 'shortcodeInline', raw: match[0], text: match[0] };
    }
  };

  function createLexer() {
    var instance = new marked.Marked({ gfm: true });
    instance.use({ extensions: [shortcodeBlock, shortcodeInline] });
    return function (markdown) {
      // `Marked#lexer` ignores instance extensions; build the lexer from the instance defaults.
      return new marked.Lexer(instance.defaults).lex(markdown);
    };
  }

  var lex = createLexer();

  // ---------------------------------------------------------------------------
  // Markdown -> Delta
  // ---------------------------------------------------------------------------

  function decodeEntities(text) {
    if (text.indexOf('&') < 0) return text;
    if (env && env.document) {
      var area = env.document.createElement('textarea');
      area.innerHTML = text;
      return area.value;
    }
    return text
      .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"')
      .replace(/&#39;/g, "'").replace(/&nbsp;/g, '\u00a0').replace(/&amp;/g, '&');
  }

  function joinSoftBreaks(text) {
    return text.replace(/[ \t]*\n[ \t]*/g, function (_, offset, whole) {
      var before = whole.charAt(offset - 1);
      var after = whole.charAt(offset + _.length);
      return CJK.test(before) && CJK.test(after) ? '' : ' ';
    });
  }

  function trimTrailingNewlines(raw) {
    return raw.replace(/\n+$/, '');
  }

  function withAttr(attrs, key, value) {
    var next = {};
    for (var k in attrs) next[k] = attrs[k];
    next[key] = value;
    return next;
  }

  function pushText(ops, text, attrs) {
    if (!text) return;
    var op = { insert: text };
    if (attrs && Object.keys(attrs).length) op.attributes = attrs;
    ops.push(op);
  }

  function pushRawInline(ops, raw, attrs) {
    var op = { insert: { rawinline: raw } };
    if (attrs && Object.keys(attrs).length) op.attributes = attrs;
    ops.push(op);
  }

  function hasTokenType(tokens, types) {
    return (tokens || []).some(function (token) {
      return types.indexOf(token.type) >= 0 || hasTokenType(token.tokens, types);
    });
  }

  function inlineToOps(tokens, attrs, ops) {
    (tokens || []).forEach(function (token) {
      switch (token.type) {
        case 'text':
          if (token.tokens && token.tokens.length) inlineToOps(token.tokens, attrs, ops);
          else pushText(ops, decodeEntities(joinSoftBreaks(token.text)), attrs);
          break;
        case 'escape':
          pushText(ops, token.text, attrs);
          break;
        case 'strong':
          inlineToOps(token.tokens, withAttr(attrs, 'bold', true), ops);
          break;
        case 'em':
          inlineToOps(token.tokens, withAttr(attrs, 'italic', true), ops);
          break;
        case 'del':
          inlineToOps(token.tokens, withAttr(attrs, 'strike', true), ops);
          break;
        case 'codespan':
          pushText(ops, token.text, withAttr(attrs, 'code', true));
          break;
        case 'link':
          var isReference = /^\[/.test(token.raw) && !/\]\(/.test(token.raw);
          if (token.title || isReference || hasTokenType(token.tokens, ['image', 'html', 'link', 'shortcodeInline', 'br'])) {
            pushRawInline(ops, token.raw, attrs);
          } else {
            inlineToOps(token.tokens, withAttr(attrs, 'link', token.href), ops);
          }
          break;
        case 'image':
          if (token.title) {
            pushRawInline(ops, token.raw, attrs);
          } else {
            var imageAttrs = withAttr(attrs, 'alt', decodeEntities(token.text || ''));
            ops.push({ insert: { image: token.href }, attributes: imageAttrs });
          }
          break;
        default:
          // br, inline html, shortcodes and anything unknown stay verbatim.
          pushRawInline(ops, token.raw, attrs);
      }
    });
  }

  function pushLine(ops, inlineTokens, blockAttrs) {
    inlineToOps(inlineTokens, {}, ops);
    ops.push(blockAttrs ? { insert: '\n', attributes: blockAttrs } : { insert: '\n' });
  }

  function listIsSimple(list, depth) {
    if (depth >= MAX_INDENT) return false;
    if (list.ordered && list.start !== '' && list.start !== 1) return false;
    return list.items.every(function (item) {
      if (item.task && list.ordered) return false;
      var textCount = 0;
      return item.tokens.every(function (child) {
        if (child.type === 'checkbox' || child.type === 'space') return true;
        if (child.type === 'text' || child.type === 'paragraph') return ++textCount <= 1;
        if (child.type === 'list') return listIsSimple(child, depth + 1);
        return false;
      });
    });
  }

  function listToOps(list, depth, ops) {
    list.items.forEach(function (item) {
      var kind = list.ordered ? 'ordered' : item.task ? (item.checked ? 'checked' : 'unchecked') : 'bullet';
      var attrs = { list: kind };
      if (depth > 0) attrs.indent = depth;
      var text = item.tokens.filter(function (t) { return t.type === 'text' || t.type === 'paragraph'; })[0];
      pushLine(ops, text ? (text.tokens || [{ type: 'text', text: text.text }]) : [], attrs);
      item.tokens.forEach(function (child) {
        if (child.type === 'list') listToOps(child, depth + 1, ops);
      });
    });
  }

  function blockquoteIsSimple(quote) {
    return quote.tokens.every(function (child) {
      return child.type === 'paragraph' || child.type === 'space';
    });
  }

  function markdownToDelta(markdown) {
    var ops = [];
    var tokens = lex(String(markdown || '').replace(/\r\n?/g, '\n'));
    tokens.forEach(function (token) {
      switch (token.type) {
        case 'space':
          break;
        case 'heading':
          pushLine(ops, token.tokens, { header: Math.min(token.depth, 6) });
          break;
        case 'paragraph':
          pushLine(ops, token.tokens, null);
          break;
        case 'code':
          var language = token.lang ? String(token.lang).trim() : '';
          var lines = token.text.split('\n');
          lines.forEach(function (line) {
            if (line) ops.push({ insert: line });
            ops.push({ insert: '\n', attributes: { 'code-block': language || 'plain' } });
          });
          break;
        case 'hr':
          ops.push({ insert: { divider: true } });
          break;
        case 'list':
          if (listIsSimple(token, 0)) listToOps(token, 0, ops);
          else ops.push({ insert: { rawblock: trimTrailingNewlines(token.raw) } });
          break;
        case 'blockquote':
          if (blockquoteIsSimple(token)) {
            token.tokens.forEach(function (child) {
              if (child.type === 'paragraph') pushLine(ops, child.tokens, { blockquote: true });
            });
          } else {
            ops.push({ insert: { rawblock: trimTrailingNewlines(token.raw) } });
          }
          break;
        default:
          // html, table, def, shortcodeBlock and anything unknown.
          ops.push({ insert: { rawblock: trimTrailingNewlines(token.raw) } });
      }
    });
    var last = ops[ops.length - 1];
    if (!last || typeof last.insert !== 'string' || !/\n$/.test(last.insert)) ops.push({ insert: '\n' });
    return { ops: ops };
  }

  // ---------------------------------------------------------------------------
  // Delta -> Markdown
  // ---------------------------------------------------------------------------

  function isWordChar(ch) {
    return !!ch && /[\p{L}\p{N}]/u.test(ch);
  }

  function escapeText(text) {
    var out = '';
    for (var i = 0; i < text.length; i++) {
      var ch = text.charAt(i);
      var next = text.charAt(i + 1);
      if (ch === '\\' || ch === '`' || ch === '*' || ch === '[' || ch === ']') {
        out += '\\' + ch;
      } else if (ch === '_') {
        out += isWordChar(text.charAt(i - 1)) && isWordChar(next) ? ch : '\\' + ch;
      } else if (ch === '~' && (next === '~' || text.charAt(i - 1) === '~')) {
        out += '\\' + ch;
      } else if (ch === '<' && /[A-Za-z\/!?]/.test(next)) {
        out += '\\<';
      } else if (ch === '&' && /^&(#\d+|#x[0-9a-f]+|[a-z][a-z0-9]*);/i.test(text.slice(i))) {
        out += '&amp;';
      } else {
        out += ch;
      }
    }
    return out;
  }

  function escapeLineStart(line) {
    return line
      .replace(/^(#{1,6})(?=\s|$)/, '\\$1')
      .replace(/^>/, '\\>')
      .replace(/^([-+])(?=\s|$)/, '\\$1')
      .replace(/^(-{3,}|={3,})\s*$/, '\\$1')
      .replace(/^(\d{1,9})([.)])(?=\s|$)/, '$1\\$2');
  }

  function codeSpan(text) {
    var longest = 0;
    (text.match(/`+/g) || []).forEach(function (run) { longest = Math.max(longest, run.length); });
    var fence = new Array(longest + 2).join('`');
    var pad = /^`|`$/.test(text) || (/^ .* $/.test(text) && /\S/.test(text)) ? ' ' : '';
    return fence + pad + text + pad + fence;
  }

  function linkDestination(url) {
    url = String(url || '');
    if (/[\s()<>]/.test(url)) return '<' + url.replace(/</g, '%3C').replace(/>/g, '%3E') + '>';
    return url;
  }

  var MARKS = [
    { key: 'bold', open: '**', close: '**' },
    { key: 'italic', open: '*', close: '*' },
    { key: 'strike', open: '~~', close: '~~' }
  ];

  // Serialises inline ops (no link attribute) using a mark stack so nested
  // bold/italic/strike runs stay balanced and whitespace stays outside markers.
  function serializeMarks(ops) {
    var out = '';
    var stack = [];
    var pendingSpace = '';

    function wanted(attrs) {
      return MARKS.filter(function (mark) { return attrs && attrs[mark.key]; });
    }

    function transition(target) {
      var keep = 0;
      while (keep < stack.length && target.indexOf(stack[keep]) >= 0) keep++;
      for (var i = stack.length - 1; i >= keep; i--) out += stack[i].close;
      stack.length = keep;
      out += pendingSpace;
      pendingSpace = '';
      target.forEach(function (mark) {
        if (stack.indexOf(mark) < 0) {
          out += mark.open;
          stack.push(mark);
        }
      });
    }

    ops.forEach(function (op) {
      var attrs = op.attributes || {};
      var piece;
      if (typeof op.insert === 'string') {
        if (!/\S/.test(op.insert)) {
          pendingSpace += op.insert;
          return;
        }
        var match = /^(\s*)([\s\S]*?)(\s*)$/.exec(op.insert);
        pendingSpace += match[1];
        piece = attrs.code ? codeSpan(match[2]) : escapeText(match[2]);
        transition(wanted(attrs));
        out += piece;
        pendingSpace = match[3];
        return;
      }
      if (op.insert.image !== undefined) {
        var alt = String(attrs.alt || '').replace(/([\\\[\]])/g, '\\$1');
        piece = '![' + alt + '](' + linkDestination(op.insert.image) + ')';
      } else if (op.insert.rawinline !== undefined) {
        piece = op.insert.rawinline;
      } else {
        return;
      }
      transition(wanted(attrs));
      out += piece;
    });
    transition([]);
    return out;
  }

  function serializeInline(ops) {
    var out = '';
    var i = 0;
    while (i < ops.length) {
      var href = ops[i].attributes && ops[i].attributes.link;
      if (!href) {
        var plain = [];
        while (i < ops.length && !(ops[i].attributes && ops[i].attributes.link)) plain.push(ops[i++]);
        out += serializeMarks(plain);
        continue;
      }
      var group = [];
      while (i < ops.length && ops[i].attributes && ops[i].attributes.link === href) {
        var copy = { insert: ops[i].insert, attributes: withAttr(ops[i].attributes, 'link', undefined) };
        delete copy.attributes.link;
        group.push(copy);
        i++;
      }
      var label = serializeMarks(group);
      var plainLabel = group.length === 1 && typeof group[0].insert === 'string' &&
        !Object.keys(group[0].attributes).length ? group[0].insert : null;
      if (plainLabel === href && /^https?:\/\/[^\s<>()]+$/.test(href)) {
        out += href;
      } else {
        out += '[' + label + '](' + linkDestination(href) + ')';
      }
    }
    return out;
  }

  function trimLineOps(ops) {
    ops = ops.slice();
    if (ops.length && typeof ops[0].insert === 'string') {
      ops[0] = { insert: ops[0].insert.replace(/^\s+/, ''), attributes: ops[0].attributes };
    }
    var last = ops.length - 1;
    if (last >= 0 && typeof ops[last].insert === 'string') {
      ops[last] = { insert: ops[last].insert.replace(/\s+$/, ''), attributes: ops[last].attributes };
    }
    return ops.filter(function (op) { return op.insert !== ''; });
  }

  function splitLines(delta) {
    var lines = [];
    var current = [];
    (delta.ops || []).forEach(function (op) {
      if (typeof op.insert === 'string') {
        var parts = op.insert.split('\n');
        parts.forEach(function (part, index) {
          if (part) current.push({ insert: part, attributes: op.attributes });
          if (index < parts.length - 1) {
            lines.push({ ops: current, attrs: op.attributes || {} });
            current = [];
          }
        });
      } else if (op.insert && typeof op.insert === 'object') {
        var key = Object.keys(op.insert)[0];
        if (BLOCK_EMBEDS[key]) {
          if (current.length) {
            lines.push({ ops: current, attrs: {} });
            current = [];
          }
          lines.push({ embed: key, value: op.insert[key] });
        } else {
          current.push(op);
        }
      }
    });
    if (current.length) lines.push({ ops: current, attrs: {} });
    return lines;
  }

  function codeLanguage(attrs) {
    var value = attrs['code-block'];
    return typeof value === 'string' && value !== 'plain' ? value : '';
  }

  function serializeCode(lines) {
    var text = lines.map(function (line) {
      return line.ops.map(function (op) { return typeof op.insert === 'string' ? op.insert : ''; }).join('');
    }).join('\n');
    var longest = 2;
    (text.match(/^`{3,}/gm) || []).forEach(function (run) { longest = Math.max(longest, run.length); });
    var fence = new Array(longest + 2).join('`');
    return fence + codeLanguage(lines[0].attrs) + '\n' + text + '\n' + fence;
  }

  // A change between ordered and unordered items at the top level starts a new
  // Markdown list; keep a blank line between them for readability.
  function serializeLists(lines) {
    var groups = [];
    lines.forEach(function (line) {
      var ordered = line.attrs.list === 'ordered';
      var top = !(Number(line.attrs.indent) > 0);
      var group = groups[groups.length - 1];
      if (!group || (top && group.ordered !== ordered)) groups.push(group = { ordered: ordered, lines: [] });
      group.lines.push(line);
    });
    return groups.map(function (group) { return serializeList(group.lines); }).join('\n\n');
  }

  function serializeList(lines) {
    var widths = [];
    var counters = [];
    var kinds = [];
    return lines.map(function (line) {
      var level = Math.max(0, Math.min(MAX_INDENT, Number(line.attrs.indent) || 0));
      var kind = line.attrs.list;
      var ordered = kind === 'ordered';
      counters.length = level + 1;
      widths.length = level + 1;
      if (kinds[level] !== (ordered ? 'ordered' : 'bullet') || counters[level] === undefined) counters[level] = 0;
      kinds.length = level + 1;
      kinds[level] = ordered ? 'ordered' : 'bullet';
      counters[level] += 1;
      var marker = ordered ? counters[level] + '.' :
        kind === 'checked' ? '- [x]' : kind === 'unchecked' ? '- [ ]' : '-';
      var indent = '';
      for (var i = 0; i < level; i++) indent += new Array((widths[i] || 2) + 1).join(' ');
      widths[level] = (ordered ? String(counters[level]).length + 2 : 2);
      var content = escapeLineStart(serializeInline(trimLineOps(line.ops)));
      return (indent + marker + (content ? ' ' + content : '')).replace(/\s+$/, '');
    }).join('\n');
  }

  function lineKind(line) {
    if (line.embed) return 'embed';
    if (line.attrs['code-block']) return 'code';
    if (line.attrs.list) return 'list';
    if (line.attrs.blockquote) return 'quote';
    if (line.attrs.header) return 'header';
    return 'paragraph';
  }

  function deltaToMarkdown(delta) {
    var lines = splitLines(delta);
    var blocks = [];
    var i = 0;
    while (i < lines.length) {
      var line = lines[i];
      var kind = lineKind(line);
      if (kind === 'embed') {
        if (line.embed === 'divider') blocks.push('---');
        else if (String(line.value).trim()) blocks.push(String(line.value));
        i++;
      } else if (kind === 'code') {
        var language = codeLanguage(line.attrs);
        var group = [];
        while (i < lines.length && lineKind(lines[i]) === 'code' && codeLanguage(lines[i].attrs) === language) group.push(lines[i++]);
        blocks.push(serializeCode(group));
      } else if (kind === 'list') {
        var items = [];
        while (i < lines.length && lineKind(lines[i]) === 'list') items.push(lines[i++]);
        blocks.push(serializeLists(items));
      } else if (kind === 'quote') {
        var quoted = [];
        while (i < lines.length && lineKind(lines[i]) === 'quote') {
          var text = escapeLineStart(serializeInline(trimLineOps(lines[i++].ops)));
          if (text) quoted.push('> ' + text);
        }
        if (quoted.length) blocks.push(quoted.join('\n>\n'));
      } else {
        var content = serializeInline(trimLineOps(line.ops));
        i++;
        if (!content) continue;
        if (kind === 'header') {
          var level = Math.max(1, Math.min(6, Number(line.attrs.header) || 1));
          blocks.push(new Array(level + 1).join('#') + ' ' + escapeLineStart(content).replace(/#$/, '\\#'));
        } else {
          blocks.push(escapeLineStart(content));
        }
      }
    }
    return blocks.length ? blocks.join('\n\n') + '\n' : '';
  }

  return {
    markdownToDelta: markdownToDelta,
    deltaToMarkdown: deltaToMarkdown
  };
});
