const api = require('../../../utils/api');
Page({
  data: {
    targetType: '', targetId: 0, targetTitle: '',
    reason: '', description: '', evidenceImages: [],
    submitting: false
  },
  onLoad() {
    // 从缓存读取举报上下文（避免 URL 编码乱码问题）
    var ctx = wx.getStorageSync('reportContext') || {};
    this.setData({
      targetType: ctx.type || '',
      targetId: ctx.id || 0,
      targetTitle: ctx.title || ''
    });
    wx.removeStorageSync('reportContext');
  },
  onReasonInput(e) { this.setData({ reason: e.detail.value }); },
  onDescInput(e) { this.setData({ description: e.detail.value }); },
  chooseImage() {
    var self = this;
    wx.chooseMedia({
      count: 6 - this.data.evidenceImages.length,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        var urls = self.data.evidenceImages.slice();
        if (res.tempFiles) {
          var promises = res.tempFiles.map(f => api.uploadFile(f.tempFilePath));
          Promise.all(promises).then(function(remoteUrls) {
            self.setData({ evidenceImages: urls.concat(remoteUrls) });
          }).catch(function() { wx.showToast({ title: '上传失败', icon: 'none' }); });
        }
      }
    });
  },
  removeImage(e) {
    var list = this.data.evidenceImages.slice();
    list.splice(e.currentTarget.dataset.index, 1);
    this.setData({ evidenceImages: list });
  },
  async submit() {
    if (!this.data.reason.trim()) { wx.showToast({ title: '请填写举报原因', icon: 'none' }); return; }
    if (!this.data.description.trim()) { wx.showToast({ title: '请填写举报描述', icon: 'none' }); return; }
    this.setData({ submitting: true });
    try {
      await api.post('/api/report/create', {
        targetType: this.data.targetType,
        targetId: this.data.targetId,
        targetTitle: this.data.targetTitle,
        reason: this.data.reason,
        description: this.data.description,
        evidenceImages: JSON.stringify(this.data.evidenceImages)
      });
      wx.showToast({ title: '举报提交成功', icon: 'success' });
      setTimeout(function() { wx.navigateBack(); }, 1000);
    } catch (e) { this.setData({ submitting: false }); }
  }
});
