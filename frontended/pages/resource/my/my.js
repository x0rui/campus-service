const api = require('../../../utils/api');

const STATUS = ['待审核', '已通过', '已拒绝'];

Page({
  data: {
    tab: 0,
    mine: [],
    favs: [],
    loading: true
  },

  onShow() {
    this.loadAll();
  },

  async loadAll() {
    this.setData({ loading: true });
    try {
      const mine = (await api.get('/api/resource/my')) || [];
      this.setData({ mine: mine.map(r => Object.assign({}, r, { statusText: STATUS[r.status] || '' })) });
    } catch (e) {}
    try {
      this.setData({ favs: (await api.get('/api/resource/my-collect')) || [] });
    } catch (e) {}
    this.setData({ loading: false });
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab) });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/resource/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    const self = this;
    wx.showModal({
      title: '删除资料',
      content: '确定删除这条资料吗？',
      success(res) {
        if (!res.confirm) return;
        api.del('/api/resource/' + id).then(function () {
          wx.showToast({ title: '已删除', icon: 'none' });
          self.loadAll();
        }).catch(function () {});
      }
    });
  },

  goPublish() {
    wx.navigateTo({ url: '/pages/resource/publish/publish' });
  }
});
