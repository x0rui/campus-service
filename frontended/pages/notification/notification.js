const api = require('../../utils/api');

Page({
  data: {
    notifications: [],
    loading: true
  },

  onShow() {
    this.loadNotifications();
  },

  async loadNotifications() {
    this.setData({ loading: true });
    try {
      const list = await api.get('/api/notification/list?page=0');
      this.setData({ notifications: list || [], loading: false });
    } catch (e) {
      this.setData({ loading: false });
    }
  },

  async markRead(e) {
    const id = e.currentTarget.dataset.id;
    const type = e.currentTarget.dataset.type;
    const relatedId = e.currentTarget.dataset.relatedid;
    try {
      await api.post('/api/notification/read/' + id);
      const list = this.data.notifications.map(function(n) {
        if (n.id === id) { n.isRead = 1; }
        return n;
      });
      this.setData({ notifications: list });
      // 跳转到相关页面
      if (relatedId && relatedId > 0) {
        if (type === 1) {
          wx.navigateTo({ url: '/pages/task/detail/detail?id=' + relatedId });
        } else if (type === 2) {
          wx.navigateTo({ url: '/pages/team/detail/detail?id=' + relatedId });
        } else if (type === 3) {
          wx.navigateTo({ url: '/pages/club/detail/detail?id=' + relatedId });
        } else if (type === 4) {
          wx.navigateTo({ url: '/pages/goods/detail/detail?id=' + relatedId });
        } else if (type === 6) {
          // 学习资源：relatedId 是求资源需求的 ID，跳到匹配结果页
          wx.navigateTo({ url: '/pages/resource/demand-match/demand-match?id=' + relatedId });
        } else if (type === 7) {
          // 支付相关通知：跳到我的订单
          wx.navigateTo({ url: '/pages/order/list/list' });
        }
      }
    } catch (e) {}
  },

  async markAllRead() {
    try {
      await api.post('/api/notification/read-all');
      const list = this.data.notifications.map(function(n) {
        n.isRead = 1;
        return n;
      });
      this.setData({ notifications: list });
      wx.showToast({ title: '已全部已读', icon: 'success' });
    } catch (e) {}
  }
});
