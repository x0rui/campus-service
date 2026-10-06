const api = require('../../../utils/api');

const DOC_EXT = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'];
const IMG_EXT = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp'];
const MEDIA_EXT = ['mp4', 'mov', 'mp3', 'm4a'];

Page({
  data: {
    r: null,
    collected: false,
    loading: true,
    downloading: false
  },

  onLoad(options) {
    this.id = options.id;
    this.load();
  },

  async load() {
    try {
      const r = await api.get('/api/resource/detail/' + this.id);
      this.setData({ r: r, loading: false });
      this.loadCollect();
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async loadCollect() {
    try {
      const c = await api.get('/api/resource/is-collect/' + this.id);
      this.setData({ collected: !!c });
    } catch (e) {}
  },

  // 下载：先让后端记一次下载并返回文件地址，再按类型处理
  async download() {
    if (this.data.downloading) return;
    this.setData({ downloading: true });
    try {
      const r = await api.post('/api/resource/download/' + this.id, {});
      const url = r && r.fileUrl;
      if (!url) { wx.showToast({ title: '文件地址缺失', icon: 'none' }); return; }
      const ext = ((r.fileExt || '').toLowerCase()).replace('.', '');

      if (IMG_EXT.indexOf(ext) >= 0) {
        wx.previewImage({ urls: [url] });
      } else if (MEDIA_EXT.indexOf(ext) >= 0) {
        const isVideo = ext === 'mp4' || ext === 'mov';
        wx.previewMedia({ sources: [{ url: url, type: isVideo ? 'video' : 'audio' }] });
      } else {
        this.downloadThenChoose(url, ext);
      }
      this.setData({ 'r.downloadCount': (this.data.r.downloadCount || 0) + 1 });
    } catch (e) {}
    this.setData({ downloading: false });
  },

  // 下载到临时文件后，让用户选「打开预览」还是「保存到电脑」
  // 保存到电脑是 PC 端能力，会弹系统的保存对话框，目录由用户自己选（可以选到 D 盘）
  downloadThenChoose(url, ext) {
    wx.showLoading({ title: '下载中' });
    wx.downloadFile({
      url: url,
      success: (res) => {
        wx.hideLoading();
        const tmp = res.tempFilePath;
        const openDoc = () => {
          if (DOC_EXT.indexOf(ext) >= 0) {
            wx.openDocument({
              filePath: tmp,
              fileType: ext,
              showMenu: true,
              fail() { wx.showToast({ title: '无法打开该文件', icon: 'none' }); }
            });
          } else {
            wx.showToast({ title: '该类型不支持预览，请用「保存到电脑」', icon: 'none' });
          }
        };

        if (typeof wx.saveFileToDisk !== 'function') { openDoc(); return; }

        wx.showActionSheet({
          itemList: ['打开预览', '保存到电脑（自选目录）'],
          success: (r) => {
            if (r.tapIndex === 0) { openDoc(); return; }
            wx.saveFileToDisk({
              filePath: tmp,
              fail() { wx.showToast({ title: '已取消保存', icon: 'none' }); }
            });
          },
          fail: () => openDoc()
        });
      },
      fail() { wx.hideLoading(); wx.showToast({ title: '下载失败', icon: 'none' }); }
    });
  },

  async collect() {
    try {
      const now = await api.post('/api/resource/collect/' + this.id, {});
      this.setData({ collected: !!now });
      wx.showToast({ title: now ? '已收藏' : '已取消收藏', icon: 'none' });
    } catch (e) {}
  },

  evaluate() {
    wx.navigateTo({
      url: '/pages/evaluation/evaluation?orderId=' + this.id +
           '&orderType=4&targetId=' + (this.data.r ? this.data.r.userId : '')
    });
  },

  report() {
    wx.setStorageSync('reportContext', {
      type: 'resource',
      id: Number(this.id),
      title: this.data.r ? this.data.r.title : ''
    });
    wx.navigateTo({ url: '/pages/report/publish/publish' });
  }
});
