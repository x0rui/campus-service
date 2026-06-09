const api = require('../../../utils/api');
const app = getApp();

Page({
  data: { teams: [], tab: 0 },

  onShow() { this.loadData(0); },

  async loadData(tab) {
    var url = tab === 0 ? '/api/team/my' : '/api/team/joined';
    try {
      var list = await api.get(url);
      if (tab === 0 && list && list.length > 0) {
        // 并行拉取所有组局的待审核数
        var tasks = list.map(function(item) {
          return api.get('/api/team/pending/' + item.teamId).then(function(p) {
            item._pendingCount = (p || []).length;
          }).catch(function() {
            item._pendingCount = 0;
          });
        });
        await Promise.all(tasks);
      }
      this.setData({ teams: list || [] });
    } catch (e) {}
  },

  switchTab(e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    this.setData({ tab: tab, teams: [] });
    this.loadData(tab);
  },
  onPullDownRefresh() { this.loadData(this.data.tab).then(function() { wx.stopPullDownRefresh(); }); },

  goToDetail(e) {
    wx.navigateTo({ url: '/pages/team/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
