const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    evaluations: [],
    currentTab: 0,
    userId: null
  },

  onLoad() {
    const userInfo = wx.getStorageSync('userInfo');
    if (!userInfo) {
      wx.navigateTo({ url: '/pages/login/login' });
      return;
    }
    this.setData({ userId: userInfo.userId });
    this.loadEvaluations(0);
  },

  async loadEvaluations(tab) {
    var url = tab === 0 ? '/api/evaluation/user/' : '/api/evaluation/given/';
    try {
      var list = await api.get(url + this.data.userId);
      var enriched = await this.enrichUsers(list || []);
      this.setData({ evaluations: enriched });
    } catch (e) {}
  },

  async enrichUsers(list) {
    var self = this;
    var tasks = (list || []).map(function(item) {
      var otherId = self.data.currentTab === 0 ? item.evaluatorId : item.targetId;
      return api.get('/api/user/simple/' + otherId).then(function(user) {
        item._otherName = user ? (user.nickName || '微信用户') : '微信用户';
        return item;
      }).catch(function() {
        item._otherName = '微信用户';
        return item;
      });
    });
    return Promise.all(tasks);
  },

  switchTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ currentTab: tab, evaluations: [] });
    this.loadEvaluations(tab);
  }
});
