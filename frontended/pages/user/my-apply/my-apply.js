const api = require('../../../utils/api');
Page({
  data: { list: [], loading: true },
  onShow() { this.load(); },
  onPullDownRefresh(){ var self=this; this.load().then(function(){ wx.stopPullDownRefresh(); }); },
  async load() {
    this.setData({ loading: true });
    try {
      var list = await api.get('/api/club/my');
      this.setData({ list: list || [] });
    } catch(e) {}
    this.setData({ loading: false });
  }
});
