const api = require('../../../utils/api');

Page({
  data: {
    list: [],
    recommend: [],
    loading: true,
    showRecommend: false
  },

  onShow() {
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      this.setData({ list: (await api.get('/api/question/wrong')) || [] });
    } catch (e) {}
    this.setData({ loading: false });
  },

  // 按薄弱知识点反向推荐资料：把题库和资料共享连起来
  async toggleRecommend() {
    if (this.data.showRecommend) {
      this.setData({ showRecommend: false });
      return;
    }
    if (this.data.recommend.length === 0) {
      try {
        this.setData({ recommend: (await api.get('/api/question/wrong/recommend')) || [] });
      } catch (e) {}
    }
    this.setData({ showRecommend: true });
  },

  goResource(e) {
    wx.navigateTo({ url: '/pages/resource/detail/detail?id=' + e.currentTarget.dataset.id });
  },

  remove(e) {
    const id = e.currentTarget.dataset.id;
    const self = this;
    wx.showModal({
      title: '移出错题本',
      content: '确定把这题移出错题本吗？',
      success(res) {
        if (!res.confirm) return;
        api.del('/api/question/wrong/' + id).then(function () {
          wx.showToast({ title: '已移出', icon: 'none' });
          self.load();
        }).catch(function () {});
      }
    });
  },

  goPractice() {
    wx.navigateTo({ url: '/pages/question/practice/practice' });
  }
});
