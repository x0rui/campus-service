const api = require('../../../utils/api');
const app = getApp();

Page({
  data: {
    list: [],
    page: 0,
    hasMore: true,
    loading: false,
    tab: 0
  },

  onLoad() {
    this.load();
  },

  onShow() {
    if (this.data.tab === 0) {
      this.setData({ page: 0, hasMore: true, list: [] });
      this.load();
    }
  },

  async load() {
    if (this.data.loading || !this.data.hasMore) return;
    this.setData({ loading: true });
    const url = this.data.tab === 0 ? '/api/resource-demand/list' : '/api/resource-demand/my';
    try {
      const arr = (await api.get(url, { page: this.data.page })) || [];
      this.setData({
        list: this.data.page === 0 ? arr : this.data.list.concat(arr),
        page: this.data.page + 1,
        hasMore: this.data.tab === 0 && arr.length >= 10
      });
    } catch (e) {}
    this.setData({ loading: false });
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab), page: 0, hasMore: true, list: [] });
    this.load();
  },

  goMatch(e) {
    wx.navigateTo({ url: '/pages/resource/demand-match/demand-match?id=' + e.currentTarget.dataset.id });
  },

  goPublish() {
    if (!app.checkLogin()) return;
    wx.navigateTo({ url: '/pages/resource/demand-publish/demand-publish' });
  },

  onReachBottom() { this.load(); }
});
