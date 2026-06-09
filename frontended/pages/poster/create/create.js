const api = require('../../../utils/api');

Page({
  data: {
    prompt: '', bgUrl: '', generating: false, saving: false,
    title: '精彩活动', location: ''
  },

  onLoad(options) {
    if (options.title) this.setData({ title: decodeURIComponent(options.title) });
    if (options.location) this.setData({ location: decodeURIComponent(options.location) });
  },

  onPromptInput(e) { this.setData({ prompt: e.detail.value }); },
  onTitleInput(e) { this.setData({ title: e.detail.value }); setTimeout(() => this.drawPoster(), 100); },
  onLocationInput(e) { this.setData({ location: e.detail.value }); setTimeout(() => this.drawPoster(), 100); },

  async generateBg() {
    if (!this.data.prompt.trim()) { wx.showToast({ title: '请输入风格描述', icon: 'none' }); return; }
    this.setData({ generating: true, bgUrl: '' });
    try {
      var url = await api.post('/api/ai/poster', { prompt: this.data.prompt });
      this.setData({ bgUrl: url });
      this.drawPoster();
    } catch(e) {
      console.error('海报生成失败:', e);
      wx.showToast({ title: '生成失败', icon: 'none' });
      this.setData({ bgUrl: '' });
      this.drawPoster();
    }
    this.setData({ generating: false });
  },

  onReady() { setTimeout(() => this.drawPoster(), 500); },

  async drawPoster() {
    var node = await this.getCanvasNode();
    if (!node) return;
    var ctx = node.getContext('2d');
    var w = node.width, h = node.height;
    ctx.clearRect(0, 0, w, h);

    if (this.data.bgUrl) {
      try {
        var img = node.createImage();
        img.src = this.data.bgUrl;
        await new Promise(function(ok, fail) { img.onload = ok; img.onerror = fail; });
        ctx.drawImage(img, 0, 0, w, h);
      } catch(e) { this.drawFallbackBg(ctx, w, h); }
    } else {
      this.drawFallbackBg(ctx, w, h);
    }

    var grad = ctx.createLinearGradient(0, h * 0.5, 0, h);
    grad.addColorStop(0, 'rgba(255,255,255,0)');
    grad.addColorStop(1, 'rgba(255,255,255,0.93)');
    ctx.fillStyle = grad;
    ctx.fillRect(0, h * 0.5, w, h * 0.5);

    var ly = h * 0.6;
    ctx.fillStyle = '#FF7E67';
    ctx.fillRect(w / 2 - 40, ly, 80, 5);

    ctx.fillStyle = '#2D3436';
    ctx.font = 'bold 28px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    var title = this.data.title || '精彩活动';
    if (ctx.measureText(title).width > w - 60) title = title.substring(0, 8) + '…';
    ctx.fillText(title, w / 2, ly + 20);

    if (this.data.location) {
      ctx.fillStyle = '#636E72';
      ctx.font = '18px sans-serif';
      ctx.fillText(this.data.location, w / 2, ly + 72);
    }

    ctx.fillStyle = '#B2BEC3';
    ctx.font = '13px sans-serif';
    ctx.textBaseline = 'bottom';
    ctx.fillText('校园多服务平台', w / 2, h - 16);
    ctx.textAlign = 'left';
  },

  drawFallbackBg(ctx, w, h) {
    var g = ctx.createLinearGradient(0, 0, w, h);
    g.addColorStop(0, '#FFB5A0'); g.addColorStop(0.5, '#FFDCC8'); g.addColorStop(1, '#FFE8D8');
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
    ctx.beginPath(); ctx.arc(w * 0.8, h * 0.15, 100, 0, 2 * Math.PI);
    ctx.fillStyle = 'rgba(255,255,255,0.15)'; ctx.fill();
    ctx.beginPath(); ctx.arc(w * 0.2, h * 0.85, 70, 0, 2 * Math.PI);
    ctx.fillStyle = 'rgba(255,255,255,0.1)'; ctx.fill();
  },

  getCanvasNode() {
    var p = this;
    return new Promise(function(resolve) {
      wx.createSelectorQuery().in(p).select('#posterCanvas')
        .fields({ node: true, size: true }).exec(function(res) {
          if (res[0]) { var n = res[0].node; n.width = 375; n.height = 667; resolve(n); }
          else resolve(null);
        });
    });
  },

  async saveToAlbum() {
    this.setData({ saving: true });
    try {
      var p = this;
      var node = await p.getCanvasNode();
      if (!node) { wx.showToast({ title: '画布未就绪', icon: 'none' }); this.setData({ saving: false }); return; }
      // 通过 Canvas.toDataURL 导出含文字叠加层的完整海报
      var tempFilePath = await new Promise(function(resolve, reject) {
        wx.canvasToTempFilePath({
          canvas: node, x: 0, y: 0, width: node.width, height: node.height, destWidth: 750, destHeight: 1334,
          success: function(r) { resolve(r.tempFilePath); },
          fail: reject
        });
      });
      var auth = await new Promise(function(r) { wx.getSetting({ success: function(s) { r(s.authSetting['scope.writePhotosAlbum']); } }); });
      if (!auth) await new Promise(function(r, j) { wx.authorize({ scope: 'scope.writePhotosAlbum', success: r, fail: j }); });
      await new Promise(function(r, j) { wx.saveImageToPhotosAlbum({ filePath: tempFilePath, success: r, fail: j }); });
      wx.showToast({ title: '已保存到相册', icon: 'success' });
    } catch(e) { wx.showToast({ title: '保存失败，请重试', icon: 'none' }); }
    this.setData({ saving: false });
  },

  async saveAndUse() {
    this.setData({ saving: true });
    try {
      if (!this.data.bgUrl) { wx.showToast({ title: '请先生成背景', icon: 'none' }); this.setData({ saving: false }); return; }
      wx.setStorageSync('posterResult', this.data.bgUrl);
      wx.showToast({ title: '海报已生成', icon: 'success' });
      setTimeout(function() { wx.navigateBack(); }, 1000);
    } catch(e) {
      console.error('保存海报失败:', e);
      wx.showToast({ title: '保存失败', icon: 'none' });
    }
    this.setData({ saving: false });
  }
});
