const api = require('../../../utils/api');
Page({
  data: { list: [] },
  onShow() {
    this.loadData();
    // 记录已查看时间，小红点不再显示
    wx.setStorageSync('reportsViewTime', Date.now());
  },
  async loadData() {
    var self = this;
    api.get('/api/report/my').then(function(list) {
      list = (list || []).map(function(item) {
        try { item.evidenceList = JSON.parse(item.evidenceImages || '[]'); } catch(e) { item.evidenceList = []; }
        return item;
      });
      self.setData({ list: list });
    }).catch(function(){});
  },
  onPullDownRefresh() { this.loadData().then(function() { wx.stopPullDownRefresh(); }); },
  preview(e) {
    var url = e.currentTarget.dataset.url;
    wx.previewImage({ urls: [url], current: url });
  }
});
