const api = require('../../utils/api');
const app = getApp();

Page({
  data: {
    orderId: null,
    orderType: 0,
    targetId: null,
    targetUser: null,
    score: 0,
    content: '',
    isAnonymous: false,
    isTask: false,
    otherId: null,
    submitting: false
  },

  onLoad(options) {
    this.setData({
      orderId: options.orderId ? parseInt(options.orderId) : null,
      orderType: options.orderType ? parseInt(options.orderType) : 0,
      targetId: options.targetId ? parseInt(options.targetId) : null,
      isTask: options.isTask === '1',
      otherId: options.otherId || null
    });
    if (this.data.targetId) {
      this.loadTargetUser(this.data.targetId);
    }
  },

  async loadTargetUser(userId) {
    try {
      const user = await api.get('/api/user/simple/' + userId);
      this.setData({ targetUser: user });
    } catch (e) {}
  },

  setScore(e) {
    this.setData({ score: parseInt(e.currentTarget.dataset.score) });
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value });
  },

  toggleAnonymous() {
    this.setData({ isAnonymous: !this.data.isAnonymous });
  },

  async submit() {
    if (this.data.score < 1 || this.data.score > 5) {
      wx.showToast({ title: '请选择评分', icon: 'none' });
      return;
    }
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    try {
      await api.post('/api/evaluation/submit', {
        orderId: this.data.orderId,
        orderType: this.data.orderType,
        targetId: this.data.targetId,
        score: this.data.score,
        content: this.data.content,
        isAnonymous: this.data.isAnonymous ? 1 : 0
      });
      wx.showToast({ title: '评价成功', icon: 'success' });
      setTimeout(function() { wx.navigateBack(); }, 800);
    } catch (e) {
      this.setData({ submitting: false });
      wx.showToast({ title: e && e.message ? e.message : '提交失败，请重试', icon: 'none' });
    }
  },

  skip() {
    wx.navigateBack();
  }
});
