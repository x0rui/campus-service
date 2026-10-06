const api = require('../../../utils/api');

const STATUS = ['待付款', '待发货', '待收货', '已完成', '已取消'];
const STATUS_CLASS = ['tag-warning', 'tag-primary', 'tag-success', 'tag-muted', 'tag-danger'];

Page({
  data: {
    tab: 0,
    all: [],
    list: [],
    loading: true,
    myId: null
  },

  onLoad() {
    const u = wx.getStorageSync('userInfo') || {};
    this.setData({ myId: u.userId });
  },

  onShow() {
    this.load();
  },

  async load() {
    this.setData({ loading: true });
    try {
      const all = ((await api.get('/api/pay/order-list')) || []).map(o => Object.assign({}, o, {
        statusText: STATUS[o.status] || '',
        statusClass: STATUS_CLASS[o.status] || 'tag-muted'
      }));
      this.setData({ all: all });
      this.split();
    } catch (e) {}
    this.setData({ loading: false });
  },

  // 按角色分成「我买的」和「我卖的」
  split() {
    const mine = this.data.all.filter(o => this.data.tab === 0
      ? o.buyerId === this.data.myId
      : o.sellerId === this.data.myId);
    this.setData({ list: mine });
  },

  switchTab(e) {
    this.setData({ tab: Number(e.currentTarget.dataset.tab) });
    this.split();
  },

  async pay(e) {
    const o = e.currentTarget.dataset.order;
    wx.navigateTo({
      url: '/pages/order/pay/pay?orderId=' + o.orderId +
           '&orderNo=' + o.orderNo + '&amount=' + o.amount
    });
  },

  async cancel(e) {
    const id = e.currentTarget.dataset.id;
    const self = this;
    wx.showModal({
      title: '取消订单', content: '确定取消这笔订单吗？',
      success(res) {
        if (!res.confirm) return;
        api.post('/api/pay/cancel/' + id, {}).then(function () {
          wx.showToast({ title: '已取消', icon: 'none' });
          self.load();
        }).catch(function () {});
      }
    });
  },

  async ship(e) {
    const self = this;
    api.post('/api/pay/ship/' + e.currentTarget.dataset.id, {}).then(function () {
      wx.showToast({ title: '已发货', icon: 'success' });
      self.load();
    }).catch(function () {});
  },

  async confirm(e) {
    const self = this;
    wx.showModal({
      title: '确认收货', content: '确认已经收到物品？',
      success(res) {
        if (!res.confirm) return;
        api.post('/api/pay/confirm/' + e.currentTarget.dataset.id, {}).then(function () {
          wx.showToast({ title: '交易完成', icon: 'success' });
          self.load();
        }).catch(function () {});
      }
    });
  },

  goGoods(e) {
    wx.navigateTo({ url: '/pages/goods/detail/detail?id=' + e.currentTarget.dataset.id });
  }
});
