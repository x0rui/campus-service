const api = require('../../../utils/api');

Page({
  data: {
    orderId: null,
    orderNo: '',
    amount: '0.00',
    channel: 'wechat',
    paying: false,
    paid: false
  },

  onLoad(options) {
    this.setData({
      orderId: options.orderId || null,
      orderNo: options.orderNo || '',
      amount: options.amount || '0.00'
    });
  },

  pickChannel(e) {
    this.setData({ channel: e.currentTarget.dataset.channel });
  },

  // 第二步：用户在自绘支付页选渠道，点确认
  // 第三步：调服务端的模拟回调接口，后端改完状态才算付款成功
  async pay() {
    if (this.data.paying || this.data.paid) return;
    this.setData({ paying: true });
    try {
      await api.post('/api/pay/notify', {
        orderNo: this.data.orderNo,
        channel: this.data.channel
      });
      this.setData({ paid: true });
      wx.showToast({ title: '支付成功', icon: 'success' });
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/order/list/list' });
      }, 1200);
    } catch (e) {}
    this.setData({ paying: false });
  },

  async cancel() {
    try {
      await api.post('/api/pay/cancel/' + this.data.orderId, {});
      wx.showToast({ title: '已取消订单', icon: 'none' });
      setTimeout(() => wx.redirectTo({ url: '/pages/order/list/list' }), 800);
    } catch (e) {}
  }
});
