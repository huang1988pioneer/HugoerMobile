/*
 * Hugoer rich Markdown editor (Quill) hosted by the native iOS / Android apps.
 *
 * Native -> page:  window.HugoerEditor.setContent({ markdown, baseURL, documentPath, dark })
 * Page -> native:  JSON strings { type: 'ready' | 'change' | 'error', ... } posted to
 *                  webkit.messageHandlers.hugoer (iOS) or hugoerBridge (Android).
 */
(function () {
  'use strict';

  var Delta = Quill.import('delta');
  var BlockEmbed = Quill.import('blots/block/embed');
  var Embed = Quill.import('blots/embed');
  var Link = Quill.import('formats/link');
  var Image = Quill.import('formats/image');
  var CodeBlock = Quill.import('formats/code-block');

  var context = { baseURL: '', documentPath: '' };

  // ---------------------------------------------------------------------------
  // Native bridge
  // ---------------------------------------------------------------------------

  function post(message) {
    var payload = JSON.stringify(message);
    try {
      if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.hugoer) {
        window.webkit.messageHandlers.hugoer.postMessage(payload);
      } else if (window.hugoerBridge) {
        window.hugoerBridge.postMessage(payload);
      }
    } catch (error) {
      // The host went away; nothing useful to do.
    }
    document.dispatchEvent(new CustomEvent('hugoer:message', { detail: message }));
  }

  window.addEventListener('error', function (event) {
    post({ type: 'error', message: String(event.message || event.error || 'unknown error') });
  });

  // ---------------------------------------------------------------------------
  // URL helpers
  // ---------------------------------------------------------------------------

  function siteBase() {
    var base = String(context.baseURL || '').trim();
    if (!base) return null;
    if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(base)) base = 'https://' + base.replace(/^\/+/, '');
    if (!/\/$/.test(base)) base += '/';
    try {
      return new URL(base);
    } catch (error) {
      return null;
    }
  }

  // content/posts/hello/index.md -> posts/hello/ ; content/posts/hello.md -> posts/hello/
  function pagePath() {
    var path = String(context.documentPath || '').replace(/^\/+/, '').replace(/^content\//, '');
    path = path.replace(/\.[^\/.]+$/, '').replace(/(^|\/)_?index$/, '');
    return path ? path.replace(/\/?$/, '/') : '';
  }

  function displayImageURL(src) {
    src = String(src || '').trim();
    if (/^https:\/\//i.test(src) || /^data:image\//i.test(src)) return src;
    if (/^\/\//.test(src)) return 'https:' + src;
    if (/^[a-z][a-z0-9+.-]*:/i.test(src)) return null;
    var base = siteBase();
    if (!base) return null;
    try {
      var resolved = /^\//.test(src)
        ? new URL(src.replace(/^\/+/, ''), base)
        : new URL(src, new URL(pagePath(), base));
      return resolved.protocol === 'https:' ? resolved.href : null;
    } catch (error) {
      return null;
    }
  }

  // ---------------------------------------------------------------------------
  // Blots
  // ---------------------------------------------------------------------------

  // Links keep their original (possibly site-relative) target for Markdown while
  // the DOM only ever points at a safe URL.
  class MarkdownLink extends Link {
    static create(value) {
      var node = super.create(value);
      node.setAttribute('data-md-href', value);
      return node;
    }

    static formats(domNode) {
      return domNode.getAttribute('data-md-href') || domNode.getAttribute('href');
    }

    static sanitize(url) {
      return /^(https?:|mailto:|tel:)/i.test(String(url)) ? url : this.SANITIZED_URL;
    }

    format(name, value) {
      super.format(name, value);
      if (name === this.statics.blotName && value) this.domNode.setAttribute('data-md-href', value);
    }
  }

  // Images keep the Markdown source path and display a resolved HTTPS URL.
  class MarkdownImage extends Image {
    static create(value) {
      var node = super.create(value);
      node.setAttribute('data-md-src', value);
      var display = displayImageURL(value);
      if (display) {
        node.setAttribute('src', display);
      } else {
        node.removeAttribute('src');
        node.classList.add('md-image-unavailable');
      }
      return node;
    }

    static sanitize(url) {
      return url;
    }

    static value(domNode) {
      return domNode.getAttribute('data-md-src') || domNode.getAttribute('src');
    }
  }

  // Code blocks remember their fence language.
  class MarkdownCodeBlock extends CodeBlock {
    static create(value) {
      var node = super.create(value);
      if (typeof value === 'string' && value !== 'plain') node.setAttribute('data-language', value);
      return node;
    }

    static formats(domNode) {
      return domNode.getAttribute('data-language') || 'plain';
    }
  }

  class Divider extends BlockEmbed {
    static create() {
      var node = super.create();
      node.setAttribute('contenteditable', 'false');
      return node;
    }

    static value() {
      return true;
    }
  }
  Divider.blotName = 'divider';
  Divider.tagName = 'HR';

  function rawLabel(raw) {
    var text = String(raw).trim();
    if (/^\{\{[<%]/.test(text)) return 'Hugo 短代碼 · 點一下編輯';
    if (/^\|/.test(text) || /\n\s*\|?\s*:?-{3,}/.test(text)) return '表格 · 點一下編輯';
    if (/^</.test(text)) return 'HTML · 點一下編輯';
    if (/^\[[^\]]+\]:/.test(text)) return '參考連結 · 點一下編輯';
    return 'Markdown 原始碼 · 點一下編輯';
  }

  // Markdown the rich editor cannot represent, kept verbatim.
  class RawBlock extends BlockEmbed {
    static create(value) {
      var node = super.create();
      node.setAttribute('contenteditable', 'false');
      node.setAttribute('data-raw', value);
      node.setAttribute('data-label', rawLabel(value));
      node.textContent = value;
      return node;
    }

    static value(domNode) {
      return domNode.getAttribute('data-raw') || '';
    }
  }
  RawBlock.blotName = 'rawblock';
  RawBlock.tagName = 'DIV';
  RawBlock.className = 'md-raw';

  class RawInline extends Embed {
    static create(value) {
      var node = super.create();
      node.setAttribute('data-raw', value);
      node.textContent = /^(?: {2,}|\\)\n$/.test(value) ? '↵' : String(value).replace(/\s*\n\s*/g, ' ');
      return node;
    }

    static value(domNode) {
      return domNode.getAttribute('data-raw') || '';
    }
  }
  RawInline.blotName = 'rawinline';
  RawInline.tagName = 'SPAN';
  RawInline.className = 'md-raw-inline';

  Quill.register({
    'formats/link': MarkdownLink,
    'formats/image': MarkdownImage,
    'formats/code-block': MarkdownCodeBlock,
    'formats/divider': Divider,
    'formats/rawblock': RawBlock,
    'formats/rawinline': RawInline
  }, true);

  // ---------------------------------------------------------------------------
  // Dialog (window.prompt is unavailable inside native web views)
  // ---------------------------------------------------------------------------

  var dialog = document.getElementById('dialog');
  var dialogForm = dialog.querySelector('form');

  function openDialog(options) {
    return new Promise(function (resolve) {
      var fields = dialog.querySelector('.md-dialog-fields');
      var actions = dialog.querySelector('.md-dialog-actions');
      dialog.querySelector('.md-dialog-title').textContent = options.title;
      fields.textContent = '';
      actions.textContent = '';

      var inputs = {};
      options.fields.forEach(function (field) {
        var label = document.createElement('label');
        label.textContent = field.label;
        var input = document.createElement(field.multiline ? 'textarea' : 'input');
        if (!field.multiline) input.type = 'text';
        // Site-relative paths are valid here, so avoid type=url validation and only ask for the URL keyboard.
        if (field.url) input.inputMode = 'url';
        input.value = field.value || '';
        input.placeholder = field.placeholder || '';
        input.autocapitalize = 'off';
        input.spellcheck = false;
        label.appendChild(input);
        fields.appendChild(label);
        inputs[field.name] = input;
      });

      function finish(action) {
        dialog.hidden = true;
        dialog.onclick = null;
        dialogForm.onsubmit = null;
        if (!action) return resolve(null);
        var values = {};
        Object.keys(inputs).forEach(function (name) { values[name] = inputs[name].value; });
        resolve({ action: action, values: values });
      }

      var primary = null;
      options.actions.forEach(function (action) {
        var button = document.createElement('button');
        button.type = action.primary ? 'submit' : 'button';
        button.textContent = action.label;
        if (action.primary) { button.className = 'primary'; primary = action.value; }
        if (action.danger) button.className = 'danger';
        if (!action.primary) button.onclick = function () { finish(action.value); };
        actions.appendChild(button);
      });

      dialogForm.onsubmit = function (event) {
        event.preventDefault();
        finish(primary);
      };
      dialog.onclick = function (event) {
        if (event.target === dialog) finish(null);
      };
      dialog.hidden = false;
      var first = fields.querySelector('input, textarea');
      if (first) first.focus();
    });
  }

  // ---------------------------------------------------------------------------
  // Editor
  // ---------------------------------------------------------------------------

  function insertBlock(type, value) {
    var range = quill.getSelection(true) || { index: quill.getLength(), length: 0 };
    quill.deleteText(range.index, range.length, 'user');
    quill.insertEmbed(range.index, type, value, 'user');
    quill.setSelection(range.index + 1, 0, 'silent');
  }

  function editLink(value) {
    var range = quill.getSelection(true);
    if (!range) return;
    if (value === false) {
      quill.format('link', false, 'user');
      return;
    }
    var current = quill.getFormat(range).link || '';
    var fields = [{ name: 'url', label: '網址（可用 /posts/... 站內路徑）', value: current, placeholder: 'https://', url: true }];
    if (!range.length) fields.unshift({ name: 'text', label: '顯示文字', value: '' });
    openDialog({
      title: current ? '編輯連結' : '插入連結',
      fields: fields,
      actions: [
        { label: '取消', value: 'cancel' },
        { label: current ? '移除連結' : '', value: 'remove', danger: true },
        { label: '套用', value: 'apply', primary: true }
      ].filter(function (action) { return action.label; })
    }).then(function (result) {
      quill.focus();
      if (!result || result.action === 'cancel') return;
      var url = result.values.url.trim();
      if (result.action === 'remove' || !url) {
        quill.formatText(range.index, range.length, 'link', false, 'user');
      } else if (range.length) {
        quill.formatText(range.index, range.length, 'link', url, 'user');
      } else {
        var text = result.values.text.trim() || url;
        quill.insertText(range.index, text, { link: url }, 'user');
        quill.setSelection(range.index + text.length, 0, 'silent');
      }
    });
  }

  function insertImage() {
    var range = quill.getSelection(true) || { index: quill.getLength(), length: 0 };
    openDialog({
      title: '插入圖片',
      fields: [
        { name: 'src', label: '圖片網址（可用 /images/... 或相對路徑）', placeholder: '/images/cover.png', url: true },
        { name: 'alt', label: '替代文字' }
      ],
      actions: [{ label: '取消', value: 'cancel' }, { label: '插入', value: 'insert', primary: true }]
    }).then(function (result) {
      quill.focus();
      if (!result || result.action !== 'insert' || !result.values.src.trim()) return;
      quill.updateContents(new Delta()
        .retain(range.index)
        .delete(range.length)
        .insert({ image: result.values.src.trim() }, { alt: result.values.alt.trim() }), 'user');
      quill.setSelection(range.index + 1, 0, 'silent');
    });
  }

  function insertRaw() {
    openDialog({
      title: '插入 Markdown／Hugo 短代碼',
      fields: [{ name: 'raw', label: '內容會原封不動寫入文章', multiline: true, placeholder: '{{< figure src="/images/cover.png" >}}' }],
      actions: [{ label: '取消', value: 'cancel' }, { label: '插入', value: 'insert', primary: true }]
    }).then(function (result) {
      quill.focus();
      if (result && result.action === 'insert' && result.values.raw.trim()) {
        insertBlock('rawblock', result.values.raw.replace(/\s+$/, ''));
      }
    });
  }

  function editRaw(node) {
    var blot = Quill.find(node);
    if (!blot || blot === quill) return;
    var index = quill.getIndex(blot);
    var inline = node.classList.contains('md-raw-inline');
    var key = inline ? 'rawinline' : 'rawblock';
    var attributes = inline ? (quill.getContents(index, 1).ops[0] || {}).attributes : undefined;
    openDialog({
      title: inline ? '編輯行內原始碼' : '編輯 Markdown 原始碼',
      fields: [{ name: 'raw', label: '內容會原封不動寫入文章', value: node.getAttribute('data-raw'), multiline: true }],
      actions: [
        { label: '刪除', value: 'delete', danger: true },
        { label: '取消', value: 'cancel' },
        { label: '套用', value: 'apply', primary: true }
      ]
    }).then(function (result) {
      if (!result || result.action === 'cancel') return;
      var change = new Delta().retain(index).delete(1);
      var raw = inline ? result.values.raw : result.values.raw.replace(/\s+$/, '');
      if (result.action === 'apply' && raw.trim()) {
        var insert = {};
        insert[key] = raw;
        change = change.insert(insert, attributes);
      }
      quill.updateContents(change, 'user');
    });
  }

  function markdownShortcut(format, value) {
    return function (range, context) {
      var start = range.index - context.prefix.length;
      this.quill.deleteText(start, context.prefix.length, 'user');
      this.quill.formatLine(start, 1, format, value, 'user');
      this.quill.setSelection(start, 0, 'silent');
    };
  }

  var quill = new Quill('#editor', {
    theme: 'snow',
    placeholder: '開始撰寫內文…',
    formats: [
      'header', 'bold', 'italic', 'strike', 'code', 'link',
      'blockquote', 'code-block', 'list', 'indent', 'image',
      'divider', 'rawblock', 'rawinline'
    ],
    modules: {
      toolbar: {
        container: '#toolbar',
        handlers: {
          undo: function () { quill.history.undo(); },
          redo: function () { quill.history.redo(); },
          link: editLink,
          image: insertImage,
          divider: function () { insertBlock('divider', true); },
          rawblock: insertRaw
        }
      },
      history: { delay: 600, maxStack: 200, userOnly: true },
      keyboard: {
        bindings: {
          'markdown heading': {
            key: ' ', collapsed: true, prefix: /^#{1,6}$/, format: { 'code-block': false, list: false },
            handler: function (range, context) {
              markdownShortcut('header', context.prefix.length).call(this, range, context);
            }
          },
          'markdown quote': {
            key: ' ', collapsed: true, prefix: /^>$/, format: { 'code-block': false, list: false },
            handler: markdownShortcut('blockquote', true)
          },
          'markdown code': {
            key: ' ', collapsed: true, prefix: /^```$/, format: { 'code-block': false, list: false },
            handler: markdownShortcut('code-block', 'plain')
          }
        }
      }
    }
  });

  quill.root.setAttribute('autocapitalize', 'sentences');
  quill.root.addEventListener('click', function (event) {
    var node = event.target.closest && event.target.closest('.md-raw, .md-raw-inline');
    if (node) {
      event.preventDefault();
      editRaw(node);
    }
  });

  // ---------------------------------------------------------------------------
  // Change reporting
  // ---------------------------------------------------------------------------

  var lastMarkdown = '';
  var timer = null;

  function flush() {
    clearTimeout(timer);
    timer = null;
    var markdown = HugoerMarkdown.deltaToMarkdown(quill.getContents());
    if (markdown !== lastMarkdown) {
      lastMarkdown = markdown;
      post({ type: 'change', markdown: markdown });
    }
    return markdown;
  }

  quill.on('text-change', function (delta, oldDelta, source) {
    if (source !== 'user') return;
    clearTimeout(timer);
    timer = setTimeout(flush, 80);
  });
  quill.on('selection-change', function (range) {
    if (!range && timer) flush();
  });
  document.addEventListener('visibilitychange', function () {
    if (document.hidden && timer) flush();
  });
  window.addEventListener('pagehide', function () {
    if (timer) flush();
  });

  function setTheme(dark) {
    if (dark === true) document.documentElement.setAttribute('data-theme', 'dark');
    else if (dark === false) document.documentElement.setAttribute('data-theme', 'light');
    else document.documentElement.removeAttribute('data-theme');
  }

  window.HugoerEditor = {
    setContent: function (options) {
      options = options || {};
      context.baseURL = options.baseURL || '';
      context.documentPath = options.documentPath || '';
      if ('dark' in options) setTheme(options.dark);
      var markdown = String(options.markdown || '');
      clearTimeout(timer);
      timer = null;
      quill.setContents(HugoerMarkdown.markdownToDelta(markdown), 'silent');
      quill.history.clear();
      // Unedited documents are never re-serialised, so opening a post is lossless.
      lastMarkdown = markdown;
    },
    getMarkdown: flush,
    setTheme: setTheme,
    focus: function () { quill.focus(); }
  };

  window.quill = quill;
  post({ type: 'ready' });
})();
