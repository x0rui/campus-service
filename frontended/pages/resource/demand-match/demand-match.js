const api = require('../../../utils/api');

Page({
  data: {
    demand: null,
    resources: [],
    users: [],
    loading: true
  },

  onLoad(options) {
    this.id = options.id;
    this.load();
  },

  async load() {
    try {
      const res = await api.get('/api/resource-demand/match/' + this.id);
      this.setData({
        demand: res.demand || null,
        resources: res.resources || [],
        users: res.users || [],
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/resource/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  contact(e) {
    wx.navigateTo({ url: '/pages/chat/chat?otherId=' + e.currentTarget.dataset.id });
  },

  async markSolved() {
    try {
      await api.put('/api/resource-demand/status/' + this.id, { status: 1 });
      wx.showToast({ title: '已标记为解决', icon: 'none' });
      this.load();
    } catch (e) {}
  }
});
